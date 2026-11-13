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
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import gg.jte.resolve.ResourceCodeResolver
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val logger = LoggerFactory.getLogger("de.gematik.epa.poc.privacy")

private val CONFIG_FILE = Path("testcases-config.yaml")
private val REPORT_DIRECTORY = Path("../reports")

private val templateEngine: TemplateEngine = TemplateEngine.create(
    ResourceCodeResolver("templates", TestCaseResult::class.java.classLoader),
    ContentType.Html,
)

fun main() {
    val config = Config.load(CONFIG_FILE)
    val validators = mutableMapOf<String, Validator>()

    for (testCase in config.testCaseList) {
        logger.info("Running testcase: {}", testCase.id)
        val validator = validators.getOrPut(testCase.sushiProjectPath) {
            Validator(FhirContextR4, Path(testCase.sushiProjectPath))
        }
        val reportPath = REPORT_DIRECTORY.resolve("${testCase.id}.html")
        writeReport(config, runTestCase(testCase, validator), reportPath)
        logger.info("Report written to: {}", reportPath)
    }
}

private fun runTestCase(testCase: TestCaseConfig, validator: Validator): TestCaseResult {
    val sushiProject = Path(testCase.sushiProjectPath)
    val pseudonyms = mutableMapOf<String, String>()
    val pseudonymize = deterministicPseudonymizer(testCase.pseudonyms).recordingInto(pseudonyms)

    val sourceBundle = Bundle().setType(Bundle.BundleType.COLLECTION)
    val filteredBundle = Bundle().setType(Bundle.BundleType.COLLECTION)
    val filterLog = mutableListOf<PrivacyFilterLogEntry>()
    val sourceValidation = mutableListOf<ValidationResult>()
    val filteredValidation = mutableListOf<ValidationResult>()

    for (resourcePath in testCase.resourcePaths) {
        val source = loadResource(sushiProject.resolve(resourcePath)) as Resource
        logger.info("Processing resource: {}", source.id)
        sourceBundle.addEntry().resource = source
        sourceValidation += validator.validateWithoutTerminology(source)

        val filtered = filter(source, validator, pseudonymize, filterLog) ?: continue
        filteredBundle.addEntry().resource = filtered
        filteredValidation += validator.validateWithoutTerminology(filtered)
    }

    return TestCaseResult(
        testCaseConfig = testCase,
        sources = testCase.sources.map { it.copy(content = sushiProject.resolve(it.path).readText()) },
        sourceBundle = sourceBundle,
        filteredBundle = filteredBundle,
        filterLog = filterLog,
        sourceValidation = sourceValidation,
        filteredValidation = filteredValidation,
        pseudonyms = pseudonyms,
    )
}

/** Returns null when the resource must not be exported: it has no research profile, or filtering failed. */
private fun filter(
    source: Resource,
    validator: Validator,
    pseudonymize: PseudonymizeFunction,
    filterLog: MutableList<PrivacyFilterLogEntry>,
): Resource? {
    val profile = researchProfileUrlFor(source)?.let(validator::fetchProfile)
    if (profile == null) {
        logger.warn("No research profile for {}; resource is not exported", source.id)
        return null
    }
    val result = PrivacyFilter(profile, pseudonymize).filter(source)
    filterLog += result.log
    if (result.resource == null) {
        logger.error("Filtering {} failed; resource is not exported", source.id)
    }
    return result.resource
}

private fun writeReport(config: Config, testCase: TestCaseResult, path: Path) {
    val output = StringOutput()
    templateEngine.render("Testcase.kte", mapOf("config" to config, "testCase" to testCase), output)
    path.parent?.createDirectories()
    path.writeText(output.toString())
}
