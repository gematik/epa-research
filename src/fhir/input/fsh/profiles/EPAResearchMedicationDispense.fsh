Profile: EPAResearchMedicationDispense
Id: epa-research-medication-dispense
Parent: MedicationDispense

* insert Meta
* insert MetaSourceProfile
  
* note 0..0
  * insert PrivacyLabelRedact
* subject.identifier
  * insert PrivacyLabelPseudonymize
* subject.reference 0..0
  * insert PrivacyLabelRedact
* performer 0..0
  * insert PrivacyLabelRedact
* location 0..0
  * insert PrivacyLabelRedact
* destination 0..0
  * insert PrivacyLabelRedact

// Extensiona
* extension contains RxPrescriptionProcessIdentifierExtension named rxPrescriptionProcessIdentifier 0..1
* extension[rxPrescriptionProcessIdentifier].value[x]
