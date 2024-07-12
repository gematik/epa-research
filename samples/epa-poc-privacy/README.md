## Privacy label

Privacy label can be specified as an extension on an element definition in the structure definition (profile). There are two ways to specify the privacy label parameters:

* composite extension containing the obligation policy and other poossible parameters.
* specify all parameters as it's own simple extensions. Currently we use only the ObligationPolicy so this method might be more suitable now, but less scalable in the future.

Complex type extension (implemented in PoC)
```yaml
Extension: PrivacyLabelExtension
Context: ElementDefinition

* ^url = "https://gematik.de/fhir/privacy/StructureDefinition/PrivacyLabelExtension"
* extension contains
    obligationPolicy 1..1 MS
* extension[obligationPolicy].value[x] only Coding
* extension[obligationPolicy].value[x] from $vs-v3-ObligationPolicy

RuleSet: PrivacyLabelPseudonymize
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#PSEUD

RuleSet: PrivacyLabelAnonymize
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#ANONY

RuleSet: PrivacyLabelRedact
* ^extension[PrivacyLabelExtension].extension[obligationPolicy].valueCoding = $cs-v3-act-code#REDACT


# now specify the privacy labels on elements:
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
```

## Privacy filter rules

Simplified sematincs to filter the confidential data from the FHIR Resources:

* **Redact** redacts the FHIR-Resource by removing the value
* **Anonymize** anonymizes the FHIR-Resource by replacing the value with a Zero-Value appropriate for the given datatype or defined by the profile
* **Pseudonymize** replaces the value with a pseudonym which can be resolved elsewhere
* **Encrypt** encrypts the value by some cryptographic means and places the encrypted value in place of the original one


### PrimitiveType values

https://build.fhir.org/datatypes.html#primitive

| Filter  | Description
| ------- | --- |
| `Redact`  | Can only be removed if the cardinality of containing element or slice allows `0..` |
| `Anonymize` | By zeroing out a confidential value an appropriate zero value must be defined. For booleans or enumerations (e.g. codes) it can become challenging to choose the right zero value. |
| `Pseudonymize` | Can only be effectively done on string values. |
| `Encrypt` | `Encrypt`ion would break most non-string datatypes |

```json
{
  "birthDate": "2008-09-12"
}
```

```json
// remove
{
}

// nullify
{
  "birthDate": "1970-01-01"
}

// encrypt: error due to invalide date format
{
  "birthDate": "A887DC8787619490FE"
}

```


### ComplexType values

https://build.fhir.org/datatypes.html#complex

FHIR ComplexTypes are elements that consist of primitve types of other complex types. Such structures may become quite deep, e.g:

```json
{
  "contact": [
    {
      "name": {
        "given": [
          "Daffy"
        ],
        "family": "Duck"
      },
      "address": {
        "line": [
          "123 Main St"
        ],
        "city": "Acmetropolis",
        "state": "CA",
        "postalCode": "90210"
      }
    }
  ]  
}
```

| Filter  | Description
| ------- | --- |
| `Redact`  | Can only be removed if the cardinality of containing element or slice allows `0..` |
| `Anonymize` | By zeroing out the complex type the zero-values of contained primitive and complex types must be taken into account, as well as their cardinality. Hence the zero values for complex types will be profile specific |
| `Encrypt` | `Encrypt`ion would break complex type structure into bytes or string. |
| `Pseudonymize` | Is not applicable fo the while complex type, single primitive types must be pseudonymized |


### Identifier

FHIR Resources may contain the [Identifiers](https://build.fhir.org/datatypes.html#Identifier), which are most certainly confidential.

```json
{
 "identifier": [
    {
      "system": "http://fhir.de/sid/gkv/kvid-10",
      "value": "X123456789"
    }
  ],
}
```

| Filter  | Description
| ------- | --- |
| `Redact`  | Can only be removed if the cardinality of containing element or slice allows `0..` |
| `Anonymize` | Anonymizing out of identifier(s) can easily be done by defining the zero values per identifier type, e.z: `Z000000000` for KVNR |
| `Encrypt` | Encryption of complex type as a whole would break the FHIR syntax. Encryption of single elements depends on the structure of the complex type (e.g. strings can easily be encrypted, other types most probably not) |
| `Pseudonymize` | Is applicable. The restrictive identifier datatypes like KVNR limit the entropy of pseudonomization and can lead to collision with real values. |


### Literal Reference

https://build.fhir.org/references.html#literal

```json
{
  "subject": {
    "reference": "Patient/ABC"
  }
}
```

| Filter  | Description
| ------- | --- |
| `Redact`  | Can only be removed if the cardinality of containing element or slice allows `0..` |
| `Anonymize` | Anonymizing out the literal reference URI can easily be done by defining the zero values per resource type, e.g. `Patient/unspecified`. In this case the reference will be broken forever and is the same as removal, but without cardinality problem. |
| `Encrypt` | Encryption of literal reference URI is quite suitable operation to hide the confidential reference without breaking the profile an being able the reconstruct the reference elsewhere |
| `Pseudonymize` | Suits very good |




```json
// encrypted reference
{
  "subject": {
    "reference": "zw+ʛ^vzq"
  }
}
```

### Logical Reference

https://build.fhir.org/references.html#logical

```json
{
  "subject": {
    "identifier": {
      "system": "http://fhir.de/sid/gkv/kvid-10",
      "value": "X123456789"
    }
  }
}
```

Since the logical reference are basicaly identifiers, all rules from Identifiers apply here. Especially the KVNR being just a Character + 9 Numbers string is very limited in terms of pseudonymisation.