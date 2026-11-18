# Read-only SPD save-utility findings — 2026-10-11
Reviewed upstream00-Evan/shattered-pixel-dungeon at57a4e06a4caf162446d1c28caa7983f0493fecf0 (v4.0.2). FileUtils.java andBundle.java were fetched from that exact commit, compiled unchanged, and are byte-for-byte identical to the approved APD branch atb4874dbd920d065813b981c2e8913c6462f06bd0. No inherited SPD production source was changed.

## Confirmed controlled save-loss reproduction — high severity
File: SPD-classes/src/main/java/com/watabou/utils/FileUtils.java, line222 ignoresBundle.write's boolean result. Lines208–210 then delete the existing file and move the temporary file into its place.
Code:
```java
Bundle.write(bundle, output);
output.close();
...
bundleToStream(temp.write(false), bundle);
file.delete();
temp.moveTo(file);
```
Bundle.java lines550–552 catchIOException and returnfalse. A temporary stream that throwsIOException on writing but closes successfully therefore letsbundleToFile returnnormally and replace a valid save with the failed, empty temp file.
Evidence: failedTempWriteCanReplaceAnExistingValidSaveWithEmptyBytes first writes a real valid bundle into a disposable directory, then injects failure only for.spdtmp writes; bundleToFile throws noIOException, and the existing game.dat becomes0bytes. This is a fault-injection reproduction, not an observed phone save loss or a claim that every IO failure behaves this way.
Proposed repair, NOT implemented: propagate false serialization asIOException before touching the existing file; retain the old save on failure; verify close and replacement behavior.

## Confirmed nested cleanup path bug — medium, conditional
FileUtils.java line84:
```java
cleanTempFiles(dirName + file.name())
```
At the second directory level, slot+nested becomes slotnested instead ofslot/nested. A validslot/nested/game.dat.spdtmp is not discovered/recovered.
Evidence: nestedTemporarySaveIsSkippedByRecursiveCleanup constructs the valid temp bundle, invokescleanTempFiles(), observesfalse, temp remains, original game.dat remainsmissing.
Limit: normal one-level save slots are not demonstrated affected; this applies to deeper directory layouts. WelcomeScene calls this cleanup, but no current ordinary two-level game save layout was established.
Proposed repair, NOT implemented: join directory components with a separator or use child paths consistently.

## Confirmed exceptional-path stream leak — low/conditional
FileUtils.java lines191–194:
```java
Bundle bundle = Bundle.read(input);
input.close();
return bundle;
```
IfBundle.read throws, the owned input stream is not closed byFileUtils.
Evidence: failedBundleReadLeavesItsOwnedInputStreamOpen supplies a tracked stream throwingIOException duringread and observesclosed=false after the expected failure.
Limit: no measured phone descriptor exhaustion or permanent leak accumulation demonstrated; garbage collection may eventually reclaim resources.
Proposed repair, NOT implemented: close viafinally/try-with-resources while preserving exception propagation.

## Reproduction evidence and scope
Three JUnit reproduction tests passed on unchanged upstream source, meaning all three documented BAD behaviors were reproduced. These are evidence tests, not a claim that the utilities work correctly.
Initial harness compilation incorrectly attempted to subclass finalHeadlessFiles; replaced with a Files interface proxy. Successful rerun compiled exactFileUtils/Bundle and passed3tests. Expected injected IOExceptions appeared in logs. Temporary files only; no player data accessed or modified.
UpstreamFileUtils SHA256: de977cbbf505236cef7888a79f1315ab841da02c0317d7fab6934b9a05cb780b
UpstreamBundle SHA256:4638b84de7e08746a53d7eb48e14bc5905ae6bb1a8ba75d6fd03ad8e605bf3ca
Reproducer preserved as tools/spd-audit/UpstreamFileUtilsReproductionTest.java. Local commands usedjava com.sun.tools.javac.Main with existing project/dependency/JUnit classes, thenJUnitCore; unchanged upstream copies took precedence on the classpath. This was host-JVM testing, not Android device testing.
Direct source:
https://github.com/00-Evan/shattered-pixel-dungeon/blob/57a4e06a4caf162446d1c28caa7983f0493fecf0/SPD-classes/src/main/java/com/watabou/utils/FileUtils.java#L222
https://github.com/00-Evan/shattered-pixel-dungeon/blob/57a4e06a4caf162446d1c28caa7983f0493fecf0/SPD-classes/src/main/java/com/watabou/utils/Bundle.java#L550
Owner requested read-only investigation of inherited SPD; findings deliberately remain unpatched pending that design/instruction change. APD UIMSD fixes and APK verification proceed independently.
This is a targeted utility review, not a complete SPD audit. Recent upstream history also contains fixes toCrystal durability caps, Kunai/knifeProjectileMomentum andRatSkull/statue generation RNG; historical fixed issues must not be presented as new current defects.
