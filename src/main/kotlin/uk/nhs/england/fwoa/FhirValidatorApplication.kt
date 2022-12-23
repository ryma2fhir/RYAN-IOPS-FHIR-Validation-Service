package uk.nhs.england.fwoa

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.LenientErrorHandler
import mu.KLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.ServletComponentScan
import org.springframework.context.annotation.Bean
import uk.nhs.england.fwoa.configuration.TerminologyValidationProperties
import uk.nhs.england.fwoa.util.CorsFilter
import javax.servlet.Filter


@SpringBootApplication
@ServletComponentScan
@EnableConfigurationProperties(TerminologyValidationProperties::class)
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
    open fun validator(terminologyValidationProperties: TerminologyValidationProperties): Validator {
        var fhirVersion = System.getenv("FHIR_VERSION")
        if (fhirVersion == null) {
            fhirVersion = ValidatorConstants.FHIR_R4
        }
        var validator = Validator(fhirVersion, null, terminologyValidationProperties)
        return validator
    }

    @Bean
    open fun corsFilter() : Filter {
        // Need to check this is working correctly
        return CorsFilter()
    }


}

fun main(args: Array<String>) {
    FhirValidatorApplication.logger.debug("STARTING THE APPLICATION")
    for (i in 0 until args.size) {
        FhirValidatorApplication.logger.debug("args[{}]: {}", i, args[i])
    }
    runApplication<FhirValidatorApplication>(*args)
}
