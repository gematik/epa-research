RuleSet: MetaSourceProfile

* meta.tag MS
  * ^slicing.discriminator.type = #value
  * ^slicing.discriminator.path = "system"
  * ^slicing.rules = #open
* meta.tag contains TagSourceProfile 1..1 MS
* meta.tag[TagSourceProfile].system = "https://gematik.de/fhir/epa-research/sid/source-profile"
