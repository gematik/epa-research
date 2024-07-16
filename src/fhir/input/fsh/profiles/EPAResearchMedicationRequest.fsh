Profile: EPAResearchMedicationRequest
Id: epa-research-medication-request
Parent: MedicationRequest

* insert Meta
* insert MetaSourceProfile

* note 0..0
  * insert PrivacyLabelRedact
* subject.identifier only EPAResearchJobNumberIdentifier
  * insert PrivacyLabelPseudonymize
* subject.reference 0..0
  * insert PrivacyLabelRedact
* requester 0..0
  * insert PrivacyLabelRedact

// Extensions
* extension contains
    MedicationRequestLinkedStatementExtension named medicationRequestLinkedStatement 0.. and
    MultiplePrescriptionExtension named multiplePrescription 0..1 MS and
    IndicatorBVGExtension named isBvg 0..1 MS
* extension[medicationRequestLinkedStatement].value[x]

* extension[isBvg].value[x] MS
* extension[isBvg].valueBoolean MS

* extension[multiplePrescription].extension[indicator].value[x] only boolean
* extension[multiplePrescription].extension[indicator].valueBoolean MS

* extension[multiplePrescription].extension[counter].value[x] only Ratio
* extension[multiplePrescription].extension[counter].valueRatio MS
* extension[multiplePrescription].extension[counter].valueRatio.numerator MS
* extension[multiplePrescription].extension[counter].valueRatio.numerator.value MS
* extension[multiplePrescription].extension[counter].valueRatio.denominator MS
* extension[multiplePrescription].extension[counter].valueRatio.denominator.value MS

* extension[multiplePrescription].extension[period].value[x] only Period
* extension[multiplePrescription].extension[period].valuePeriod MS
* extension[multiplePrescription].extension[period].valuePeriod.start MS
* extension[multiplePrescription].extension[period].valuePeriod.end MS

* extension[multiplePrescription].extension[id].value[x] only Identifier
* extension[multiplePrescription].extension[id].valueIdentifier MS
* extension[multiplePrescription].extension[id].valueIdentifier.system MS
* extension[multiplePrescription].extension[id].valueIdentifier.value MS

