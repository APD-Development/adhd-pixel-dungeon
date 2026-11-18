# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
import copy
import hashlib
import io
import json
import os
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch
import zipfile
import zlib

import apd_recover439_440 as recovery

from apd_recover439_440 import (CASES, SPECS, check_core, check_images, check_instrumentation,
                                extract_archive, safe_name, sanitize_snapshot)


class RecoverySafeguardsTest(unittest.TestCase):
    image_names = (
        'wall-geometry-corner-no-false-check.png', 'wall-geometry-valid-searched-face.png',
        'wall-geometry-retained-after-zoom-pan-pause-rebuild.png', 'wall-geometry-door-no-stale-check.png',
        'trap-discovery-setting-on.png', 'trap-discovery-setting-restarted.png',
        'trap-discovery-setting-off.png', 'trap-discovery-runtime-actual-ooze.png',
        *('qol-automatic-inventory-' + str(index) + '.png' for index in range(5)),
    )

    @staticmethod
    def png(width=320, height=640, compressed_pixels=None):
        def chunk(kind, payload):
            return (struct.pack('>I', len(payload)) + kind + payload
                    + struct.pack('>I', zlib.crc32(kind + payload) & 0xffffffff))

        if compressed_pixels is None:
            compressed_pixels = zlib.compress((b'\0' + b'\x20\x60\xc0' * width) * height, 0)
        return (b'\x89PNG\r\n\x1a\n'
                + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0))
                + chunk(b'tEXt', b'fixture\0' + b'x' * 2100)
                + chunk(b'IDAT', compressed_pixels) + chunk(b'IEND', b''))

    def write_images(self, root):
        data = self.png()
        for name in self.image_names:
            (root / name).write_bytes(data)
        return data

    def native_text(self):
        return ('INSTRUMENTATION_STATUS: class=example.NativeTest\n'
                'INSTRUMENTATION_STATUS: test=expectedMethod\n'
                'INSTRUMENTATION_STATUS_CODE: 1\n'
                'INSTRUMENTATION_STATUS: class=example.NativeTest\n'
                'INSTRUMENTATION_STATUS: test=expectedMethod\n'
                'INSTRUMENTATION_STATUS_CODE: 0\n'
                'OK (1 test)\nINSTRUMENTATION_CODE: -1\n')

    def test_requires_completed_exact_native_case(self):
        text = self.native_text()
        check_instrumentation(text, 'example.NativeTest', 'expectedMethod')
        for broken in (text.replace('expectedMethod', 'anotherMethod'),
                       text.replace('example.NativeTest', 'example.OtherTest'),
                       text.replace('INSTRUMENTATION_CODE: -1', ''),
                       text.replace('OK (1 test)', ''),
                       text.replace('STATUS_CODE: 0', 'STATUS_CODE: -3'),
                       text + 'INSTRUMENTATION_STATUS_CODE: 0\n',
                       text + 'AssumptionFailure\n'):
            with self.subTest(output=broken[-100:]), self.assertRaises(RuntimeError):
                check_instrumentation(broken, 'example.NativeTest', 'expectedMethod')

    def test_six_legacy_cases_include_fresh_process_and_stall(self):
        self.assertEqual([case[0] for case in CASES], ['qol', 'wall', 'seed', 'restart', 'gameplay', 'freeze'])
        self.assertEqual(len({(case[1], case[2]) for case in CASES}), 6)
        self.assertEqual(CASES[3][2], 'b_freshProcessReadsEnabledAndCanDisable')

    def finalize_fixture(self, directory, cases, omitted=None):
        root = directory / 'candidate'
        native = directory / 'native'
        number = 445
        apk_dir = root / 'built/owner-delivery'
        apk_dir.mkdir(parents=True)
        source_dir = root / 'source'
        source_dir.mkdir()
        source = source_dir / f'APD-Source-{number}.zip'
        source.write_bytes(b'unit-test corresponding-source fixture')
        Path(str(source) + '.sha256').write_text(recovery.sha(source))
        (root / 'preparation.json').write_text(json.dumps({'source_archive_sha256': recovery.sha(source)}))
        spec = {'source': 'a' * 40, 'hashes': {}}
        for variant in recovery.FEATURES:
            apk = apk_dir / f'APD-{variant}-{number}.apk'
            apk.write_bytes(('unit-test application fixture ' + variant).encode())
            spec['hashes'][variant] = recovery.sha(apk)
            evidence = native / f'apd-recovery-{number}-native-{variant}'
            visuals = evidence / 'visuals'
            visuals.mkdir(parents=True)
            self.write_images(visuals)
            completed = []
            for label, short_class, method, _ in cases:
                if label == omitted:
                    continue
                report = self.native_text().replace('example.NativeTest', recovery.CLASS_PREFIX + short_class).replace('expectedMethod', method)
                (evidence / (label + '.txt')).write_text(report)
                completed.append(label)
            (evidence / 'native-logcat.txt').write_text('unit-test captured logcat fixture\n')
            (evidence / 'native-verification.json').write_text(json.dumps({
                'number': number, 'variant': variant, 'source_commit': spec['source'],
                'apk_sha256': spec['hashes'][variant], 'instrumentation_sha256': 'b' * 64,
                'completed_cases': completed, 'native_recovery_complete': True,
                'images': recovery.check_images(visuals),
            }))
        return root, native, spec

    def test_finalize_counts_only_the_complete_configured_native_inventory(self):
        additional = tuple((f'extra-{i}', f'Extra{i}Test', f'case{i}', 30) for i in range(3))
        for cases in (CASES, CASES + additional):
            with self.subTest(cases=len(cases)), tempfile.TemporaryDirectory() as temporary:
                directory = Path(temporary)
                root, native, spec = self.finalize_fixture(directory, cases)
                destination = directory / 'accepted'
                # Pair/signature compilation is outside this exact-case aggregation unit test.
                with patch.object(recovery, 'CASES', cases), patch.dict(recovery.SPECS, {445: spec}), \
                        patch.object(recovery, 'check_pair', return_value=({}, {'instrumentation_sha256': 'b' * 64})), \
                        patch.dict(os.environ, {'GITHUB_RUN_ID': '1', 'GITHUB_SHA': spec['source']}):
                    recovery.finalize(445, root, native, destination)
                proof = json.loads((destination / 'APD-445-Recovery-Verification.json').read_text())
                self.assertEqual(proof['android_tests'], len(cases) * len(recovery.FEATURES))
                self.assertFalse(proof['manual_visual_review_complete'])

    def test_finalize_rejects_an_omitted_additional_native_case(self):
        cases = CASES + (('new-required', 'NewRequiredTest', 'requiredCase', 30),)
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            root, native, spec = self.finalize_fixture(directory, cases, omitted='new-required')
            destination = directory / 'accepted'
            with patch.object(recovery, 'CASES', cases), patch.dict(recovery.SPECS, {445: spec}), \
                    patch.object(recovery, 'check_pair', return_value=({}, {'instrumentation_sha256': 'b' * 64})), \
                    patch.dict(os.environ, {'GITHUB_RUN_ID': '1', 'GITHUB_SHA': spec['source']}), \
                    self.assertRaisesRegex(RuntimeError, 'identity/case set differs'):
                recovery.finalize(445, root, native, destination)
            self.assertFalse(destination.exists())

    def test_native_screenshots_decode_and_keep_exact_hashes(self):
        with tempfile.TemporaryDirectory() as temporary:
            data = self.write_images(Path(temporary))
            self.assertEqual(check_images(temporary),
                             {name: hashlib.sha256(data).hexdigest() for name in self.image_names})

    def test_native_screenshots_reject_corruption_and_placeholder_images(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            data = self.write_images(root)
            damaged_crc = bytearray(data)
            damaged_crc[data.index(b'IDAT') + 8] ^= 1
            # These have valid PNG chunk CRCs but cannot decode complete screen pixels.
            invalid_pixels = self.png(compressed_pixels=b'not a zlib pixel stream')
            missing_pixels = self.png(compressed_pixels=zlib.compress(b''))
            invalid_filter = self.png(compressed_pixels=zlib.compress((b'\x05' + b'\0' * 960) * 640))
            placeholder = self.png(width=1, height=1)
            target = root / self.image_names[0]
            for label, broken in (('CRC', damaged_crc), ('pixel stream', invalid_pixels),
                                  ('missing rows', missing_pixels), ('row filter', invalid_filter),
                                  ('placeholder', placeholder),
                                  ('truncated', data[:-5])):
                target.write_bytes(broken)
                with self.subTest(damage=label), self.assertRaises(RuntimeError):
                    check_images(root)

    def test_native_screenshots_require_complete_existing_capture_set(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.write_images(root)
            (root / 'trap-discovery-setting-restarted.png').unlink()
            with self.assertRaises(RuntimeError):
                check_images(root)

    def test_rejects_path_escape_and_artifact_symlink(self):
        for name in ('../outside', '/absolute', 'a/../../outside', 'a\\..\\outside'):
            with self.subTest(name=name), self.assertRaises(RuntimeError):
                safe_name(name)
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            archive = root / 'bad.zip'
            with zipfile.ZipFile(archive, 'w') as packed:
                entry = zipfile.ZipInfo('link')
                entry.external_attr = 0o120777 << 16
                packed.writestr(entry, '../outside')
            with self.assertRaises(RuntimeError):
                extract_archive(archive, root / 'unpacked')
            self.assertFalse((root / 'unpacked').exists())

    def test_source_removes_keys_credentials_and_externalizes_signing(self):
        snapshot = {
            'android/build.gradle': b"storeFile rootProject.file('keystore/apd-debug.jks')\nstorePassword 'example-private-password'\nkeyPassword 'example-private-password'\nsigningConfig signingConfigs.apdDebug\n",
            'keystore/apd-debug.jks': b'keybytes',
            'other/copy.P12': b'keybytes',
            'docs/old-signing.md': b'example-private-password',
            'core/Unchanged.java': b'package test;\n',
        }
        removed, substitutions = sanitize_snapshot(snapshot)
        self.assertEqual(set(removed), {'keystore/apd-debug.jks', 'other/copy.P12'})
        self.assertIn('docs/old-signing.md', substitutions)
        self.assertTrue(all(b'example-private-password' not in blob for blob in snapshot.values()))
        self.assertIn(b"System.getenv('APD_SIGNING_KEYSTORE')", snapshot['android/build.gradle'])
        self.assertIn(b'apdUnsignedVerification', snapshot['android/build.gradle'])
        self.assertEqual(snapshot['core/Unchanged.java'], b'package test;\n')

    def test_unrecognized_signing_configuration_fails_closed(self):
        with self.assertRaises(RuntimeError):
            sanitize_snapshot({'android/build.gradle': b'unknownSigningConfiguration()'})

    def test_core_rejects_duplicate_hidden_skipped_and_incomplete_results(self):
        previous = SPECS[439]
        SPECS[439] = dict(previous, suites=1, tests=2)
        try:
            good = ('<testsuite tests="2" failures="0" errors="0" skipped="0">'
                    '<testcase classname="A" name="one"/><testcase classname="A" name="two"/>'
                    '</testsuite>')
            with tempfile.TemporaryDirectory() as temporary:
                path = Path(temporary) / 'TEST-test.xml'
                path.write_text(good)
                self.assertEqual(check_core(temporary, 439)['tests'], 2)
                broken = (good.replace('name="two"', 'name="one"'),
                          good.replace('<testcase classname="A" name="two"/>', ''),
                          good.replace('name="two"/>', 'name="two"><skipped/></testcase>'),
                          good.replace('failures="0"', 'failures="1"'))
                for text in broken:
                    path.write_text(text)
                    with self.subTest(text=text), self.assertRaises(RuntimeError):
                        check_core(temporary, 439)
        finally:
            SPECS[439] = previous


if __name__ == '__main__':
    unittest.main()
