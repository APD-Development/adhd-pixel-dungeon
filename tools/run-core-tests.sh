#!/usr/bin/env bash
# Runs the core unit tests WITHOUT the Android module (which needs Android SDK/AGP).
# Builds a throwaway Gradle project that includes only SPD-classes, core and services.
# Usage: tools/run-core-tests.sh [extra gradle args, e.g. --tests '*FirstFloorExitTest*']
set -euo pipefail
REPO="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
cat > "$TMP/build.gradle" <<'G'
allprojects {
  ext { appJavaCompatibility = JavaVersion.VERSION_11
        gdxVersion='1.14.0'
        gdxControllersVersion='2.2.4' }
  repositories { mavenCentral() }
}
G
cat > "$TMP/settings.gradle" <<G
rootProject.name='apd-core-tests'
include ':SPD-classes',':core',':services'
project(':SPD-classes').projectDir=file('$REPO/SPD-classes')
project(':core').projectDir=file('$REPO/core')
project(':services').projectDir=file('$REPO/services')
G
"$REPO/gradlew" -p "$TMP" :core:test "$@"
echo "Results: $REPO/core/build/test-results/test (XML), $REPO/core/build/reports/tests/test/index.html"
