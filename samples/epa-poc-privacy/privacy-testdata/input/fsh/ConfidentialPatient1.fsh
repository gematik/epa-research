Profile: ConfidentialPatient
Parent: Patient
Description: "An example profile of the Patient resource."
* identifier MS
* identifier ^slicing.discriminator.type = #value
* identifier ^slicing.discriminator.path = "system"
* identifier ^slicing.rules = #open
* identifier contains KVNR 1..1 MS
* identifier[KVNR] only IdentifierKvid10
  * insert PrivacyLabelPseudonymize
* name MS
  * insert PrivacyLabelRedact
* id
  * insert PrivacyLabelPseudonymize
* contact MS
  * insert PrivacyLabelRedact

Instance: ExampleConfidentialPatient1
InstanceOf: ConfidentialPatient
Description: "An example of a patient with a license to krill."
* identifier[KVNR].value = "X123456789"
* name
  * given[0] = "Daffy"
  * family = "Duck"
* contact
  * name
    * given[0] = "Daffy"
    * family = "Duck"
  * address
    * line[0] = "123 Main St"
    * city = "Acmetropolis"
    * state = "CA"
    * postalCode = "90210"
