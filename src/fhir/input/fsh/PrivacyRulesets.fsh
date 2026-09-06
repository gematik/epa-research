RuleSet: PrivacyLabelPseudonymize
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#PSEUD

RuleSet: PrivacyLabelAnonymizeString(dummyValue)
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#ANONY
* ^extension[PrivacyLabelExtension].extension[dummyValue].valueString = {dummyValue}

// Für code-wertige Elemente: bei required/extensible Binding muss der Platzhalter
// selbst ein gültiger Code des gebundenen ValueSets sein.
RuleSet: PrivacyLabelAnonymizeCode(dummyValue)
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#ANONY
* ^extension[PrivacyLabelExtension].extension[dummyValue].valueCode = #{dummyValue}

RuleSet: PrivacyLabelRedact
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#REDACT
