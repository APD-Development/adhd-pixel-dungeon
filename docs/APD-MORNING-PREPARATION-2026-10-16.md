# APD morning preparation — October 16, 2026

## Candidate, fallback and delivery state
Candidate TEST241 uses exact application source b5d562ca37fff9ac628af3f4ade731e9fa7d83f1 and full verification run38894980574. Full-source core/debug/history/API34/visual evidence is pending at this preparation cutline. The stage workflow must wait for successful exact-source checks, then independently verify actual tests, APK checksum, signing, package and install-over version against the existing privately published TEST231. Its verification appendix records the actual completed result. Failed preparation retains the fallback; it does not make a failed candidate verified.

Privately published fallback TEST231: source5134ef6438f9562a37b3f8db727d5d004fa93527, release apd-alpha-preview-231, APK SHA25620f1d87ab58afd21cd37ae4027e6ebb79c01fcf802d0869801e6ffa03943bff9. Install over the existing application; never uninstall or clear data. No physical owner-save acceptance is established.

## Actual changes since the published fallback
The repository records separate before/after regressions and complete application gates for each of these CI candidates. They were not individually published, and are not retrospectively labelled as new work in TEST241.
- TEST232: unreadable lifecycle payloads no longer stop recovery of later readable ruleset state.
- TEST233: app-background or other idle gaps exceeding30seconds no longer add30seconds of false active-play time.
- TEST234: an unreadable typed run-end event no longer marks a live run finished or enables unredacted live-run exports.
- TEST235: a malformed first run-start event no longer hides a later readable recorded run ID, class, seed, difficulty or exact custom settings.
- TEST236: export/import finds the first readable logical run ID after a damaged start; a conflicting archive label is rejected.
- TEST237: archive folder names and declared entry kinds must agree, preventing run-identity validation bypass.
- TEST238: an empty decoded run ID no longer hides a later valid identity.
- TEST239: turning hidden information off removes aggregates that depend on hidden denominator, condition or distinct-by columns.
- TEST240: per-distinct aggregates disclose uncertain identities while preserving their rows and existing numeric results.
- TEST241 candidate: the circular Imp-vault sentry uses upstream SPD's two-turn scan interval. Existing affected floors receive a narrow interval-only repair during normal loading, preserving the saved phase, next countdown and scan pattern.

Original history bytes are preserved by the statistics/archive corrections. The sentry correction changes future attack pacing and the identified legacy room interval; it does not rewrite or delete run history, inventory or maps. Original scores and the18custom controls remain unchanged.

## Current investigation and exact regression evidence
The DM300 pylon was investigated separately. Its actor and sprite match upstream; the real scheduler produced16acts for16normal-turn actions at all three visual speeds. Actions costing two turns correctly allow32pylon acts. Its permanent four-method fixture passed alongside8existing playback tests; exact-source CI38893931161 independently passed1711unique core methods. No pylon timing modification was made.

Widening the investigation to the rotating Imp-vault room found the actual gameplay timing difference: APD had not adopted the explicit two-turn room interval from upstream commit40f1fe754f9f31e39eacdd27a946fc3711e2e834. The room configuration, not doubled scheduler updates or animation speed, caused twice as many scan advances as current SPD.

Five retained circular-room tests produced four expected old timing/repair failures and one unrelated-sentry control pass. Corrected local code passed all5; combined with pylon and playback checks,17unique methods passed. Full current acceptance must independently reproduce those old failures and pass1716unique core methods, debug assembly, exact preceding TEST240 compiled-history checks,33API34methods and42valid screenshots. Counts overlap; do not add local/targeted totals to the complete suite.

## Failures, possible regressions and remaining work
TEST235's first Android attempt passed its33methods but produced an unreadable text-entry screenshot; that capture was rejected and the Android-only rerun passed with42reviewed images. The original evidence remains documented. The new sentry fixtures initially lacked normal Level collection initialization; the corrected fixture's four assertion failures, not those setup exceptions, establish reproduction. Its RNG probe also required room construction outside the measured callback.

Potential regressions to check on a physical phone: existing circular-room phase/countdown preservation, changed attack pacing and normal movement avoidance; malformed-history warnings; archive import rejection and run identity; saved query cleanup and distinct-key disclosure. Emulator screenshots do not establish the full avoidance strategy or a human-played boss fight. The original reported actor/run is not identified with certainty.

The complete UIMSD audit, physical RedMagic/Pixel install-over/save acceptance, broad human-played slider/class/boss combinations, audio delay and original-run armor availability remain unfinished. Custom scoring remains deferred pending the owner's formula. No production compression-format migration, unrelated balance changes, hidden lore disclosure, paid service, unfinished online activation or automatic tester distribution is included.

## Timing and scheduled delivery
The handoff contains bounded overnight implementation and CI records through TEST240. That contradicts a blanket claim of no overnight work, but does not prove uninterrupted activity or exact idle hours. CI elapsed time is not developer-active time.

This preparation stages a verified candidate and PDF only; it does not publish before the separate lunch delivery tasks. Preparation is not delivery. Daily scheduled publication/link delivery has not been demonstrated reliable; task metadata and publication timestamps alone do not identify which chat invocation caused a release. No unproven permission-block explanation or repaired-scheduler claim is made.
