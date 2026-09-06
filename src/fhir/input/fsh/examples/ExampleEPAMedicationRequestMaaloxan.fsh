// Full Maaloxan MedicationRequest — pre-redaction reference.
Instance: ExampleEPAMedicationRequestMaaloxan
InstanceOf: EPAMedicationRequest
Usage: #example
Description: "Full Maaloxan MedicationRequest — pre-redaction reference."

// meta.source names the originating system and is therefore redacted.
* meta.source = "https://praxis-mustermann.example/fhir"

// Explicitly defined extension slices are copied; others are filtered out.
* extension[multiplePrescription].extension[indicator].valueBoolean = false
* extension[isBvg].valueBoolean = false
* extension[renderedDosageInstruction].valueMarkdown = "1-0-0-0 Stück"
* extension[generatedDosageInstructionsMeta]
  * extension[algorithmVersion].valueString = "1.0.1"
  * extension[language].valueCode = #de-DE

* identifier[RxPrescriptionProcessIdentifier].value = "160.100.000.000.011.09_20250906"
* identifier[RxOriginatorProcessIdentifier].value = "59b9fa64-4ca1-4bd2-8388-652d0fec32b2_160.100.000.000.011.09"

* status = #completed
* intent = #filler-order

* medicationReference = Reference(ExampleEPAMedicationMaaloxan)

* subject.reference = "Patient/12c0c2a4-71a2-4d49-9e6f-9b7a3b4f0d11"
* subject.identifier.system = "http://fhir.de/sid/gkv/kvid-10"
* subject.identifier.value = "X110411319"

* authoredOn = "2025-09-06"

* requester.reference = "PractitionerRole/73a551f8-d8cd-4b44-823d-ab5f8aeab1aa"
* requester.display = "Dr. Max Manfred Mustermann / Praxis Dr. med. Max Mustermann"

* dosageInstruction.text = "Morgens 1 Tablette einnehmen"
* dosageInstruction.timing.repeat
  * frequency = 1
  * period = 1
  * periodUnit = #d
  * when = #MORN
* dosageInstruction.doseAndRate.doseQuantity.value = 1
* dosageInstruction.doseAndRate.doseQuantity.unit = "Stück"
* dosageInstruction.doseAndRate.doseQuantity.system = "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_BMP_DOSIEREINHEIT"
* dosageInstruction.doseAndRate.doseQuantity.code = #1

* dispenseRequest.quantity.value = 1
* dispenseRequest.quantity.unit = "Packung"

* substitution.allowedBoolean = true

* note.text = "Verträglichkeitstest erforderlich."
