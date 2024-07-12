package de.gematik.epa.poc.privacy

import ca.uhn.fhir.validation.ValidationResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureDefinition


private var objectMapper = ObjectMapper()

class TestCaseResult(
    val testCaseConfig: TestCaseConfig,
    val sources: List<SourceFile>,
    val bundle: Bundle,
    val filteredBudle: Bundle,
    val filterLog: List<PrivacyFilterLogEntry>,
    val validationResults: List<ValidationResult>,
    val filtereValidationResults: List<ValidationResult>,
    val pseudonyms: Map<String, String>,
) {
    val bundleJson = bundle.toPrettyString()
    val filteredBudleJson = filteredBudle.toPrettyString()

    val validationMessages: List<ValidationMessage> = validationResults.flatMap { it.messages }.map {
        ValidationMessage(
            severity = it.severity.name,
            location = it.locationString,
            message = it.message,
            messageId = it.messageId ?: "Generic"
        )
    }

    val filteredValidationMessages: List<ValidationMessage> = filtereValidationResults.flatMap { it.messages }.map {
        ValidationMessage(
            severity = it.severity.name,
            location = it.locationString,
            message = it.message,
            messageId = it.messageId ?: "Generic"
        )
    } ?: emptyList()

    val bundleDiff: String get() {
        val bundleLines = bundleJson.lines()
        val patch = DiffUtils.diff(bundleLines, filteredBudleJson.lines())
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("FHIR.json", "FHIR.json", bundleLines, patch, 1000)
        return objectMapper.writeValueAsString(unifiedDiff.joinToString("\n"))
    }
}

data class ValidationMessage(
    val severity: String,
    val location: String,
    val message: String,
    val messageId: String,
)