# Prepared explosion-source correction

Bomb fuses previously retained the Bomb but not its creator. The correction captures throw ownership for lit bombs, persists an optional immutable role alongside the existing fuse, preserves missing historical ownership as unknown, and clears ownership when defused or recovered. Cursed-wand explosions use their actual caster; blast stones use the throw captured by Item. Raw bomb damage is scaled once before existing apdDrRoll armor reduction; the original damage source, range, RNG, fuse delay, turn behavior and death logic are unchanged. Neutral and environmental explosions bypass scaling allocations.

The initial draft replacement targeted the older drRoll method spelling and missed the real branch's apdDrRoll; the boundary test caught this before any commit or APK. The correction explicitly preserves the newer defense hook. All preparation was based on actual GitHub source, with existing contributor changes retained.

Seven permanent targeted methods passed in0.243s, including all20 percentages for each of three source roles, neutral/unowned RNG identity, real blast-stone and cursed-wand explosions, malformed metadata preservation, exception cleanup, and an actual lit throw saved/restored before its fuse triggers. The two-turn fuse remains unchanged.633 broader recording/query/archive/rollback/inventory checks passed in23.407s. Full source and Android checks still required.

The score popup labels the unmodified value Original score; the existing Android test requires that label. No earned scoring formula or adjustment introduced.

Incomplete: chained bombs, remaining specialized bombs, untracked boss explosions, persistent mixed blobs and other outstanding offensive sources. This package does not complete the three damage controls; they remain disabled. No compression format, signing identity, application ID, main branch, player inventory, raw history, gameplay ranges or unrelated balance changes. No APK or physical-phone verification claimed.

## TEST138 actual evidence
Commit62b96391dabd8bc139642c1ade35439cdd49f874/run38493524276 passed the complete verify-and-build and API34 jobs. Downloaded reports:1472core tests in281XML files and26Android tests,0failures/errors/skips.33PNG validated; constrained score popup visually inspected. Artifact11863410855 is the built private debug APK;11863535510 core;11862537890 Android. No physical-phone evidence, owner release or new signer/checksum comparison claimed.

The before/after score capture used the same MediaStore filename twice, resulting in distinct gallery files while the collector selected the earlier capture. The next test uses separate filenames and requires a minimum meaningful scroll distance rather than merely a positive offset. Required34valid PNG.
