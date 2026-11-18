#!/usr/bin/env python3
"""Verify and scan exact prepared APD source archives without rebuilding applications.

Copyright (C) 2026 Chris Charlton. SPDX-License-Identifier: GPL-3.0-or-later
"""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import sys
import zipfile

REPOSITORY = "APD-Development/ShieldingShouldHaveWorked"
SCANNER_VERSION = "8.30.1"
SCANNER_ARCHIVE_SHA256 = "551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb"
SCANNER = Path("/tmp/apd-gitleaks/gitleaks")
SCANNER_ARCHIVE = Path("/tmp/apd-gitleaks.tar.gz")
REQUIRED = {"number", "source_commit", "artifact_run", "artifact_head", "artifact_name", "artifact_id", "artifact_sha256"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(path):
    result = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            result.update(chunk)
    return result.hexdigest()


def is_hex(value, length):
    return isinstance(value, str) and re.fullmatch("[0-9a-f]{" + str(length) + "}", value) is not None


def validate_record(record):
    require(isinstance(record, dict), "Build record must be an object")
    require(REQUIRED <= record.keys() <= REQUIRED | {"source_sha256"}, "Unexpected or missing build fields")
    require(type(record["number"]) is int and record["number"] in (439, 440, 441), "Only APD439, APD440 and APD441 are authorized here")
    for key in ("artifact_run", "artifact_id"):
        require(type(record[key]) is int and record[key] > 0, "Invalid " + key)
    for key in ("source_commit", "artifact_head"):
        require(is_hex(record[key], 40), "Invalid " + key)
    require(is_hex(record["artifact_sha256"], 64), "Invalid artifact_sha256")
    if "source_sha256" in record:
        require(is_hex(record["source_sha256"], 64), "Invalid source_sha256")
    require(isinstance(record["artifact_name"], str) and re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,199}", record["artifact_name"]), "Invalid artifact_name")
    return dict(record)


def load_request(path):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    require(isinstance(data, dict) and set(data) == {"schema", "builds"}, "Request must contain schema and builds only")
    require(type(data["schema"]) is int and data["schema"] == 1, "Unsupported request schema")
    require(isinstance(data["builds"], list) and 1 <= len(data["builds"]) <= 3, "Request must select one to three builds")
    records = [validate_record(row) for row in data["builds"]]
    require(len({row["number"] for row in records}) == len(records), "Duplicate build number")
    return records


def gh_json(endpoint):
    return json.loads(subprocess.check_output(["gh", "api", endpoint], text=True))


def validate_artifact_metadata(artifact, record):
    require(artifact.get("id") == record["artifact_id"], "Artifact ID mismatch")
    require(artifact.get("name") == record["artifact_name"], "Artifact name mismatch")
    require(artifact.get("expired") is False, "Artifact is expired or availability is unknown")
    require(artifact.get("digest") == "sha256:" + record["artifact_sha256"], "Artifact metadata digest mismatch")
    require(type(artifact.get("size_in_bytes")) is int and artifact["size_in_bytes"] > 0, "Artifact size missing")
    producer = artifact.get("workflow_run") or {}
    require(producer.get("id") == record["artifact_run"], "Artifact producer run mismatch")
    require(producer.get("head_sha") == record["artifact_head"], "Artifact producer head mismatch")


def safe_members(archive):
    members = archive.infolist()
    require(len({member.filename for member in members}) == len(members), "Duplicate ZIP entry")
    for member in members:
        name = member.filename
        path = PurePosixPath(name)
        require(name and not path.is_absolute() and ".." not in path.parts and "\\" not in name, "Unsafe ZIP path")
        require(not (member.flag_bits & 1), "Encrypted ZIP member is not reviewable")
        require(((member.external_attr >> 16) & 0o170000) != 0o120000, "ZIP symlinks are not allowed")
    return members


def verify_file(path, expected, expected_size=None):
    if expected_size is not None:
        require(Path(path).stat().st_size == expected_size, "Downloaded artifact size mismatch")
    actual = digest(path)
    require(actual == expected, "Downloaded file SHA256 mismatch")
    return actual


def extract_source(artifact_zip, record, destination):
    with zipfile.ZipFile(artifact_zip) as archive:
        members = safe_members(archive)
        sources = [member for member in members if not member.is_dir() and re.fullmatch(r"APD-Source-[0-9]+\.zip", PurePosixPath(member.filename).name)]
        require(len(sources) == 1, "Prepared artifact must contain exactly one source ZIP")
        require(PurePosixPath(sources[0].filename).name == "APD-Source-%d.zip" % record["number"], "Source ZIP build number mismatch")
        require(not Path(destination).exists(), "Never overwrite an existing source archive")
        with archive.open(sources[0]) as source, Path(destination).open("xb") as output:
            shutil.copyfileobj(source, output)
    if "source_sha256" in record:
        verify_file(destination, record["source_sha256"])


def validate_source_zip(source, record):
    prefix = "APD-Source-%d/" % record["number"]
    manifest_path = "CORRESPONDING-SOURCE/manifest.json"
    with zipfile.ZipFile(source) as archive:
        members = safe_members(archive)
        files = {}
        for member in members:
            require(member.filename.startswith(prefix), "Source ZIP has an unexpected root")
            if not member.is_dir():
                relative = member.filename[len(prefix):]
                require(relative, "Empty source file name")
                require(".git" not in PurePosixPath(relative).parts and not relative.lower().endswith((".jks", ".keystore")), "Signing material or Git metadata is present in source")
                files[relative] = member
        require(manifest_path in files, "Corresponding-source manifest missing")
        manifest_bytes = archive.read(files[manifest_path])
        manifest = json.loads(manifest_bytes)
        require(manifest.get("source_commit") == record["source_commit"], "Corresponding-source commit mismatch")
        expected = manifest.get("files")
        require(isinstance(expected, dict) and expected, "Source manifest has no file inventory")
        require(set(files) == set(expected) | {manifest_path}, "Source archive file set differs from its manifest")
        for name, expected_hash in expected.items():
            require(is_hex(expected_hash, 64), "Invalid manifest file digest: " + name)
            actual = hashlib.sha256()
            with archive.open(files[name]) as stream:
                for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                    actual.update(chunk)
            require(actual.hexdigest() == expected_hash, "Source manifest hash mismatch: " + name)
    return {"manifest_file_count": len(expected), "manifest_sha256": hashlib.sha256(manifest_bytes).hexdigest(), "source_sha256": digest(source), "source_commit": manifest["source_commit"]}


def validate_scan_report(report, source_hash):
    supplemental = report.get("supplemental") or {}
    require(supplemental.get("source_sha256") == source_hash, "Scanner report is for a different source ZIP")
    require(supplemental.get("source_findings_reviewed") is True, "Source findings have not passed the existing review rules")
    require(supplemental.get("history_not_rewritten") is True, "History preservation was not recorded")
    for scope in ("source", "history"):
        result = report.get(scope) or {}
        require(type(result.get("exit")) is int and result["exit"] in (0, 1), "Missing or failed " + scope + " scan")
        require(isinstance(result.get("findings"), list), "Missing redacted " + scope + " findings")
        require(not any(item.get("disposition") == "REQUIRES REVIEW" for item in result["findings"]), "Unreviewed " + scope + " finding")


def scan(record, repository, evidence):
    evidence.mkdir(parents=True, exist_ok=False)
    receipt = {"schema": 1, "pins": record, "scan_workflow_run": os.environ.get("GITHUB_RUN_ID"), "scan_workflow_commit": os.environ.get("GITHUB_SHA"), "status": "in_progress", "applications_rebuilt": False, "instrumentation_rebuilt": False, "release_published": False}
    receipt_path = evidence / "prepared-source-scan-verification.json"
    receipt_path.write_text(json.dumps(receipt, indent=2) + "\n")
    try:
        require(subprocess.check_output(["git", "rev-parse", "--is-shallow-repository"], cwd=repository, text=True).strip() == "false", "Reachable-history scan requires a full checkout")
        require(subprocess.check_output(["git", "cat-file", "-t", record["source_commit"]], cwd=repository, text=True).strip() == "commit", "Pinned application source commit is missing from repository history")
        artifact = gh_json("repos/%s/actions/artifacts/%d" % (REPOSITORY, record["artifact_id"]))
        validate_artifact_metadata(artifact, record)
        producer = gh_json("repos/%s/actions/runs/%d" % (REPOSITORY, record["artifact_run"]))
        require(producer.get("id") == record["artifact_run"] and producer.get("head_sha") == record["artifact_head"], "Producing workflow identity mismatch")
        receipt["producer_status"] = producer.get("status")
        receipt["producer_conclusion"] = producer.get("conclusion")
        (evidence / "artifact-metadata.json").write_text(json.dumps(artifact, indent=2) + "\n")
        work = repository / ".apd-source-scan-work" / str(record["number"])
        work.mkdir(parents=True, exist_ok=False)
        artifact_zip = work / "prepared-artifact.zip"
        with artifact_zip.open("xb") as output:
            subprocess.run(["gh", "api", "repos/%s/actions/artifacts/%d/zip" % (REPOSITORY, record["artifact_id"])], stdout=output, check=True)
        verify_file(artifact_zip, record["artifact_sha256"], artifact["size_in_bytes"])
        output = repository / "owner-delivery"
        output.mkdir(exist_ok=True)
        require(not list(output.glob("APD-Source-*.zip")), "Owner-delivery already contains a source ZIP")
        source = output / ("APD-Source-%d.zip" % record["number"])
        extract_source(artifact_zip, record, source)
        receipt.update(validate_source_zip(source, record))
        verify_file(SCANNER_ARCHIVE, SCANNER_ARCHIVE_SHA256)
        version = subprocess.check_output([str(SCANNER), "version"], text=True).strip()
        require(version == SCANNER_VERSION, "Unexpected gitleaks version")
        scanner_script = repository / "tools/apd_qol_source_scan.py"
        receipt.update(scanner_version=version, scanner_archive_sha256=SCANNER_ARCHIVE_SHA256, scanner_binary_sha256=digest(SCANNER), existing_scanner_script_sha256=digest(scanner_script), source_findings_reviewed=False)
        receipt_path.write_text(json.dumps(receipt, indent=2) + "\n")
        with (evidence / "source-scan-wrapper.log").open("w") as log:
            subprocess.run([sys.executable, str(scanner_script)], cwd=repository, stdout=log, stderr=subprocess.STDOUT, check=True)
        require(digest(scanner_script) == receipt["existing_scanner_script_sha256"], "Existing scanner script changed during scanning")
        verify_file(source, receipt["source_sha256"])
        report_path = output / "APD-Source-Scan.json"
        report = json.loads(report_path.read_text())
        validate_scan_report(report, receipt["source_sha256"])
        receipt.update(status="success", source_findings_reviewed=True, source_candidate_count=report["source"].get("candidate_count"), history_candidate_count=report["history"].get("candidate_count"), scan_report_sha256=digest(report_path), completed_utc=datetime.datetime.now(datetime.timezone.utc).isoformat())
    except Exception as error:
        receipt.update(status="failure", failure_type=type(error).__name__, failure=str(error), completed_utc=datetime.datetime.now(datetime.timezone.utc).isoformat())
        raise
    finally:
        receipt_path.write_text(json.dumps(receipt, indent=2) + "\n")
        report_path = repository / "owner-delivery/APD-Source-Scan.json"
        if report_path.is_file():
            shutil.copyfile(report_path, evidence / "APD-Source-Scan.json")
        for name in ("source", "history"):
            for suffix in ("redacted.json", "scan.log"):
                path = Path("/tmp/apd-%s-%s" % (name, suffix))
                if path.is_file():
                    shutil.copyfile(path, evidence / path.name)
    print(json.dumps(receipt, sort_keys=True))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--matrix", type=Path)
    parser.add_argument("--request", type=Path)
    parser.add_argument("--number", type=int)
    args = parser.parse_args()
    if args.matrix:
        require(args.request is None and args.number is None, "Matrix validation cannot also execute a scan")
        print(json.dumps({"include": load_request(args.matrix)}, separators=(",", ":")))
        return
    require(args.request is not None and args.number is not None, "A pinned request and build number are required")
    records = load_request(args.request)
    selected = [row for row in records if row["number"] == args.number]
    require(len(selected) == 1, "Selected build is not in the approved request")
    repository = Path(__file__).resolve().parent.parent
    scan(selected[0], repository, repository / "source-scan-evidence" / str(args.number))


if __name__ == "__main__":
    main()
