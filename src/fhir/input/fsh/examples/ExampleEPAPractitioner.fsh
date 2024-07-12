Instance: ExampleEPAPractitioner
InstanceOf: PractitionerDirectory

* meta.tag[Origin] = Origin#ldap
* id = "e94eb9b2-a8ac-4077-ba64-51af77a031f0"
* identifier[TelematikID].value = "1-1012345678"
* identifier[LANR].value = "123456789"
* name
  * prefix = "Dr."
  * given[+] = "Max"
  * given[+] = "Manfred"
  * family = "Musterman"
  * text = "Dr. Max Manfred Mustermann"
* qualification[+].code = urn:oid:1.2.276.0.76.5.114#010
* qualification[+].code = PractitionerProfessionOID#1.2.276.0.76.4.30


Instance: ExampleEPAPractitionerLocation
InstanceOf: LocationDirectory

* id = "5541eeb9-4b4f-4f0c-bdd3-53dc0280898b"
* meta.tag[Origin] = Origin#ldap
* address
  * use = #work
  * type = #both
  * text = "Raiffeisenstr. 42&#13;&#10;10123 Berlin&#13;&#10;Berlin&#13;&#10;DE"
  * line[+] = "Raiffeisenstr. 42"
  * city = "Berlin"
  * state = Region#Berlin
  * postalCode = "10123"
  * country = "DE"

Instance: ExampleEPAPractitionerRole
InstanceOf: PractitionerRoleDirectory

* id = "115aceb7-d4a9-4843-aa6a-96a6999a803c"
* meta.tag[Origin] = Origin#ldap
* practitioner = Reference(ExampleEPAPractitioner)
* location[+] = Reference(ExampleEPAPractitionerLocation)