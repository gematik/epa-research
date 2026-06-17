// Redacted version of full EPA MedicationRequest
Instance: ExampleEPAResearchMedicationRequestMaaloxan
InstanceOf: EPAResearchMedicationRequest
Usage: #example
Description: "Maaloxan MedicationRequest after EPAResearchMedicationRequest redaction rules."

* meta.tag[TagSourceProfile].code = #"https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication-request"
* meta.security = http://terminology.hl7.org/CodeSystem/v3-ObservationValue#PSEUDED "pseudonymized"

// Explicitly defined extension slices are copied; others are filtered out.
* extension[multiplePrescription].extension[indicator].valueBoolean = false
* extension[isBvg].valueBoolean = false

* status = #completed
* intent = #filler-order

* medicationReference = Reference(ExampleEPAResearchMedicationMaaloxan)

* subject.identifier.value = "PSEUDO-X110411319-9F2C"

* authoredOn = "2025-09-06"

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
