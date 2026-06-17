// Full MedicationDispense for Maaloxan — pre-redaction reference.
Instance: ExampleEPAMedicationDispenseMaaloxan
InstanceOf: EPAMedicationDispense
Usage: #example
Description: "Full Maaloxan MedicationDispense — pre-redaction reference."

// Explicitly defined extension slices are copied; others are filtered out.
* extension[rxPrescriptionProcessIdentifier].valueIdentifier.value = "160.100.000.000.010.12_20251027"
* extension[renderedDosageInstruction].valueMarkdown = "1-0-0-0 Stück"
* extension[generatedDosageInstructionsMeta]
  * extension[algorithmVersion].valueString = "1.0.1"
  * extension[language].valueCode = #de-DE

* identifier[RxOriginatorProcessIdentifier].value = "3a534e48-80e8-4bbb-a19e-43e83d6f3a3e_160.100.000.000.010.12"

* status = #completed

* medicationReference = Reference(ExampleEPAMedicationMaaloxan)

* subject.reference = "Patient/12c0c2a4-71a2-4d49-9e6f-9b7a3b4f0d11"
* subject.identifier.system = "http://fhir.de/sid/gkv/kvid-10"
* subject.identifier.value = "X110411319"

* performer.actor.reference = "Organization/151f1697-7512-4e21-9466-1b75207475d8"
* performer.actor.identifier
  * type.coding.system = "http://terminology.hl7.org/CodeSystem/v2-0203"
  * type.coding.code = #PRN
  * type.coding.display = "Provider number"
  * system = "https://gematik.de/fhir/sid/telematik-id"
  * value = "9-2.58.00000023"
* performer.actor.display = "gematik Apotheke"

* authorizingPrescription = Reference(ExampleEPAMedicationRequestMaaloxan)

* quantity.value = 1
* quantity.unit = "Packung"

* whenHandedOver = "2025-10-29"

* dosageInstruction.text = "Morgens 1 Tablette einnehmen, mit etwas Wasser."

* substitution.wasSubstituted = false
