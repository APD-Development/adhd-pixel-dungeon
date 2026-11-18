# TEST87: detect damaged derived fact caches
Preceding verified produced/delivered APK86 sourcebeefe620896ca1f29ecb67e039817f97ba4e1854/run38314568910. Real branch parent95e36e6d80581bb28e38bc219aa2849aadbe5036 preserves86delivery evidence/main/Claude.

## Reproduced cause
DerivedStore strictUTF8 catches malformedbytes but format1 hadno checksum. A validASCII seedbitflip, numeric100→101 change orappendedbytesafterEND wasaccepted asCACHE while sourcefilelengths remainedsame. The permanent5-case baselinehas4failures0.236s: threecorruptionfailures plusnewmigration-policy expectation; largeUnicodecontrolpassed. Original2-caseASCIIprobe1fail0.111s. No rawhistory/savecorruptionwasclaimed.

## Fix
Cache-only format2 adds15-byteCRC32footer. Writerchecksums streamedUTF8bytes without a whole-cachecopy. Reader validates the sameopenFileInputStream/filechannel before acceptingfacts, preservesdecoderread-ahead position, requiresfooterandEOFafterEND. A changedfooter/body ortrailingbytesrebuildsfromoriginalrawlogs. Olderformat1 orolderderivationrebuildonce; derivationversion4/formulas/log/saveformats unchanged. Olderappsrejectnewcacheheaderandrebuildtheirowncachefromsame rawlogs. This is accidental-corruption detection, not authentication.
One existing truncationfixture nowusescurrentformat2 so itstilltestsBAD_CACHEratherthanoldformatmigration. Allassertionsandraw-preservationchecks retained.

## Tests and actual timing
Five new permanent cases: ASCIIcorruption/rawbytepreservation/nextwarmreuse;numericcorruption;extra trailingbytes;oldformatmigration;200Unicode records spanningmorethan8192readerbytes. New+existingcache19casespass0.251s. Combined66cache/recovery/index/query/storage/performancechecks pass2.591s. Existing60-run/270120-event benchmark: rawrebuild317ms, valid81KiBcache10ms onthisworkspace. Not AndroidFPS/phone evidence andnot a statisticalspeedupclaim. CRC addsone bounded-buffer pass; oldcachefirstopenrequiresrebuild.
ProductionJava andexisting/newtestcompilationpassed. Full source verification run38316987461 completed successfully at source f2dc6a54836171acc3c2f9872fa5617da7bbf496. Delivery independently validates all1236core/21API34 results and18PNG artifacts, application inputs, signing identity and checksum before publication; signing/publication still pending at this report commit.86remainslastactuallyverifiedsignedprivateAPK(cb2eb063a667927c0784c719adcbe3db5f724093c7245d673d9b2038465a22c2).

## Limits
Same-length raw-source changes canstill evade length-basedfingerprints; this batch verifiescachebytes, noteveryraw-source fingerprint. EntireUIMSDaudit/nativeoverhaul/customrules/fullheadlesssimulator/livehub/fullSPDaudit/owneractualsave/phone/forkline-report unfinished. No gameplay, capacities, balance, seededRNG, scoring, signing, appID orrawrecordformats changed. No paidservices orunfinishedservers enabled.
