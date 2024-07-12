Profile: ConfidentialPractitioner
Parent: Practitioner

* id
  * insert PrivacyLabelPseudonymize
* identifier
  * insert PrivacyLabelPseudonymize
* name
  * insert PrivacyLabelRedact


Instance: ExampleConfidentialMedicationRequest1Practitioner
InstanceOf: ConfidentialPractitioner
Usage: #example

* identifier.system = "https://gematik.de/fhir/sid/telematik-id"
* identifier.value = "1-10123456789"
* name.family = "Mustermann"
* name.given[+] = "Max"

Profile: ConfidentialPractitionerRole
Parent: PractitionerRole

* id
  * insert PrivacyLabelPseudonymize
* identifier
  * insert PrivacyLabelPseudonymize

Instance: ExampleConfidentialMedicationRequest1Requester
InstanceOf: ConfidentialPractitionerRole
Usage: #example

* practitioner = Reference(ExampleConfidentialMedicationRequest1Practitioner)

Profile: ConfidentialMedicationRequest
Parent: MedicationRequest

* subject.identifier
  * insert PrivacyLabelPseudonymize
* requester.reference
  * insert PrivacyLabelPseudonymize

Instance: ExampleConfidentialMedicationRequest1
InstanceOf: ConfidentialMedicationRequest
Usage: #example

* status = medicationrequest-status#active
* intent = http://hl7.org/fhir/CodeSystem/medicationrequest-intent#plan
* medicationReference = Reference(ExampleEpaMedication1)
* subject.identifier.vtagalue = "X123456789"
* authoredOn = 2024-05-04
* dispenseRequest.quantity.value = 3
* requester = Reference(ExampleConfidentialMedicationRequest1Requester)

Instance: ExampleConfidentialMedicationRequest1Bundle
InstanceOf: Bundle
Usage: #example

* type = bundle-type#collection
* entry[+].resource = ExampleConfidentialMedicationRequest1
* entry[+].resource = ExampleConfidentialPatient1
* entry[+].resource = ExampleConfidentialMedicationRequest1Requester
* entry[+].resource = ExampleConfidentialMedicationRequest1Practitioner
