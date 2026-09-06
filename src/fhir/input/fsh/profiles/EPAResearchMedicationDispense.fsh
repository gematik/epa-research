Profile: EPAResearchMedicationDispense
Id: epa-research-medication-dispense
Parent: MedicationDispense
Title: "MedicationDispense-Profil für ePA-Forschungsdaten"
Description: "Pseudonymisiertes Gegenstück zu EPAMedicationDispense für den ePA-Forschungsdatensatz. Patientenbezogene Referenzen werden pseudonymisiert; identifizierende Sub-Felder strukturell erhaltener Elemente werden anonymisiert oder entfernt; nur explizit deklarierte Extensions werden aus der Quellinstanz übernommen."

* insert Meta
* insert MetaSourceProfile

// status ist 1..1 mit required binding — REDACT ist nicht möglich; der Platzhalter
// muss ein gültiger Code aus medicationdispense-status sein.
* status = #unknown
  * insert PrivacyLabelAnonymizeCode(unknown)

* subject
  * insert PrivacyLabelPseudonymize
* subject.identifier only EPAResearchJobNumberIdentifier

// performer is 1..1 in upstream EPAMedicationDispense — cannot be redacted to absence.
// Approach 1 (leaf-level ANONY): keep the Reference structure, anonymize the
// human-readable display, and redact the identifying sub-fields.
* performer.actor.display = "ANONYMIZED"
  * insert PrivacyLabelAnonymizeString(ANONYMIZED)
* performer.actor.reference 0..0
  * insert PrivacyLabelRedact
* performer.actor.identifier 0..0
  * insert PrivacyLabelRedact

* dosageInstruction.text 0..0
  * insert PrivacyLabelRedact

* dosageInstruction.patientInstruction 0..0
  * insert PrivacyLabelRedact

// Explicitly defined extension slices are copied; others are filtered out.
