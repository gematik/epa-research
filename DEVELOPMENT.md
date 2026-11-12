# Development

Notes for working on the FHIR profiles and the privacy filter sample in this repository.

## Repository layout

| Path | Content |
|------|---------|
| `src/fhir/input/fsh/` | FSH sources: profiles, datatypes, extensions, rulesets, examples |
| `src/fhir/fsh-generated/resources/` | Generated StructureDefinitions and example instances (checked in) |
| `src/fhir/sushi-config.yaml`, `package.json` | Package metadata and dependencies; keep versions in both files in sync |
| `docs/` | Pseudonymisation guide |
| `samples/epa-poc-privacy/` | Kotlin proof-of-concept applying the privacy labels, with golden tests |

## Prerequisites

- Node.js with [SUSHI](https://github.com/FHIR/sushi): `npm install -g fsh-sushi`
- Java 21 (HL7 validator, sample project)
- Python 3 (used by `snapshots.sh`)
- [just](https://github.com/casey/just) as task runner (optional; the recipes are one-liners)

Alternatively open the repository in the dev container (`.devcontainer/`), which provides
SUSHI, the validators and `fhirscripts`.

## Building the profiles

```sh
just snapshots   # once per dependency change
just build
```

`just build` runs `sushi -s .` in `src/fhir` and writes to `src/fhir/fsh-generated/`.

SUSHI needs snapshots of the parent profiles, but gematik packages are published with
differentials only. `just snapshots` (`src/fhir/snapshots.sh`) resolves the dependencies
declared in `sushi-config.yaml` and generates the missing snapshots with the HL7 validator
directly in the local package cache (`~/.fhir/packages`). A backup of each modified package
is kept next to it as `<package>#<version>.bak`.

### Generated resources

The StructureDefinitions checked in under `fsh-generated/resources/` are produced by the
release toolchain (`fhirscripts`, see `build-record.yaml`). A local `just build` generates
valid but less detailed snapshots and therefore shows them as modified. Do not commit those
local results unless the release build is being replaced on purpose; restore them with:

```sh
git checkout -- src/fhir/fsh-generated
```

## Privacy filter sample

```sh
cd samples/epa-poc-privacy
./gradlew test   # golden tests: filtered source example == expected research example
./gradlew run    # regenerates the HTML reports in reports/
```

The sample reads profiles and examples from `src/fhir/fsh-generated/resources/`. After
changing a profile or example, rebuild the FHIR resources first, then run the tests. See
[samples/epa-poc-privacy/README.md](samples/epa-poc-privacy/README.md) for details and for
adding test cases.

## Continuous integration

`.github/workflows/main.yml` validates the content of `src/fhir/fsh-generated/resources/`
with the Firely Terminal pipeline on every push that touches `src/fhir/`.
