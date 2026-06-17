Profile: EPAResearchMedication
Id: epa-research-medication
Parent: Medication
Title: "Medication-Profil für ePA-Forschungsdaten"
Description: "Pseudonymisiertes Gegenstück zu EPAMedication für den ePA-Forschungsdatensatz. Identifizierende Elemente werden entfernt oder anonymisiert; nur explizit deklarierte Extensions werden aus der Quellinstanz übernommen."

* insert Meta
* insert MetaSourceProfile

* code.coding ^slicing.discriminator.type = #value
* code.coding ^slicing.discriminator.path = "$this"
* code.coding ^slicing.rules = #open

* code.coding contains atc-de 0..0
// Convention: attach PrivacyLabel to the slice itself
* code.coding[atc-de]
  * insert PrivacyLabelRedact
* code.coding[atc-de].system = "http://fhir.de/CodeSystem/bfarm/atc"

* code.text 0..0
  * insert PrivacyLabelRedact

* manufacturer 0..0
  * insert PrivacyLabelRedact

* form.coding ^slicing.discriminator.type = #value
* form.coding ^slicing.discriminator.path = "$this"
* form.coding ^slicing.rules = #open

* form.coding contains edqm 0..0 and kbvDarreichungsform 0..0
* form.coding[edqm]
  * insert PrivacyLabelRedact
* form.coding[edqm].system = "http://standardterms.edqm.eu"
* form.coding[kbvDarreichungsform]
  * insert PrivacyLabelRedact
* form.coding[kbvDarreichungsform].system = "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_KBV_DARREICHUNGSFORM"

* ingredient.itemCodeableConcept.coding ^slicing.discriminator.type = #value
* ingredient.itemCodeableConcept.coding ^slicing.discriminator.path = "$this"
* ingredient.itemCodeableConcept.coding ^slicing.rules = #open

* ingredient.itemCodeableConcept.coding contains atc-de 0..0
* ingredient.itemCodeableConcept.coding[atc-de]
  * insert PrivacyLabelRedact
* ingredient.itemCodeableConcept.coding[atc-de].system = "http://fhir.de/CodeSystem/bfarm/atc"

* ingredient.itemCodeableConcept.text 0..0
  * insert PrivacyLabelRedact

* batch.lotNumber 0..0
  * insert PrivacyLabelRedact

// Explicitly defined extension slices are copied; others are filtered out.
// * extension contains
//   MedicationIsVaccineExtension named isVaccine 0..1 and
//   DrugCategoryExtension named drugCategory 0..1 and
//   ExtensionNormgroesseDeBasis named normSizeCode 0..1 and
//   MedicationFormulationPackagingExtension named packaging 0..1 and
//   MedicationManufacturingInstructionsExtension named manufacturingInstructions 0..1 and
//   EPAMedicationTypeExtension named type 0..1 MS
