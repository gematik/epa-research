Profile: EPAResearchMedication
Id: epa-research-medication
Parent: Medication
Title: "Medication-Profil für ePA-Forschungsdaten"
Description: "Pseudonymisiertes Gegenstück zu EPAMedication für den ePA-Forschungsdatensatz. Identifizierende Elemente werden entfernt oder anonymisiert; nur explizit deklarierte Extensions werden aus der Quellinstanz übernommen."

* insert Meta
* insert MetaSourceProfile

// The narrative repeats the content of the resource in free text, including what is redacted below.
* text 0..0
  * insert PrivacyLabelRedact

// Contained medications (PZN ingredients, pharmaceutical products) are pseudonymised by the same rules.
* contained only EPAResearchMedication

// only Medication's status is 0..1 and can be redacted to absence
* status 0..0
  * insert PrivacyLabelRedact

* code.coding ^slicing.discriminator.type = #value
* code.coding ^slicing.discriminator.path = "$this"
* code.coding ^slicing.rules = #open

* code.coding contains atc-de 0..0
// REDACT is attached to the atc-de slice only — not to code.coding itself.
// Only the ATC-DE coding is removed; PZN and ASK codings are retained intentionally.
* code.coding[atc-de]
  * insert PrivacyLabelRedact
* code.coding[atc-de].system = "http://fhir.de/CodeSystem/bfarm/atc"

* code.text 0..0
  * insert PrivacyLabelRedact

* ingredient.itemCodeableConcept.coding ^slicing.discriminator.type = #value
* ingredient.itemCodeableConcept.coding ^slicing.discriminator.path = "$this"
* ingredient.itemCodeableConcept.coding ^slicing.rules = #open

* ingredient.itemCodeableConcept.coding contains atc-de 0..0
* ingredient.itemCodeableConcept.coding[atc-de]
  * insert PrivacyLabelRedact
* ingredient.itemCodeableConcept.coding[atc-de].system = "http://fhir.de/CodeSystem/bfarm/atc"

* ingredient.itemCodeableConcept.text 0..0
  * insert PrivacyLabelRedact

// Explicitly defined extension slices are copied; others are filtered out.
// packaging and manufacturingInstructions are free text and stay filtered.
* extension contains
  MedicationIsVaccineExtension named isVaccine 0..1 MS and
  DrugCategoryExtension named drugCategory 0..1 MS and
  ExtensionNormgroesseDeBasis named normSizeCode 0..1 MS and
  EPAMedicationTypeExtension named type 0..1 MS
