Instance: ExampleEPAMedicationStatement
InstanceOf: EPAMedicationStatement
Usage: #example
* meta
  * versionId = "1"
  * lastUpdated = "2025-07-22T14:43:33.244Z"

* id = "0b152eb2-6d0a-48e9-839b-5ee3023f2ad4"

* status = #active
* medicationReference = Reference(ExampleEpaMedication1)
* subject.identifier.value = "X123456789"
* effectivePeriod.start = "2025-07-22"
* dateAsserted = "2025-07-22"
* informationSource = Reference(Practitioner/ed1e019f-d50f-4c89-ace9-b54c588662c1)
* dosage[+].timing.repeat.frequency = 1
* dosage[=].timing.repeat.period = 1.0
* dosage[=].timing.repeat.periodUnit = #d
* dosage[=].timing.repeat.when = #NIGHT
* dosage[=].timing.repeat.boundsDuration = 7.0 $cs-ucum#wk "Week"
* dosage[=].asNeededBoolean = false
* dosage[=].doseAndRate.doseQuantity = 1 $cs-kbv-sfhir-bmp-dosiereinheit#1 "Stück"
* dosage[+].timing.repeat.frequency = 1
* dosage[=].timing.repeat.period = 1.0
* dosage[=].timing.repeat.periodUnit = #d
* dosage[=].timing.repeat.when = #MORN
* dosage[=].timing.repeat.boundsDuration = 7.0 $cs-ucum#wk "Week"
* dosage[=].asNeededBoolean = false
* dosage[=].doseAndRate.doseQuantity = 2 $cs-kbv-sfhir-bmp-dosiereinheit#1 "Stück"
* note[+].text = "Unbedingt eine halbe Stunde vor dem Frühstück einnehmen, Herr Mustermann!"


Instance: ExampleEPAMedicationStatementBundle
InstanceOf: Bundle
Usage: #example

* type = #collection
* entry[+].resource = ExampleEPAMedicationStatement
