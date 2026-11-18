# APD delivery and online roadmap — owner update, October 9, 2026

## Release targets

- **November 1, 2026: mandatory playable alpha test APK.** Critical stability/data-integrity bugs must be fixed, regressions investigated, and actual phone results distinguished from automated tests. This is not feature-complete public launch.
- **January–February 2027: target Android and Steam public release**, conditional on stability and sufficient testing. The alpha deadline remains the first priority.
- Surpassing Shattered Pixel Dungeon within roughly a year of release is the owner's ambition, not a promised or measurable outcome of this sprint.

## Long-term identity and expansion

APD's defining philosophy: **Players should never run out of content they WANT to play.** The community proposes, votes on and selects additions through the overlapping monthly tournament cohorts. The owner's ambition is to surpass SPD in players, content variety, replayability, customization, statistics, community engagement and wanted features within roughly a year of public release. That is an ambition, not a guarantee or permission to displace the alpha work.

Design future extension points, content manifests, save migrations and community contracts for long-term modding and expansion across both Android and Steam. Use versioned compatibility and testable extension boundaries instead of arbitrary permanent content ceilings. Technical security/resource bounds must remain justified and tested; this direction does not justify removing validation or loading unreviewed executable content. No new mod loader, hard-limit removal or unrelated gameplay feature is implemented in this fix sprint.

## Online work is a near-term priority

Begin planning and implementing shared server infrastructure as early as practical while protecting offline gameplay, saves and the alpha deadline. Testing should begin shaping content; community work is not postponed until after release. Use one server identity, data contract and community across Steam and Android.

Existing code to reuse rather than duplicate: Accounts (opaque APD player IDs and provider credentials/account linking), LocalProfile/SessionStore, Gateway/HttpGateway, Outbox/SubmissionQueue, SyncReplica/SyncCollections, Voting/VoteMessages, CommunityMessages, approved content catalogues and trust/approval primitives. These are foundations/reference implementations, not a deployed server or verified provider sign-in. Existing Accounts credentials must come from independently verified provider authentication; a client-supplied provider/subject is not proof of identity.

First online vertical slice: server-validated authentication → one APD account → authenticated profile read → explicit account linking. Enforce uniqueness of linked provider identities, recent proof of both identities for linking, session expiry/revocation, safe recovery and no silent account merging. Steam and mobile must resolve to the same internal player ID. Never embed server signing secrets in either client.

Then build opt-in cloud saves and account-scoped shared statistics. Preserve local-first play through outages. Use versioned save snapshots, integrity checks, atomic restore, retained recovery copies and explicit conflict handling; never overwrite a diverged local save automatically. Preserve history/privacy rules: raw UIMSD logs remain local under current DataPolicy; specify the permitted synchronized summary/schema before uploading anything. UIMSD remains a major feature on both platforms.

Then add the in-game Community tab, profiles, submissions, moderation and authenticated voting against the same shared service. Community classes/content require cross-platform compatibility and existing review/signature/version protections before loading. A live unfinished service, fabricated sign-in or client-authoritative vote tally is not an acceptable shortcut.

## Continuous three-month voting pipeline

The latest instruction supersedes earlier shorthand about one monthly three-stage vote. Every submission cohort spans three consecutive months:

| Cohort | January | February | March | April | May |
|---|---|---|---|---|---|
| January ideas | Submit ideas | Qualification votes | Weekly head-to-head eliminations | Completed | Completed |
| February ideas | Not started | Submit ideas | Qualification votes | Weekly head-to-head eliminations | Completed |
| March ideas | Not started | Not started | Submit ideas | Qualification votes | Weekly head-to-head eliminations |

Once established, each calendar month hosts submissions for the new cohort, qualification for the preceding cohort, and eliminations for the cohort before that. Each cohort, submission and vote round needs a stable server ID. The server owns phase boundaries, accepted ballots, qualification and winners. Android and Steam display the same schedule and results.

Tournament size, qualification thresholds, ties, late moderation/disqualifications, weekly boundaries and winner-to-development commitments still need explicit rules before that behavior is implemented. The roadmap is approved; those unprovided numbers/rules must not be silently invented. Server clock/calendar decisions belong in one shared contract rather than separate platform implementations.

## Next implementation sequence

1. Complete the current alpha fixes and record phone acceptance against the verified APK.
2. Review existing reference-server tests/contracts, select deployable hosting/storage/auth adapters, and implement a secure authenticated profile/linking test environment.
3. Add save synchronization integrity/conflict tests before a client restore path; define the statistics summary sync contract.
4. Build community submission/moderation/profile flows and cohort scheduling/qualification tests.
5. Add weekly bracket/tally rules and cross-platform content compatibility tests once the detailed rules are specified.

No major unrelated feature is part of this roadmap. No real server, provider credentials, Steam partner configuration or online production deployment was supplied or activated in the current fix build. These implementation dependencies stay recorded without blocking independent alpha fixes.
