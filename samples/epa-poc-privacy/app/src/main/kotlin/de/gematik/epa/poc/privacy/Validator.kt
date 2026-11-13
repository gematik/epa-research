/*
 * Copyright 2024-2026, gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 */

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
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.system.measureTimeMillis

/** Validates resources against the profiles of a SUSHI project and its package dependencies. */
class Validator(ctx: FhirContext, sushiDir: Path) {
    val validationSupport: IValidationSupport
    private val fhirValidator: FhirValidator = ctx.newValidator()

    init {
        val timeTaken = measureTimeMillis {
            validationSupport = SushiProjectValidationSupport(ctx, sushiDir)
            val chain = ValidationSupportChain(
                validationSupport,
                DefaultProfileValidationSupport(ctx),
                CommonCodeSystemsTerminologyService(ctx),
                InMemoryTerminologyServerValidationSupport(ctx),
                SnapshotGeneratingValidationSupport(ctx),
            )
            val instanceValidator = FhirInstanceValidator(CachingValidationSupport(chain))
            instanceValidator.setNoTerminologyChecks(true)
            fhirValidator.registerValidatorModule(instanceValidator)
        }
        logger.info("Initialized Validator in {}ms", timeTaken)
    }

    fun validateWithoutTerminology(resource: IBaseResource): ValidationResult {
        val result = fhirValidator.validateWithResult(resource)
        return ValidationResult(result.context, result.messages.filterNot { it.isNoise })
    }

    fun fetchProfile(profileUrl: String): StructureDefinition? =
        validationSupport.fetchResource(StructureDefinition::class.java, profileUrl)

    private companion object {
        val logger = LoggerFactory.getLogger(Validator::class.java)

        // Terminology is switched off, and dom-6 only recommends a narrative the examples do not carry.
        val ca.uhn.fhir.validation.SingleValidationMessage.isNoise: Boolean
            get() = messageId == "Terminology_PassThrough_TX_Message" || message.orEmpty().contains("dom-6")
    }
}
