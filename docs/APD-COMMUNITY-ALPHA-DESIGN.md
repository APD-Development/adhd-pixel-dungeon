# Community hub: owner clarification and existing-code inspection

October 9, 2026. Gameplay-blocking fixes and playable APK delivery take priority. November 1 is a private alpha, not public release. This document records scope; it does not enable online services.

## Owner requirements

Community is a central hub for player interaction and community-driven development, not just voting. Proposals may cover characters/subclasses, equipment, enemies/bosses, challenges/mechanics, QoL, systems, events/clans, cosmetics, modding and APIs. Difficulty or implementation time alone must not disqualify an idea.

Month 1 submissions; Month 2 qualification voting; Month 3 weekly elimination tournament. A manageable bracket, potentially 16 finalists, is intended, but its size is undecided. Eventually different categories/cohorts run concurrently across one Android/Steam community. Winners enter development and remain winners if work exceeds the approximate two-month goal. Development-stage community testing is planned.

Run review is by players, not AI judgments. A reviewer calling a decision bad must attempt the same situation. Exact replay scope remains undecided. Downloadable mobile-compatible mods, monthly modded-character competitions and voting for integration, clans, events and clan competitions are planned.

Cleric lore/personal vendetta and the hidden Remember When You Needed Help? achievement remain exclusively creator-controlled. Do not implement the achievement or change Cleric restrictions.

Alpha scope: coherent readable preview of these systems, with functional local features clearly separated from planned/unavailable features. No fake votes, players, submissions, rankings, successful uploads or downloads. Do not implement the whole system now.

## Existing functionality inspected

- No dedicated Community hub or title-screen Community entry was found.
- WndUimsdOptions contains Community votes under Privacy. It displays locally cached server offers and can queue a checked ballot; this is not a working hosted voting service.
- Provider-neutral foundations include ballot/report messages, one-account ballot replacement, accounts/profile models, generic groups/permissions, signed content catalogues and cached approval/revocation handling. Preserve these foundations.
- Run submission preview/queue/withdraw and evidence-consent primitives exist. They do not implement player decision reviews or enforce same-situation replay.
- There is no deployed server or sign-in method. No community submission UI, monthly pipeline scheduler, elimination bracket UI, development-testing hub, downloadable-mod UI, clan/event experience or cross-platform live community was found.

Inspection used unchanged remote branches: Codex 04294dbc1fe31bc0a01e9a56f3ebb875812e361e, Claude 714badbd313c66c41ec4d9fb50429b353a815840, main 345aaaee74a51981cfdf13281d1cbd469c2924c1. Local C30 fixes remain separate and preserved.

## Decisions requested; do not silently finalize

1. Hub entry: main menu, UIMSD, or both; immediate availability was suggested rather than assuming a statistics unlock gates community access.
2. Required reviewer attempt: identical checkpoint/situation, full same-seed run, or intentionally unspecified for alpha. Seed alone does not establish identical inventory/state/RNG position.

Later decisions before operational implementation: bracket size and qualification/tie rules; category scheduling; review-attempt evidence and information disclosure; submission/mod moderation and safety; clan/event rules; authentication/provider and infrastructure. None needs to delay an informational alpha preview.

## Verification

Existing BackendCommunityTest, CommunityMessagesTest, VoteActionsTest and ContentCatalogueTest: 23 local JVM tests passed, 0 failures, 0.275s. An initial runner invocation named nonexistent VotingTest and failed initialization; corrected class selection passed. These test primitives, not live service operation or physical Android UI. No application code, network settings, player data or release changed during this inspection.

## Interface implementation checkpoint

Owner selected a separate main-menu Community button, accessible without UIMSD unlock. Implemented eight informational destinations using the existing wrapped scrolling list UI and title background. Each destination opens its own explanation and availability page; explicit Back and Android Back return to the hub/title. The existing APD Settings row is split between Settings and Community, preserving title vertical geometry and other buttons. Existing UIMSD votes/queue remain untouched. No backend or run reconstruction enabled. Tester voting timing (late December/early January) is internal planning only and must not appear in player text. Replay scope and bracket size remain undecided. Application classes compile locally; Android touch/layout and APK acceptance remain pending.

Targeted Community content/navigation and frozen title guard: 8 passed, 0.077s. Initial broad suite found one source-contract assertion assuming APD Settings directly precedes News in the fade list; updated it to include the authorized Community button while retaining Settings fade verification. An earlier new test used the wrong relative source path; corrected it to the existing apd.game.src property. Neither was a confirmed application runtime failure. Full Android/emulator layout and touch checks are not yet run.

Owner correction: clans are still under consideration and must not appear in the current hub. The Events destination describes community events only; no clan functionality or promise is added.

Final broad local non-native suite: 563 tests passed, 0 failures, 44.768s. Final targeted suite after removing clans and adding no-clan/no-internal-date assertions: 8 passed, 0.080s. No physical-device or APK verification claimed.

## Submission discussion clarification implemented

Month 1 submissions are readable and each will have dedicated threaded comments, replies, author participation, questions and improvements. Discussion continues during Month 2 voting. Comments never count as votes or increase the voting score. Month 2 selects the top 16; Month 3 begins with those final 16. This supersedes the undecided bracket-size notes above; tie rules and exact review replay remain open.

Submission preview now separates the submission description area, comments explanation, thread placeholder and availability. No invented submissions/comments, example pitches, posting controls that pretend to work, network service or scoring implementation. General proposal categories were replaced by an invitation for players' own ideas, following owner direction against spoon-feeding. Clans and internal tester dates remain absent.

Changed CommunityPreview, CommunityScene and CommunityPreviewTest. Compilation succeeded; 14 targeted content/navigation/title/settings tests passed (0.134s). Android rendering/touch verification and delivery remain pending; local implementation is not yet in a delivered APK. Community continues between higher-priority bug tasks, not only when no work remains.

## Real branch verification checkpoint — 2026-10-10 18:24:47 UTC

Community source and retained regression tests are being committed to the real approved Codex branch, preserving existing history. Fourteen targeted tests passed (0.087s); a native real-touch test traverses all eight pages, checks dialog/text containment, and returns to the menu. Native verification and a delivered APK containing Community remain pending. Previous humor Android verification failed due to cross-test remembered-scene initialization; setup corrected rather than changing game navigation behavior. No live services activated.
