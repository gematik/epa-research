#!/usr/bin/env bash
# snapshots.sh — install FHIR dependencies declared in sushi-config.yaml,
# then bake snapshots into ~/.fhir/packages for any StructureDefinition
# that ships with a differential only. SUSHI requires parent snapshots
# when deriving profiles; gematik packages typically publish differentials
# only, so we generate them locally with the HL7 validator.

set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
CONFIG="$HERE/sushi-config.yaml"
CACHE="${FHIR_CACHE:-$HOME/.fhir/packages}"
TOOL_DIR="$HOME/.local/share/fhir-tools"
VALIDATOR="$TOOL_DIR/validator_cli.jar"
VALIDATOR_URL="https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar"

[ -f "$CONFIG" ] || { echo "sushi-config.yaml not found at $CONFIG" >&2; exit 1; }
command -v java >/dev/null || { echo "java not on PATH" >&2; exit 1; }
command -v sushi >/dev/null || { echo "sushi not on PATH (npm i -g fsh-sushi)" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 not on PATH" >&2; exit 1; }

# --- parse fhirVersion + dependencies from sushi-config.yaml --------------
FHIR_VERSION=$(awk -F: '/^fhirVersion:/{gsub(/[ "]/,"",$2); print $2}' "$CONFIG")
: "${FHIR_VERSION:=4.0.1}"

DEPS=()
while IFS= read -r line; do
  DEPS+=("$line")
done < <(awk '
  /^dependencies:/{p=1; next}
  p && /^[^[:space:]#]/{p=0}
  p && /^[[:space:]]+[A-Za-z0-9._-]+[[:space:]]*:/{
    gsub(/^[[:space:]]+/,"")
    gsub(/[[:space:]"]/,"")
    print
  }
' "$CONFIG")

if [ ${#DEPS[@]} -eq 0 ]; then
  echo "No dependencies declared in $CONFIG"
  exit 0
fi

echo "FHIR version: $FHIR_VERSION"
echo "Dependencies:"
for d in "${DEPS[@]}"; do echo "  - $d"; done
echo

# --- 1. install deps — sushi resolves and downloads as a side effect ------
echo "==> Resolving dependencies (sushi .)…"
(cd "$HERE" && sushi . >/dev/null 2>&1 || true)

# --- 2. ensure validator_cli.jar is on disk -------------------------------
if [ ! -f "$VALIDATOR" ]; then
  echo "==> Downloading validator_cli.jar to $VALIDATOR…"
  mkdir -p "$TOOL_DIR"
  curl -fL --progress-bar -o "$VALIDATOR" "$VALIDATOR_URL"
fi

# --- 3. snapshot each dependency ------------------------------------------
# Pass every declared dep as -ig so cross-package parent resolution works.
IG_ARGS=()
for d in "${DEPS[@]}"; do
  IG_ARGS+=( -ig "${d%%:*}#${d#*:}" )
done

needs_snapshot() {
  local dir=$1
  python3 - "$dir" <<'PY'
import json, sys, glob, os
d = sys.argv[1]
for f in glob.glob(os.path.join(d, 'StructureDefinition-*.json')):
    try:
        sd = json.load(open(f))
    except Exception:
        continue
    if not sd.get('snapshot', {}).get('element'):
        sys.exit(0)  # at least one missing → needs work
sys.exit(1)
PY
}

for d in "${DEPS[@]}"; do
  id=${d%%:*}; ver=${d#*:}
  pkg_dir="$CACHE/${id}#${ver}/package"

  if [ ! -d "$pkg_dir" ]; then
    echo "==> $id#$ver — not in cache ($pkg_dir), skipping"
    continue
  fi

  if ! needs_snapshot "$pkg_dir"; then
    echo "==> $id#$ver — already snapshotted, skipping"
    continue
  fi

  echo "==> Snapshotting $id#$ver"
  bak="$CACHE/${id}#${ver}.bak"
  [ -d "$bak" ] || cp -r "$CACHE/${id}#${ver}" "$bak"

  work=$(mktemp -d)
  trap 'rm -rf "$work"' EXIT
  cp "$pkg_dir"/StructureDefinition-*.json "$work/"

  ( cd "$work" && java -jar "$VALIDATOR" -snapshot \
      *.json \
      -version "$FHIR_VERSION" \
      "${IG_ARGS[@]}" \
      -tx n/a \
      -outputSuffix snapped.json ) > "$work/.log" 2>&1 \
    || { echo "    validator failed — see $work/.log"; cat "$work/.log" | tail -20; exit 1; }

  count=0
  for f in "$work"/*.snapped.json; do
    [ -f "$f" ] || continue
    base=$(basename "$f" .snapped.json)
    cp "$f" "$pkg_dir/$base"
    count=$((count+1))
  done
  echo "    baked $count snapshot(s)"

  rm -rf "$work"
  trap - EXIT
done

echo
echo "Done. Run 'sushi .' to build."
