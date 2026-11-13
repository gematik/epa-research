# epa-poc-privacy — Privacy Filter PoC

A Kotlin/JVM proof-of-concept that consumes the FHIR profiles in `../../src/fhir/` and
applies their `PrivacyLabelExtension` rules (`REDACT` / `ANONY` / `PSEUD`) to FHIR
resource instances. For the rule definitions themselves, see
[`docs/leitfaden_pseudonymisierung.md`](../../docs/leitfaden_pseudonymisierung.md).

## Prerequisites

- JDK 21
- The parent FHIR project must have been built so its StructureDefinitions exist under
  `../../src/fhir/fsh-generated/resources/`:
  ```sh
  cd ../../src/fhir && sushi build -s .
  ```

## Run the sample

From this directory:

```sh
./gradlew run
```

This loads the three Maaloxan example resources declared in
[`app/testcases-config.yaml`](app/testcases-config.yaml), applies the privacy filter
against each one's matching EPAResearch* profile, and writes one HTML report per test case:

```
samples/epa-poc-privacy/reports/index.html                (Medication)
samples/epa-poc-privacy/reports/MedicationRequest.html
samples/epa-poc-privacy/reports/MedicationDispense.html
```

Open a report in a browser to see:

- a side-by-side diff of the source bundle vs. the filtered bundle,
- the filter-rule log (per element, the action taken),
- the pseudonym table (Arbeitsnummern emitted by the run),
- validation results before and after filtering,
- the FSH sources that drive the rules.

## Run the tests

```sh
./gradlew test
```

- `MaaloxanGoldenTest` runs the filter against the three upstream
  `ExampleEPAMedication*Maaloxan` JSON inputs and asserts each output is semantically
  equal (HAPI `Resource.equalsDeep`) to the FSH-generated `ExampleEPAResearch*Maaloxan`
  golden. It also asserts that every privacy rule of a research profile changes something
  in its source example, so a new rule without example coverage fails the build.
  Deterministic pseudonyms come from `app/testcases-config.yaml`'s `pseudonyms` map.
- `ResearchProfileRulesTest` lists, per research profile, the rules and the retained
  extensions the filter derives from it. Update it together with the profile.
- `PrivacyFilterTest` covers the filter rule by rule with hand-built resources,
  including the failure modes.

The tests read the StructureDefinitions from `fsh-generated/resources/` and do not need
the FHIR package cache.

## How the filter behaves

- Rules come from the profile's differential: privacy labels plus every element with `max = 0`.
- Extensions the profile does not declare are removed on every element, not only on the resource root.
- `PSEUD` replaces a `Reference` or `Identifier` by the job number identifier alone.
- If any rule fails, the filter returns no resource; resources without a research profile are not exported.
- A contained resource is filtered by the same rules when the profile names itself for `contained`
  (EPAResearchMedication); a contained resource the profile does not cover makes the filter fail.

## Adding a new test case

1. Author the source resource (FSH) and its expected EPAResearch* counterpart, regenerate
   with `sushi build -s .` in `src/fhir/`.
2. Append a new entry to `testCaseList` in `app/testcases-config.yaml` with
   `resourcePaths`, `expectedResearchPaths`, any required `pseudonyms`, and `sources`
   for the report.
3. `./gradlew run` produces `reports/<id>.html`; `./gradlew test` covers the new pair
   automatically.
