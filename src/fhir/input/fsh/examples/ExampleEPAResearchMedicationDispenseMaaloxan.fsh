// Redacted version of full EPA MedicationDispense
Instance: ExampleEPAResearchMedicationDispenseMaaloxan
InstanceOf: EPAResearchMedicationDispense
Usage: #example
Description: "Maaloxan MedicationDispense after EPAResearchMedicationDispense redaction rules."

* meta.tag[TagSourceProfile].code = #"https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication-dispense"
* meta.security = http://terminology.hl7.org/CodeSystem/v3-ObservationValue#PSEUDED "pseudonymized"

* status = #completed

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
