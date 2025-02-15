package uk.nhs.england.fhirvalidator.provider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.ConceptValidationOptions
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.IValidationSupport.CodeValidationResult
import ca.uhn.fhir.context.support.IValidationSupport.ValueSetExpansionOutcome
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.context.support.ValueSetExpansionOptions
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.server.RequestDetails
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.StringOrListParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import mu.KLogging
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.utilities.npm.NpmPackage
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.awsProvider.AWSValueSet
import uk.nhs.england.fhirvalidator.interceptor.CognitoAuthInterceptor
import uk.nhs.england.fhirvalidator.service.CodingSupport
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser
import uk.nhs.england.fhirvalidator.util.FhirSystems
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@Component
class ValueSetProvider (@Qualifier("R4") private val fhirContext: FhirContext,
                        private val supportChain: ValidationSupportChain,
                        private val codingSupport: CodingSupport,
                        private val awsValueSet: AWSValueSet,
                        private val cognitoAuthInterceptor: CognitoAuthInterceptor
) : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<ValueSet> {
        return ValueSet::class.java
    }
    private val validationSupportContext = ValidationSupportContext(supportChain)

    companion object : KLogging()

    @Update
    fun update(
        theRequest: HttpServletRequest,
        @ResourceParam valueSet: ValueSet,
        @IdParam theId: IdType,
        theRequestDetails: RequestDetails?
    ): MethodOutcome? {
        return awsValueSet.update(valueSet, theId)
    }
    @Create
    fun create(theRequest: HttpServletRequest, @ResourceParam valueSet: ValueSet): MethodOutcome? {
        return awsValueSet.create(valueSet)
    }

    @Read
    fun read(httpRequest : HttpServletRequest, @IdParam internalId: IdType): ValueSet? {
        val resource: Resource? = cognitoAuthInterceptor.readFromUrl(httpRequest.pathInfo, null,"ValueSet")
        return if (resource is ValueSet) resource else null
    }

    @Delete
    fun create(theRequest: HttpServletRequest, @IdParam theId: IdType): MethodOutcome? {
        return awsValueSet.delete(theId)
    }
    @Search
    fun search(@OptionalParam(name = ValueSet.SP_URL) url: TokenParam?): List<ValueSet> {
        val list = mutableListOf<ValueSet>()
        if (url == null) {
            val resources = supportChain.fetchAllConformanceResources()
            if (resources != null) {
                resources.forEach{
                    if (it is ValueSet) {
                        val valueSet = it as ValueSet
                        var found=false
                        list.forEach{
                            if (it.url === valueSet.url) found = true
                        }
                        // Remove US valuesets
                        if (valueSet.hasJurisdiction() && valueSet.jurisdictionFirstRep.hasCoding()
                            && valueSet.jurisdictionFirstRep.codingFirstRep.code.equals("US")) found = true
                        // Only UK. This really should use jurisdiction
                        if (!valueSet.url.contains(".uk")) found = true
                        // Only add SNOMED. Will need to review this
                        var isSnomed = false
                        if (valueSet.hasCompose() && valueSet.compose.hasInclude()) {
                            valueSet.compose.include.forEach{
                                if (it.hasSystem() && it.system.equals(FhirSystems.SNOMED_CT)) isSnomed = true
                            }
                        }
                        // remove expansion
                        if (!found && isSnomed) {
                            valueSet.expansion = null
                            valueSet.text = null
                            if (valueSet.id == null) valueSet.id = UUID.randomUUID().toString()
                            list.add(valueSet)
                        }
                    }
                }
            }
            return list
        }

        val resource = supportChain.fetchResource(ValueSet::class.java,java.net.URLDecoder.decode(url.value, StandardCharsets.UTF_8.name()))
        if (resource != null) { list.add(resource)
        } else {
            val resources = awsValueSet.search(url)
            if (resources.size>0) list.addAll(resources)
        }
        return list
    }

    @Operation(name = "\$validate-code", idempotent = true)
    fun validateCode (
        @OperationParam(name = "url") url: String?,
        @OperationParam(name = "context") context: String?,
        @ResourceParam valueSet: ValueSet?,
        @OperationParam(name = "valueSetVersion") valueSetVersion: String?,
        @OperationParam(name = "code") code: String?,
        @OperationParam(name = "system") system: String?,
        @OperationParam(name = "systemVersion") systemVersion: String?,
        @OperationParam(name = "display") display: String?,
        @OperationParam(name = "coding") coding: TokenParam?,
        @OperationParam(name = "codeableConcept") codeableConcept: CodeableConcept?,
        @OperationParam(name = "date") date: DateParam?,
        @OperationParam(name = "abstract") abstract: BooleanType?,
        @OperationParam(name = "displayLanguage") displayLanguage: CodeType?
    ) : OperationOutcome {
        val input = OperationOutcome()
        input.issueFirstRep.severity = OperationOutcome.IssueSeverity.INFORMATION
        if (code != null) {
            val conceptValidaton = ConceptValidationOptions()
            var validationResult: CodeValidationResult? =
                supportChain.validateCode(this.validationSupportContext, conceptValidaton, java.net.URLDecoder.decode(system, StandardCharsets.UTF_8.name()), code, display, java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name()))

            if (validationResult != null) {
                //logger.info(validationResult?.code)
                if (validationResult.severity != null) {
                    when (validationResult.severity) {
                        IValidationSupport.IssueSeverity.ERROR -> input.issueFirstRep.severity = OperationOutcome.IssueSeverity.ERROR;
                        IValidationSupport.IssueSeverity.WARNING -> input.issueFirstRep.severity = OperationOutcome.IssueSeverity.WARNING;
                        else -> {}
                    }
                }
                input.issueFirstRep.diagnostics = validationResult.message
                //logger.info(validationResult?.message)
            }
        }
        return input;
    }

    @Operation(name = "\$expandSCT", idempotent = true)
    fun subsumes (  @OperationParam(name = "filter") filter: String?,
                    @OperationParam(name = "count") count: IntegerType?,
                    @OperationParam(name = "includeDesignations") includeDesignations: BooleanType?,
                    @OperationParam(name = "elements") elements: StringOrListParam?,
                    @OperationParam(name = "property") property: StringOrListParam?
    ) : ValueSet? {

        var param = codingSupport.search(filter,count,includeDesignations, elements, property)
        if (param !== null) {
            if (param.parameterFirstRep.hasResource() && param.parameterFirstRep.resource is ValueSet) return param.parameterFirstRep.resource as ValueSet
        }
        return null
    }

    @Operation(name = "\$expandEcl", idempotent = true)
    fun eclExpand (  @OperationParam(name = "ecl", min = 1) ecl: String?,
                     @OperationParam(name = "filter") filter: String?,
                     @OperationParam(name = "count") count: IntegerType?
    ) : ValueSet? {

        var param = codingSupport.expandEcl(ecl,count,filter)
        if (param !== null) {
            if (param.parameterFirstRep.hasResource() && param.parameterFirstRep.resource is ValueSet) return param.parameterFirstRep.resource as ValueSet
         }
        return null;
    }

    @Operation(name = "\$expand", idempotent = true)
    fun expand(@ResourceParam valueSet: ValueSet?,
               @OperationParam(name = ValueSet.SP_URL) url: TokenParam?,
                @OperationParam(name = "filter") filter: StringParam?): ValueSet? {
        if (url == null && valueSet == null) throw UnprocessableEntityException("Both resource and url can not be null")
        var valueSetR4: ValueSet? = null;
        if (url != null) {
            var valueSets = url.let { search(it) }
            if (valueSets != null) {
                if (valueSets.isNotEmpty())  {
                    valueSetR4= valueSets[0]
                }
            };
        } else {
            valueSetR4 = valueSet;
        }
        if (valueSetR4 != null) {
            var valueSetExpansionOptions = ValueSetExpansionOptions();
            valueSetR4.expansion = null; // remove any previous expansion
            if (filter != null) valueSetExpansionOptions.filter = filter.value
            var expansion: ValueSetExpansionOutcome? =
                supportChain.expandValueSet(this.validationSupportContext, valueSetExpansionOptions, valueSetR4)
            if (expansion != null) {
                if (expansion.valueSet is ValueSet) {
                    var newValueSet = expansion.valueSet as ValueSet
                    valueSetR4.expansion = newValueSet.expansion
                }
                if (expansion?.error != null) { throw UnprocessableEntityException(expansion?.error ) }

            }
            return valueSetR4;
        } else {
            throw UnprocessableEntityException("ValueSet not found");
        }

    }
}
