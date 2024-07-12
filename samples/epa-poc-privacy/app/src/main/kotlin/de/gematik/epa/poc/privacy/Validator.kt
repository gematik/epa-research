package de.gematik.epa.poc.privacy

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.validation.FhirValidator
import ca.uhn.fhir.validation.ValidationResult
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.StructureDefinition
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.system.measureTimeMillis

class Validator(
    ctx: FhirContext,
    sushiDir: Path = Path("privacy-testdata")
) {
    val validationSupport: IValidationSupport
    val fhirValidator: FhirValidator

    init {
        val timeTaken = measureTimeMillis {
            validationSupport = SushiProjectValidationSupport(ctx, sushiDir)

            // Create a support chain including the NPM Package Support
            val validationSupportChain = ValidationSupportChain(
                validationSupport,
                DefaultProfileValidationSupport(ctx),
                CommonCodeSystemsTerminologyService(ctx),
                InMemoryTerminologyServerValidationSupport(ctx),
                SnapshotGeneratingValidationSupport(ctx)
            )
            val validationSupport = CachingValidationSupport(validationSupportChain)

            fhirValidator = ctx.newValidator()
            val instanceValidator = FhirInstanceValidator(validationSupport)
            fhirValidator.registerValidatorModule(instanceValidator)
        }
        logger.info("Initialized Validator in {}ms", timeTaken)
    }

    fun validateWithoutTerminology(resource: IBaseResource): ValidationResult {
        val result = fhirValidator.validateWithResult(resource)
        return ValidationResult(
            result.context,
            result.messages.filter { it.messageId != "Terminology_PassThrough_TX_Message" }
        )
    }

    fun fetchProfile(profileUrl: String): StructureDefinition? {
        return validationSupport.fetchResource(StructureDefinition::class.java, profileUrl)
    }
}