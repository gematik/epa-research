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

import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.ElementDefinition
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.StructureDefinition
import org.hl7.fhir.r4.model.Type
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name

val SUSHI_OUTPUT: Path = Path("../../../src/fhir/fsh-generated/resources")

const val KVNR = "X110411319"
const val JOB_NUMBER = "PSEUDO-X110411319-9F2C"
val testPseudonymizer = deterministicPseudonymizer(mapOf(KVNR to JOB_NUMBER))

/** The research profiles as SUSHI generated them, keyed by canonical URL. */
fun loadResearchProfiles(directory: Path = SUSHI_OUTPUT): Map<String, StructureDefinition> =
    Files.list(directory).use { files ->
        files.toList()
            .filter { it.name.startsWith("StructureDefinition-") && it.name.endsWith(".json") }
            .map { loadResource(it) as StructureDefinition }
            .associateBy { it.url }
    }

fun researchProfileFor(resource: Resource): StructureDefinition =
    loadResearchProfiles().getValue(researchProfileUrlFor(resource)!!)

/** As [researchProfileFor], but null for a resource type that has no research profile. */
fun researchProfileOrNullFor(resource: Resource): StructureDefinition? =
    researchProfileUrlFor(resource)?.let(loadResearchProfiles()::get)

fun filterWithResearchProfile(resource: Resource): PrivacyFilterResult =
    PrivacyFilter(researchProfileFor(resource), testPseudonymizer, ::researchProfileOrNullFor).filter(resource)

/** A minimal hand-written profile, for cases the real research profiles do not contain. */
fun profileOf(type: String, vararg elements: ElementDefinition): StructureDefinition =
    StructureDefinition().apply {
        url = "https://example.org/StructureDefinition/test-$type"
        this.type = type
        differential.element = elements.toList()
    }

/** Builds an ElementDefinition from its id; path and slice name follow from it as SUSHI would write them. */
fun element(id: String, max: String? = null, label: Extension? = null, configure: ElementDefinition.() -> Unit = {}): ElementDefinition =
    ElementDefinition().apply {
        this.id = id
        path = id.split('.').joinToString(".") { it.substringBefore(':') }
        id.substringAfterLast('.').substringAfter(':', "").takeIf { it.isNotEmpty() }?.let { sliceName = it }
        max?.let { this.max = it }
        label?.let { addExtension(it) }
        configure()
    }

fun privacyLabel(obligationPolicy: String, dummyValue: Type? = null): Extension =
    Extension(PRIVACY_LABEL_EXTENSION_URL).apply {
        addExtension("obligationPolicy", Coding("http://terminology.hl7.org/CodeSystem/v3-ActCode", obligationPolicy, null))
        dummyValue?.let { addExtension("dummyValue", it) }
    }

val REDACT get() = privacyLabel("REDACT")
val PSEUD get() = privacyLabel("PSEUD")
fun anony(placeholder: String) = privacyLabel("ANONY", StringType(placeholder))
