#!/usr/bin/env python3
"""Reject substituted or incomplete prepared-source scan inputs without network access.

Copyright (C) 2026 Chris Charlton. SPDX-License-Identifier: GPL-3.0-or-later
"""
import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile

import apd_scan_prepared_sources as scan


class PreparedSourceGuardsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.record = {
            "number": 439, "source_commit": "a" * 40,
            "artifact_run": 101, "artifact_head": "b" * 40,
            "artifact_name": "apd-recovery-439-original-pair-and-source",
            "artifact_id": 202, "artifact_sha256": "c" * 64,
        }

    def source_zip(self, *, commit=None, contents=b"verified source\n", inventory=None, extra=()):
        archive = self.directory / "APD-Source-439.zip"
        manifest = {
            "source_commit": self.record["source_commit"] if commit is None else commit,
            "files": {"core/example.txt": hashlib.sha256(contents).hexdigest()} if inventory is None else inventory,
        }
        with zipfile.ZipFile(archive, "w") as output:
            output.writestr("APD-Source-439/core/example.txt", contents)
            output.writestr("APD-Source-439/CORRESPONDING-SOURCE/manifest.json", json.dumps(manifest))
            for name, content in extra:
                output.writestr(name, content)
        return archive

    def metadata(self):
        return {"id": 202, "name": self.record["artifact_name"], "expired": False,
                "digest": "sha256:" + self.record["artifact_sha256"], "size_in_bytes": 100,
                "workflow_run": {"id": 101, "head_sha": "b" * 40}}

    def report(self):
        return {"source": {"exit": 0, "findings": []},
                "history": {"exit": 1, "findings": [{"disposition": "History retained; findings reported, not erased."}]},
                "supplemental": {"source_sha256": "d" * 64,
                                 "source_findings_reviewed": True, "history_not_rewritten": True}}

    def test_exact_source_and_retained_history_are_accepted(self):
        source = self.source_zip()
        result = scan.validate_source_zip(source, self.record)
        self.assertEqual(result["manifest_file_count"], 1)
        self.assertEqual(result["source_sha256"], scan.digest(source))
        scan.validate_artifact_metadata(self.metadata(), self.record)
        scan.validate_scan_report(self.report(), "d" * 64)

    def test_substituted_artifact_metadata_is_rejected(self):
        for field, value in (("id", 999), ("name", "other-artifact"), ("expired", True),
                             ("digest", "sha256:" + "f" * 64), ("size_in_bytes", 0)):
            with self.subTest(field=field):
                metadata = self.metadata()
                metadata[field] = value
                with self.assertRaises(ValueError):
                    scan.validate_artifact_metadata(metadata, self.record)
        for field, value in (("id", 999), ("head_sha", "f" * 40)):
            with self.subTest(producer_field=field):
                metadata = self.metadata()
                metadata["workflow_run"][field] = value
                with self.assertRaises(ValueError):
                    scan.validate_artifact_metadata(metadata, self.record)

    def test_download_byte_hash_and_size_are_both_required(self):
        source = self.source_zip()
        with self.assertRaisesRegex(ValueError, "SHA256"):
            scan.verify_file(source, "f" * 64)
        with self.assertRaisesRegex(ValueError, "size"):
            scan.verify_file(source, scan.digest(source), source.stat().st_size + 1)

    def test_different_source_commit_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "commit mismatch"):
            scan.validate_source_zip(self.source_zip(commit="f" * 40), self.record)

    def test_changed_source_file_is_rejected(self):
        source = self.source_zip(inventory={"core/example.txt": hashlib.sha256(b"old source").hexdigest()})
        with self.assertRaisesRegex(ValueError, "hash mismatch"):
            scan.validate_source_zip(source, self.record)

    def test_missing_or_uninventoried_source_files_are_rejected(self):
        for inventory, extra in (({"core/missing.txt": "f" * 64}, ()),
                                 (None, (("APD-Source-439/unlisted.txt", b"extra"),))):
            with self.subTest(inventory=inventory):
                with self.assertRaisesRegex(ValueError, "file set"):
                    scan.validate_source_zip(self.source_zip(inventory=inventory, extra=extra), self.record)

    def test_traversal_duplicate_and_symlink_zip_members_are_rejected(self):
        for filename in ("../outside", "/absolute", "APD-Source-439/../outside", "folder\\outside"):
            with self.subTest(filename=filename):
                with self.assertRaisesRegex(ValueError, "Unsafe ZIP"):
                    scan.validate_source_zip(self.source_zip(extra=((filename, b"x"),)), self.record)
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            source = self.source_zip(extra=(("APD-Source-439/core/example.txt", b"duplicate"),))
        with self.assertRaisesRegex(ValueError, "Duplicate ZIP"):
            scan.validate_source_zip(source, self.record)
        source = self.source_zip()
        with zipfile.ZipFile(source, "a") as archive:
            link = zipfile.ZipInfo("APD-Source-439/link")
            link.create_system = 3
            link.external_attr = 0o120777 << 16
            archive.writestr(link, "../outside")
        with self.assertRaisesRegex(ValueError, "symlinks"):
            scan.validate_source_zip(source, self.record)

    def test_signing_material_and_git_files_are_rejected(self):
        for name in ("signing/release.jks", "signing/release.KEYSTORE", ".git/config"):
            with self.subTest(filename=name):
                source = self.source_zip(extra=(("APD-Source-439/" + name, b"x"),))
                with self.assertRaisesRegex(ValueError, "Signing material or Git"):
                    scan.validate_source_zip(source, self.record)

    def test_artifact_must_have_exactly_one_matching_source_archive(self):
        source = self.source_zip()
        for names in ((), ("APD-Source-440.zip",), ("APD-Source-439.zip", "APD-Source-440.zip")):
            with self.subTest(names=names):
                artifact = self.directory / "artifact.zip"
                with zipfile.ZipFile(artifact, "w") as archive:
                    for name in names:
                        archive.write(source, "source/" + name)
                with self.assertRaises(ValueError):
                    scan.extract_source(artifact, self.record, self.directory / "extracted.zip")

    def test_optional_source_pin_and_no_overwrite_are_enforced(self):
        source = self.source_zip()
        artifact = self.directory / "artifact.zip"
        with zipfile.ZipFile(artifact, "w") as archive:
            archive.write(source, "source/APD-Source-439.zip")
        wrong = dict(self.record, source_sha256="f" * 64)
        destination = self.directory / "extracted.zip"
        with self.assertRaisesRegex(ValueError, "SHA256"):
            scan.extract_source(artifact, wrong, destination)
        with self.assertRaisesRegex(ValueError, "overwrite"):
            scan.extract_source(artifact, self.record, destination)

    def test_unbound_unreviewed_or_failed_scan_reports_are_rejected(self):
        for field, value in (("source_sha256", "f" * 64), ("source_findings_reviewed", False),
                             ("history_not_rewritten", False)):
            with self.subTest(field=field):
                report = self.report()
                report["supplemental"][field] = value
                with self.assertRaises(ValueError):
                    scan.validate_scan_report(report, "d" * 64)
        for scope in ("source", "history"):
            with self.subTest(scope=scope):
                report = self.report()
                report[scope]["exit"] = 2
                with self.assertRaises(ValueError):
                    scan.validate_scan_report(report, "d" * 64)
        report = self.report()
        report["source"]["findings"] = [{"disposition": "REQUIRES REVIEW"}]
        with self.assertRaisesRegex(ValueError, "Unreviewed"):
            scan.validate_scan_report(report, "d" * 64)

    def test_unpinned_out_of_scope_and_duplicate_requests_are_rejected(self):
        request = self.directory / "request.json"
        variants = []
        missing = copy.deepcopy(self.record)
        del missing["artifact_sha256"]
        variants.append({"schema": 1, "builds": [missing]})
        variants.append({"schema": 1, "builds": [dict(self.record, number=442)]})
        variants.append({"schema": 1, "builds": [self.record, self.record]})
        variants.append({"schema": True, "builds": [self.record]})
        for data in variants:
            with self.subTest(request=data):
                request.write_text(json.dumps(data))
                with self.assertRaises(ValueError):
                    scan.load_request(request)
        request.write_text(json.dumps({"schema": 1, "builds": [self.record]}))
        self.assertEqual(scan.load_request(request), [self.record])


if __name__ == "__main__":
    unittest.main()
