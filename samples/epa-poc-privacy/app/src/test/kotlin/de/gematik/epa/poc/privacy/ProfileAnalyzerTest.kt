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

import org.hl7.fhir.r4.model.StructureDefinition
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class ProfileAnalyzerTest {
    /**
     * Developer-only utility: prints all non-HL7 StructureDefinitions known to the validator,
     * which requires the full FHIR package transitive closure to be present in `~/.fhir/packages/`.
     * Disabled by default so CI does not fail on a developer machine missing one of those packages.
     */
    @Disabled("Requires full FHIR package transitive closure under ~/.fhir/packages — run manually when needed")
    @Test
    fun testFindAllProfiles() {
        val validator = Validator(FhirContextR4, Path("../../../src/fhir"))
        val l = validator.validationSupport.fetchAllNonBaseStructureDefinitions<StructureDefinition>() ?: emptyList()
        val s = l.distinctBy { it.url }.filter { !it.url.startsWith("http://hl7.org/") }.sortedBy { it.name }.sortedBy { it.type }

        println("|Name                                       |Type                                       |URL|")
        println("|-------------------------------------------|-------------------------------------------|---|")
        s.forEach {
            println("|${it.name.padEnd(40)}|${it.type.padEnd(40)}|${it.url}|")
        }

    }
}