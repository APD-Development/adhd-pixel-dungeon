#!/usr/bin/env python3
"""Require the latest published APD checkpoint to exist in the completed ledger.

The guard intentionally reads documentation only.  It does not infer publication from a
workflow name and it does not mutate either file.
"""

from __future__ import annotations

import argparse
import dataclasses
import pathlib
import re
import sys


PUBLISHED_HEADER = re.compile(
    r"^##\s+[^\n]+—\s+VERIFIED private APK(?P<ordinal>\d+) pair published\b",
    re.MULTILINE,
)
SHA256 = r"[0-9a-f]{64}"
COMMIT = r"[0-9a-f]{40}"


class CheckpointError(ValueError):
    """The documentation cannot prove one exact published checkpoint."""


@dataclasses.dataclass(frozen=True)
class PublishedCheckpoint:
    ordinal: int
    application_source: str
    developer_sha256: str
    alpha_testers_sha256: str


def _first_section(text: str, start: int) -> str:
    following = re.search(r"^##\s+", text[start + 3 :], re.MULTILINE)
    if following is None:
        return text[start:]
    return text[start : start + 3 + following.start()]


def _required(pattern: str, text: str, label: str) -> str:
    match = re.search(pattern, text, re.IGNORECASE)
    if match is None:
        raise CheckpointError(f"latest published HANDOFF section lacks {label}")
    return match.group(1).lower()


def latest_published_checkpoint(handoff: str) -> PublishedCheckpoint:
    header = PUBLISHED_HEADER.search(handoff)
    if header is None:
        raise CheckpointError("HANDOFF has no verified private APK publication header")
    section = _first_section(handoff, header.start())
    return PublishedCheckpoint(
        ordinal=int(header.group("ordinal")),
        application_source=_required(
            rf"Application source\s+({COMMIT})", section, "application source"
        ),
        developer_sha256=_required(
            rf"Developer:[^\n]*SHA256\s+({SHA256})", section, "Developer SHA256"
        ),
        alpha_testers_sha256=_required(
            rf"Alpha-Testers:[^\n]*SHA256\s+({SHA256})",
            section,
            "Alpha-Testers SHA256",
        ),
    )


def verify_completed_ledger(handoff: str, ledger: str) -> PublishedCheckpoint:
    checkpoint = latest_published_checkpoint(handoff)
    marker = re.compile(rf"\b(?:TEST|APK)\s*{checkpoint.ordinal}\b", re.IGNORECASE)
    match = marker.search(ledger)
    if match is None:
        raise CheckpointError(
            f"completed ledger lacks published TEST{checkpoint.ordinal} checkpoint"
        )
    section_start = ledger.rfind("\n## ", 0, match.start())
    section_start = 0 if section_start < 0 else section_start + 1
    section = _first_section(ledger, section_start)
    required = {
        "application source": checkpoint.application_source,
        "Developer SHA256": checkpoint.developer_sha256,
        "Alpha-Testers SHA256": checkpoint.alpha_testers_sha256,
    }
    missing = [label for label, value in required.items() if value not in section.lower()]
    if missing:
        raise CheckpointError(
            f"TEST{checkpoint.ordinal} ledger section lacks " + ", ".join(missing)
        )
    return checkpoint


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("handoff", type=pathlib.Path)
    parser.add_argument("ledger", type=pathlib.Path)
    args = parser.parse_args(argv)
    try:
        checkpoint = verify_completed_ledger(
            args.handoff.read_text(encoding="utf-8"),
            args.ledger.read_text(encoding="utf-8"),
        )
    except (OSError, CheckpointError) as exc:
        print(f"APD documentation checkpoint guard: FAIL: {exc}", file=sys.stderr)
        return 1
    print(
        "APD documentation checkpoint guard: PASS: "
        f"TEST{checkpoint.ordinal} {checkpoint.application_source}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
