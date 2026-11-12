# Show all available tasks
info:
    just -l

# Build the FHIR resources using SUSHI
build:
    cd ./src/fhir && sushi -s .

# Run sushi (alias for build)
sushi:
    just build

# Install FHIR dependencies and generate missing snapshots in the local package cache
snapshots:
    cd ./src/fhir && ./snapshots.sh
