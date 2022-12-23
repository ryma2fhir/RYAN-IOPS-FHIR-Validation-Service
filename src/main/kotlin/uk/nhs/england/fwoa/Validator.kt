package uk.nhs.england.fwoa

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException
import ca.uhn.fhir.validation.FhirValidator
import ca.uhn.fhir.validation.SingleValidationMessage
import ca.uhn.fhir.validation.ValidationOptions
import ca.uhn.fhir.validation.ValidationResult
import com.google.common.collect.ImmutableList
import com.google.gson.JsonSyntaxException
import io.github.classgraph.ClassGraph
import io.github.classgraph.Resource
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureDefinition
import org.hl7.fhir.utilities.npm.NpmPackage
import uk.nhs.england.fwoa.service.CapabilityStatementApplier
import uk.nhs.england.fwoa.service.MessageDefinitionApplier
import uk.nhs.england.fwoa.shared.PrePopulatedValidationSupport
import uk.nhs.england.fwoa.util.createOperationOutcome
import java.io.ByteArrayInputStream
import java.time.Duration
import java.time.Instant
import java.util.function.Function
import java.util.stream.Collectors


class Validator(var fhirVersion: String, var implementationGuidesFolder: String?) {

    var ctx : FhirContext
    var fhirValidator : FhirValidator
    var capabilityStatementApplier: CapabilityStatementApplier
    var messageDefinitionApplier: MessageDefinitionApplier
    companion object : KLogging()

    init {
        if (fhirVersion != ValidatorConstants.FHIR_R4 && fhirVersion != ValidatorConstants.FHIR_STU3) {
            throw RuntimeException("Invalid FHIR version $fhirVersion")
        }
        fhirVersion = fhirVersion
        if (implementationGuidesFolder == null) implementationGuidesFolder =
            ValidatorConstants.DEFAULT_IMPLEMENTATION_GUIDES_FOLDER
        // To learn more about the different ways to configure FhirInstanceValidator see: https://hapifhir.io/hapi-fhir/docs/validation/validation_support_modules.html
        // To learn more about the different ways to configure FhirInstanceValidator see: https://hapifhir.io/hapi-fhir/docs/validation/validation_support_modules.html
        ctx =
            if (ValidatorConstants.FHIR_R4 == fhirVersion) FhirContext.forR4() else FhirContext.forDstu3()

        // Create a chain that will hold our modules

        // Create a chain that will hold our modules
        val supportChain = ValidationSupportChain()

        // DefaultProfileValidationSupport supplies base FHIR definitions. This is generally required
        // even if you are using custom profiles, since those profiles will derive from the base
        // definitions.

        // DefaultProfileValidationSupport supplies base FHIR definitions. This is generally required
        // even if you are using custom profiles, since those profiles will derive from the base
        // definitions.
        val defaultSupport = DefaultProfileValidationSupport(ctx)
        supportChain.addValidationSupport(defaultSupport)

        // This module supplies several code systems that are commonly used in validation

        // This module supplies several code systems that are commonly used in validation
        supportChain.addValidationSupport(CommonCodeSystemsTerminologyService(ctx))

        // This module implements terminology services for in-memory code validation

        // This module implements terminology services for in-memory code validation
        supportChain.addValidationSupport(InMemoryTerminologyServerValidationSupport(ctx))

        // Create a PrePopulatedValidationSupport which can be used to load custom definitions.

        loadIgs(supportChain)

        supportChain.addValidationSupport(SnapshotGeneratingValidationSupport(ctx))

        generateSnapshots(supportChain)

        // Create a validator using the FhirInstanceValidator module.

        // Create a validator using the FhirInstanceValidator module.
        val validatorModule = FhirInstanceValidator(supportChain)
        fhirValidator = ctx.newValidator().registerValidatorModule(validatorModule)
        // Validating a complex Patient yields better results. validating a trivial "empty" Patient won't load all the validation classes.
        val someSyntheaPatient =
            "{\"resourceType\":\"Patient\",\"id\":\"a8bc0c9f-47b3-ee31-60c6-fb8ce8077ac7\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">Generated by <a href=\\\"https://github.com/synthetichealth/synthea\\\">Synthea</a>.Version identifier: master-branch-latest-2-gfd2217b\\n .   Person seed: -5969330820059413579  Population seed: 1614314878171</div>\"},\"extension\":[{\"url\":\"http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName\",\"valueString\":\"Son314 Vandervort697\"},{\"url\":\"http://hl7.org/fhir/StructureDefinition/patient-birthPlace\",\"valueAddress\":{\"city\":\"New Bedford\",\"state\":\"Massachusetts\",\"country\":\"US\"}},{\"url\":\"http://synthetichealth.github.io/synthea/disability-adjusted-life-years\",\"valueDecimal\":1.1872597438165626},{\"url\":\"http://synthetichealth.github.io/synthea/quality-adjusted-life-years\",\"valueDecimal\":70.81274025618343}],\"identifier\":[{\"system\":\"https://github.com/synthetichealth/synthea\",\"value\":\"a8bc0c9f-47b3-ee31-60c6-fb8ce8077ac7\"},{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MR\",\"display\":\"Medical Record Number\"}],\"text\":\"Medical Record Number\"},\"system\":\"http://hospital.smarthealthit.org\",\"value\":\"a8bc0c9f-47b3-ee31-60c6-fb8ce8077ac7\"},{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"SS\",\"display\":\"Social Security Number\"}],\"text\":\"Social Security Number\"},\"system\":\"http://hl7.org/fhir/sid/us-ssn\",\"value\":\"999-49-6778\"},{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"DL\",\"display\":\"Driver's License\"}],\"text\":\"Driver's License\"},\"system\":\"urn:oid:2.16.840.1.113883.4.3.25\",\"value\":\"S99922723\"},{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PPN\",\"display\":\"Passport Number\"}],\"text\":\"Passport Number\"},\"system\":\"http://standardhealthrecord.org/fhir/StructureDefinition/passportNumber\",\"value\":\"X72123203X\"}],\"name\":[{\"use\":\"official\",\"family\":\"Beier427\",\"given\":[\"Minnie888\"],\"prefix\":[\"Mrs.\"]},{\"use\":\"maiden\",\"family\":\"Jaskolski867\",\"given\":[\"Minnie888\"],\"prefix\":[\"Mrs.\"]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"555-390-9260\",\"use\":\"home\"}],\"gender\":\"female\",\"birthDate\":\"1949-01-01\",\"address\":[{\"extension\":[{\"url\":\"http://hl7.org/fhir/StructureDefinition/geolocation\",\"extension\":[{\"url\":\"latitude\",\"valueDecimal\":41.83492774608349},{\"url\":\"longitude\",\"valueDecimal\":-70.58336455010793}]}],\"line\":[\"862 Sauer Station Suite 31\"],\"city\":\"Plymouth\",\"state\":\"Massachusetts\",\"country\":\"US\"}],\"maritalStatus\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-MaritalStatus\",\"code\":\"M\",\"display\":\"M\"}],\"text\":\"M\"},\"multipleBirthInteger\":3,\"communication\":[{\"language\":{\"coding\":[{\"system\":\"urn:ietf:bcp:47\",\"code\":\"en-US\",\"display\":\"English\"}],\"text\":\"English\"}}]}"
        fhirValidator.validateWithResult(someSyntheaPatient)
        capabilityStatementApplier = CapabilityStatementApplier(supportChain)
        messageDefinitionApplier = MessageDefinitionApplier(supportChain)
    }

    private fun loadIgs(supportChain: ValidationSupportChain) {

        ClassGraph().acceptPathsNonRecursive(implementationGuidesFolder).scan().use { scanResult ->
            scanResult.getResourcesWithExtension("tgz")
                .forEach {  res: Resource ->
                    val content = res.load()
                    println(res.path)
                    if (content != null) {
                        var packageContent = getPackages(content)
                        // May need to use the other version of PrePopulated validation support in the
                        supportChain.addValidationSupport(createPrePopulatedValidationSupport(packageContent))
                    }
                }
        }
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

                }
            }

        structureDefinitions
            .filter { shouldGenerateSnapshot(it) }
            .forEach {
                try {
                    supportChain.generateSnapshot(context, it, it.url, "https://fhir.nhs.uk/R4", it.name)
                } catch (e: Exception) {

                }
            }
    }

    private fun shouldGenerateSnapshot(structureDefinition: StructureDefinition): Boolean {
        return !structureDefinition.hasSnapshot() && structureDefinition.derivation == StructureDefinition.TypeDerivationRule.CONSTRAINT
    }

    private fun circularReferenceCheck(structureDefinition: StructureDefinition, supportChain: IValidationSupport): StructureDefinition {

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
                                it.id.contains("Encounter.reasonReference")
                        )
                && it.hasType()) {

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

    private fun toValidatorResponse(result: ValidationResult): ValidatorResponse {
        var validatorResponse = ValidatorResponse(isSuccessful = result.isSuccessful)
        validatorResponse.errorMessages =
            result.messages.stream()
                .map(Function<SingleValidationMessage, Any> { singleValidationMessage: SingleValidationMessage ->
                    var errorMessage = ValidatorErrorMessage()
                    errorMessage.severity = singleValidationMessage.severity.code
                    errorMessage.msg = singleValidationMessage.locationString + " - " + singleValidationMessage.message
                    errorMessage

                }).collect(Collectors.toList()) as List<ValidatorErrorMessage>?

        return validatorResponse
    }

    fun validate(strResource: String?) : ValidatorResponse{

        return try {
            val resource = ctx.newJsonParser().parseResource(strResource)
            capabilityStatementApplier.applyCapabilityStatementProfiles(resource)
            //if (resource.meta != null && resource.meta.profile.size>0) println(resource.meta.profile[0])
            val result: ValidationResult = fhirValidator.validateWithResult(resource)
            toValidatorResponse(result)
        }
        catch (e: DataFormatException) {

            var validatorResponse = ValidatorResponse(isSuccessful = false)
            val msg = ValidatorErrorMessage()
            msg.msg ="Invalid JSON"
            msg.severity = "error"
            validatorResponse.errorMessages =
                ImmutableList.of(
                    msg
                )
            validatorResponse
        }
        catch (e: JsonSyntaxException) {

            var validatorResponse = ValidatorResponse(isSuccessful = false)
            val msg = ValidatorErrorMessage()
            msg.msg ="Invalid JSON"
            msg.severity = "error"
            validatorResponse.errorMessages =
                    ImmutableList.of(
                        msg
                    )
            validatorResponse
        } catch (e: NullPointerException) {
            var validatorResponse = ValidatorResponse(isSuccessful = false)
            val msg = ValidatorErrorMessage()
            msg.msg ="Invalid JSON"
            msg.severity = "error"
            validatorResponse.errorMessages =
                ImmutableList.of(
                    msg
                )
            validatorResponse
        } catch (e: IllegalArgumentException) {
            var validatorResponse = ValidatorResponse(isSuccessful = false)
            val msg = ValidatorErrorMessage()
            msg.msg ="Invalid JSON"
            msg.severity = "error"
            validatorResponse.errorMessages =
                ImmutableList.of(
                    msg
                )
            validatorResponse
        } catch (e: InvalidRequestException) {
            var validatorResponse = ValidatorResponse(isSuccessful = false)
            val msg = ValidatorErrorMessage()
            msg.msg ="Invalid JSON"
            msg.severity = "error"
            validatorResponse.errorMessages =
                ImmutableList.of(
                    msg
                )
            validatorResponse
        }
    }
    fun validate(resource : IBaseResource, profile: String?) : OperationOutcome? {

        return try {
            val resources = getResourcesToValidate(resource)
            val operationOutcomeList = resources.map { validateResource(it, profile) }
            val operationOutcomeIssues = operationOutcomeList.filterNotNull().flatMap { it.issue }
            return createOperationOutcome(operationOutcomeIssues)
        } catch (e: DataFormatException) {
            logger.error("Caught parser error", e)
            createOperationOutcome(e.message ?: "Invalid JSON", null)
        }
    }

    fun createPrePopulatedValidationSupport(npmPackage: NpmPackage): PrePopulatedValidationSupport {
        val prePopulatedSupport =
            PrePopulatedValidationSupport(ctx)
        getResourcesFromPackage(npmPackage).forEach(prePopulatedSupport::addResource)
        return prePopulatedSupport
    }
    fun getResourcesFromPackage(npmPackage: NpmPackage): List<IBaseResource> {
        return getResourcesFromFolder(npmPackage, "package")
            .plus(getResourcesFromFolder(npmPackage, "examples"))
    }
    fun getResourcesFromFolder(npmPackage: NpmPackage, folderName: String): List<IBaseResource> {
        val jsonParser = ctx.newJsonParser()
        val list = npmPackage.list(folderName).map {
            //println(cnt.toString() + " " + it)
            //cnt++
            npmPackage.load(folderName, it)
        }

        return list
            .map {
                //  println(cnt)
                //  cnt++
                jsonParser.parseResource(it)
            }
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

    fun getPackages(content : ByteArray) : NpmPackage  {
        return NpmPackage.fromPackage(ByteArrayInputStream(content))
    }

    fun validateResource(resource: IBaseResource, profile: String?): OperationOutcome? {
        if (profile != null) return fhirValidator.validateWithResult(resource, ValidationOptions().addProfile(profile))
            .toOperationOutcome() as? OperationOutcome
        capabilityStatementApplier.applyCapabilityStatementProfiles(resource)
        val messageDefinitionErrors = messageDefinitionApplier.applyMessageDefinition(resource)
        if (messageDefinitionErrors != null) {
            return messageDefinitionErrors
        }
        return fhirValidator.validateWithResult(resource).toOperationOutcome() as? OperationOutcome
    }
    fun getResourcesToValidate(inputResource: IBaseResource?): List<IBaseResource> {
        if (inputResource == null) {
            return emptyList()
        }

        if (inputResource is Bundle
            && inputResource.type == Bundle.BundleType.SEARCHSET) {
            val bundleEntries = inputResource.entry
                .map { it }
            val bundleResources = bundleEntries.map { it.resource }
            if (bundleResources.all { it.resourceType == ResourceType.Bundle }) {
                return bundleResources
            }
        }

        return listOf(inputResource)
    }
}
