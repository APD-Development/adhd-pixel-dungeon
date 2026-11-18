#!/usr/bin/env bash
# Runs precompiled simulator APKs on a new disposable CI emulator.
set -uo pipefail
apd_device_hardware=$(adb shell getprop ro.hardware | tr -d '\r')
case "$apd_device_hardware" in
  ranchu|goldfish) ;;
  *) echo 'Refusing simulation outside a disposable Android emulator.' >&2; exit 1 ;;
esac
python3 - <<'PY'
import json, os
from pathlib import Path
expected = os.environ.get('APD_SIMULATION_SOURCE')
manifest = json.loads(Path('simulator-binaries/manifest.json').read_text())
if not expected or manifest.get('sourceCommit') != expected:
    raise SystemExit('Simulator source does not match the verified baseline.')
PY
apd_manifest_status=$?
if [ "$apd_manifest_status" -ne 0 ]; then exit "$apd_manifest_status"; fi
sha256sum --check simulator-binaries/SHA256SUMS
apd_checksum_status=$?
if [ "$apd_checksum_status" -ne 0 ]; then exit "$apd_checksum_status"; fi
adb install -r simulator-binaries/android-debug.apk
apd_install_status=$?
if [ "$apd_install_status" -ne 0 ]; then exit "$apd_install_status"; fi
adb install -r simulator-binaries/android-debug-androidTest.apk
apd_install_status=$?
if [ "$apd_install_status" -ne 0 ]; then exit "$apd_install_status"; fi
adb logcat -c
adb logcat -v time > simulator-logcat.txt &
apd_logcat_pid=$!
trap 'kill "$apd_logcat_pid" 2>/dev/null || true' EXIT
apd_seed_arguments=()
if [ -n "${APD_SIMULATION_SEEDS:-}" ]; then
  apd_seed_arguments=(-e simulationSeeds "$APD_SIMULATION_SEEDS")
fi
adb shell am instrument -w -r \
  -e class com.shatteredpixel.shatteredpixeldungeon.android.WarriorSimulationDeviceTest \
  -e apdSimulation true -e simulationCount "${APD_SIMULATION_COUNT:-20}" \
  -e simulationSeed "${APD_SIMULATION_SEED:-1000000}" \
  -e simulationActions "${APD_SIMULATION_ACTIONS:-1000}" \
  "${apd_seed_arguments[@]}" \
  com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner \
  > simulation-instrumentation.txt 2>&1
apd_test_status=$?
adb exec-out run-as com.charltonchris29.adhdpixeldungeon.indev cat files/simulation-results.jsonl > simulation-results.jsonl
apd_trace_status=$?
if [ "$apd_test_status" -ne 0 ]; then exit "$apd_test_status"; fi
if [ "$apd_trace_status" -ne 0 ]; then exit "$apd_trace_status"; fi
python3 - <<'PY'
import json, os
from pathlib import Path
text = Path('simulation-instrumentation.txt').read_text()
if 'OK (1 test)' not in text or 'FAILURES!!!' in text or 'INSTRUMENTATION_FAILED' in text:
    raise SystemExit('Instrumentation did not report a passing test; inspect saved output.')
rows = [json.loads(line) for line in Path('simulation-results.jsonl').read_text().splitlines() if line.strip()]
count, seed = int(os.environ.get('APD_SIMULATION_COUNT', '20')), int(os.environ.get('APD_SIMULATION_SEED', '1000000'))
explicit = os.environ.get('APD_SIMULATION_SEEDS', '')
expected_seeds = [int(s.strip()) for s in explicit.split(',')] if explicit else list(range(seed, seed + count))
if len(expected_seeds) != count or len(set(expected_seeds)) != count:
    raise SystemExit('Invalid requested seed plan.')
if len(rows) != count or [r['seed'] for r in rows] != expected_seeds:
    raise SystemExit('Missing, duplicate or unexpected seed results.')
if not all(r['completed'] is True and r['elapsedMillis'] >= 0 and r['trace'] for r in rows):
    raise SystemExit('Incomplete episode or missing action evidence; no successful batch claimed.')
print(f'{count} actual early-game episodes completed. No counterfactual survival claim.')
PY
