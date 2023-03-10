package uk.nhs.england.fhirvalidator.awsProvider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component


@Component
class AWSCodeSystem(val awsClient: IGenericClient,
    //sqs: AmazonSQS?,
                    @Qualifier("R4") val ctx: FhirContext,
                    val awsAuditEvent: AWSAuditEvent
) {

    private val log = LoggerFactory.getLogger("FHIRAudit")

    public fun search(url : TokenParam?) : List<CodeSystem> {
        var resources = mutableListOf<CodeSystem>()
        var bundle: Bundle? = null
        var retry = 3
        while (retry > 0) {
            try {
                if (url != null) {
                    bundle = awsClient
                        .search<IBaseBundle>()
                        .forResource(CodeSystem::class.java)
                        .where(
                            CodeSystem.URL.matches().value(url.value)
                        )
                        .returnBundle(Bundle::class.java)
                        .execute()
                    break
                } else {
                    bundle = awsClient
                        .search<IBaseBundle>()
                        .forResource(CodeSystem::class.java)
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
                if (entry.hasResource() && entry.resource is CodeSystem) resources.add(entry.resource as CodeSystem)
            }
        }
        return resources
    }

    fun update(codeSystem: CodeSystem, theId: IdType): MethodOutcome? {
        var response: MethodOutcome? = null
        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient
                    .update()
                    .resource(codeSystem)
                    .withId(theId)
                    .execute()
                val storedCodeSystem = response.resource as CodeSystem
                val auditEvent = awsAuditEvent.createAudit(storedCodeSystem, AuditEvent.AuditEventAction.U)
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

    fun create(codeSystem: CodeSystem): MethodOutcome? {
        var response: MethodOutcome? = null

        if (!codeSystem.hasUrl()) throw UnprocessableEntityException("CodeSystem.url is required")
        val duplicateCheck = search(TokenParam().setValue(codeSystem.url))
        if (duplicateCheck.size>0) throw UnprocessableEntityException("A CodeSystem with this definition already exists.")

        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient
                    .create()
                    .resource(codeSystem)
                    .execute()
                val storedCodeSystem = response.resource as CodeSystem
                val auditEvent = awsAuditEvent.createAudit(storedCodeSystem, AuditEvent.AuditEventAction.C)
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
                val auditEvent = awsAuditEvent.createAudit(storedCodeSystem, AuditEvent.AuditEventAction.D)
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
