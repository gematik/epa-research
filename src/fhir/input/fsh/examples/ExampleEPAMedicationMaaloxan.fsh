// Full Maaloxan EPAMedication — pre-redaction reference.
Instance: ExampleEPAMedicationMaaloxan
InstanceOf: EPAMedication
Usage: #example
Description: "Full Maaloxan EPAMedication — pre-redaction reference."

// Explicitly defined extension slices are copied; others are filtered out.
* extension[rxPrescriptionProcessIdentifier].valueIdentifier.system = "https://gematik.de/fhir/epa-medication/sid/rx-prescription-process-identifier"
* extension[rxPrescriptionProcessIdentifier].valueIdentifier.value = "160.100.000.000.011.09_20250906"
* extension[isVaccine].valueBoolean = false
* extension[drugCategory].valueCoding.system = "https://gematik.de/fhir/epa-medication/CodeSystem/epa-drug-category-cs"
* extension[drugCategory].valueCoding.code = #00
* extension[normSizeCode].valueCode = #N1
* extension[type].valueCoding.system = "http://snomed.info/sct"
* extension[type].valueCoding.code = #781405001
* extension[type].valueCoding.display = "Medicinal product package"
* extension[packaging].valueString = "Packung mit 30 Kautabletten"
* extension[manufacturingInstructions].valueString = "Bei Raumtemperatur trocken lagern."

// Upstream EPAMedication slices code.coding into pzn / atc-de / ask —
// reference each by its slice name. Contrast with the research example,
// which uses [+] array-index because EPAResearchMedication leaves these
// codings to flow through the inherited #open slicing.
* code.coding[pzn].code = #9717395
* code.coding[pzn].display = "Maaloxan® 25 mVal Sodbrennen Kautabletten Lemon"

* code.coding[atc-de].version = "2024"
* code.coding[atc-de].code = #A02AD10
* code.coding[atc-de].display = "Aluminiumoxid in Kombination mit Magnesiumhydroxid"

* code.coding[ask].code = #02250
* code.coding[ask].display = "Magnesiumhydroxid"

* code.text = "Maaloxan® 25 mVal Sodbrennen Kautabletten Lemon"

* status = #active

* manufacturer.display = "Sanofi-Aventis Deutschland GmbH"

* form.coding[+].system = "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_KBV_DARREICHUNGSFORM"
* form.coding[=].code = #KTA
* form.coding[=].display = "Kautabletten"
// EDQM coding — research profile redacts this slice too (in addition to KBV).
* form.coding[+].system = "http://standardterms.edqm.eu"
* form.coding[=].code = #10219000
* form.coding[=].display = "Chewable tablet"

* amount.numerator.value = 30
* amount.numerator.unit = "Tabletten"
* amount.numerator.system = "http://unitsofmeasure.org"
* amount.numerator.code = #{Tabletten}
* amount.denominator.value = 1
* amount.denominator.unit = "Package"
* amount.denominator.system = "http://unitsofmeasure.org"
* amount.denominator.code = #{Package}

// Ingredient 1: Magnesiumhydroxid
* ingredient[+].itemCodeableConcept.coding[+].system = "http://fhir.de/CodeSystem/ask"
* ingredient[=].itemCodeableConcept.coding[=].code = #2250
* ingredient[=].itemCodeableConcept.coding[=].display = "Magnesiumhydroxid"
* ingredient[=].itemCodeableConcept.coding[+].system = "http://fhir.de/CodeSystem/bfarm/atc"
* ingredient[=].itemCodeableConcept.coding[=].version = "2024"
* ingredient[=].itemCodeableConcept.coding[=].code = #A02AA04
* ingredient[=].itemCodeableConcept.coding[=].display = "Magnesiumhydroxid"
* ingredient[=].itemCodeableConcept.text = "Magnesiumhydroxid 400 mg"
* ingredient[=].strength.numerator.value = 400
* ingredient[=].strength.numerator.unit = "milligramm"
* ingredient[=].strength.numerator.system = "http://unitsofmeasure.org"
* ingredient[=].strength.numerator.code = #mg
* ingredient[=].strength.denominator.value = 1
* ingredient[=].strength.denominator.unit = "Tabletten"
* ingredient[=].strength.denominator.system = "http://unitsofmeasure.org"
* ingredient[=].strength.denominator.code = #{Tabletten}

// Ingredient 2: Algeldrat
* ingredient[+].itemCodeableConcept.coding[+].system = "http://fhir.de/CodeSystem/ask"
* ingredient[=].itemCodeableConcept.coding[=].code = #01253
* ingredient[=].itemCodeableConcept.coding[=].display = "Algeldrat"
* ingredient[=].itemCodeableConcept.coding[+].system = "http://fhir.de/CodeSystem/bfarm/atc"
* ingredient[=].itemCodeableConcept.coding[=].version = "2024"
* ingredient[=].itemCodeableConcept.coding[=].code = #A02AB01
* ingredient[=].itemCodeableConcept.coding[=].display = "Aluminiumhydroxid"
* ingredient[=].itemCodeableConcept.text = "Algeldrat 400 mg"
* ingredient[=].strength.numerator.value = 400
* ingredient[=].strength.numerator.unit = "milligramm"
* ingredient[=].strength.numerator.system = "http://unitsofmeasure.org"
* ingredient[=].strength.numerator.code = #mg
* ingredient[=].strength.denominator.value = 1
* ingredient[=].strength.denominator.unit = "Tabletten"
* ingredient[=].strength.denominator.system = "http://unitsofmeasure.org"
* ingredient[=].strength.denominator.code = #{Tabletten}

* batch.lotNumber = "LOT-2024-A0042"
* batch.expirationDate = "2026-12-31"
