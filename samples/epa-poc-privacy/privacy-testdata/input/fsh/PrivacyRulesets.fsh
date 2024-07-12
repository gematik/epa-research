RuleSet: PrivacyLabelPseudonymize
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#PSEUD

RuleSet: PrivacyLabelAnonymize
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#ANONY

RuleSet: PrivacyLabelRedact
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#REDACT
