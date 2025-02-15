package uk.nhs.england.fhirvalidator.configuration

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import ca.uhn.fhir.validation.FhirValidator
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.*
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.hl7.fhir.r4.model.StructureDefinition
import org.hl7.fhir.utilities.json.model.JsonProperty
import org.hl7.fhir.utilities.npm.NpmPackage
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import uk.nhs.england.fhirvalidator.awsProvider.*
import uk.nhs.england.fhirvalidator.model.SimplifierPackage
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser
import uk.nhs.england.fhirvalidator.util.AccessTokenInterceptor
import uk.nhs.england.fhirvalidator.validationSupport.SwitchedTerminologyServiceValidationSupport
import uk.nhs.england.fhirvalidator.validationSupport.UnsupportedCodeSystemWarningValidationSupport
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.function.Predicate


@Configuration
open class ValidationConfiguration(
    private val implementationGuideParser: ImplementationGuideParser,
    private val terminologyValidationProperties: TerminologyValidationProperties,
    val messageProperties: MessageProperties,
    val fhirServerProperties: FHIRServerProperties,
    val objectMapper: ObjectMapper
) {
    companion object : KLogging()

    var npmPackages: List<NpmPackage>? = null

    @Bean
    open fun validator(@Qualifier("R4") fhirContext: FhirContext, instanceValidator: FhirInstanceValidator): FhirValidator {
        return fhirContext.newValidator().registerValidatorModule(instanceValidator)
    }

    @Bean
    open fun instanceValidator(supportChain: ValidationSupportChain): FhirInstanceValidator {
       return FhirInstanceValidator(uk.nhs.england.fhirvalidator.shared.NHSDCachingValidationSupport(supportChain))
       // return FhirInstanceValidator(supportChain)
    }

    @Bean open fun validationSupportContext(supportChain: ValidationSupportChain): ValidationSupportContext {
        return ValidationSupportContext(supportChain)
    }


    @Bean("SupportChain")
    open fun validationSupportChain(
        @Qualifier("R4") fhirContext: FhirContext,
        switchedTerminologyServiceValidationSupport: SwitchedTerminologyServiceValidationSupport,
        awsQuestionnaire: AWSQuestionnaire,
        awsCodeSystem: AWSCodeSystem,
        awsValueSet: AWSValueSet,
        awsAuditEvent: AWSAuditEvent,
        awsConceptMap: AWSConceptMap
    ): ValidationSupportChain {
        val supportChain = ValidationSupportChain(
            DefaultProfileValidationSupport(fhirContext),
            SnapshotGeneratingValidationSupport(fhirContext),
            CommonCodeSystemsTerminologyService(fhirContext),
            switchedTerminologyServiceValidationSupport
        )
        if (messageProperties.getAWSValidationSupport()) supportChain.addValidationSupport( AWSValidationSupport(fhirContext, awsQuestionnaire,awsCodeSystem,awsValueSet, awsConceptMap))
        getPackages()
        if (npmPackages != null) {
            npmPackages!!
                .filter { !it.name().equals("hl7.fhir.r4.examples") }
                .map(implementationGuideParser::createPrePopulatedValidationSupport)

                .forEach(supportChain::addValidationSupport)

            //Initialise now instead of when the first message arrives
            generateSnapshots(supportChain)
            supportChain.fetchCodeSystem("http://snomed.info/sct")
            // packages have been processed so remove them
            npmPackages = emptyList()
            return supportChain
        } else {
            throw UnprocessableEntityException("Unable to process npm package configuration")
        }
    }

    @Bean
    open fun switchedTerminologyServiceValidationSupport(
        @Qualifier("R4") fhirContext: FhirContext,
        optionalRemoteTerminologySupport: Optional<uk.nhs.england.fhirvalidator.shared.RemoteTerminologyServiceValidationSupport>
    ): SwitchedTerminologyServiceValidationSupport {
        val snomedValidationSupport = if (optionalRemoteTerminologySupport.isPresent) {
            uk.nhs.england.fhirvalidator.shared.NHSDCachingValidationSupport(optionalRemoteTerminologySupport.get())
            // Disabled default caching as it was causing invalid results (on snomed display terms)
        } else {
            UnsupportedCodeSystemWarningValidationSupport(fhirContext)
        }

        return SwitchedTerminologyServiceValidationSupport(
            fhirContext,
            InMemoryTerminologyServerValidationSupport(fhirContext),
            snomedValidationSupport,
            Predicate { it.startsWith("http://snomed.info/sct")
                    || it.startsWith("https://dmd.nhs.uk")
                    || it.startsWith("http://read.info")
                    || it.startsWith("http://hl7.org/fhir/sid/icd")
            }
        )
    }

    @Bean
    @ConditionalOnProperty("terminology.url")
    open fun remoteTerminologyServiceValidationSupport(
        @Qualifier("R4") fhirContext: FhirContext,
        optionalAuthorizedClientManager: Optional<OAuth2AuthorizedClientManager>
    ): uk.nhs.england.fhirvalidator.shared.RemoteTerminologyServiceValidationSupport {
        logger.info("Using remote terminology server at ${terminologyValidationProperties.url}")
        val validationSupport =
            uk.nhs.england.fhirvalidator.shared.RemoteTerminologyServiceValidationSupport(
                fhirContext
            )
        validationSupport.setBaseUrl(terminologyValidationProperties.url)

        if (optionalAuthorizedClientManager.isPresent) {
            val authorizedClientManager = optionalAuthorizedClientManager.get()
            val accessTokenInterceptor = AccessTokenInterceptor(authorizedClientManager)
            validationSupport.addClientInterceptor(accessTokenInterceptor)
        }

        return validationSupport
    }

        fun generateSnapshots(supportChain: IValidationSupport) {
            val structureDefinitions = supportChain.fetchAllStructureDefinitions<StructureDefinition>() ?: return
            val context = ValidationSupportContext(supportChain)
            structureDefinitions
                .filter { shouldGenerateSnapshot(it) }
                .forEach {
                    try {
                        circularReferenceCheck(it,supportChain)
                    } catch (e: Exception) {
                        logger.error("Failed to generate snapshot for $it", e)
                    }
                }

            structureDefinitions
                .filter { shouldGenerateSnapshot(it) }
                .forEach {
                    try {
                        val start: Instant = Instant.now()
                        supportChain.generateSnapshot(context, it, it.url, "https://fhir.nhs.uk/R4", it.name)
                        val end: Instant = Instant.now()
                        val duration: Duration = Duration.between(start, end)
                        logger.info(duration.toMillis().toString() + " ms $it")
                    } catch (e: Exception) {
                        logger.error("Failed to generate snapshot for $it", e)
                    }
                }
        }

    private fun circularReferenceCheck(structureDefinition: StructureDefinition, supportChain: IValidationSupport): StructureDefinition {
        if (structureDefinition.hasSnapshot()) logger.error(structureDefinition.url + " has snapshot!!")
        structureDefinition.differential.element.forEach{
            //   ||
            if ((
                        it.id.endsWith(".partOf") ||
                        it.id.endsWith(".basedOn") ||
                        it.id.endsWith(".replaces") ||
                        it.id.contains("Condition.stage.assessment") ||
                        it.id.contains("Observation.derivedFrom") ||
                                it.id.contains("Observation.hasMember") ||
                                it.id.contains("CareTeam.encounter") ||
                        it.id.contains("CareTeam.reasonReference") ||
                        it.id.contains("ServiceRequest.encounter") ||
                        it.id.contains("ServiceRequest.reasonReference") ||
                        it.id.contains("EpisodeOfCare.diagnosis.condition") ||
                        it.id.contains("Encounter.diagnosis.condition") ||
                        it.id.contains("Encounter.reasonReference") ||
                                it.id.contains("Encounter.appointment")
                                )
                && it.hasType()) {
                logger.warn(structureDefinition.url + " has circular references ("+ it.id + ")")
                it.type.forEach{
                    if (it.hasTargetProfile())
                        it.targetProfile.forEach {
                            it.value = getBase(it.value, supportChain);
                        }
                }
            }
        }
        return structureDefinition
    }


    open fun getPackages() {
        var manifest : Array<SimplifierPackage>? = null
        if (fhirServerProperties.ig != null   ) {
            manifest = arrayOf(SimplifierPackage(fhirServerProperties.ig!!.name, fhirServerProperties.ig!!.version))
        } else {
            val configurationInputStream = ClassPathResource("manifest.json").inputStream
            manifest = objectMapper.readValue(configurationInputStream, Array<SimplifierPackage>::class.java)
        }
        val packages = arrayListOf<NpmPackage>()
        if (manifest == null) throw UnprocessableEntityException("Error processing IG manifest")
        for (packageNpm in manifest ) {
            val packageName = packageNpm.packageName + "-" + packageNpm.version+ ".tgz"

            var inputStream: InputStream? = null
            try {
                inputStream = ClassPathResource(packageName).inputStream
            } catch (ex : Exception) {
                if (ex.message != null) logger.info(ex.message)
            }
            if (inputStream == null) {
                val downloadedPackages = downloadPackage(packageNpm.packageName,packageNpm.version)
                packages.addAll(downloadedPackages)
            } else {
                logger.info("Using local cache for {} - {}",packageNpm.packageName, packageNpm.version)
                packages.add(NpmPackage.fromPackage(inputStream))
            }
        }
        this.npmPackages = packages
        /*
        if (fhirServerProperties.ig != null && !fhirServerProperties.ig!!.isEmpty()) {
            return downloadPackage(fhirServerProperties.ig!!)
        }
        return Arrays.stream(packages)
            .map { "${it.packageName}-${it.version}.tgz" }
            .map { ClassPathResource(it).inputStream }
            .map { NpmPackage.fromPackage(it) }
            .toList()

         */
    }

    open fun downloadPackage(name : String, version : String) : List<NpmPackage> {
        logger.info("Downloading from AWS Cache {} - {}",name, version)
        // Try self first
        var inputStream : InputStream? = null;
        try {
            val packUrl =  "https://fhir.nhs.uk/ImplementationGuide/" + name+"-" + version
            inputStream = readFromUrl(messageProperties.getNPMFhirServer() + "/FHIR/R4/ImplementationGuide/\$package?url="+packUrl )
            logger.info("Found Package on AWS Cache {} - {}",name,version)
        } catch (ex : Exception) {
            logger.warn("Package not found in AWS Cache trying simplifier {} - {}",name,version)
            if (ex.message!=null) logger.info(ex.message)
            try {
                inputStream = readFromUrl("https://packages.simplifier.net/" + name + "/" + version)
                logger.info("Found Package on Simplifier {} - {}",name,version)
            } catch (exSimplifier: Exception) {
                logger.error("Package not found on simplifier {} - {}",name, version)
            }
        }
        if (inputStream == null) logger.error("Failed to download  {} - {}",name, version)
        val packages = arrayListOf<NpmPackage>()
        val npmPackage = NpmPackage.fromPackage(inputStream)

        val dependency= npmPackage.npm.get("dependencies")

        if (dependency !== null) {
            if (dependency.isJsonArray) logger.info("isJsonArray")
            if (dependency.isJsonObject) {
                val obj = dependency.asJsonObject()
                obj.properties
                val entrySet: MutableList<JsonProperty>? = obj.properties
                entrySet?.forEach()
                {
                    logger.info(it.name + " version =  " + it.value)
                    if (it.name != "hl7.fhir.r4.core") {
                        val entryVersion = it.value?.asString()?.replace("\"","")
                        if (it.name != null && entryVersion != null) {
                            val packs = downloadPackage(it.name!!, entryVersion)
                            if (packs.size > 0) {
                                for (pack in packs) {
                                    packages.add(pack)
                                }
                            }
                        }
                    }
                }
            }
            if (dependency.isJsonNull) logger.info("isNull")
            if (dependency.isJsonPrimitive) logger.info("isJsonPrimitive")
        } else {
            logger.info("No dependencies found for {} - {}",name,version)
        }

        packages.add(npmPackage)


        return packages
    }

    fun readFromUrl(url: String): InputStream {

        val myUrl =  URL(url)

        var retry = 2
        while (retry > 0) {
            val conn = myUrl.openConnection() as HttpURLConnection


            conn.requestMethod = "GET"

            try {
                conn.connect()
                return conn.inputStream
            } catch (ex: FileNotFoundException) {
                retry--
                if (retry < 1) throw UnprocessableEntityException(ex.message)
            } catch (ex: IOException) {
                retry--
                if (retry < 1) throw UnprocessableEntityException(ex.message)

            }
        }
        throw UnprocessableEntityException("Number of retries exhausted")
    }

    private fun getBase(profile : String,supportChain: IValidationSupport): String? {
        val structureDefinition : StructureDefinition=
            supportChain.fetchStructureDefinition(profile) as StructureDefinition;
        if (structureDefinition.hasBaseDefinition()) {
            var baseProfile = structureDefinition.baseDefinition
            if (baseProfile.contains(".uk")) baseProfile = getBase(baseProfile, supportChain)
            return baseProfile
        }
        return null;
    }
    private fun shouldGenerateSnapshot(structureDefinition: StructureDefinition): Boolean {
        return !structureDefinition.hasSnapshot() && structureDefinition.derivation == StructureDefinition.TypeDerivationRule.CONSTRAINT
    }
}
