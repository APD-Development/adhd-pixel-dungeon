# UIMSD follow-up, separate from TEST79

Confirmed DerivedStore malformed UTF-8 cache is trusted as replacement text. New two-case reproduction: baseline one failure, corrected both pass. Strict cache-local decoder rebuilds using unchanged raw logs, retaining valid international text and existing cache format. Combined derived-cache, store, damaged-log recovery and index validation:39 passed0.864s.

An initial combined local run loaded an obsolete RunIndex class; compiling actual TEST79 source resolved the existing count-validation test. This was a local harness mismatch, not a new TEST79 failure.

Reviewed complete DerivedStore, FactTable, Fact, RunFacts, SavedViews and UimsdModel. Exact blob proof for five files saved separately. Potential overlapping UimsdModel loads remain a review concern only, with no reproducible user-path failure established; no speculative change made. Existing SavedViews incomplete-read protections and atomic replacement already cover valid-entry recovery, future query preservation, refusal to delete destination directories and failed temporary writes. No redundant reimplementation added.

Committed as TEST80 candidate; not included in TEST79 APK. Full UIMSD audit remains unfinished. Original TEST50 save unavailable. No physical-phone test performed. Unattended Codex coding remains unproven; the verified-artifact delivery workflow is actual GitHub Actions automation, not a persistent developer.


TEST79 delivery sourcea611c841e64a8bca592831d3200a5a65e1735b61/run38299325341:1200core+21API34green; delivery38299495979, published3:53:48EDT. CertmatchesTEST67/appIDsame/version920. SHA256bbab8a1ebb0ac206260fb9a6fbed4f377c87f8a4eb601eaba32f817205f5e80d. Source/application tree exactly validated against publicationdd99e959b3fec1c7313c91718fbb089ef9611d26. TEST78backup38300641403failedapp-inputcomparison, notpublished, notneededandretriesavoided. TEST80fullcore/debug/API34requested; currentchangeisnotin79. Potentialregressioncachefallback/Unicode coveredlocally;physicalphonepending.


Full verification sourceee750f30687eccb89096ed858a12cc54cec3e299/run38301092448 succeeded. Downloaded actual coreXML1202 cases,zero failures/errors/skips; API34jobpassed. No signer or separate delivery claim. TEST79 remains signed delivered artifact.
