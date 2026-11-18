# TEST109 — item-history reader reliability and verified feature news

## Implemented, awaiting Android verification
ItemHistory.ancestors previously returned the queried item as its own ancestor when recorded lineage contained a self-reference or cycle. Seed the visited set with the queried identifier. ItemHistory.allUids previously omitted identifiers mentioned only by equipment, removal, knowledge, truth or lineage events despite its any-item-event contract. Include every supported item event in payload appearance order.

These are derived, read-only utility corrections; no production caller currently uses these helpers. This is not a newly reproduced inventory pickup problem, nor evidence that normal recording creates cyclic lineage. Raw logs, saves, record encoding and gameplay remain unchanged.

Four new regression methods use encoded logs, cover self-reference, multi-item cycles, sparse events and lineage-only identifiers, and verify retained raw bytes. All four failed on the original implementation. The corrected four plus existing item-event, identity, lifecycle and rollback tests passed locally: 40 unique methods, 1.506 seconds. This is host JVM evidence, not phone testing.

Added one short offline News announcement about deterministic metamorphosis already verified and delivered in TEST108. The feature itself remains attributed to its first compiled APK, TEST107; TEST109 introduces only its announcement. Existing two news articles are retained byte-for-byte. Native News coverage now derives the expected article count from the packaged published entries, while still opening and closing every card popup.

## Pending checks and risks
Required full core suite (expected1304), debug build and21 API34 instrumentation tests/22 valid screenshots have not run for this source yet. No new verified APK claim. Sparse-log identifier lists can correctly contain more identifiers; cyclic ancestry excludes the root while preserving other reachable ancestors. The new article must pass native layout and popup checks. No physical-phone or owner-save testing performed.

## Continuing work
The comprehensive UIMSD audit is not complete. Unmatched rollback uncertainty is under investigation; no unsafe guessed lower-bound conversion has been implemented. No persistent unattended Codex worker has been demonstrated.

## Completed verification
Source845972359883207d3b2dea41861e25ae3610bab3; workflow38367192732,08:07:58–08:19:04UTC. Actual downloaded XML:1304unique core and21 API34 instrumentation methods,0failed/error/skipped.22valid PNGs. Successful debug build; native News card popup test includes all3published articles. Verified candidate not privately delivered: TEST110 follows with additional confirmed UIMSD fixes. Last delivered TEST108 remains available. No physical-phone claim.
