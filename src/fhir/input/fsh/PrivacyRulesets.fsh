RuleSet: PrivacyLabelPseudonymize
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#PSEUD

RuleSet: PrivacyLabelAnonymizeString(dummyValue)
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#ANONY
* ^extension[PrivacyLabelExtension].extension[dummyValue].valueString = {dummyValue}

RuleSet: PrivacyLabelRedact
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#REDACT
