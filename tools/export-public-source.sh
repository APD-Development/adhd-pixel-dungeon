#!/bin/bash
# Makes the GPL source package for a release: the exact files of one commit, with no git history, no keystore and
# nothing secret, ready to publish in the public source repo (Phase 2). Usage: tools/export-public-source.sh [commit] [out.zip]
# Refuses to finish if anything that looks like a key, keystore or password file is inside.
set -euo pipefail
COMMIT="${1:-HEAD}"
OUT="${2:-apd-source-$(git rev-parse --short "$COMMIT").zip}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
git archive --format=tar "$COMMIT" | tar -x -C "$TMP"
# Never public: signing keys (old alpha key included) and anything that only makes sense privately.
rm -rf "$TMP/keystore"
# The public copy has no alpha-key fallback: builders sign with their own key via the APD_DEBUG_* variables.
python3 - "$TMP" <<'PY'
import re, sys, os
root = sys.argv[1]
g = os.path.join(root, 'android/build.gradle')
t = open(g).read()
t = re.sub(r"\} else \{\s*storeFile rootProject\.file\('keystore/apd-debug\.jks'\)[^}]*\}", "}", t)
open(g, 'w').write(t)
d = os.path.join(root, 'docs/SIGNING.md')
if os.path.exists(d): os.remove(d)
PY
BAD=$(cd "$TMP" && find . -type f \( -iname '*.jks' -o -iname '*.keystore' -o -iname '*.p12' -o -iname '*.pem' -o -iname '*.key' -o -iname '.env' -o -iname 'secrets.*' -o -iname '*.secret' -o -iname 'local.properties' \) | head -20)
if [ -n "$BAD" ]; then echo "REFUSED: secret-looking files in the export:"; echo "$BAD"; exit 1; fi
if grep -rIl --exclude-dir=.github --exclude="test_*.py" -E "storePassword *'[^']+'|BEGIN (RSA |EC )?PRIVATE KEY" "$TMP" >/dev/null 2>&1; then
  echo "REFUSED: a password or private key is written in these files:"
  grep -rIl --exclude-dir=.github --exclude="test_*.py" -E "storePassword *'[^']+'|BEGIN (RSA |EC )?PRIVATE KEY" "$TMP" | sed "s|$TMP/||" | head
  exit 1
fi
# Private secret values (never written in this repo): one per line in $APD_FORBIDDEN, if set.
if [ -n "${APD_FORBIDDEN:-}" ] && [ -f "$APD_FORBIDDEN" ]; then
  if grep -rIlF -f "$APD_FORBIDDEN" "$TMP" >/dev/null 2>&1; then echo "REFUSED: a private secret value is in the export"; exit 1; fi
fi
for f in LICENSE.txt; do [ -f "$TMP/$f" ] || { echo "REFUSED: $f is missing (GPL needs it)"; exit 1; }; done
(cd "$TMP" && zip -qr - .) > "$OUT"
echo "OK: $OUT ($(du -h "$OUT" | cut -f1)) from $(git rev-parse --short "$COMMIT"), no keystore, LICENSE included"
