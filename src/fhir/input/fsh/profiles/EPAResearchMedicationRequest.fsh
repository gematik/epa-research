Profile: EPAResearchMedicationRequest
Id: epa-research-medication-request
Parent: MedicationRequest
Title: "MedicationRequest-Profil für ePA-Forschungsdaten"
Description: "Pseudonymisiertes Gegenstück zu EPAMedicationRequest für den ePA-Forschungsdatensatz. Patientenbezogene Referenzen werden pseudonymisiert; identifizierende und freitextliche Elemente werden entfernt; nur explizit deklarierte Extensions werden aus der Quellinstanz übernommen."

* insert Meta
* insert MetaSourceProfile

// The narrative repeats the content of the resource in free text, including what is redacted below.
* text 0..0
  * insert PrivacyLabelRedact

// No research profile describes resources contained here, so none may be passed on.
* contained 0..0
  * insert PrivacyLabelRedact

// status ist 1..1 mit required binding — REDACT ist nicht möglich; der Platzhalter
// muss ein gültiger Code aus medicationrequest-status sein.
* status = #unknown
  * insert PrivacyLabelAnonymizeCode(unknown)

* subject
  * insert PrivacyLabelPseudonymize
* subject.identifier only EPAResearchJobNumberIdentifier

* requester 0..0
  * insert PrivacyLabelRedact

* recorder 0..0
  * insert PrivacyLabelRedact

* note 0..0
  * insert PrivacyLabelRedact

* dosageInstruction.text 0..0
  * insert PrivacyLabelRedact

* dosageInstruction.patientInstruction 0..0
  * insert PrivacyLabelRedact

* dispenseRequest.performer 0..0
  * insert PrivacyLabelRedact

// Explicitly defined extension slices are copied; others are filtered out.
* extension contains
  MultiplePrescriptionExtension named multiplePrescription 0..1 MS
