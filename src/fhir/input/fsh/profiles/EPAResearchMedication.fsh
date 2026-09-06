Profile: EPAResearchMedication
Id: epa-research-medication
Parent: Medication
Title: "Medication-Profil für ePA-Forschungsdaten"
Description: "Pseudonymisiertes Gegenstück zu EPAMedication für den ePA-Forschungsdatensatz. Identifizierende Elemente werden entfernt oder anonymisiert; nur explizit deklarierte Extensions werden aus der Quellinstanz übernommen."

* insert Meta
* insert MetaSourceProfile

// only Medication's status is 0..1 and can be redacted to absence
* status 0..0
  * insert PrivacyLabelRedact

* code.text 0..0
  * insert PrivacyLabelRedact

* manufacturer 0..0
  * insert PrivacyLabelRedact

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
