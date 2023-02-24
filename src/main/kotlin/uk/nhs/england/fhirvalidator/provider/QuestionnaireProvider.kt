package uk.nhs.england.fhirvalidator.provider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.server.RequestDetails
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.awsProvider.AWSQuestionnaire
import uk.nhs.england.fhirvalidator.interceptor.CognitoAuthInterceptor
import uk.nhs.england.fhirvalidator.service.CodingSupport
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest

@Component
class QuestionnaireProvider (@Qualifier("R4") private val fhirContext: FhirContext,
                             private val cognitoAuthInterceptor: CognitoAuthInterceptor,
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
    fun search(@OptionalParam(name = Questionnaire.SP_URL) url: TokenParam?,
               @OptionalParam(name = Questionnaire.SP_IDENTIFIER) identifier: TokenParam?): List<Questionnaire> {
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

    @Read
    fun read(httpRequest : HttpServletRequest, @IdParam internalId: IdType): Questionnaire? {
        val resource: Resource? = cognitoAuthInterceptor.readFromUrl(httpRequest.pathInfo, null)
        return if (resource is Questionnaire) resource else null
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
        if (questionnaire.hasCode() && !questionnaire.hasIdentifier()) {
            questionnaire.identifier.add(Identifier()
                .setSystem(questionnaire.codeFirstRep.system)
                .setValue(questionnaire.codeFirstRep.code))
        }
        if (questionnaire.hasCode() && questionnaire.codeFirstRep.hasCode() && !questionnaire.hasUrl()) {
            questionnaire.url = "https://example.fhir.nhs.uk/Questionnaire/"+questionnaire.codeFirstRep.code
        }
        if (!questionnaire.hasUrl()) throw UnprocessableEntityException("A Questionnaire.code or Questionnaire.url is required")
        val duplicateCheck = search(TokenParam().setValue(questionnaire.url),null)
        if (duplicateCheck.size>0) throw UnprocessableEntityException("A Questionnaire with this definition alrady exists.")
        return awsQuestionnaire.create(questionnaire)
    }
}
