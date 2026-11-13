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

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.hl7.fhir.r4.model.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.io.path.Path

/**
 * Runs the filter over every source example in `testcases-config.yaml` and compares the outcome with
 * the hand-written research example next to it.
 *
 * StructureDefinitions are read straight from `fsh-generated/resources/`, so the tests do not need
 * the FHIR package cache that the full HAPI validation chain depends on.
 */
class MaaloxanGoldenTest {

    private class Example(val name: String, val source: Resource, val expected: Resource, val pseudonymize: PseudonymizeFunction)

    private val examples: List<Example> = Config.load(Path("testcases-config.yaml")).testCaseList.flatMap { testCase ->
        assertEquals(
            testCase.resourcePaths.size,
            testCase.expectedResearchPaths.size,
            "resourcePaths/expectedResearchPaths length mismatch in ${testCase.id}",
        )
        val sushiRoot = Path(testCase.sushiProjectPath)
        testCase.resourcePaths.zip(testCase.expectedResearchPaths).map { (sourcePath, expectedPath) ->
            Example(
                name = "${testCase.id}: ${sourcePath.substringAfterLast('/')}",
                source = loadResource(sushiRoot.resolve(sourcePath)) as Resource,
                expected = loadResource(sushiRoot.resolve(expectedPath)) as Resource,
                pseudonymize = deterministicPseudonymizer(testCase.pseudonyms),
            )
        }
    }

    @TestFactory
    fun `filter output matches the research example`(): List<DynamicTest> = examples.map { example ->
        dynamicTest(example.name) {
            val result = PrivacyFilter(researchProfileFor(example.source), example.pseudonymize, ::researchProfileOrNullFor).filter(example.source)
            assertEquals(emptyList<PrivacyFilterLogEntry>(), result.log.filter { it.severity == PrivacyLogSeverity.ERROR })

            val actual = withoutExampleNaming(result.resource!!)
            val expected = withoutExampleNaming(example.expected)
            assertTrue(actual.equalsDeep(expected)) {
                val expectedLines = expected.toPrettyString().lines()
                val patch = DiffUtils.diff(expectedLines, actual.toPrettyString().lines())
                UnifiedDiffUtils.generateUnifiedDiff("expected", "actual", expectedLines, patch, 3).joinToString("\n")
            }
        }
    }

    /**
     * A rule that finds nothing to do proves nothing, so every privacy rule of a research profile must
     * change something in its source example. A new rule in a profile fails here until the example covers it.
     */
    @TestFactory
    fun `source example exercises every privacy rule of its profile`(): List<DynamicTest> = examples.map { example ->
        dynamicTest(example.name) {
            val log = PrivacyFilter(researchProfileFor(example.source), example.pseudonymize, ::researchProfileOrNullFor).filter(example.source).log
            val idle = log.filter { it.affectedValues == 0 }.map { it.path } - NOT_IN_SOURCE_EXAMPLES
            assertEquals(emptyList<String>(), idle)
        }
    }

    private companion object {
        /** Rules no source example can exercise; PrivacyFilterTest covers them with hand-built resources. */
        val NOT_IN_SOURCE_EXAMPLES = setOf(
            // The EPAMedication profiles already forbid patientInstruction.
            "MedicationRequest.dosageInstruction.patientInstruction",
            "MedicationDispense.dosageInstruction.patientInstruction",
            // The examples reference their Medication as a resource of its own.
            "MedicationRequest.contained",
            "MedicationDispense.contained",
        )
    }

    /**
     * Source and research example are separate FSH instances with their own ids and reference targets
     * (`ExampleEPAMedicationMaaloxan` vs. `ExampleEPAResearchMedicationMaaloxan`). The filter keeps ids
     * and references as they are, so the naming difference is not part of what is compared.
     */
    private fun withoutExampleNaming(resource: Resource): Resource {
        val parser = FhirContextR4.newJsonParser()
        val json = parser.encodeResourceToString(resource).replace("ExampleEPAResearch", "ExampleEPA")
        return parser.parseResource(json) as Resource
    }
}
