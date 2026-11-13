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

import org.hl7.fhir.r4.model.Resource

private const val RESEARCH_PROFILE_BASE = "https://gematik.de/fhir/epa-research/StructureDefinition"

private val RESEARCH_PROFILE_URLS = mapOf(
    "Medication" to "$RESEARCH_PROFILE_BASE/epa-research-medication",
    "MedicationRequest" to "$RESEARCH_PROFILE_BASE/epa-research-medication-request",
    "MedicationDispense" to "$RESEARCH_PROFILE_BASE/epa-research-medication-dispense",
)

/** Canonical URL of the research profile for a resource, or null when its type must not be exported. */
fun researchProfileUrlFor(resource: Resource): String? = RESEARCH_PROFILE_URLS[resource.fhirType()]
