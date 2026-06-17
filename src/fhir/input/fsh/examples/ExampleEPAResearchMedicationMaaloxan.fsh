// Redacted version of full EPA Medication
Instance: ExampleEPAResearchMedicationMaaloxan
InstanceOf: EPAResearchMedication
Usage: #example
Description: "Maaloxan after EPAResearchMedication redaction rules."

* meta.tag[TagSourceProfile].code = #"https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication"
* meta.security = http://terminology.hl7.org/CodeSystem/v3-ObservationValue#PSEUDED "pseudonymized"

// Explicitly defined extension slices are copied; others are filtered out.
// * extension[isVaccine].valueBoolean = false
// * extension[drugCategory].valueCoding.system = "https://gematik.de/fhir/epa-medication/CodeSystem/epa-drug-category-cs"
// * extension[drugCategory].valueCoding.code = #00
// * extension[normSizeCode].valueCode = #N1
// * extension[packaging].valueString = "Packung mit 30 Kautabletten"
// * extension[manufacturingInstructions].valueString = "Bei Raumtemperatur trocken lagern."
// * extension[type].valueCoding.system = "http://snomed.info/sct"
// * extension[type].valueCoding.code = #781405001
// * extension[type].valueCoding.display = "Medicinal product package"

// EPAResearchMedication explicitly slices code.coding only for atc-de
// (cardinality 0..0 — redacted). pzn and ask flow through implicitly via
// the inherited #open slicing rule, so we add them by array-index here;
// the absence of a slice name is the signal that they are not enumerated
// by the research profile.
* status = #active

* code.coding[+].system = "http://fhir.de/CodeSystem/ifa/pzn"
* code.coding[=].code = #9717395
* code.coding[=].display = "Maaloxan® 25 mVal Sodbrennen Kautabletten Lemon"

* code.coding[+].system = "http://fhir.de/CodeSystem/ask"
* code.coding[=].code = #02250
* code.coding[=].display = "Magnesiumhydroxid"

* amount.numerator.value = 30
* amount.numerator.unit = "Tabletten"
* amount.numerator.system = "http://unitsofmeasure.org"
* amount.numerator.code = #{Tabletten}
* amount.denominator.value = 1
* amount.denominator.unit = "Package"
* amount.denominator.system = "http://unitsofmeasure.org"
* amount.denominator.code = #{Package}

// Ingredient 1: Magnesiumhydroxid — ASK kept, ATC-DE + text removed
* ingredient[+].itemCodeableConcept.coding[+].system = "http://fhir.de/CodeSystem/ask"
* ingredient[=].itemCodeableConcept.coding[=].code = #2250
* ingredient[=].itemCodeableConcept.coding[=].display = "Magnesiumhydroxid"
* ingredient[=].strength.numerator.value = 400
* ingredient[=].strength.numerator.unit = "milligramm"
* ingredient[=].strength.numerator.system = "http://unitsofmeasure.org"
* ingredient[=].strength.numerator.code = #mg
* ingredient[=].strength.denominator.value = 1
* ingredient[=].strength.denominator.unit = "Tabletten"
* ingredient[=].strength.denominator.system = "http://unitsofmeasure.org"
* ingredient[=].strength.denominator.code = #{Tabletten}

// Ingredient 2: Algeldrat — ASK kept, ATC-DE + text removed
* ingredient[+].itemCodeableConcept.coding[+].system = "http://fhir.de/CodeSystem/ask"
* ingredient[=].itemCodeableConcept.coding[=].code = #01253
* ingredient[=].itemCodeableConcept.coding[=].display = "Algeldrat"
* ingredient[=].strength.numerator.value = 400
* ingredient[=].strength.numerator.unit = "milligramm"
* ingredient[=].strength.numerator.system = "http://unitsofmeasure.org"
* ingredient[=].strength.numerator.code = #mg
* ingredient[=].strength.denominator.value = 1
* ingredient[=].strength.denominator.unit = "Tabletten"
* ingredient[=].strength.denominator.system = "http://unitsofmeasure.org"
* ingredient[=].strength.denominator.code = #{Tabletten}

* batch.expirationDate = "2026-12-31"
