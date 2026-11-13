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

import ca.uhn.fhir.validation.ValidationResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.hl7.fhir.r4.model.Bundle

/** Everything the HTML report of one test case shows. */
class TestCaseResult(
    val testCaseConfig: TestCaseConfig,
    val sources: List<SourceFile>,
    sourceBundle: Bundle,
    filteredBundle: Bundle,
    val filterLog: List<PrivacyFilterLogEntry>,
    sourceValidation: List<ValidationResult>,
    filteredValidation: List<ValidationResult>,
    val pseudonyms: Map<String, String>,
) {
    val bundleJson: String = sourceBundle.toPrettyString()
    private val filteredBundleJson: String = filteredBundle.toPrettyString()

    val validationMessages: List<ValidationMessage> = sourceValidation.toMessages()
    val filteredValidationMessages: List<ValidationMessage> = filteredValidation.toMessages()

    /** Unified diff of source against filtered bundle, as a JavaScript string literal for diff2html. */
    val bundleDiff: String
        get() {
            val sourceLines = bundleJson.lines()
            val patch = DiffUtils.diff(sourceLines, filteredBundleJson.lines())
            val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("FHIR.json", "FHIR.json", sourceLines, patch, WHOLE_FILE_CONTEXT)
            return objectMapper.writeValueAsString(unifiedDiff.joinToString("\n"))
        }

    private companion object {
        const val WHOLE_FILE_CONTEXT = 1000
        val objectMapper = ObjectMapper()

        fun List<ValidationResult>.toMessages(): List<ValidationMessage> = flatMap { it.messages }.map {
            ValidationMessage(
                severity = it.severity.name,
                location = it.locationString,
                message = it.message,
                messageId = it.messageId ?: "Generic",
            )
        }
    }
}

data class ValidationMessage(
    val severity: String,
    val location: String,
    val message: String,
    val messageId: String,
)
