package uk.nhs.england.fhirvalidator.awsProvider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.rest.param.TokenParam
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.BaseValidationSupport
import org.hl7.fhir.instance.model.api.IBaseResource

class AWSValidationSupport(theFhirContext: FhirContext?, _awsQuestionnaire: AWSQuestionnaire?) : BaseValidationSupport(theFhirContext), IValidationSupport {

    private var awsQuestionnaire: AWSQuestionnaire? = null
    companion object : KLogging()

   init {
       awsQuestionnaire = _awsQuestionnaire
   }



    override fun fetchAllConformanceResources(): List<IBaseResource>? {
        val retVal = ArrayList<IBaseResource>()
        try {
            val questionnaires = awsQuestionnaire!!.search(null)
            for (questionnaire in questionnaires) {
                retVal.add(questionnaire)
            }
        } catch (ex : Exception) {
            logger.error(ex.message)
            return retVal;
        }
        return retVal
    }

    override fun <T : IBaseResource?> fetchResource(theClass: Class<T>?, theUri: String?): T? {
        try {
            val foundQuestionnaire = awsQuestionnaire!!.search(TokenParam().setValue(theUri))

            return if (foundQuestionnaire.size > 0) foundQuestionnaire[0] as T else null
        }
        catch (ex : Exception) {
            logger.error(ex.message)
            return null
        }
    }


}
