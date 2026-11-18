# TEST323 private staging continuation — October 28, 2026
## Current result
Continuation resumed18:16:59UTC. Accepted main-emulator candidate TEST323 source51fe6b502bee67893b9773eacddeb01eac778547, producer40737013809, matched player40737013887, display40737013998. Current documentation head07095aa0cc7d9a7b8d8f54de667804a270b79f80 changes only four docs relative to that source. No APK rebuild is needed.
Both core variants2121methods/410suites,zero failures/errors/skips; native62unique methods/zero failures/errors/skips;82reviewedPNG. All five exact-source display profiles passed;20intended states reviewed,5extra post-close launcher captures are not app evidence.
Developer SHA256f599e05202277cc2056255e1e6c8ca915eca68a8849af4959e633b678e515467,38,786,669bytes. Playerad5a2febbccc6460c5644fdd7fbc8c7f35d4cc26ed917c0730b09ed815b14d24,38,786,500bytes. Both preserve349preceding records,350indexed/351TSVassets. Player privateflags absent.
Signer067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e; applicationcom.charltonchris29.adhdpixeldungeon.indev. Install over existing matching app; never uninstall/clear.

## Changes and causes
Returning menus recreated title/cameras and repeated ready-screen work. Android now retains one inactive title with cameras/pointers detached and held inputs canceled, restored by normal Back; reset/resume/context/display/callback/gameplay changes invalidate it. Dormant key controls cannot dispatch. Nested menu parents keep normal lifecycle.
ReadyUIMSD content and pending scene requests draw earlier using initialized camera projection without advancing simulation/RNG. Final width is set before Home/general text construction, preserving long-token wrapping and removing redundant first-draw work. APDSettings implementation and frozen title layout remain unchanged.
Measured API34 synthetic72runfixture: real tap to completeHome162ms under unchanged250ms budget. Phase durations35.966/34.892/76.289ms overlap; never sum them. Nine Back key/render round trips15–64ms require same title/camera identity. No physicalREDMAGIC or realplayer latency claim.
Intermediate321superseded after first-draw projection review;322failed2/61native (288msHome, Settingsidentity).323fixes actual text work; Settingsfixture observes stable completedframes and asserts samecapturedscene before/after. No budget/assertion weakened.

## Unresolved verification
Legacy death40737014047 and hotbar40737014217 fail beforetests: shallowcheckout misses pinnededa307d5; their code also still pins old314 and a narrow oldallowlist. Fetch-depth alone would not make them validate323. Historicalbaseline artifacts were previously expired. Do not call these passing or weaken expectedfailure/save assertions.
Originalstage40737013907 failed before producer artifact availability. Sharedprofile remained268. This change selects exact completed323/2121core/62native/82captures and currentreport, removes staleOctober16appendix, and leaves strict app-tree/test/signature guards intact. Actualstaging success needs real workflow receipt.
No independent coldrestart/deathorder rerun acceptance or physicalphone install-over is claimed. Main play/save/continue/end native coverage passed. Currentcandidate status is bounded to the statedgates, not everyworkflowgreen.

## Fallback and boundaries
Preserve existing releaseassets. Previously confirmed published262fallback:
https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-262/ADHD-Pixel-Dungeon-Alpha-Preview-262.apk
SHA2565ad5beb717673a51a10a646c28b887ed54a4f78f8d25ee351834a39a29ecf8d2.
No appsource, scores, saves/rawhistory,18rules, balance, Clericrestriction, onlineactivation, publicrelease or testerdistribution change by this stagingcorrection. No newAPK requested. GitHubActions staging is not publication/delivery. Preserve allfailedruns and physical-phone/backlog limitations.
Fullreceipt docs/APD-TEST323-VERIFICATION-2026-10-28.json; latesthandoff remains authority for otherwork. Lunch deadlines were already missed; this continuation must not be backdated.
