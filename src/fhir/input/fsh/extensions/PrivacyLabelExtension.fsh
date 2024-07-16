Extension: PrivacyLabelExtension
Id: privacy-label-extension
Context: ElementDefinition

* insert Meta

* extension contains
    obligationPolicy 1..1 MS and
    dummyValue 0..1 MS
* extension[obligationPolicy].value[x] only Coding
* extension[obligationPolicy].value[x] from $vs-v3-ObligationPolicy
