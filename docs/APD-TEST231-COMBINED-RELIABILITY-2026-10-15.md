# APD combined reliability candidate — October 15, 2026

## Source and actual verification
This report describes TEST231, application source 5134ef6438f9562a37b3f8db727d5d004fa93527, successful workflow 38837749762. The delivery wrapper is not the compiled application source. Reuse this existing APK; no rebuild is required.

Independently downloaded and parsed core XML contains 1,693 unique tests with zero failures, errors or skips. Android XML contains 33 unique API34 emulator tests with zero failures, errors or skips. All 42 screenshots decode at 320 by 640 pixels; the complete contact sheet was visually reviewed. Debug assembly and the compiled-history gate passed. Emulator evidence does not establish physical RedMagic/Pixel testing or acceptance of the owner's actual save.

Core artifact 12019543747 ZIP SHA256 1c45ea9a4f12713250ac0677d5743e372ed5e45411a39e8c5ddb6408023f0ee4.
Android artifact 12020332852 ZIP SHA256 c82f76ca457f845c3693e3573a969378406d1fd4d39ac696ab5529dc07228be5.
APK artifact 12019893382 ZIP SHA256 628e81017d06409ab3e4e55c346e29b31f6f96597ad06faac592d4ccf0612e8d. This archive digest is not the APK payload checksum. The staging verification appendix must establish actual APK checksum, package, version and signing compatibility against privately published TEST226.

## Changes since privately published TEST226
- Preserve partially readable Cleric collection ledgers: the old automatic save could overwrite skipped damaged bytes, and a replacement fallback could replace a directory. The correction refuses unsafe automatic writes after partial reads, serializes temporary-file access, preserves directory destinations and keeps the existing explicit player reset. No lore entries or triggers were changed.
- Restore UIMSD tables when a saved layout contains only hidden or retired columns. Use currently visible columns and fall back to existing view defaults when no saved column is usable, preserving valid order and the saved layout file.
- Keep incomplete-history warnings for item and enemy totals when a correctly framed typed event cannot be decoded. Earlier facts already knew counts were incomplete; derived tables failed to carry that provenance. Rebuild disposable row caches without changing raw history or inventing missing counts.
- Keep readable public attacks, damage, kills and heals when optional hidden detail payloads are malformed. Hidden source/identity remains unknown instead of dropping the public event. No additional hidden information is exposed.

These fixes were introduced in separate compiled builds and retain their individual technical build histories. TEST230 was cancelled and is not a successful verification result. Do not assign all changes to TEST231 simply because it is the delivery candidate.

## Regression evidence and limits
The typed-payload correction independently reproduced two failures and one clean control on unchanged TEST226, then passed the complete 1,691-method combined core gate 38835333283. Four new retained tests cover clean counts, malformed attack/equipment payloads, stale row-cache replacement and unchanged raw bytes. Subsequent malformed-detail source has two further retained methods and the complete 1,693-method source gate above. Collection-ledger and saved-column fixes retain their own before/after tests documented in the repository. Targeted and earlier-suite counts overlap this complete suite; do not add them together.

Possible regressions concern derived-table completeness labels, disposable cache rebuilding, saved column ordering and recovery behavior after damaged collection ledgers. Raw player history is never deleted to fix derived results. Existing cache-version tests remain enforced.

## Unfinished work
The full UIMSD audit is incomplete. Owner-device install-over and actual-save acceptance remain unverified. The reported audio delay, sentry behavior and owner-run armor availability are not declared fixed by this candidate. Broader custom-rule gameplay combinations and remaining approved acceptance require their own evidence. Original scoring remains unchanged pending the owner's formula. No production storage-format migration or compression is introduced.

All 18 existing custom controls, provisional limits, gameplay, combat timing, RNG, save formats, permanent records, Cleric content and opt-out remain unchanged by this delivery package. No public release, paid service, unfinished online activation or automatic tester distribution is authorized.

## Delivery and timing
Private staging is not delivery. Signing and actual APK checksum are pending until the attached validation succeeds. Preserve published TEST226 as fallback if it fails. Install over the existing APD application; never uninstall or clear player data. Do not claim uninterrupted overnight work or measure active work from CI wall time.

The daily scheduled-delivery incident remains unresolved at the per-invocation level: scheduler metadata and GitHub publication timestamps do not prove that a particular manual or scheduled chat caused an upload or sent a link. No historical approval-block cause is asserted. This report does not claim the scheduler has been repaired.
