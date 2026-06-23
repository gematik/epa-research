Profile: EPAResearchMedicationRequest
Id: epa-research-medication-request
Parent: MedicationRequest
Title: "MedicationRequest-Profil für ePA-Forschungsdaten"
Description: "Pseudonymisiertes Gegenstück zu EPAMedicationRequest für den ePA-Forschungsdatensatz. Patientenbezogene Referenzen werden pseudonymisiert; identifizierende und freitextliche Elemente werden entfernt; nur explizit deklarierte Extensions werden aus der Quellinstanz übernommen."

* insert Meta
* insert MetaSourceProfile

* subject
  * insert PrivacyLabelPseudonymize
* subject.identifier only EPAResearchJobNumberIdentifier

* requester 0..0
  * insert PrivacyLabelRedact

* note 0..0
  * insert PrivacyLabelRedact

* dosageInstruction.text 0..0
  * insert PrivacyLabelRedact

* dosageInstruction.patientInstruction 0..0
  * insert PrivacyLabelRedact

// Explicitly defined extension slices are copied; others are filtered out.
* extension contains
  MultiplePrescriptionExtension named multiplePrescription 0..1 MS and
  IndicatorBVGExtension named isBvg 0..1 MS
