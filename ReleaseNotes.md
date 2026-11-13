<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/> <br/>    
 
# Release Notes ePA Research
## Release 1.3.0
- package de.gematik.epa.research 1.3.0
### changes
- all research profiles: the narrative (`text`) is redacted
- EPAResearchMedication
  - contained medications must conform to EPAResearchMedication and are pseudonymised by the same rules
  - `code.coding[atc-de]` and `ingredient.itemCodeableConcept.coding[atc-de]` are redacted again
  - `manufacturer` and `batch.lotNumber` are no longer redacted
  - extensions `isVaccine`, `drugCategory`, `normSizeCode` and `type` are retained
- EPAResearchMedicationRequest
  - `recorder`, `dispenseRequest.performer` and `contained` are redacted
  - extension `isBvg` is no longer retained
- EPAResearchMedicationDispense
  - `location`, `destination`, `receiver`, `note`, `substitution.responsibleParty` and `contained` are redacted
## Release 3.1.0
- release ePA-3.1.0
## Release 3.1.0-rc
- release candidate ePA-3.1.0
### changes
- Work in progress