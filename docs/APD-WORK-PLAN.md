# APD work plan: read this first every run (Chris, Nov 18 2026)

Every scheduled run starts fresh. This file is the memory: read it, work, then add a dated line to "Run log" below.

## Standing rules
- Repo: APD-Development/ShieldingShouldHaveWorked. Work branch: `release/play-closed-test`. Never push to `main`.
  Never force-push. Never delete branches. Daily alpha APKs come from `fix/apd-trap-room-search-20261116`.
- Never commit keystores, passwords, API keys or tokens. GitHub Actions secrets can't be written from the cloud
  sandbox; Chris adds them himself (see docs/PLAY-RELEASE.md).
- Ask Chris before changing any game mechanic, balance or scope. Fix real bugs right away and list them.
  Small decisions: make the call, list it in the summary, revert if Chris says no.
- No local Gradle in the sandbox. Test via CI: `gh workflow run apd-extended-bug-report-ci.yml --ref <branch>`
  (full :core:test). Release build + emulator launch check: `.github/workflows/apd-play-release.yml` (runs on push).
- Chris is the designer: never invent mechanics or features. Brainstorms are not approvals.
- Say exactly what was verified (which CI run passed) vs assumed.

## Release: public on Google Play Jan 1, 2027
Package `com.charltonchris29.adhdpixeldungeon`, personal account: 12-tester closed test for 14 straight days
before production, so the closed test must start by Nov 23.

### Priority order (Chris, Nov 18 09:49; 09:55: "Only community is important above all else")
Chris 09:57: don't drop Phase 1: closed test as close to Nov 23 as possible. Phase 1 code is done; the moment the
APD_UPLOAD_* secrets exist, push to release/play-closed-test (touch the workflow) to rebuild, confirm the .aab is signed
(`jarsigner -verify` in CI), then give Chris the Play Console upload steps one at a time. Otherwise all time goes to Community.

1. **Phase 1 (target Nov 21-22):** signed release .aab. Done except signing: blocked on Chris adding the
   APD_UPLOAD_* secrets. Then: Play Console upload steps for Chris, one at a time.
2. **Community (most important for Jan 1), starts right after Phase 1.** Goal for Dec 22: full Community loop:
   submissions with threaded comments, voting down to a top 16, the weekly elimination tournament, moderation
   (report, block, profanity/slur filter, no private messages), admin tools for Chris (hide/remove posts, ban
   accounts), account deletion, accounts (Google sign-in OR email/password with emailed verification code), and the
   16+ age gate for Community only (Play Age Signals API first; else birth month+year, store only pass/fail, no
   retry with a different year on that device). Backend: Cloudflare Workers + D1 + Turnstile (recommended to Chris
   Nov 18; he answered yes). Use the existing provider-neutral foundations (docs/APD-BACKEND.md, apd/backend,
   apd/backendbridge; the HttpGateway contract `/v1/envelope`, `/v1/requests` and EndToEndSubmissionTest's
   reference server). Real tests for the server and the age gate. Design: docs/APD-COMMUNITY-ALPHA-DESIGN.md
   (Month 1 submissions, Month 2 voting to top 16, Month 3 weekly elimination; comments never count as votes).
   DECIDED Nov 18 (Chris): Month 2 upvote-only, one per submission, never your own; top 16 plus everything tied with
   16th. Tournament week 1: everyone picks up to 8 of the qualifiers, top 8 advance, cut ties to higher Month 2 score.
   Weeks 2-4: seeded 1v1 bracket (1v8, 4v5, 2v7, 3v6), one vote per account per matchup, changeable until the week
   ends, tie to the higher seed. Claude's calls (revertable): full ties go to the earlier submission; no voting for
   your own entry in the tournament either; banned voters' and removed posts' votes don't count; byes with <8. Test the full cycle with a
   compressed clock. Needs from Chris: Cloudflare account + API token secret, Google OAuth client, email sender.
   If done early: run reviews, then events.
3. **Phase 2 cleanup, alongside Community (due Dec 10):** GPL source public (new public repo, clean history;
   `GplLicense.SOURCE_URL` must point to it; repo name from Chris), keep LICENSE and copyright headers, About
   credits Shattered Pixel Dungeon; remove Shattered/Evan branding, support and Patreon text and anything implying
   endorsement; nothing contacts Shattered servers (done Nov 18); ChatGPT auto-dev workflow disabled (done);
   adult humor OFF by default (done). `tools/export-public-source.sh` makes the GPL source zip (no history, no
   keystore) and REFUSES while android/build.gradle still has the old alpha key password fallback: remove that
   fallback (and keystore/) once Chris adds the APD_DEBUG_* secrets.
4. **Phase 4, Dec 23-31:** bug fixing only, final build; Chris submits for production.

### Play Console paperwork Chris must do (remind him)
Add the 4 upload-key secrets; Data safety form (accounts, user content); content rating questionnaire again (users
interact); privacy policy update; account deletion web link.

## Daily alpha APK: PAUSED (Chris, Nov 18 11:30)
No more alpha APKs after C489S/P489S until the Community server is up and running. Do not run the steps below
until Chris says so.

## Daily alpha APK steps (3 PM ET run only, when un-paused)
Merge `release/play-closed-test` into `fix/apd-trap-room-search-20261116`, add the Change History entry
(core/src/main/assets/apd/changes.tsv, player-noticeable changes only), bump the build label in
`.github/workflows/apd488t-trap-room-search-apk.yml` (C489O -> C489P -> ...; that push triggers the build), fix any
failure, and give Chris both artifact links (Developer = C, Alpha Testers = P) by 4 PM ET.

## Community server
`server/community/` (Worker + D1). Tests: `npm test` (node 22 `--experimental-sqlite`), CI workflow
`apd-community-server.yml`. Done Nov 18: accounts (email+code, Google), age pass flag, submissions, threaded comments,
filter, reports, blocks, admin, account deletion. Seasons + voting + tournament done. Nov 18 PM: login/sign-up rate limits, deletion keeps others' replies,
real Google token check, Cloudflare Email Service mailer (needs Workers Paid + domain onboarding: ASK CHRIS, costs
money), deploy workflow (needs CLOUDFLARE_API_TOKEN/ACCOUNT_ID secrets; D1 found/created by name; deploy = push the
release head to branch deploy/community-server), Java client + all Community screens
wired (CommunityController/CommunityScene; live when SPDSettings.communityServer is set). Existing players keep old
default-on settings (SPDSettings.keepOldDefaultsForExistingPlayers). Next: Google sign-in button on Android
(Credential Manager), server URL default once deployed, run reviews. HttpGateway contract routes, deployment workflow, Android Community screens wired to it, Play Age Signals.

## Run log
- 2026-11-18 5 PM (scheduled): no code; a live session was pushing the Cloudflare Workers deploy to this branch
  (16:34-16:52), so this run stood down to avoid clashing. Next: Android Google sign-in (needs OAuth client id from Chris).
- 2026-11-18 2 PM: upvote button shows state + count, week-1 picks toggle locally, offline no longer signs out
  (Try again), plain words for every server error (+ test), hub links to planned pages, preview text matches decided
  rules, deploy finds/creates D1 and runs on push to deploy/community-server (dry run verified: tests pass, no token).
- 2026-11-18 1 PM: blocked-players list, in-game Start a season, Find a player (ban/unban), web /delete page (Play),
  admin promoted at sign-in, client/server field contract test, GPL export script, last Shattered text in all languages
  and crash messages. Play paperwork draft: docs/PLAY-COMMUNITY-PAPERWORK.md. Run reviews need Chris's design answers.
- 2026-11-18 12 PM: Community server 24 tests, client screens + tests, settings migration; APKs paused (Chris).
- 2026-11-18: Phase 1 build/emulator check done, unsigned (secrets pending). noNews/noUpdates, auto-dev disabled,
  Clean humor default, player-facing Shattered text replaced. Alpha C489O/P489O delivered.
