package uk.nhs.england.fhirvalidator.awsProvider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.rest.param.TokenParam
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.BaseValidationSupport
import org.hl7.fhir.instance.model.api.IBaseResource

class AWSValidationSupport(theFhirContext: FhirContext?, _awsQuestionnaire: AWSQuestionnaire?,
    _awsCodeSystem: AWSCodeSystem,
    _awsValueSet: AWSValueSet) : BaseValidationSupport(theFhirContext), IValidationSupport {

    private var awsQuestionnaire: AWSQuestionnaire? = null
    private var awsValueSet: AWSValueSet? = null
    private var awsCodeSystem: AWSCodeSystem? = null
    companion object : KLogging()

   init {
       awsQuestionnaire = _awsQuestionnaire
       awsCodeSystem = _awsCodeSystem
       awsValueSet = _awsValueSet
   }



    override fun fetchAllConformanceResources(): List<IBaseResource>? {
        val retVal = ArrayList<IBaseResource>()
        try {
            val questionnaires = awsQuestionnaire!!.search(null)
            for (questionnaire in questionnaires) {
                retVal.add(questionnaire)
            }
            val codeSystems = awsCodeSystem!!.search(null)
            for (codeSystem in codeSystems) retVal.add(codeSystem)
            val valueSets = awsValueSet!!.search(null)
            for (valueSet in valueSets) retVal.add(valueSet)
        } catch (ex : Exception) {
            logger.error(ex.message)
            return retVal;
        }
        return retVal
    }

    override fun <T : IBaseResource?> fetchResource(theClass: Class<T>?, theUri: String?): T? {
        try {
            if (theClass != null) {
                if (theClass.simpleName.equals("Questionnaire")) {
                    val foundQuestionnaire = awsQuestionnaire!!.search(TokenParam().setValue(theUri))
                    return if (foundQuestionnaire.size > 0) foundQuestionnaire[0] as T else null
                }
                if (theClass.simpleName.equals("CodeSystem")) {
                    val found = awsCodeSystem!!.search(TokenParam().setValue(theUri))
                    return if (found.size > 0) found[0] as T else null
                }
                if (theClass.simpleName.equals("ValueSet")) {
                    val found = awsValueSet!!.search(TokenParam().setValue(theUri))
                    return if (found.size > 0) found[0] as T else null
                }
            }
        }
        catch (ex : Exception) {
            logger.error(ex.message)
            return null
        }
        return null
    }

    override fun fetchCodeSystem(theSystem: String?): IBaseResource? {
        var results = awsCodeSystem?.search(TokenParam().setValue(theSystem))
        if (results != null && results.size>0) {
            return results.get(0)
        } else return null
    }

    override fun fetchValueSet(theUri: String?): IBaseResource? {
        var results = awsValueSet?.search(TokenParam().setValue(theUri))
        if (results != null && results.size>0) {
            return results.get(0)
        } else return null
    }

    override fun isCodeSystemSupported(
        theValidationSupportContext: ValidationSupportContext?,
        theSystem: String?
    ): Boolean {
        var results = awsCodeSystem?.search(TokenParam().setValue(theSystem))
        if (results != null) {
            return (results.size >0)
        } else return false
    }

    override fun isValueSetSupported(
        theValidationSupportContext: ValidationSupportContext?,
        theValueSetUrl: String?
    ): Boolean {
        var results = awsValueSet?.search(TokenParam().setValue(theValueSetUrl))
        if (results != null) {
            return (results.size >0)
        } else return false
    }


}
