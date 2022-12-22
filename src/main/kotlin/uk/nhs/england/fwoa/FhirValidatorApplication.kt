package uk.nhs.england.fwoa

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.LenientErrorHandler
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.server.RestfulServer
import mu.KLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.ServletComponentScan
import org.springframework.context.annotation.Bean


@SpringBootApplication
@ServletComponentScan
open class FhirValidatorApplication : ApplicationRunner {
    companion object : KLogging()

    var ctx: FhirContext? = null

    override fun run(args: ApplicationArguments?) {
        logger.debug("EXECUTING THE APPLICATION")
        if (args != null) {
            for (opt in args.optionNames) {
                FhirValidatorApplication.logger.debug("args: {}", opt)
            }
        }
    }

    @Bean
    open fun fhirContext(): FhirContext? {
        if (this.ctx == null) {
            this.ctx = FhirContext.forR4()
        }
        // To allow for ODS errors
        this.ctx?.setParserErrorHandler(LenientErrorHandler())
        return this.ctx
    }

    @Bean
    open fun validator(): Validator {
        var fhirVersion = System.getenv("FHIR_VERSION")
        if (fhirVersion == null) {
            fhirVersion = ValidatorConstants.FHIR_R4
        }
        var validator = Validator(fhirVersion, null)
        return validator
    }
}

fun main(args: Array<String>) {
    FhirValidatorApplication.logger.debug("STARTING THE APPLICATION")
    for (i in 0 until args.size) {
        FhirValidatorApplication.logger.debug("args[{}]: {}", i, args[i])
    }
    runApplication<FhirValidatorApplication>(*args)
}
