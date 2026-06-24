

Version: 1.0 (ePA Pre-Release 3.1.0)

Veröffentlichung vom 24.06.2026

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
1. eine Liste von zufällig generierten Arbeitsnummern als Ersatz der logischen Referenzen zur Krankenversichertennummer (KVNR). Die Methodik zur Generierung der Arbeitsnummer ist im Dokument des RKI (Robert Koch Institut) "Pseudonymisierungskonzept zur Datenausleitung ePA zu Forschungszwecken" spezifiziert.

## Non-Scope
Die Bewertung des Re-Identifikationsrisiko der resultierenden pseudonymisierten Daten ist NICHT Teil des Pseudonymisierungs-Prozesses vor der Ausleitung an das FDZ.

Dieser Leitfaden beschreibt NICHT, welche FHIR-Ressourcen an das FDZ ausgeleitet werden müssen.

# Pseudonymisierungs-Profil

Ein P-Profil enthält Informationen zur Pseudonymisierung der jeweiligen FHIR-Ressource. Die zu verwendenden P-Profile sind unter https://simplifier.net/epa-research zu finden. Falls dort zu einem Ressourcen-Typ ein P-Profil spezifiziert wurde, ist jede Instanz dieses Ressourcen-Typs vor der Ausleitung zu pseudonymisieren.

Das für die Pseudonymisierung zu verwendende P-Profil ist folgenderweise zu ermitteln:

* Falls ein P-Profil für ein spezifisches zu pseudonymisierendes Quell-Profil vorhanden ist, muss dieses verwendet werden. In diesem Fall ist im P-Profil ein Meta-Datum mit `system` `https://gematik.de/fhir/epa-research/sid/source-profile` angegeben. Als Wert ist die URI des Quell-Profils angegeben.
* Ansonsten ist jenes P-Profil zu verwenden, das für den zu pseudonymisierenden FHIR-Ressourcen-Typ vorgesehen ist. In diesem Fall ist im P-Profil der Ressourcen-Typ im `type`-Element seiner `StructureDefinition` angegeben.

## Struktur

Ein P-Profil bildet das Profil der zu pseudonymisierenden FHIR-Ressource mit folgenden Anpassungen ab:
- falls benötigt: Änderungen von Kardinalitäten
- Privacy Label Extensions an den `ElementDefinition`s der zu bearbeitenden FHIR-Elemente.
- explizite Slice-Deklarationen jener Extensions, die in der pseudonymisierten Instanz erhalten bleiben sollen. Extensions ohne entsprechende Slice-Deklaration werden bei der Pseudonymisierung verworfen (siehe Abschnitt "[Anwendung](#anwendung-eines-pseudonymisierungs-profils)").

Zusätzlich wird in der pseudonymisierten Instanz (zur Laufzeit, nicht durch das P-Profil selbst) ein Security Label als Metadatum gesetzt — siehe Abschnitt "[Security Label](#security-label)".

Das Security Label und die Privacy Labels bilden Code-Werte aus den entsprechenden HL7-v3-CodeSystems (`ObservationValue` bzw. `ActCode`/`ObligationPolicy`) ab.

## Security Label

Nach erfolgreicher Pseudonymisierung wird in der Ressourcen-Instanz das Security Label auf `meta.security` gesetzt:

```json
"security": [{
  "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
  "code": "PSEUDED",
  "display": "pseudonymized"
}]
```

Bedeutung des Codes `PSEUDED` (laut [HL7 v3 ObservationValue CodeSystem v7.1.0](https://terminology.hl7.org/7.1.0/en/CodeSystem-v3-ObservationValue.html#v3-ObservationValue-PSEUDED), Gruppe `_SECALTINTOBV` "alteration integrity"):

> _Security metadata observation value conveying the alteration integrity of an IT resource (data, information object, service, or system capability), by indicating the mechanism by which software systems can strip portions of the resource that could allow the identification of the source of the information or the information subject. Custodian may retain a key to relink data necessary to reidentify the information subject._

`PSEUDED` ist damit semantisch korrekt für den ePA→FDZ-Fluss: identifizierende Bestandteile werden ersetzt; der Datentreuhänder (RKI) kann über die Arbeitsnummer eine Re-Identifikation durchführen. Abgrenzung zu verwandten Codes derselben Gruppe:

- `ANONYED` — anonymisiert, kein Re-Identifikationsschlüssel.
- `REDACTED` — vollständige Entfernung unzulässiger Inhalte.
- `MASKED` — Daten durch Verschlüsselung unlesbar gemacht.

`PSEUDED` ist somit redundant zum P-Profil-Bezug in `meta.profile` + `meta.tag[TagSourceProfile]`, dient aber als FHIR-Standard-Signal für Konsumenten, die das P-Profil-Konzept nicht kennen.

## "Privacy Label" Extension

Eine sogenannte Privacy Label Extension wird über die angegebene canonical url https://gematik.de/fhir/epa-research/StructureDefinition/privacy-label-extension erkannt. Sie ist an einer `ElementDefinition` im Rahmen der `StructureDefinition` des P-Profils angegeben und zeigt durch den entsprechenden Code-Wert an, wie das FHIR-Element für die Pseudonymisierung zu bearbeiten ist. *Das heißt: die Privacy Label Extension erscheint ausschließlich in der `StructureDefinition` des P-Profils — nicht im Instanz-JSON der pseudonymisierten Daten.*

Die Regeln der Pseudonymisierung werden über die Werte aus dem ValueSet
ObligationPolicy spezifiziert: `http://terminology.hl7.org/ValueSet/v3-ObligationPolicy`

* `PSEUD` - Werte sind zu pseudonymisieren. Die KVNR werden durch Arbeitsnummer ersetzt.
* `REDACT`- Das FHIR-Element ist zu entfernen.
* `ANONY` - Werte sind durch einen festen Platzhalterwert zu ersetzen. Der eingesetzte Platzhalter wird in einer zusätzlichen `dummyValue` Sub-Extension der Privacy Label Extension mitgegeben.

### Anwendbarkeit der Labels

Die drei Labels unterscheiden sich darin, an welchen Elementen sie angebracht werden dürfen:

* `REDACT` darf an jedem Element angegeben werden, sofern die strukturelle Kardinalität des Quell-Profils dies erlaubt (also eine Kardinalität von `0..0` zulässt). Auf komplexen Elementen entfernt es das gesamte Sub-Bäumchen, auf primitiven Elementen den jeweiligen Wert.
* `ANONY` ist ausschließlich für primitive (z. B. string-wertige) Elemente vorgesehen. Bei komplexen Elementen (`BackboneElement`, `Reference`, weitere strukturierte Typen) wird `ANONY` nicht direkt am Element selbst angewendet, sondern an seinen Sub-Elementen mit primitivem Typ (z. B. `display` bei einer `Reference`). Identifizierende strukturelle Sub-Elemente werden parallel mit `REDACT` markiert; die strukturelle Kardinalität (z. B. `1..1`) bleibt damit erhalten.
* `PSEUD` ist ausschließlich auf Elemente anwendbar, die einen Patient-Bezug über die KVNR tragen — typischerweise `subject` als `Reference(Patient)` oder direkte `Identifier`-Felder mit System `http://fhir.de/sid/gkv/kvid-10`. Eine Anwendung von `PSEUD` auf andere Element-Typen ist nicht definiert.

#### `PSEUD` auf `Reference(Patient)` (z. B. `subject`)

Wenn `PSEUD` an einer `Reference(Patient)` angegeben ist (z. B. `MedicationRequest.subject`, `MedicationDispense.subject`), führt die Pseudonymisierung folgende Transformation an der Instanz durch:

1. `Reference.reference` wird entfernt — die direkte Verlinkung auf eine konkrete `Patient`-Ressource ist identifizierend.
2. `Reference.identifier` wird neu gesetzt:
   - `system` = `https://gematik.de/fhir/epa-research/sid/job-number-identifier`
   - `value` = die generierte Arbeitsnummer (gemäß RKI-Pseudonymisierungskonzept).
3. Vorhandene `Reference.identifier` mit System `http://fhir.de/sid/gkv/kvid-10` werden dadurch ersetzt; jeglicher KVNR-Wert wird durch die zugehörige Arbeitsnummer ausgetauscht.
4. `Reference.display` bleibt erhalten, sofern es keine personenbezogenen Inhalte enthält — andernfalls ist es separat mit `REDACT` oder `ANONY` zu markieren.


Beispiel
```json
{
  "id": "MedicationStatement.subject.identifier",
  "extension": [{
    "url": "https://gematik.de/fhir/epa-research/StructureDefinition/privacy-label-extension",
    "extension": [{
      "url": "obligationPolicy",
      "valueCoding": { "code": "PSEUD", "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode" }
    }]
  }],
  "type": [{
    "code": "Identifier",
    "profile": ["https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-job-number-identifier"]
  }]
  // ...
}
```



# Anwendung eines Pseudonymisierungs-Profils

Für jede an das FDZ auszuleitende FHIR-Ressourcen-Instanz ist zu prüfen, ob für diese Ressource ein P-Profil im Package https://simplifier.net/epa-research spezifiziert ist.

Falls ein P-Profil spezifiziert ist, wird es verwendet um die Ressourcen-Instanz zu pseudonymisieren. Das Ergebnis ist eine neue pseudonymisierte Ressourcen-Instanz, die ...
1. … entsprechend der privacy label extensions des P-Profil pseudonymisiert wird. 
1. … den Kardinalitäten des P-Profils folgt. 

- Bei `PSEUD`: Ersetzen des Element-Wertes mit der Arbeitsnummer laut Anweisungen zur Generierung der Arbeitsnummer. Alle Identifier mit System `http://fhir.de/sid/gkv/kvid-10` werden durch neue Identifier mit System `https://gematik.de/fhir/epa-research/sid/job-number-identifier` ersetzt, also die KVNR durch eine Arbeitsnummer ersetzt.

- Bei `REDACT`: Das FHIR-Element ist zu entfernen.

- Bei `ANONY`: Den Element-Wert durch den in der `dummyValue` Sub-Extension der Privacy Label Extension angegebenen Platzhalterwert ersetzen.

Um die Referenz zum ursprünglichen Profil(en) anzugeben, werden es/sie als Meta-Tag mit `system` `https://gematik.de/fhir/epa-research/sid/source-profile` der pseudonomysierten FHIR-Ressourcen-Instanz angegeben, z. B.:

Meta-Header der nicht-pseudonymisierten FHIR-Ressource-Instanz
```json
"meta": {
  "profile": ["https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication-statement"]
}
```

Meta-Header der pseudonymisierten FHIR-Ressource-Instanz
```json
"meta": {
  "profile": ["https://gematik.de/fhir/epa-research/StructureDefinition/epa-research-medication-statement"],
  "tag": [{
    "system": "https://gematik.de/fhir/epa-research/sid/source-profile",
    "code": "https://gematik.de/fhir/epa-medication/StructureDefinition/epa-medication-statement"
  }],
  "security": [{
    "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
    "code": "PSEUDED",
    "display": "pseudonymized"
  }]
}
```

Extensions, die nicht als Teil des P-Profils spezifiziert sind, müssen aus der Instanz entfernt werden.

Nach erfolgreicher Pseudonymisierung wird in der Ressourcen-Instanz das Security Label `PSEUDED` aus dem CodeSystem `http://terminology.hl7.org/CodeSystem/v3-ObservationValue` auf `meta.security` gesetzt (siehe [Security Label](#security-label) für Bedeutung).


# Validierung der Pseudonymisierung

Nach der Pseudonymisierung muss die pseudonymisierte FHIR-Ressource-Instanz nach dem P-Profil validiert werden. Nur valide FHIR-Ressourcen dürfen ausgeleitet werden.

# Überblick zum Ablauf der Pseudonymisierung 

1. Pseudonymisierung-Komponente ermittelt alle konfigurierten P-Profile aus dem FHIR-Package.
1. ePA-Aktensystem übergibt eine zu pseudonymisierende FHIR-Ressource-Instanz an die Pseudonymisierung-Komponente. Dies kann auch in Bulk erfolgen.
1. Pseudonymisierung-Komponente ermittelt das passende P-Profil, entweder anhand des Ressourcen-Typs oder über das Profil-Metadatum (s. o.).
1. Pseudonymisierung-Komponente wendet alle im P-Profil enthaltenen Regeln an und erzeugt eine neue, pseudonymisierte FHIR-Ressource-Instanz.
1. Pseudonymisierung-Komponente validiert die erstellte FHIR-Ressourcen-Instanz.
1. Pseudonymisierung-Komponente übergibt die pseudonymisierte FHIR-Ressource-Instanz inkl. einer Liste aller entstandenen Arbeitsnummer an das ePA-Aktensystem zur Ausleitung an Forschungsdatenzentrum bzw. der Vertrauensstelle.
