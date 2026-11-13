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

import de.gematik.epa.poc.privacy.PrivacyFilterMethod.Anonymize
import de.gematik.epa.poc.privacy.PrivacyFilterMethod.Pseudonymize
import de.gematik.epa.poc.privacy.PrivacyFilterMethod.Redact
import org.hl7.fhir.r4.model.DomainResource
import org.hl7.fhir.r4.model.Medication
import org.hl7.fhir.r4.model.MedicationDispense
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Pins down what the filter reads out of each research profile: the privacy rules and the extensions
 * that survive. A change to a profile fails here first, so the expectation is updated on purpose
 * rather than slipping through with a regenerated example.
 */
class ResearchProfileRulesTest {

    @Test
    fun `EPAResearchMedication rules`() {
        assertEquals(
            listOf(
                "Medication.meta.source" to Redact,
                "Medication.text" to Redact,
                "Medication.code.coding:atc-de" to Redact,
                "Medication.code.text" to Redact,
                "Medication.status" to Redact,
                "Medication.form.text" to Redact,
                "Medication.ingredient.item[x]:itemCodeableConcept.coding:atc-de" to Redact,
                "Medication.ingredient.item[x]:itemCodeableConcept.text" to Redact,
            ),
            rulesAppliedTo(Medication()),
        )
    }

    @Test
    fun `EPAResearchMedicationRequest rules`() {
        assertEquals(
            listOf(
                "MedicationRequest.meta.source" to Redact,
                "MedicationRequest.text" to Redact,
                "MedicationRequest.contained" to Redact,
                "MedicationRequest.status" to Anonymize,
                "MedicationRequest.subject.display" to Redact,
                "MedicationRequest.requester" to Redact,
                "MedicationRequest.recorder" to Redact,
                "MedicationRequest.note" to Redact,
                "MedicationRequest.dosageInstruction.text" to Redact,
                "MedicationRequest.dosageInstruction.patientInstruction" to Redact,
                "MedicationRequest.dispenseRequest.performer" to Redact,
                "MedicationRequest.subject" to Pseudonymize,
            ),
            rulesAppliedTo(MedicationRequest()),
        )
    }

    @Test
    fun `EPAResearchMedicationDispense rules`() {
        assertEquals(
            listOf(
                "MedicationDispense.meta.source" to Redact,
                "MedicationDispense.text" to Redact,
                "MedicationDispense.contained" to Redact,
                "MedicationDispense.status" to Anonymize,
                "MedicationDispense.subject.display" to Redact,
                "MedicationDispense.performer.actor.reference" to Redact,
                "MedicationDispense.performer.actor.identifier" to Redact,
                "MedicationDispense.performer.actor.display" to Anonymize,
                "MedicationDispense.location" to Redact,
                "MedicationDispense.destination" to Redact,
                "MedicationDispense.receiver" to Redact,
                "MedicationDispense.note" to Redact,
                "MedicationDispense.dosageInstruction.text" to Redact,
                "MedicationDispense.dosageInstruction.patientInstruction" to Redact,
                "MedicationDispense.substitution.responsibleParty" to Redact,
                "MedicationDispense.subject" to Pseudonymize,
            ),
            rulesAppliedTo(MedicationDispense()),
        )
    }

    @Test
    fun `EPAResearchMedication keeps only its declared extensions`() {
        assertEquals(
            setOf(
                "$EPA_MEDICATION/medication-id-vaccine-extension",
                "$EPA_MEDICATION/drug-category-extension",
                "http://fhir.de/StructureDefinition/normgroesse",
                "$EPA_MEDICATION/epa-medication-type-extension",
            ),
            extensionsKeptFrom("Medication-ExampleEPAMedicationMaaloxan.json"),
        )
    }

    @Test
    fun `EPAResearchMedicationRequest keeps only its declared extensions`() {
        assertEquals(
            setOf("$EPA_MEDICATION/multiple-prescription-extension"),
            extensionsKeptFrom("MedicationRequest-ExampleEPAMedicationRequestMaaloxan.json"),
        )
    }

    @Test
    fun `EPAResearchMedicationDispense keeps no extensions`() {
        assertEquals(
            emptySet<String>(),
            extensionsKeptFrom("MedicationDispense-ExampleEPAMedicationDispenseMaaloxan.json"),
        )
    }

    /** The log has one entry per rule whether or not it found anything, so an empty resource lists them all. */
    private fun rulesAppliedTo(emptyResource: Resource): List<Pair<String, PrivacyFilterMethod?>> =
        filterWithResearchProfile(emptyResource).log.map { it.path to it.method }

    private fun extensionsKeptFrom(exampleFile: String): Set<String> {
        val source = loadResource(SUSHI_OUTPUT.resolve(exampleFile)) as Resource
        val filtered = filterWithResearchProfile(source).resource as DomainResource
        return filtered.extension.map { it.url }.toSet()
    }

    private companion object {
        const val EPA_MEDICATION = "https://gematik.de/fhir/epa-medication/StructureDefinition"
    }
}
