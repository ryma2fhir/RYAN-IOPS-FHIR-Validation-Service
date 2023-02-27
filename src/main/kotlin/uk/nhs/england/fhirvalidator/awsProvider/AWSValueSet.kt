package uk.nhs.england.fhirvalidator.awsProvider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.param.TokenParam
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component


@Component
class AWSValueSet(val awsClient: IGenericClient,
    //sqs: AmazonSQS?,
                  @Qualifier("R4") val ctx: FhirContext,
                  val awsAuditEvent: AWSAuditEvent
) {

    private val log = LoggerFactory.getLogger("FHIRAudit")

    public fun search(url : TokenParam?) : List<ValueSet> {
        var resources = mutableListOf<ValueSet>()
        var bundle: Bundle? = null
        var retry = 3
        while (retry > 0) {
            try {
                if (url != null) {
                    bundle = awsClient
                        .search<IBaseBundle>()
                        .forResource(ValueSet::class.java)
                        .where(
                            ValueSet.URL.matches().value(url.value)
                        )
                        .returnBundle(Bundle::class.java)
                        .execute()
                    break
                } else {
                    bundle = awsClient
                        .search<IBaseBundle>()
                        .forResource(ValueSet::class.java)
                        .returnBundle(Bundle::class.java)
                        .execute()
                    break
                }
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        if (bundle!=null && bundle.hasEntry()) {
            for (entry in bundle.entry) {
                if (entry.hasResource() && entry.resource is ValueSet) resources.add(entry.resource as ValueSet)
            }
        }
        return resources
    }

    fun update(valueSet: ValueSet, theId: IdType): MethodOutcome? {
        var response: MethodOutcome? = null
        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient
                    .update()
                    .resource(valueSet)
                    .withId(theId)
                    .execute()
                val storedValueSet = response.resource as ValueSet
                val auditEvent = awsAuditEvent.createAudit(storedValueSet, AuditEvent.AuditEventAction.U)
                awsAuditEvent.writeAWS(auditEvent)
                break
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        return response
    }

    fun create(valueSet: ValueSet): MethodOutcome? {
        var response: MethodOutcome? = null
        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient
                    .create()
                    .resource(valueSet)
                    .execute()
                val storedValueSet = response.resource as ValueSet
                val auditEvent = awsAuditEvent.createAudit(storedValueSet, AuditEvent.AuditEventAction.C)
                awsAuditEvent.writeAWS(auditEvent)
                break
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        return response
    }

    fun delete(theId: IdType): MethodOutcome? {
        var response: MethodOutcome? = null
        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient
                    .delete()
                    .resourceById(theId)
                    .execute()

                /*
                val auditEvent = awsAuditEvent.createAudit(storedValueSet, AuditEvent.AuditEventAction.D)
                awsAuditEvent.writeAWS(auditEvent)
                */
                break

            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        return response
    }


}
