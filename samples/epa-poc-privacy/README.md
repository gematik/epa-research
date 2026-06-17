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
  cd ../../src/fhir && sushi .
  ```

## Run the sample

From this directory:

```sh
./gradlew run
```

This loads the three Maaloxan example resources declared in
[`app/testcases-config.yaml`](app/testcases-config.yaml), applies the privacy filter
against each one's matching EPAResearch* profile, and writes an HTML report:

```
samples/epa-poc-privacy/reports/Maaloxan.html
```

Open that file in a browser to see:

- a side-by-side diff of the source bundle vs. the filtered bundle,
- the filter-rule log (per element, the action taken),
- the pseudonym table (Arbeitsnummern emitted by the run),
- validation results before and after filtering,
- the FSH sources that drive the rules.

## Run the golden tests

```sh
./gradlew test
```

`MaaloxanGoldenTest` runs the filter against the three upstream
`ExampleEPAMedication*Maaloxan` JSON inputs and asserts each output is semantically
equal (HAPI `Resource.equalsDeep`) to the FSH-generated `ExampleEPAResearch*Maaloxan`
golden. Deterministic pseudonyms come from `app/testcases-config.yaml`'s
`pseudonyms` map so the goldens are reproducible.

## Adding a new test case

1. Author the source resource (FSH) and its expected EPAResearch* counterpart, regenerate
   with `sushi .` in `src/fhir/`.
2. Append a new entry to `testCaseList` in `app/testcases-config.yaml` with
   `resourcePaths`, `expectedResearchPaths`, any required `pseudonyms`, and `sources`
   for the report.
3. `./gradlew run` produces `reports/<id>.html`; `./gradlew test` covers the new pair
   automatically.
