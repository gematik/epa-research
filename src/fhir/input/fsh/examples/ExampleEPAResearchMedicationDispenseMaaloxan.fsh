// Redacted version of full EPA MedicationDispense
Instance: ExampleEPAResearchMedicationDispenseMaaloxan
InstanceOf: EPAResearchMedicationDispense
Usage: #example
Description: "Maaloxan MedicationDispense after EPAResearchMedicationDispense redaction rules."

* meta.tag[TagSourceProfile].code = #"https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication-dispense"
* meta.security = http://terminology.hl7.org/CodeSystem/v3-ObservationValue#PSEUDED "pseudonymized"

// identifier carries no privacy label and is therefore preserved unchanged.
* identifier[0].system = "https://gematik.de/fhir/epa-medication/sid/rx-originator-process-identifier"
* identifier[0].value = "3a534e48-80e8-4bbb-a19e-43e83d6f3a3e_160.100.000.000.010.12"

* status = #unknown

* medicationReference = Reference(ExampleEPAResearchMedicationMaaloxan)

// performer is 1..1 in upstream — anonymized: actor.display fixed to
// "ANONYMIZED" by the profile, identifying sub-fields removed.
* performer.actor.display = "ANONYMIZED"

* subject.identifier.value = "PSEUDO-X110411319-9F2C"

* authorizingPrescription = Reference(ExampleEPAResearchMedicationRequestMaaloxan)

* quantity.value = 1
* quantity.unit = "Packung"

* whenHandedOver = "2025-10-29"

* substitution.wasSubstituted = false
