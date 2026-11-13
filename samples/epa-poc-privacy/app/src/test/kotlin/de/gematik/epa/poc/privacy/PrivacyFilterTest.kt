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

import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Medication
import org.hl7.fhir.r4.model.MedicationDispense
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.UriType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val ATC = "http://fhir.de/CodeSystem/bfarm/atc"
private const val PZN = "http://fhir.de/CodeSystem/ifa/pzn"
private const val ASK = "http://fhir.de/CodeSystem/ask"
private const val KVID = "http://fhir.de/sid/gkv/kvid-10"

/**
 * Behaviour of the filter rule by rule. Cases the research profiles contain run against the real
 * profiles; the rest (failure modes, constructs no research profile uses yet) against hand-written ones.
 */
class PrivacyFilterTest {

    @Nested
    inner class Redact {

        @Test
        fun `removes a single value`() {
            val medication = Medication().setCode(CodeableConcept().setText("Maaloxan"))

            val filtered = filterWithResearchProfile(medication).resource as Medication

            assertFalse(filtered.code.hasText())
        }

        @Test
        fun `removes every repetition of a repeating element`() {
            val request = MedicationRequest()
            request.addNote().text = "first"
            request.addNote().text = "second"

            val filtered = filterWithResearchProfile(request).resource as MedicationRequest

            assertFalse(filtered.hasNote())
        }

        @Test
        fun `reaches into every repetition of a repeating parent`() {
            val request = MedicationRequest()
            request.addDosageInstruction().setText("morgens").setPatientInstruction("mit Wasser")
            request.addDosageInstruction().setText("abends").setPatientInstruction("nach dem Essen")

            val filtered = filterWithResearchProfile(request).resource as MedicationRequest

            assertEquals(2, filtered.dosageInstruction.size)
            assertTrue(filtered.dosageInstruction.none { it.hasText() || it.hasPatientInstruction() })
        }

        @Test
        fun `leaves siblings of the redacted element alone`() {
            val dispense = MedicationDispense()
            dispense.addDosageInstruction().setText("morgens").setSequence(1)

            val filtered = filterWithResearchProfile(dispense).resource as MedicationDispense

            assertEquals(1, filtered.dosageInstructionFirstRep.sequence)
        }

        @Test
        fun `removes elements inside meta`() {
            val medication = Medication()
            medication.meta.source = "https://praxis.example/fhir"

            val filtered = filterWithResearchProfile(medication).resource as Medication

            assertFalse(filtered.meta.hasSource())
        }

        @Test
        fun `reports an absent element as not present instead of failing`() {
            val result = filterWithResearchProfile(Medication())

            val entry = result.log.single { it.path == "Medication.code.text" }
            assertEquals(PrivacyLogSeverity.INFO, entry.severity)
            assertEquals(0, entry.affectedValues)
        }

        @Test
        fun `removes an element the profile forbids without a privacy label`() {
            val profile = profileOf("Medication", element("Medication.manufacturer", max = "0"))
            val medication = Medication().setManufacturer(Reference().setDisplay("Sanofi"))

            val filtered = PrivacyFilter(profile, testPseudonymizer).filter(medication).resource as Medication

            assertFalse(filtered.hasManufacturer())
        }
    }

    @Nested
    inner class RedactSlice {

        @Test
        fun `removes only the codings of the redacted slice`() {
            val medication = Medication()
            medication.code.addCoding(Coding(PZN, "9717395", null))
            medication.code.addCoding(Coding(ATC, "A02AD10", null))
            medication.code.addCoding(Coding(ASK, "02250", null))

            val filtered = filterWithResearchProfile(medication).resource as Medication

            assertEquals(listOf(PZN, ASK), filtered.code.coding.map { it.system })
        }

        @Test
        fun `removes the slice in every ingredient`() {
            val medication = Medication()
            repeat(2) {
                medication.addIngredient().itemCodeableConcept
                    .addCoding(Coding(ASK, "02250", null))
                    .addCoding(Coding(ATC, "A02AA04", null))
            }

            val filtered = filterWithResearchProfile(medication).resource as Medication

            assertEquals(listOf(listOf(ASK), listOf(ASK)), filtered.ingredient.map { i -> i.itemCodeableConcept.coding.map { it.system } })
        }

        @Test
        fun `rules for one choice type skip ingredients of the other type`() {
            val medication = Medication()
            medication.addIngredient().item = Reference("#wirkstoff").setDisplay("Magnesiumhydroxid")
            medication.addIngredient().itemCodeableConcept.setText("Algeldrat")

            val result = filterWithResearchProfile(medication)

            val filtered = result.resource as Medication
            assertEquals("Magnesiumhydroxid", filtered.ingredient[0].itemReference.display)
            assertFalse(filtered.ingredient[1].itemCodeableConcept.hasText())
        }

        @Test
        fun `rejects a profile whose slice cannot be told apart`() {
            val profile = profileOf("Medication", element("Medication.code.coding:atc-de", max = "0", label = REDACT))

            assertThrows(UnsupportedProfileException::class.java) { PrivacyFilter(profile, testPseudonymizer) }
        }

        @Test
        fun `recognises a slice by a pattern value as well as a fixed one`() {
            val profile = profileOf(
                "Medication",
                element("Medication.code.coding:atc-de", max = "0", label = REDACT),
                element("Medication.code.coding:atc-de.system") { pattern = UriType(ATC) },
            )
            val medication = Medication()
            medication.code.addCoding(Coding(PZN, "9717395", null)).addCoding(Coding(ATC, "A02AD10", null))

            val filtered = PrivacyFilter(profile, testPseudonymizer).filter(medication).resource as Medication

            assertEquals(listOf(PZN), filtered.code.coding.map { it.system })
        }
    }

    @Nested
    inner class Anonymize {

        @Test
        fun `replaces a code with the placeholder code`() {
            val request = MedicationRequest().setStatus(MedicationRequest.MedicationRequestStatus.COMPLETED)

            val filtered = filterWithResearchProfile(request).resource as MedicationRequest

            assertEquals(MedicationRequest.MedicationRequestStatus.UNKNOWN, filtered.status)
        }

        @Test
        fun `replaces a string with the placeholder string`() {
            val dispense = MedicationDispense()
            dispense.addPerformer().actor = Reference().setDisplay("gematik Apotheke")

            val filtered = filterWithResearchProfile(dispense).resource as MedicationDispense

            assertEquals("ANONYMIZED", filtered.performerFirstRep.actor.display)
        }

        @Test
        fun `writes the placeholder where the value was missing`() {
            val dispense = MedicationDispense()
            dispense.addPerformer().actor = Reference("Organization/1")

            val filtered = filterWithResearchProfile(dispense).resource as MedicationDispense

            assertEquals("ANONYMIZED", filtered.performerFirstRep.actor.display)
            assertEquals(MedicationDispense.MedicationDispenseStatus.UNKNOWN, filtered.status)
        }

        @Test
        fun `does not invent a parent element to hold the placeholder`() {
            val filtered = filterWithResearchProfile(MedicationDispense()).resource as MedicationDispense

            assertFalse(filtered.hasPerformer())
        }

        @Test
        fun `rejects a profile without a placeholder`() {
            val profile = profileOf("Medication", element("Medication.code.text", label = privacyLabel("ANONY")))

            assertThrows(UnsupportedProfileException::class.java) { PrivacyFilter(profile, testPseudonymizer) }
        }
    }

    @Nested
    inner class Pseudonymize {

        @Test
        fun `replaces a reference by the job number alone`() {
            val request = MedicationRequest()
            request.subject = Reference("Patient/12c0c2a4").setDisplay("Erika Mustermann")
            request.subject.identifier = Identifier().setSystem(KVID).setValue(KVNR)

            val filtered = filterWithResearchProfile(request).resource as MedicationRequest

            val expected = Reference().setIdentifier(Identifier().setSystem(SYSTEM_JOB_NUMBER).setValue(JOB_NUMBER))
            assertTrue(filtered.subject.equalsDeep(expected)) { filtered.subject.toPrettyString() }
        }

        @Test
        fun `fails closed when the reference has no identifier to derive the job number from`() {
            val request = MedicationRequest().setSubject(Reference("Patient/12c0c2a4"))

            val result = filterWithResearchProfile(request)

            assertNull(result.resource)
            assertEquals(PrivacyLogSeverity.ERROR, result.log.single { it.path == "MedicationRequest.subject" }.severity)
        }

        @Test
        fun `replaces an identifier element by the job number`() {
            val profile = profileOf("Patient", element("Patient.identifier", label = PSEUD))
            val patient = Patient()
            patient.addIdentifier().setSystem(KVID).setValue(KVNR)

            val filtered = PrivacyFilter(profile, testPseudonymizer).filter(patient).resource as Patient

            assertEquals(SYSTEM_JOB_NUMBER, filtered.identifierFirstRep.system)
            assertEquals(JOB_NUMBER, filtered.identifierFirstRep.value)
        }

        @Test
        fun `fails closed on a type it cannot pseudonymize`() {
            val profile = profileOf("Medication", element("Medication.code.text", label = PSEUD))
            val medication = Medication().setCode(CodeableConcept().setText("Maaloxan"))

            val result = PrivacyFilter(profile, testPseudonymizer).filter(medication)

            assertNull(result.resource)
        }
    }

    @Nested
    inner class Extensions {
        private val declared = "https://gematik.de/fhir/epa-medication/StructureDefinition/multiple-prescription-extension"
        private val undeclared = "https://example.org/StructureDefinition/free-text"

        @Test
        fun `drops undeclared extensions and keeps declared ones with their content`() {
            val request = MedicationRequest()
            request.addExtension(Extension(declared).apply { addExtension("indicator", BooleanType(false)) })
            request.addExtension(undeclared, StringType("Freitext"))

            val filtered = filterWithResearchProfile(request).resource as MedicationRequest

            assertEquals(listOf(declared), filtered.extension.map { it.url })
            assertTrue(filtered.extension.single().hasExtension("indicator"))
        }

        @Test
        fun `drops modifier extensions`() {
            val request = MedicationRequest()
            request.addModifierExtension(Extension(undeclared, StringType("Freitext")))

            val filtered = filterWithResearchProfile(request).resource as MedicationRequest

            assertFalse(filtered.hasModifierExtension())
        }

        @Test
        fun `drops extensions on nested elements`() {
            val request = MedicationRequest()
            request.addDosageInstruction().addExtension(undeclared, StringType("Freitext"))
            request.dispenseRequest.quantity.addExtension(undeclared, StringType("Freitext"))

            val filtered = filterWithResearchProfile(request).resource as MedicationRequest

            assertFalse(filtered.dosageInstructionFirstRep.hasExtension())
            assertFalse(filtered.dispenseRequest.quantity.hasExtension())
        }

        @Test
        fun `drops extensions on primitive values`() {
            val request = MedicationRequest()
            request.authoredOnElement.setValueAsString("2025-09-06")
            request.authoredOnElement.addExtension(undeclared, StringType("Freitext"))

            val filtered = filterWithResearchProfile(request).resource as MedicationRequest

            assertEquals("2025-09-06", filtered.authoredOnElement.valueAsString)
            assertFalse(filtered.authoredOnElement.hasExtension())
        }

        @Test
        fun `keeps an extension a nested element declares`() {
            val profile = profileOf(
                "MedicationRequest",
                element("MedicationRequest.dosageInstruction.extension:kept") { addType().addProfile(declared) },
            )
            val request = MedicationRequest()
            val dosage = request.addDosageInstruction()
            dosage.addExtension(declared, StringType("kept"))
            dosage.addExtension(undeclared, StringType("dropped"))

            val filtered = PrivacyFilter(profile, testPseudonymizer).filter(request).resource as MedicationRequest

            assertEquals(listOf(declared), filtered.dosageInstructionFirstRep.extension.map { it.url })
        }

        @Test
        fun `drops an extension slice the profile forbids`() {
            val profile = profileOf(
                "MedicationRequest",
                element("MedicationRequest.extension:forbidden", max = "0") { addType().addProfile(declared) },
            )
            val request = MedicationRequest()
            request.addExtension(declared, StringType("x"))

            val result = PrivacyFilter(profile, testPseudonymizer).filter(request)

            assertFalse((result.resource as MedicationRequest).hasExtension())
        }
    }

    @Nested
    inner class Narrative {

        @Test
        fun `removes the narrative`() {
            val request = MedicationRequest()
            request.text.setStatus(NarrativeStatus.GENERATED).setDivAsString("<div>Verordnung für Erika Mustermann</div>")

            val filtered = filterWithResearchProfile(request).resource as MedicationRequest

            assertFalse(filtered.hasText())
        }
    }

    @Nested
    inner class Contained {
        private val sourceProfile = "https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication-pzn-ingredient"
        private val researchProfile = "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication"

        private fun medicationWithContainedIngredient(): Medication {
            val ingredient = Medication().setCode(CodeableConcept().setText("Magnesiumhydroxid"))
            ingredient.id = "wirkstoff"
            ingredient.meta.addProfile(sourceProfile)
            ingredient.code.addCoding(Coding(PZN, "9717395", null)).addCoding(Coding(ATC, "A02AA04", null))
            ingredient.addExtension("https://example.org/StructureDefinition/free-text", StringType("Freitext"))

            val medication = Medication()
            medication.addContained(ingredient)
            medication.addIngredient().item = Reference("#wirkstoff")
            return medication
        }

        @Test
        fun `applies the research profile for its resource type to a contained medication`() {
            val filtered = filterWithResearchProfile(medicationWithContainedIngredient()).resource as Medication

            val ingredient = filtered.contained.single() as Medication
            assertFalse(ingredient.code.hasText())
            assertEquals(listOf(PZN), ingredient.code.coding.map { it.system })
            assertFalse(ingredient.hasExtension())
        }

        @Test
        fun `keeps the contained medication referable`() {
            val filtered = filterWithResearchProfile(medicationWithContainedIngredient()).resource as Medication

            assertEquals("#wirkstoff", filtered.ingredientFirstRep.itemReference.reference)
            assertEquals("wirkstoff", filtered.contained.single().idElement.idPart)
        }

        @Test
        fun `records the source profile of a contained medication but gives it no security label`() {
            val filtered = filterWithResearchProfile(medicationWithContainedIngredient()).resource as Medication

            val meta = filtered.contained.single().meta
            assertEquals(listOf(researchProfile), meta.profile.map { it.value })
            assertEquals(listOf(sourceProfile), meta.tag.map { it.code })
            assertFalse(meta.hasSecurity())
        }

        @Test
        fun `logs the rules applied to a contained medication under the contained path`() {
            val log = filterWithResearchProfile(medicationWithContainedIngredient()).log

            val entry = log.single { it.path == "Medication.contained > Medication.code.text" }
            assertEquals(1, entry.affectedValues)
        }

        @Test
        fun `removes contained resources where the profile forbids them`() {
            val request = MedicationRequest()
            request.addContained(Medication().setCode(CodeableConcept().setText("Maaloxan")))

            val filtered = filterWithResearchProfile(request).resource as MedicationRequest

            assertFalse(filtered.hasContained())
        }

        @Test
        fun `fails closed on a contained resource of another type`() {
            val medication = Medication()
            medication.addContained(Patient().addName(HumanName().setFamily("Mustermann")))

            val result = filterWithResearchProfile(medication)

            assertNull(result.resource)
            assertEquals(PrivacyLogSeverity.ERROR, result.log.single { it.path == "Medication.contained" }.severity)
        }

        @Test
        fun `fails closed when no research profile is known for a contained resource`() {
            val profile = profileOf("Medication", element("Medication.code.text", max = "0", label = REDACT))
            val medication = Medication()
            medication.addContained(Medication().setCode(CodeableConcept().setText("Maaloxan")))

            assertNull(PrivacyFilter(profile, testPseudonymizer).filter(medication).resource)
        }

        @Test
        fun `applies the research profile of the contained type, not the one of the container`() {
            val profile = profileOf("MedicationRequest", element("MedicationRequest.note", max = "0", label = REDACT))
            val request = MedicationRequest()
            request.addContained(Medication().setCode(CodeableConcept().setText("Maaloxan")))

            val filtered = PrivacyFilter(profile, testPseudonymizer, ::researchProfileOrNullFor).filter(request)

            val medication = (filtered.resource as MedicationRequest).contained.single() as Medication
            assertFalse(medication.code.hasText())
            assertEquals(listOf(researchProfile), medication.meta.profile.map { it.value })
        }
    }

    @Nested
    inner class Meta {
        private val sourceProfile = "https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication"
        private val researchProfile = "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication"

        @Test
        fun `swaps the source profile for the research profile and records it as a tag`() {
            val medication = Medication()
            medication.meta.addProfile(sourceProfile)

            val meta = filterWithResearchProfile(medication).resource!!.meta

            assertEquals(listOf(researchProfile), meta.profile.map { it.value })
            assertEquals(listOf(SYSTEM_SOURCE_PROFILE to sourceProfile), meta.tag.map { it.system to it.code })
        }

        @Test
        fun `marks the resource as pseudonymized`() {
            val meta = filterWithResearchProfile(Medication()).resource!!.meta

            assertEquals(listOf(SYSTEM_PSEUDED to CODE_PSEUDED), meta.security.map { it.system to it.code })
        }

        @Test
        fun `filtering an already filtered resource does not pile up tags and labels`() {
            val medication = Medication()
            medication.meta.addProfile(sourceProfile)
            val once = filterWithResearchProfile(medication).resource!!

            val twice = filterWithResearchProfile(once).resource!!

            assertEquals(1, twice.meta.security.size)
            assertEquals(listOf(researchProfile), twice.meta.tag.map { it.code })
        }

        @Test
        fun `keeps tags that are not source profile tags`() {
            val medication = Medication()
            medication.meta.addTag("https://example.org/tags", "keep", null)

            val meta = filterWithResearchProfile(medication).resource!!.meta

            assertEquals(listOf("keep"), meta.tag.map { it.code })
        }
    }

    @Nested
    inner class Contract {

        @Test
        fun `does not change the resource it is given`() {
            val medication = Medication().setCode(CodeableConcept().setText("Maaloxan"))

            filterWithResearchProfile(medication)

            assertEquals("Maaloxan", medication.code.text)
            assertFalse(medication.meta.hasSecurity())
        }

        @Test
        fun `refuses a resource of another type than the profile`() {
            val medicationProfile = researchProfileFor(Medication())

            assertThrows(IllegalArgumentException::class.java) {
                PrivacyFilter(medicationProfile, testPseudonymizer).filter(MedicationRequest())
            }
        }

        @Test
        fun `rejects a profile with an unknown obligation policy`() {
            val profile = profileOf("Medication", element("Medication.code.text", label = privacyLabel("ENCRYPT")))

            assertThrows(UnsupportedProfileException::class.java) { PrivacyFilter(profile, testPseudonymizer) }
        }

        @Test
        fun `returns no resource when a rule fails`() {
            val request = MedicationRequest().setSubject(Reference("Patient/12c0c2a4"))
            request.addNote().text = "would otherwise be exported half filtered"

            assertNull(filterWithResearchProfile(request).resource)
        }

        @Test
        fun `uses a code placeholder for a code element`() {
            val profile = profileOf(
                "MedicationRequest",
                element("MedicationRequest.intent", label = privacyLabel("ANONY", CodeType("order"))),
            )
            val request = MedicationRequest().setIntent(MedicationRequest.MedicationRequestIntent.PLAN)

            val filtered = PrivacyFilter(profile, testPseudonymizer).filter(request).resource as MedicationRequest

            assertEquals(MedicationRequest.MedicationRequestIntent.ORDER, filtered.intent)
        }
    }
}
