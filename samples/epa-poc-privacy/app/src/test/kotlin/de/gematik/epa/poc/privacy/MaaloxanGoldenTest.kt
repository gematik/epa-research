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
 */

package de.gematik.epa.poc.privacy

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import org.hl7.fhir.r4.model.MedicationDispense
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureDefinition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * Asserts that PrivacyFilter, fed the upstream EPAMedication* Maaloxan example,
 * produces output that is semantically equal (HAPI [Resource.equalsDeep]) to the
 * FSH-generated EPAResearch* Maaloxan golden.
 *
 * Loads StructureDefinitions directly from `fsh-generated/resources/` to avoid
 * pulling the full HAPI validation chain (and its transitive FHIR-package
 * dependencies) into the test.
 */
class MaaloxanGoldenTest {

    @TestFactory
    fun `tool output matches FSH golden`(): List<DynamicTest> {
        val config = Yaml.decodeFromString<Config>(Path("testcases-config.yaml").readText())

        return config.testCaseList.flatMap { testCase ->
            assertEquals(
                testCase.resourcePaths.size,
                testCase.expectedResearchPaths.size,
                "resourcePaths/expectedResearchPaths length mismatch in ${testCase.id}"
            )

            val sushiRoot = Path(testCase.sushiProjectPath)
            val researchProfiles = loadResearchStructureDefinitions(sushiRoot)
            val pseudonymize = deterministicPseudonymizer(testCase.pseudonyms)

            testCase.resourcePaths.zip(testCase.expectedResearchPaths).map { (sourcePath, expectedPath) ->
                DynamicTest.dynamicTest("${testCase.id}: ${sourcePath.substringAfterLast('/')}") {
                    val source = loadResource(sushiRoot.resolve(sourcePath)) as Resource
                    val expected = loadResource(sushiRoot.resolve(expectedPath)) as Resource

                    val url = researchProfileUrlFor(source)
                    assertNotNull(url, "No privacy-aware profile URL for ${source.resourceType.name}")
                    val profile = researchProfiles[url]
                    assertNotNull(profile, "Profile not found in fsh-generated: $url")

                    val actual = PrivacyFilter(profile!!, pseudonymize).filter(source).resource

                    val actualNorm = normalizeForComparison(actual)
                    val expectedNorm = normalizeForComparison(expected)
                    if (!actualNorm.equalsDeep(expectedNorm)) {
                        val expectedLines = expectedNorm.toPrettyString().lines()
                        val actualLines = actualNorm.toPrettyString().lines()
                        val patch = DiffUtils.diff(expectedLines, actualLines)
                        val diff = UnifiedDiffUtils.generateUnifiedDiff("expected", "actual", expectedLines, patch, 3)
                        System.err.println(diff.joinToString("\n"))
                    }
                    assertTrue(
                        actualNorm.equalsDeep(expectedNorm),
                        "Filtered ${source.resourceType.name} does not match ${expectedPath.substringAfterLast('/')}"
                    )
                }
            }
        }
    }

    /**
     * Normalize away differences that are not part of the redaction rules:
     *
     * - **FSH instance naming convention** — the source FSH `ExampleEPAMedicationMaaloxan` and the
     *   redacted-golden FSH `ExampleEPAResearchMedicationMaaloxan` are separate FSH instances and
     *   therefore have different `id`s and cross-resource reference targets. The privacy filter
     *   preserves the source id and references (it does not transform them), so comparison treats
     *   `ExampleEPAResearch` and `ExampleEPA` as equivalent.
     *
     * - **identifier preservation** — the EPAResearch* profiles do not redact `identifier`
     *   (this is intentional per project decision), but the FSH-authored research examples omit it.
     *   The tool correctly preserves it; we strip it from both sides for golden comparison.
     */
    private fun normalizeForComparison(resource: Resource): Resource {
        val parser = FhirContextR4.newJsonParser()
        val json = parser.encodeResourceToString(resource).replace("ExampleEPAResearch", "ExampleEPA")
        val normalized = parser.parseResource(json) as Resource
        when (normalized) {
            is MedicationRequest -> normalized.identifier.clear()
            is MedicationDispense -> normalized.identifier.clear()
        }
        return normalized
    }

    private fun loadResearchStructureDefinitions(sushiRoot: java.nio.file.Path): Map<String, StructureDefinition> {
        val resourcesDir = sushiRoot.resolve("fsh-generated/resources")
        return Files.list(resourcesDir).use { stream ->
            stream.toList()
                .filter { it.name.startsWith("StructureDefinition-") && it.name.endsWith(".json") }
                .map { loadResource(it) as StructureDefinition }
                .associateBy { it.url }
        }
    }
}
