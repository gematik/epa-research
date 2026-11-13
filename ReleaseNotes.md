<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/> <br/>    
 
# Release Notes ePA Research
## Release 1.3.1
- package de.gematik.epa.research 1.3.1
- guide (Leitfaden) 1.1.2
### changes
- all research profiles: the narrative (`text`) is redacted
- EPAResearchMedication
  - `code.coding[atc-de]` and `ingredient.itemCodeableConcept.coding[atc-de]` are redacted again
  - `form.text` is redacted
  - `manufacturer` and `batch.lotNumber` are no longer redacted
  - extensions `isVaccine`, `drugCategory`, `normSizeCode` and `type` are retained
  - contained resources are pseudonymised by the research profile for their resource type, as described in the guide
- EPAResearchMedicationRequest
  - `subject.display`, `recorder`, `dispenseRequest.performer` and `contained` are redacted
  - extension `isBvg` is no longer retained
- EPAResearchMedicationDispense
  - `subject.display`, `location`, `destination`, `receiver`, `note`, `substitution.responsibleParty` and `contained` are redacted
- guide (Leitfaden)
  - `PSEUD` on a `Reference(Patient)`: `display` is always removed; the profiles mark it with `REDACT`
  - extensions are filtered at every level of a resource, and contained resources are pseudonymised like standalone ones

## Release 1.2.0
- package de.gematik.epa.research 1.2.0
### changes
- `meta.source` wird bei allen Profilen entfernt (REDACT)
- `EPAResearchMedicationRequest.status` wird auf `#unknown` anonymisiert (ANONY)
- `EPAResearchMedicationDispense.status` wird auf `#unknown` anonymisiert (ANONY)
- `EPAResearchMedication.status` wird entfernt (REDACT)
- `EPAResearchMedication`: ATC-DE-, EDQM- und KBV-Darreichungsform-Codings bleiben erhalten; redigiert werden nur noch `code.text`, `ingredient.itemCodeableConcept.text` und `manufacturer`
- Es wurde klargestellt, dass ANONY auch code-basierte Werte unterstützen soll, nicht nur String-Primitive: neues RuleSet `PrivacyLabelAnonymizeCode` gibt den Platzhalter als `valueCode` statt `valueString` mit. Bei required/extensible Binding muss der Platzhalter ein gültiger Code des gebundenen ValueSets sein.
