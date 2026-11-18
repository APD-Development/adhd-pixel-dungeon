#!/usr/bin/env python3
"""Verify APD466 frozen APK native case and all device screenshots."""
import argparse
import hashlib
import json
import subprocess
import time
from pathlib import Path
from apd_native_screenshot_transfer import sync_images, REMOTE
from validate_android_visuals import validate as validate_png

def verify(variant, case, directory, source):
    if variant not in ("Developer", "Alpha-Testers"):
        raise ValueError("Unknown variant")
    if case not in ("WallGeometryDeviceTest", "SearchContextDeviceTest", "SeedHistoryDeviceTest"):
        raise ValueError("Unknown native case")
    output = (directory / "instrumentation.txt").read_text(encoding="utf-8")
    if "OK (1 test)" not in output or any(x in output for x in ("FAILURES!!!", "INSTRUMENTATION_FAILED", "Process crashed.")):
        raise RuntimeError("Native test did not pass one complete case")
    # Keep the original strict complete-manifest verifier. If adb pull's
    # transport fails, recover the *same* device file via exec-out and verify
    # its bytes against device SHA-256, never accepting partial evidence.
    from apd_native_screenshot_transfer import device_manifest, pull_image
    visuals = directory / "visuals"
    visuals.mkdir(parents=True, exist_ok=True)
    first = device_manifest("cd " + REMOTE + " && sha256sum -- *.png")
    for name, digest in first.items():
        last_error = None
        for attempt in range(3):
            try:
                pull_image(visuals, name, digest)
                last_error = None
                break
            except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError) as exc:
                last_error = exc
                if isinstance(exc, subprocess.CalledProcessError):
                    print("adb pull transport diagnostic:", exc.stderr, flush=True)
                time.sleep(1)
        if last_error is not None:
            data = subprocess.check_output(["adb", "exec-out", "cat", REMOTE + "/" + name], timeout=45)
            validate_png(data)
            if hashlib.sha256(data).hexdigest() != digest:
                raise RuntimeError("Screenshot exec-out digest mismatch: " + name)
            (visuals / name).write_bytes(data)
    after = device_manifest("cd " + REMOTE + " && sha256sum -- *.png")
    if first != after:
        raise RuntimeError("Device screenshot manifest changed during transfer")
    screenshots = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in visuals.glob("*.png")}
    if screenshots != first:
        raise RuntimeError("Incomplete or corrupt native visual evidence")
    if not screenshots:
        raise RuntimeError("Missing native screenshots")
    receipt = {
        "build": 466, "source": source, "variant": variant, "case": case,
        "tests": 1, "screenshots": screenshots, "same_frozen_apk": True,
        "device_transfer_complete": True, "physical_phone_verified": False,
    }
    (directory / "native-verification.json").write_text(json.dumps(receipt, indent=2) + "\n")
    return receipt

if __name__ == "__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--variant", required=True)
    p.add_argument("--case", required=True)
    p.add_argument("--directory", type=Path, required=True)
    p.add_argument("--source", required=True)
    a=p.parse_args()
    print(json.dumps(verify(a.variant,a.case,a.directory,a.source),indent=2))
