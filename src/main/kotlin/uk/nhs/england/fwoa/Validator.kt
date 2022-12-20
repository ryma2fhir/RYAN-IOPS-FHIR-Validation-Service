package uk.nhs.england.fwoa

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException
import ca.uhn.fhir.validation.FhirValidator
import ca.uhn.fhir.validation.SingleValidationMessage
import ca.uhn.fhir.validation.ValidationResult
import com.google.common.collect.ImmutableList
import com.google.gson.JsonSyntaxException
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.hl7.fhir.instance.model.api.IBaseResource
import java.util.function.Function
import java.util.stream.Collectors





class Validator(var fhirVersion: String, var implementationGuidesFolder: String?) {

    var ctx : FhirContext
    var fhirValidator : FhirValidator
    init {
        if (fhirVersion != ValidatorConstants.FHIR_R4 && fhirVersion != ValidatorConstants.FHIR_STU3) {
            throw RuntimeException("Invalid FHIR version $fhirVersion")
        }
        fhirVersion = fhirVersion
        if (implementationGuidesFolder == null) implementationGuidesFolder =
            ValidatorConstants.DEFAULT_IMPLEMENTATION_GUIDES_FOLDER
        // To learn more about the different ways to configure FhirInstanceValidator see: https://hapifhir.io/hapi-fhir/docs/validation/validation_support_modules.html
        // To learn more about the different ways to configure FhirInstanceValidator see: https://hapifhir.io/hapi-fhir/docs/validation/validation_support_modules.html
        ctx =
            if (ValidatorConstants.FHIR_R4 == fhirVersion) FhirContext.forR4() else FhirContext.forDstu3()

        // Create a chain that will hold our modules

        // Create a chain that will hold our modules
        val supportChain = ValidationSupportChain()

        // DefaultProfileValidationSupport supplies base FHIR definitions. This is generally required
        // even if you are using custom profiles, since those profiles will derive from the base
        // definitions.

        // DefaultProfileValidationSupport supplies base FHIR definitions. This is generally required
        // even if you are using custom profiles, since those profiles will derive from the base
        // definitions.
        val defaultSupport = DefaultProfileValidationSupport(ctx)
        supportChain.addValidationSupport(defaultSupport)

        // This module supplies several code systems that are commonly used in validation

        // This module supplies several code systems that are commonly used in validation
        supportChain.addValidationSupport(CommonCodeSystemsTerminologyService(ctx))

        // This module implements terminology services for in-memory code validation

        // This module implements terminology services for in-memory code validation
        supportChain.addValidationSupport(InMemoryTerminologyServerValidationSupport(ctx))

        // Create a PrePopulatedValidationSupport which can be used to load custom definitions.

        //  TODO Create a PrePopulatedValidationSupport which can be used to load custom definitions.

        val prepopulatedValidationSupport: PrePopulatedValidationSupport = loadIgs(ctx)
        supportChain.addValidationSupport(prepopulatedValidationSupport)

        // Create a validator using the FhirInstanceValidator module.

        // Create a validator using the FhirInstanceValidator module.
        val validatorModule = FhirInstanceValidator(supportChain)
        fhirValidator = ctx.newValidator().registerValidatorModule(validatorModule)
    }

    private fun loadIgs(ctx: FhirContext): PrePopulatedValidationSupport {
        val jsonParser = ctx.newJsonParser()
        val myCodeSystems: Map<String, IBaseResource> = HashMap()
        val myStructureDefinitions: Map<String, IBaseResource> = HashMap()
        val myValueSets: Map<String, IBaseResource> = HashMap()
        return PrePopulatedValidationSupport(ctx, myStructureDefinitions, myValueSets, myCodeSystems)
    }

    private fun toValidatorResponse(result: ValidationResult): ValidatorResponse {
        var validatorResponse = ValidatorResponse(isSuccessful = result.isSuccessful)
        validatorResponse.errorMessages =
            result.messages.stream()
                .map(Function<SingleValidationMessage, Any> { singleValidationMessage: SingleValidationMessage ->
                    var errorMessage = ValidatorErrorMessage()
                    errorMessage.severity = singleValidationMessage.severity.code
                    errorMessage.msg = singleValidationMessage.locationString + " - " + singleValidationMessage.message
                    errorMessage

                }).collect(Collectors.toList()) as List<ValidatorErrorMessage>?

        return validatorResponse
    }

    fun validate(resource: String?) : ValidatorResponse{
        return try {
            val result: ValidationResult = fhirValidator.validateWithResult(resource)
            toValidatorResponse(result)
        } catch (e: JsonSyntaxException) {

            var validatorResponse = ValidatorResponse(isSuccessful = false)
            val msg = ValidatorErrorMessage()
            msg.msg ="Invalid JSON"
            msg.severity = "error"
            validatorResponse.errorMessages =
                    ImmutableList.of(
                        msg
                    )
            validatorResponse
        } catch (e: NullPointerException) {
            var validatorResponse = ValidatorResponse(isSuccessful = false)
            val msg = ValidatorErrorMessage()
            msg.msg ="Invalid JSON"
            msg.severity = "error"
            validatorResponse.errorMessages =
                ImmutableList.of(
                    msg
                )
            validatorResponse
        } catch (e: IllegalArgumentException) {
            var validatorResponse = ValidatorResponse(isSuccessful = false)
            val msg = ValidatorErrorMessage()
            msg.msg ="Invalid JSON"
            msg.severity = "error"
            validatorResponse.errorMessages =
                ImmutableList.of(
                    msg
                )
            validatorResponse
        } catch (e: InvalidRequestException) {
            var validatorResponse = ValidatorResponse(isSuccessful = false)
            val msg = ValidatorErrorMessage()
            msg.msg ="Invalid JSON"
            msg.severity = "error"
            validatorResponse.errorMessages =
                ImmutableList.of(
                    msg
                )
            validatorResponse
        }
    }
}
