Extension: PrivacyLabelExtension
Id: privacy-label-extension
Context: ElementDefinition
Title: "Privacy Label Extension"
Description: "Element-Ebene-Annotation zur Deklaration der Pseudonymisierungs-Behandlung eines FHIR-Elements innerhalb eines P-Profils. Trägt einen obligationPolicy-Code (PSEUD / REDACT / ANONY aus HL7 v3) und einen optionalen dummyValue für die Anonymisierung primitiver Elemente. Erscheint ausschließlich in StructureDefinitions, niemals in Instanzdaten."

* insert Meta

* extension contains
    obligationPolicy 1..1 MS and
    dummyValue 0..1 MS
* extension[obligationPolicy].value[x] only Coding
* extension[obligationPolicy].value[x] from $vs-v3-ObligationPolicy
