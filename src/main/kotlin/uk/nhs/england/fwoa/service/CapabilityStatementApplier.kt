package uk.nhs.england.fwoa.service

import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.CapabilityStatement
import org.springframework.stereotype.Service
import uk.nhs.england.fwoa.util.applyProfile
import uk.nhs.england.fwoa.util.getResourcesOfType


class CapabilityStatementApplier(
    val supportChain: ValidationSupportChain
) {
    private val restResources = supportChain.fetchAllConformanceResources()?.filterIsInstance(CapabilityStatement::class.java)
        ?.flatMap { it.rest }
        ?.flatMap { it.resource }

    fun applyCapabilityStatementProfiles(resource: IBaseResource) {
        restResources?.forEach { applyRestResource(resource, it) }
    }

    private fun applyRestResource(
        resource: IBaseResource,
        restResource: CapabilityStatement.CapabilityStatementRestResourceComponent
    ) {
        val matchingResources = getResourcesOfType(resource, restResource.type)
        if (restResource.hasProfile()) {
            applyProfile(matchingResources, restResource.profileElement)
        }
    }

}
