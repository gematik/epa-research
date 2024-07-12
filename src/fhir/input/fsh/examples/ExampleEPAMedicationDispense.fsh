Instance: ExampleEPAMedicationDispenseMedication
InstanceOf: EPAMedication
Usage: #example
* id = "6b0350b6-f5ab-495b-9aeb-1806170a814f"
* extension[rxPrescriptionProcessIdentifier].valueIdentifier.value = "160.153.303.257.459_20240122"
* identifier[EPAMedicationUniqueIdentifier].value = "583DC39E2BFFBFD6C05AE0AC404516309F56729EA44813F9FB881C73EE58EA5D"
* identifier[RxOriginatorProcessIdentifier].value = "6ae6a7ca-c9b5-46bf-9411-2ba49d96f988_160.153.303.257.459"
* status = #active
* code.coding
  * system = $cs-ask
  * code = #5682
  * display = "Ibuprofen"

Instance: ExampleEPAMedicationDispensePerformer
InstanceOf: OrganizationDirectory

* id = "e6b0cd54-a6f2-4b8c-ad68-76c3ae953b3e"
* meta.tag[Origin] = Origin#ldap
* name = "Example EPA Pharmacy"
* identifier[TelematikID].value = "3-21234567890"
* type = OrganizationProfessionOID#1.2.276.0.76.4.54 "Öffentliche Apotheke"

Instance: ExampleEPAMedicationDispense
InstanceOf: EPAMedicationDispense
Usage: #example

* id = "cd6c2892-eef2-4eed-8b82-1b15e8e0fd4f"
* status = medicationdispense-status#completed
* performer.actor = Reference(ExampleEPAMedicationDispensePerformer)
* medicationReference = Reference(ExampleEPAMedicationDispenseMedication)
* subject.identifier.value = "X123456789"
* whenHandedOver[+] = 2025-01-15

Instance: ExampleEPAMedicationDispenseBundle
InstanceOf: Bundle
Usage: #example

* type = #collection
* entry[+].resource = ExampleEPAMedicationDispense
* entry[+].resource = ExampleEPAMedicationDispenseMedication
* entry[+].resource = ExampleEPAMedicationDispensePerformer