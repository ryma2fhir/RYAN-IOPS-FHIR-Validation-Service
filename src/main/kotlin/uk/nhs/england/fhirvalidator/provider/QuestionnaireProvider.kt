package uk.nhs.england.fhirvalidator.provider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.server.RequestDetails
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.awsProvider.AWSQuestionnaire
import uk.nhs.england.fhirvalidator.service.CodingSupport
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest

@Component
class QuestionnaireProvider (@Qualifier("R4") private val fhirContext: FhirContext,
                             private val supportChain: ValidationSupportChain,
    private val awsQuestionnaire: AWSQuestionnaire
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
    fun search(@OptionalParam(name = Questionnaire.SP_URL) url: TokenParam?): List<Questionnaire> {
        val list = mutableListOf<Questionnaire>()
        return awsQuestionnaire.search(url)
    }

    @Delete
    fun delete(
        theRequest: HttpServletRequest,
        @IdParam theId: IdType,
        theRequestDetails: RequestDetails?
    ): MethodOutcome? {
        return awsQuestionnaire.delete(theId)
    }
    @Update
    fun update(
        theRequest: HttpServletRequest,
        @ResourceParam questionnaire: Questionnaire,
        @IdParam theId: IdType,
        theRequestDetails: RequestDetails?
    ): MethodOutcome? {
        return awsQuestionnaire.update(questionnaire, theId)
    }
    @Create
    fun create(theRequest: HttpServletRequest, @ResourceParam questionnaire: Questionnaire): MethodOutcome? {
        return awsQuestionnaire.create(questionnaire)
    }
}
