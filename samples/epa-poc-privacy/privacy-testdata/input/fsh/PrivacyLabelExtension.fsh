Extension: PrivacyLabelExtension
Context: ElementDefinition

* ^url = "https://gematik.de/fhir/privacy/StructureDefinition/PrivacyLabelExtension"
* extension contains
    obligationPolicy 1..1 MS
* extension[obligationPolicy].value[x] only Coding
* extension[obligationPolicy].value[x] from $vs-v3-ObligationPolicy