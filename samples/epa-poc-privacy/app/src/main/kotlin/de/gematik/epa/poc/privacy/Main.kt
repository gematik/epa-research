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
import gg.jte.CodeResolver
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import gg.jte.resolve.ResourceCodeResolver
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Medication
import org.hl7.fhir.r4.model.MedicationDispense
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureDefinition
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

val logger = LoggerFactory.getLogger("de.gematik.epa.poc.privacy")

val codeResolver: CodeResolver = ResourceCodeResolver("templates", TestCaseResult::class.java.classLoader)
var templateEngine: TemplateEngine = TemplateEngine.create(codeResolver, ContentType.Html)

/** Maps a resource type to its EPA Research counterpart profile canonical URL. */
fun researchProfileUrlFor(resource: Resource): String? = when (resource) {
    is Medication -> "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication"
    is MedicationRequest -> "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication-request"
    is MedicationDispense -> "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication-dispense"
    else -> null
}

/** Resolves the EPA Research counterpart profile for a given source resource. */
fun Validator.findPrivacyAwareProfile(resource: Resource): StructureDefinition? {
    val url = researchProfileUrlFor(resource) ?: run {
        logger.error("No privacy aware profile found for resource: {}", resource)
        return null
    }
    return fetchProfile(url)
}

/**
 * Deterministic pseudonymizer: looks up [overrides] first, otherwise derives a stable
 * suffix from a SHA-256 prefix. Memoized so repeated lookups of the same value return identically.
 */
fun deterministicPseudonymizer(overrides: Map<String, String>): PseudonymizeFunction {
    val cache = mutableMapOf<String, String>()
    return { raw ->
        cache.getOrPut(raw) {
            overrides[raw] ?: run {
                val hash = MessageDigest.getInstance("SHA-256")
                    .digest(raw.toByteArray())
                    .take(2)
                    .joinToString("") { "%02X".format(it) }
                "PSEUDO-$raw-$hash"
            }
        }
    }
}

fun main() {
    val config = Yaml.decodeFromString<Config>(Path("testcases-config.yaml").readText())
    logger.info("Loaded config: {}", config)

    val validators = mutableMapOf<String, Validator>()

    config.testCaseList.forEach { testCaseConfig ->
        logger.info("Running testcase: {}", testCaseConfig.id)
        val validator = validators.computeIfAbsent(testCaseConfig.sushiProjectPath) {
            Validator(FhirContextR4, Path(it))
        }
        val pseudonymize = deterministicPseudonymizer(testCaseConfig.pseudonyms)
        val pseudonymsRecord = mutableMapOf<String, String>()

        val sourceBundle = Bundle().apply { type = Bundle.BundleType.COLLECTION }
        val filteredBundle = Bundle().apply { type = Bundle.BundleType.COLLECTION }

        val filterLog = mutableListOf<PrivacyFilterLogEntry>()
        val validationResults = mutableListOf<ValidationResult>()
        val filteredValidationResults = mutableListOf<ValidationResult>()

        testCaseConfig.resourcePaths.forEach { resourcePath ->
            val source = loadResource(Path(testCaseConfig.sushiProjectPath).resolve(resourcePath)) as Resource
            if (RESOURCE_BLOCK_LIST.contains(source.resourceType.name)) {
                logger.info("Skipping blocked resource: {}", source.id)
                return@forEach
            }

            sourceBundle.addEntry().resource = source

            logger.info("Processing resource: {}", source.id)
            validationResults.add(validator.validateWithoutTerminology(source))

            val profile = validator.findPrivacyAwareProfile(source)
            val filtered = if (profile != null) {
                logger.info("Found privacy aware profile: {}", profile.url)
                val tracker = trackingPseudonymizer(pseudonymize, pseudonymsRecord)
                val filterResult = PrivacyFilter(profile, tracker).filter(source)
                filterLog.addAll(filterResult.log)
                filterResult.resource
            } else {
                source
            }

            filteredBundle.addEntry().resource = filtered
            filteredValidationResults.add(validator.validateWithoutTerminology(filtered))
        }

        testCaseConfig.sources.forEach { source ->
            val path = Path(testCaseConfig.sushiProjectPath).resolve(source.path)
            source.content = path.readText()
            logger.info("Loaded FSH source: {}", path)
        }

        val testcaseResult = TestCaseResult(
            testCaseConfig = testCaseConfig,
            sources = testCaseConfig.sources,
            bundle = sourceBundle,
            filteredBudle = filteredBundle,
            filterLog = filterLog,
            validationResults = validationResults,
            filtereValidationResults = filteredValidationResults,
            pseudonyms = pseudonymsRecord,
        )

        val outputPath = Path("../reports/${testCaseConfig.id}.html")
        writeTestCase(config, testcaseResult, outputPath)
        logger.info("Testcase written to: {}", outputPath)
    }
}

/** Wraps [delegate] so every call's mapping (input -> pseudonym) lands in [record] for reporting. */
private fun trackingPseudonymizer(delegate: PseudonymizeFunction, record: MutableMap<String, String>): PseudonymizeFunction =
    { raw -> delegate(raw).also { record[raw] = it } }

fun writeTestCase(config: Config, testCase: TestCaseResult, path: Path) {
    val output = StringOutput()
    templateEngine.render(
        "Testcase.kte",
        mapOf(
            "config" to config,
            "testCase" to testCase
        ),
        output
    )
    path.parent?.toFile()?.mkdirs()
    path.writeText(output.toString())
}
