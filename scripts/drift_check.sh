#!/usr/bin/env bash
set -euo pipefail

# Drift detection: fail the build on doc inconsistencies that a script can verify mechanically.
# Runs in CI (see .github/workflows/ci.yml) and is safe to run locally from the repo root.
#
# Lives in scripts/ (NOT context/) on purpose: context/ is stripped from the public mirror, so a
# CI step that references it would break on the public repo. This script ships publicly and runs
# there too. It only inspects published files, never private context/ paths.

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"
FAILED=0

echo "Drift check: verifying doc consistency..."

# --- Gate 1: no machine-local absolute paths in PUBLISHED files -----------------------------
# Published = every tracked file except context/ (which is private and removed from the mirror,
# and is allowed to hold machine-specific paths such as the publish script's source dir).
# A /Users/... path in a published file is both an information leak and a dead link.
echo "  [1/3] Machine-local paths in published files..."
# Build the needle dynamically and exclude this script, so the checker never matches its own
# pattern/messages or scans itself.
NEEDLE="$(printf '/%s/' Users)"
PUBLISHED_HITS="$(git ls-files | grep -v '^context/' | grep -v '^scripts/drift_check.sh$' \
  | xargs grep -l "$NEEDLE" 2>/dev/null || true)"
if [ -n "$PUBLISHED_HITS" ]; then
  echo "  FAIL: /Users/... path found in published file(s):"
  echo "$PUBLISHED_HITS" | sed 's/^/        /'
  echo "        Move machine-specific references into context/ (private) or remove them."
  FAILED=1
else
  echo "  OK: no machine-local paths in published files."
fi

# --- Gate 2: English and Hindi string keys must match ---------------------------------------
echo "  [2/3] String-key mirroring (English vs Hindi)..."
EN="app/src/main/res/values/strings.xml"
HI="app/src/main/res/values-hi/strings.xml"
KEYS_EN="$(grep -o 'name="[^"]*"' "$EN" | sort)"
KEYS_HI="$(grep -o 'name="[^"]*"' "$HI" | sort)"
if [ "$KEYS_EN" != "$KEYS_HI" ]; then
  echo "  FAIL: string keys differ between $EN and $HI:"
  diff <(echo "$KEYS_EN") <(echo "$KEYS_HI") | sed 's/^/        /' || true
  FAILED=1
else
  echo "  OK: English and Hindi string keys match."
fi

# --- Gate 3: offline guarantee in the merged manifest (soft, only if a build exists) --------
# The authoritative hard check is the dedicated CI step that runs after assembleRelease. Here we
# only run it opportunistically when build output is already present, so local runs stay useful.
echo "  [3/3] Offline guarantee (merged manifest, if built)..."
shopt -s nullglob
MANIFEST_DIRS=(app/build/intermediates/merged_manifest*/)
if [ ${#MANIFEST_DIRS[@]} -eq 0 ]; then
  echo "  SKIP: no merged manifest yet (run a build); the dedicated CI step enforces this."
elif grep -rq "android.permission.INTERNET" "${MANIFEST_DIRS[@]}"; then
  echo "  FAIL: INTERNET permission present in the merged manifest — the offline rule is broken."
  FAILED=1
else
  echo "  OK: no INTERNET permission in the merged manifest."
fi

if [ $FAILED -eq 0 ]; then
  echo "Drift check passed."
  exit 0
else
  echo "Drift check failed — fix the issues above."
  exit 1
fi
