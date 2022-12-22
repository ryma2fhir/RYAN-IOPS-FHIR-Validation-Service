package uk.nhs.england.fwoa.provider

import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.server.RequestDetails
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import mu.KLogging
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.*
import org.springframework.stereotype.Component
import uk.nhs.england.fwoa.Validator
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest

@Component
class ValidateR4Provider (

    private val validate: Validator,
) {
    companion object : KLogging()


    @Validate
    fun validate(
        servletRequest: HttpServletRequest,
        theRequestDetails : RequestDetails,
        @ResourceParam resource: IBaseResource?,
        @Validate.Profile parameterResourceProfile: String?
    ): MethodOutcome {
        var profile:String? = null
        try {
            if (parameterResourceProfile != null) profile = parameterResourceProfile ?: servletRequest.getParameter("profile")
            else if (servletRequest.parameterMap != null && servletRequest.parameterMap.size> 0) {
                profile = servletRequest.getParameter("profile")
            }
        } catch (e : Exception) {
            logger.error(e.message)
        }
        if (profile!= null) profile = URLDecoder.decode(profile, StandardCharsets.UTF_8.name());

        if (resource == null && theRequestDetails.resource == null) throw UnprocessableEntityException("Not resource supplied to validation")
        val operationOutcome : OperationOutcome?
        if (resource == null) {
            // This should cope with Parameters resources being passed in
            operationOutcome = validate.validate(theRequestDetails.resource, profile)

        } else {
            operationOutcome = validate.validate(resource, profile)
        }

        val methodOutcome = MethodOutcome()

        if (operationOutcome != null ) {
            if (operationOutcome.hasIssue()) {
                // Temp workaround for onto validation issues around workflow code
                for (issue in operationOutcome.issue) {
                    if (issue.hasDiagnostics() && issue.diagnostics.contains("404")) {
                        if(
                            issue.diagnostics.contains("https://fhir.nhs.uk/CodeSystem/NHSDataModelAndDictionary-treatment-function")) {
                            issue.severity = OperationOutcome.IssueSeverity.INFORMATION
                        }
                    }
                }
            } else {
                // https://nhsd-jira.digital.nhs.uk/browse/IOPS-829
                operationOutcome.issue.add(OperationOutcome.OperationOutcomeIssueComponent()
                    .setCode(OperationOutcome.IssueType.INFORMATIONAL)
                    .setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
                    .setDiagnostics("No issues detected during validation"))
            }
        }
        methodOutcome.operationOutcome = operationOutcome
        return methodOutcome
    }


}
