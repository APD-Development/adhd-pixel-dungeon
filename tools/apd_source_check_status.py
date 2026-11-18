# ADHD Pixel Dungeon
# Copyright (C) 2026 Chris Charlton
# GPLv3 or later; no warranty. See the root GPL license for full terms.
"""Publish source-check evidence through standard GitHub commit statuses, never APK delivery."""
import json
import os
import pathlib
import re
import urllib.request

REPOSITORY = "APD-Development/ShieldingShouldHaveWorked"
CONTEXT = "APD verified source checks"

def payload(repository, commit, run_id, job_status, expected_tests, proof=None):
    if repository.lower() != REPOSITORY.lower() or not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("Unexpected repository or source commit")
    if type(run_id) is not int or run_id <= 0 or type(expected_tests) is not int or expected_tests <= 0:
        raise ValueError("Missing run or expected test inventory")
    if job_status not in ("success", "failure", "cancelled", "running"):
        raise ValueError("Unknown job outcome")
    state={"success":"success", "failure":"failure", "cancelled":"error", "running":"pending"}[job_status]
    if state == "success":
        if not isinstance(proof, dict) or proof.get("source_commit") != commit:
            raise ValueError("Missing or stale source proof")
        if type(proof.get("suites")) is not int or proof["suites"] <= 0:
            raise ValueError("No core suites")
        if type(proof.get("tests")) is not int or proof["tests"] != expected_tests:
            raise ValueError("Changed test inventory")
        if any(type(proof.get(key)) is not int or proof[key] != 0 for key in ("failures", "errors", "skipped")):
            raise ValueError("Failed, missing or skipped tests")
        if proof.get("apk_assembled") is not False or proof.get("physical_phone_tested") is not False:
            raise ValueError("Unexpected source-only evidence boundary")
        description=f"{expected_tests} core tests; Android debug/release/instrumentation and desktop compilation passed; no APK"
    elif state == "pending":
        description="Source verification running; no completed checks claimed"
    else:
        description="Source verification did not pass; inspect the linked run; no APK"
    return {"state":state, "context":CONTEXT, "description":description,
            "target_url":f"https://github.com/{repository}/actions/runs/{run_id}"}

def main():
    status=os.environ["APD_JOB_STATUS"]
    proof_path=pathlib.Path("qol-verification.json")
    proof=json.loads(proof_path.read_text()) if status == "success" and proof_path.is_file() else None
    data=payload(os.environ["GITHUB_REPOSITORY"],os.environ["GITHUB_SHA"],int(os.environ["GITHUB_RUN_ID"]),
                 status,int(os.environ["APD_EXPECTED_TESTS"]),proof)
    request=urllib.request.Request(
        f"https://api.github.com/repos/{os.environ['GITHUB_REPOSITORY']}/statuses/{os.environ['GITHUB_SHA']}",
        data=json.dumps(data).encode(),method="POST",
        headers={"Authorization":"Bearer "+os.environ["GH_TOKEN"],"Accept":"application/vnd.github+json",
                 "Content-Type":"application/json","X-GitHub-Api-Version":"2022-11-28"})
    with urllib.request.urlopen(request,timeout=30) as response:
        published=json.load(response)
    if published.get("state") != data["state"] or published.get("target_url") != data["target_url"]:
        raise RuntimeError("Commit status publication response did not match")
    print(json.dumps({"source_commit":os.environ["GITHUB_SHA"],"state":published["state"],
                      "context":CONTEXT,"target_url":published["target_url"],"apk_delivered":False}))

if __name__ == "__main__":
    main()
