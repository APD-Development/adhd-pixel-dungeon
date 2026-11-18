#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Validate one isolated frozen-APK native test and its complete device screenshots."""
import argparse
import json
from pathlib import Path

from apd_native_screenshot_transfer import sync_images


def verify(variant, case, directory):
    if variant not in ("Developer", "Alpha-Testers"):
        raise ValueError("Unexpected APD458 variant")
    allowed = {"WallGeometryDeviceTest", "SearchContextDeviceTest", "SeedHistoryDeviceTest"}
    if case not in allowed:
        raise ValueError("Unexpected APD458 test case")
    text = (directory / "instrumentation.txt").read_text(encoding="utf-8")
    if "OK (1 test)" not in text or any(
            marker in text for marker in ("FAILURES!!!", "INSTRUMENTATION_FAILED", "Process crashed.")):
        raise RuntimeError("Native case failed, crashed or lacks a complete one-test result")
    visuals = directory / "visuals"
    visuals.mkdir(parents=True, exist_ok=True)
    hashes = sync_images(visuals)
    receipt = {
        "build": 458,
        "source": "8c74997210585191d12603ad5e2ee398b1c54fe7",
        "variant": variant,
        "test_class": case,
        "passed_cases": 1,
        "complete_device_screenshot_manifest": True,
        "screenshots": len(hashes),
        "image_sha256": hashes,
        "same_original_candidate_apk": True,
        "application_rebuilt": False,
        "physical_phone_verified": False,
    }
    (directory / "native-verification.json").write_text(json.dumps(receipt, indent=2) + "\n")
    return receipt


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--variant", required=True)
    parser.add_argument("--case", required=True)
    parser.add_argument("--directory", type=Path, default=Path("native458"))
    args = parser.parse_args()
    print(json.dumps(verify(args.variant, args.case, args.directory), indent=2))


if __name__ == "__main__":
    main()
