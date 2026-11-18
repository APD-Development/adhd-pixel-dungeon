# Audio investigation — November 8, 2026

**Finding: the October 13 delayed/bunched combat-sound report is OPEN. No verified Prison/Tengu fix was found.** The symptom was recorded and a queue mechanism was investigated; spontaneous occurrence in APD and a physical-phone resolution have not been established. No audio code, dependency, setting or playback behavior was changed for this investigation.

## What was actually investigated or fixed

| Status | Evidence | What it establishes |
|---|---|---|
| Report recorded | `d017bd3009b40bd015779e68f43d19f898bc412b`, October 13; `docs/APD-AUDIO-INVESTIGATION-2026-10-13.md` | The repository records the owner's vanilla SPD speaker symptom, notably Prison/Tengu. The owner now reports no Sewers occurrence; that is an observation, not proof of region exclusivity. |
| Investigation implemented, not a fix | `75db5ea6913f55dfa537168182dbbe9b8656fe7a`, October 16; `android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/AudioSchedulingDiagnosticDeviceTest.java` | A deliberately blocked fake Sound endpoint, wrapped by real Android HandlerThread/libGDX AsynchronousSound, made later requests queue and dispatch after release. No audible output, spontaneous backend blockage, speaker timing or owner-encounter reproduction. No production audio correction. |
| Separate production fix implemented | `aa01b0be9eeff7455028a5b9bd3af98b36f41538`, October 18; `BlacksmithSprite.java`, `RealTimeSoundCadence.java`, `PlaybackGroup.java` | Blacksmith ambient hammer audio no longer speeds up with accelerated visual playback: a roughly 0.8-second real-time gate limits repeat strikes. This does not change Prison combat, Tengu SFX, the shared worker or general Sample scheduling. |
| Rounding follow-up implemented | `7638f7f7e0046f2f62d325255cdb290d2cf53ca7`, October 18 | Allows sub-millisecond floating-point drift in that hammer gate; not a general delayed-audio fix. |
| Reported issue fixed | **NOT VERIFIED** | No measured reproduction/resolution or corresponding production fix was found for the owner's delayed combat sounds. Do not relabel the Blacksmith fix or queue diagnostic as one. |

Commit author/account names are not treated as proof of owner approval. Dates above are repository commit dates, not reconstructed chat timestamps.

## Current implementation and why sounds can queue or overlap

Inspected APD378 producing source `5c225bdf7c6a181f7a4d0ee16b1edbdbae2a3e4d` and current checked-out source; relevant audio behavior is unchanged. Public upstream master resolved to `57a4e06a4caf162446d1c28caa7983f0493fecf0`; its pinned Sample.java is byte-for-byte identical to APD's, Git blob `d83ff2e37c28cc20f6b8b97f54a56112ea97bafd`. Both launchers select AsynchronousAndroidAudio and declare libGDX 1.14.0. Fetched and read the actual 1.14.0 AsynchronousAndroidAudio, AsynchronousSound, AndroidSound, DefaultAndroidAudio and configuration sources. This comparison does not prove that the reported symptom cannot occur.

* `SPD-classes/.../audio/Sample.java` forwards a loaded sound immediately to its wrapper. A missing sound returns -1; Sample does not save that failed ordinary request for later replay. `ShatteredPixelDungeon.java` queues all sound assets at startup; Sample loads at most one queued asset per update. No Prison-only asset-loading queue was found in those inspected paths.
* `AndroidLauncher.createAudio` selects libGDX's asynchronous implementation. It creates **one shared HandlerThread** named libGDX Sound Management. Every AsynchronousSound.play posts work to this Handler and immediately returns 0. The worker later calls AndroidSound.play and Android SoundPool.play. A slow/blocked earlier worker operation can delay subsequent requests; there is no per-request deadline or stale-event cutoff in this wrapper. Dispatching the backlog close together is a supported possibility, not an observed root cause on the owner's phone.
* SoundPool permits concurrent voices: the unoverridden libGDX configuration is 16 simultaneous streams. Normal combat can legitimately overlap effects. AndroidSound maps a failed SoundPool start to -1, but AsynchronousSound does not return that later result to the original caller. A quick return from Sample.play is therefore not proof of audible onset or successful playback.
* Sample also has an **intentional** delayed-effect collection, updated using Game.elapsed. For example, `Mob.defenseProc` schedules the strong-hit cue 0.125 seconds later for bow/dart surprise hits. `Game.update` caps a frame's graphics delta at 0.2 seconds; several due cues can be dispatched in one update. Such intended timing and simultaneous effects must not be removed to hide an unmeasured fault.
* Inspected Tengu call sites request PUFF for movement, BLAST for bombs, BURNING for fire and LIGHTNING for shockers/electricity. No Tengu-specific accumulated-SFX queue was found in these call sites. Higher event density, late game/render triggers, worker backlog, loading/lock contention or output latency remain hypotheses; none is measured as the cause. No device blame or region-exclusive conclusion is justified.

## Evidence limits

The October 16 controlled diagnostic workflow [38973571857](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38973571857) currently reports success at test source75db5ea69. The historical document records one raw successful method on unchanged TEST259, artifact12078745290 and its checksum; it explicitly says no acoustic reproduction and no JUnit XML. Today's attempt to fetch that historical artifact's metadata returned **HTTP404**. Its raw archive was not newly downloaded or rerun; disappearance/expiration reason is not established. The retained report, test source and successful workflow metadata support only the stated controlled scheduling result.

Current producing CI raw XML was independently downloaded: all four `RealTimeSoundCadenceTest` methods passed within APD378's 2,986-test suite, with zero failures/errors/skips. They test arithmetic cadence at 1x–32x, duplicate acceptance and invalid elapsed values. They do not play or measure sound. APD378's 12 targeted native checks do not select AudioSchedulingDiagnosticDeviceTest, and the emulator is launched with **-noaudio** (`.github/workflows/apd-prepare378.yml`). Those successful checks cannot verify speaker timing. No current physical-phone, audible Prison/Tengu test, onset-latency capture, before/after comparison or spontaneous audio-queue stall was performed.

## Recommended Android reproduction and acceptance plan — not performed

1. Use the same RedMagic/Android version and built-in speakers, fixed volume and a noted music setting. Compare the affected vanilla SPD build, if available, with APD378. Preserve live saves; use disposable runs or backed-up test copies. Record versions and settings. Start at 1x playback, then repeat at the owner's usual playback speed; do not assume speed caused the original symptom.
2. Establish a Sewers control, then play Prison floors6–9 with single and multi-enemy encounters, rapid attacks, ranged/wand effects, traps and doors. Test first entry and repeat/warmed encounters. On floor10, cover both Tengu phases, attacks, jumps, bombs, fire and shockers, including dense bursts. Repeat runs rather than treating one quiet encounter as proof.
3. Capture synchronized action/video and **actual speaker output** with an external microphone/camera where possible. Internal screen-recording audio alone does not measure speaker onset and can affect load; compare recorder-on/off trials. An audio-enabled API36 emulator can support controlled Android checks, but does not substitute for the actual Android16 phone and speaker path.
4. If further diagnostics are approved, timestamp the game request, Handler enqueue, worker entry, SoundPool call/result and acoustic onset using a shared monotonic clock. Correlate frame stalls, GC and queue age. This separates late game triggers from backend queue delay and output delay. Test-only endpoint timestamps have now been added as described below; acoustic onset and physical-phone encounter measurements remain unperformed.
5. Log request-to-dispatch and action-to-audible delays, worst cases and clustered late events during repeated Prison/Tengu trials. Preserve intentional overlap. Only after a spontaneous failure is reproduced and a bounded correction passes the same scenarios plus Sewers/normal simultaneous effects should this specific issue be marked fixed. A passing fake-endpoint or cadence test is insufficient.

Read-only evidence sources: [libGDX1.14.0 Android backend](https://github.com/libgdx/libgdx/tree/1.14.0/backends/gdx-backend-android/src/com/badlogic/gdx/backends/android); [pinned upstream Sample](https://github.com/00-Evan/shattered-pixel-dungeon/blob/57a4e06a4caf162446d1c28caa7983f0493fecf0/SPD-classes/src/main/java/com/watabou/noosa/audio/Sample.java). Retrieved-source hashes are retained locally in `/workspace/apd-audio-investigation-2026-11-08/fetched-sources.json`; no third-party source was added to production.


## November 8 clarification and real Android endpoint diagnostic

The owner clarified that, to their recollection, the delayed/bunched sounds happened **only in vanilla SPD**. APD occurrence remains unconfirmed. No production audio correction is justified by that recollection or the diagnostics below.

Audio-enabled API36 emulator run [42529880268](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/42529880268) reused the original APD379 Developer APK. Test-only instrumentation timed requests through the real shared AsynchronousSound worker and original AndroidSound/SoundPool endpoint. Three asset mixes (Sewers, Prison, Tengu), three burst intervals (500,125,16ms), and16 bursts per case produced480 requests. No worker blockage was deliberately injected. This exercises asset mixes, **not actual encounters or acoustic speaker output**.

| Asset mix | Burst interval (ms) | Requests | Failed starts (-1) | Maximum worker queue delay (ms) | Maximum endpoint return time (ms) |
|---|---:|---:|---:|---:|---:|
| Sewers |500|48|26|216.194|2.946|
| Sewers |125|48|16|10.253|5.460|
| Sewers |16|48|16|34.223|57.854|
| Prison |500|48|17|216.425|216.036|
| Prison |125|48|16|39.847|0.987|
| Prison |16|48|16|144.975|8.312|
| Tengu |500|64|13|22.067|4.621|
| Tengu |125|64|0|14.112|10.369|
| Tengu |16|64|0|5.429|0.175|

Raw JSON: artifact13636996609, `apd379-audio-endpoint-evidence/audio-real-endpoint.json`; summary reader [42530934839](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/42530934839). The120 failed starts and timing outliers are observations, not an established cause of the owner's SPD report. Loading, stream capacity, emulator scheduling, GC and codec behavior were not separated. Return time does not measure audible onset. Neither a specific fix nor physical-phone resolution has been demonstrated. Production playback, effects and intentional overlaps remain unchanged.
