Instance: ExampleEPAMedicationRequestMedication
InstanceOf: EPAMedication
Usage: #example
* id = "50438bb0-34cd-4e48-a311-af6357f38681"
* extension[rxPrescriptionProcessIdentifier].valueIdentifier.value = "160.153.303.257.459_20240122"
* identifier[EPAMedicationUniqueIdentifier].value = "583DC39E2BFFBFD6C05AE0AC404516309F56729EA44813F9FB881C73EE58EA5D"
* identifier[RxOriginatorProcessIdentifier].value = "6ae6a7ca-c9b5-46bf-9411-2ba49d96f988_160.153.303.257.459"
* status = #active
* code.coding
  * system = $cs-ask
  * code = #5682
  * display = "Ibuprofen"

Instance: ExampleEPAMedicationRequest
InstanceOf: EPAMedicationRequest
Usage: #example

* id = "bf1b08e5-5cfe-4ec0-b6b3-580d4bdc7975"
* status = medicationrequest-status#active
* intent = http://hl7.org/fhir/CodeSystem/medicationrequest-intent#plan
* medicationReference = Reference(ExampleEPAMedicationRequestMedication)
* subject.identifier.value = "X123456789"
* authoredOn = 2024-07-01
* dispenseRequest.quantity.value = 3
* requester = Reference(ExampleEPAPractitionerRole)

Instance: ExampleEPAMedicationRequestBundle
InstanceOf: Bundle
Usage: #example

* type = #collection
* entry[+].resource = ExampleEPAMedicationRequest
* entry[+].resource = ExampleEPAMedicationRequestMedication
* entry[+].resource = ExampleEPAPractitionerRole
* entry[+].resource = ExampleEPAPractitioner
