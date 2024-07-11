

Version: 1.0 (ePA Pre-Release 3.1.0)

Veröffentlichung vom 15.07.2024

Herausgeber: [gematik GmbH](https://www.gematik.de/)

Allgemeine Fragen und Kommentare zum Leitfaden bitte über das [Anfragenportal](https://service.gematik.de/servicedesk/customer/portal/13/user/login?nokerberos&destination=portal%2F13) einreichen. Falls kein Zugang besteht, senden Sie bitte eine E-Mail an "patientteam [ at ] gematik.de" mit dem Betreff "Anfragenportal Zugang".

# Pseudonymisierung von FHIR-Ressourcen zur Ausleitung an das Forschungsdatenzentrum Gesundheit
## Scope
Daten der ePA werden pseudonymisiert an das Forschungsdatenzentrum Gesundheit (FDZ) der nationalen Datenzugangs- und Koordinierungsstelle für Gesundheitsdaten am BfArM (Bundesinstitut für Arzneimittel und Medizinprodukte) ausgeleitet.

In diesem Leitfaden wird die ressourcenbasierte Pseudonymisierung von FHIR-Daten beschrieben. 

Dafür werden FHIR-Profile beschrieben, die für die Pseudonymisierung von FHIR-Ressourcen-Instanzen verwendet werden. 

Diese Profile werden hier als Pseudonymisierungs-Profile (P-Profile) bezeichnet.

Für jeden zu pseudonymisierende FHIR-Ressource-Typ existiert versionsunabhängig genau 1 P-Profil.

Das Ergebnis einer ressourcenbasierten Pseudonymisierung sind 
1. FHIR-Ressourcen-Instanzen, die keine personenbezogenen Daten enthalten, jedoch u. a. mittels eines Pseudonyms in der selben Ressource oder in einer referenzierten Ressource personenbeziehbare Daten enthalten. 
1. eine Liste von zufällig generierten Arbeitsnummern als Ersatz der logischen Referenzen zur Krankenversichertennummer (KVNR) - s. u.

## Non-Scope
Die Bewertung des Re-Identifikationsrisiko der resultierenden pseudonymisierten Daten ist NICHT Teil des Pseudonymisierungs-Prozesses vor der Ausleitung an das FDZ.

Dieser Leitfaden beschreibt NICHT, welche FHIR-Ressourcen an das FDZ ausgeleitet werden müssen.

# Pseudonymisierungs-Profil

Ein P-Profil enthält Informationen zur Pseudonymisierung der jeweiligen FHIR-Ressource. Die zu verwendenden P-Profile sind unter https://simplifier.net/epa-research zu finden. Falls dort zu einem Ressourcen-Typ ein P-Profil spezifiziert wurde, ist jede Instanz dieses Ressourcen-Typs vor der Ausleitung zu pseudonymisieren.

Das für die Pseudonymisierung zu verwendende P-Profil ist folgenderweise zu ermitteln:

* Falls ein P-Profil für ein spezifisches zu pseudonymisierendes Quell-Profil vorhanden ist, muss dieses verwendet werden. In diesem Fall ist im P-Profil ein Meta-Datum mit `system` `https://gematik.de/fhir/epa-research/sid/source-profile` angegeben. Als Wert ist die URI des Quell-Profils angegeben.
* Ansonsten ist jenes P-Profil zu verwenden, das für den zu pseudonymisierenden FHIR-Ressourcen-Typ vorgehsehen ist. In diesem Fall ist im P-Profil der Ressourcen-Typ im `type`-Element seiner `StuctureDefinition` angegeben.

## Struktur

Ein P-Profil bildet das Profil der zu pseudonymisierenden FHIR-Ressource mit folgenden Anpassungen ab:
- falls benötigt: Änderungen von Kardinalitäten 
- Ergänzung folgender FHIR-Elemente:
-- Security Label als Metadatum
-- Eine Extension für jedes in der Pseudonymisierung zu bearbeitende FHIR-Element. Diese Extension wird hier als "Privacy Label" bezeichnet.

Das Security Label und die Privacy Labels bilden Code-Werte der ValueSets ObligationPolicy bzw. ObservationValue ab.

## Security Label
Nach erfolgreicher Pseudonymisierung wird in der Ressourcen-Instanz das Security Label mit dem ObservationValue-Wert "PSEUDED" für "pseudonymized" (http://terminology.hl7.org/CodeSystem/v3-ObservationValue) gesetzt.

## "Privacy Label" Extension

Eine sogenannte Privacy Label Extension ist an einer `ElementDefinition` im Rahmen der `StructureDefinition` des P-Profils angegeben und zeigt durch den entsprechenden Code-Wert an, wie das FHIR-Element für die Pseudonymisierung zu bearbeiten ist. Sie kann über die angegebene canonical url https://gematik.de/fhir/epa-research/StructureDefinition/privacy-label-extension erkannt werden.

Die Regeln der Pseudonymisierung werden über die Werte aus dem ValueSet
ObligationPolicy spezifiziert: `http://terminology.hl7.org/ValueSet/v3-ObligationPolicy`

* `PSEUD` - Werte sind zu pseudonymisieren. Die KVNR werden durch Arbeitsnummer ersetzt.
* `REDACT`- Das FHIR-Element ist zu entfernen.


Beispiel
```json
{
    "id": "MedicationStatement.subject.identifier",
    "extension": [
        {
        "url": "https://gematik.de/fhir/epa-research/StructureDefinition/privacy-label-extension",
        "extension": [
            {
            "url": "obligationPolicy",
            "valueCoding": {
                "code": "PSEUD",
                "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode"
            }
            }
        ],
        }
    ],
    "path": "MedicationStatement.subject.identifier",
    "type": [
        {
        "code": "Identifier",
        "profile": [
            "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-job-number-identifier"
        ]
        }
    ]
},
```



# Anwendung eines Pseudonymisierungs-Profils

Für jede an das FDZ auszuleitende FHIR-Ressourcen-Instanz ist zu prüfen, ob für diese Ressource ein P-Profil im Package https://simplifier.net/epa-research spezifiziert ist.

Falls ein P-Profil spezifiziert ist, wird es verwendet um die Ressourcen-Instanz zu pseudonymisieren. Das Ergebnis ist eine neue pseudonymisierte Ressourcen-Instanz, die ...
1. … entsprechend der privacy label extensions des P-Profil pseudonymisiert wird. 
1. … den Kardinalitäten des P-Profils folgt. 

- Bei `PSEUD`: Ersetzen des Element-Wertes mit der Arbeitsnummer laut Anweisungen zur Generierung der Arbeitsnummer. Alle Identifier mit System `http://fhir.de/sid/gkv/kvid-10` werden durch neue Identifier mit System `https://gematik.de/fhir/epa-research/sid/job-number-identifier` ersetzt, also die KVNR durch eine Arbeitsnummer ersetzt.

- `REDACT`- Das FHIR-Element ist zu entfernen.

Um die Referenz zum ursprünglichen Profil(en) anzugeben, werden es/sie als Meta-Tag mit `system` `https://gematik.de/fhir/epa-research/sid/source-profile` der pseudonomysierten FHIR-Ressourcen-Instanz angegeben, z. B.:

Meta-Header der nicht-pseudonymisierten FHIR-Ressource-Instanz
```json
"meta": {
    "profile": [ "https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication-statement" ]
}
```

Meta-Header der pseudonymisierten FHIR-Ressource-Instanz
```json
"meta": {
    "profile": [ "https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication-statement" ],
    "tag": [ {
        "system": "https://gematik.de/fhir/epa-research/sid/source-profile",
        "code": "https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication-statement"
    } ]
}
```

Extensions, die nicht als Teil des P-Profils spezifiziert sind, müssen aus der Instanz entfernt werden.

Nach erfolgreicher Pseudonymisierung wird in der Ressourcen-Instanz das Security Label mit dem ObservationValue-Wert "PSEUDED" für "pseudonymized" (http://terminology.hl7.org/CodeSystem/v3-ObservationValue) gesetzt.


# Validierung der Pseudonymisierung

Nach der Pseudonomyiserung muss die pseudonomyisierte FHIR-Ressource-Instanz nach dem P-Profil validiert werden. Nur valide FHIR-Ressourcen dürfen ausgeleitet werden.

# Überblick zum Ablauf der Pseudonymisierung 

1. Pseudonymisierung-Komponente ermittelt alle konfigurierten P-Profile aus dem FHIR-Package.
1. ePA-Aktensystem übergibt eine zu pseudonymisierende FHIR-Ressource-Instanz an die Pseudonymisierung-Komponente. Dies kann auch in Bulk erfolgen.
1. Pseudonymisierung-Komponente ermittelt das passende P-Profil, entweder anhand des Ressourcen-Typs oder über das Profil-Metadatum (s. o.).
1. Pseudonymisierung-Komponente wendet alle im P-Profil enthaltenen Regeln an und erzeugt eine neue, pseudonymisierte FHIR-Ressource-Instanz.
1. Pseudonymisierung-Komponente validiert die erstellte FHIR-Ressourcen-Instanz.
1. Pseudonymisierung-Komponente übergibt die pseudonymisierte FHIR-Ressource-Instanz inkl. einer Liste aller entstandenen Arbeitsnummer an das ePA-Aktensystem zur Ausleitung an Forschungsdatenzentrum bzw. der Vertrauensstelle.
