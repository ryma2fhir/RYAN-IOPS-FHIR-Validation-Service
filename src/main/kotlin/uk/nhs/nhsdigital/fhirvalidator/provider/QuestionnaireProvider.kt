package uk.nhs.nhsdigital.fhirvalidator.provider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.nhsdigital.fhirvalidator.service.CodingSupport
import uk.nhs.nhsdigital.fhirvalidator.service.ImplementationGuideParser
import java.nio.charset.StandardCharsets

@Component
class QuestionnaireProvider (@Qualifier("R4") private val fhirContext: FhirContext,
                             private val supportChain: ValidationSupportChain
) : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<Questionnaire> {
        return Questionnaire::class.java
    }

    var implementationGuideParser: ImplementationGuideParser? = ImplementationGuideParser(fhirContext)

    companion object : KLogging()

    @Search
    fun search(@RequiredParam(name = Questionnaire.SP_URL) url: TokenParam): List<Questionnaire> {
        val list = mutableListOf<Questionnaire>()
        val resource = supportChain.fetchResource(Questionnaire::class.java,java.net.URLDecoder.decode(url.value, StandardCharsets.UTF_8.name()))
        if (resource != null) list.add(resource)

        return list
    }
}
