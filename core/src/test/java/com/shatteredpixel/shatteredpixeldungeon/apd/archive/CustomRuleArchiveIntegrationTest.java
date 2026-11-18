package com.shatteredpixel.shatteredpixeldungeon.apd.archive;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import static org.junit.Assert.*;
public class CustomRuleArchiveIntegrationTest {
 @Rule public TemporaryFolder tmp=new TemporaryFolder();
 @Test public void unfinishedExportRetainsAllSettingsButRedactsSeedAndPreservesOriginalLog()throws Exception{
  File root=tmp.newFolder("source"),runs=new File(root,"runs");assertTrue(runs.mkdir());File file=new File(runs,"custom.apdlog");CustomDungeonRules selected=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_HP,500).with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,60);
  RunLogWriter w=RunLogWriter.create(file,TelemetryVersion.CURRENT,"custom","test",1);LifecycleEvents.RunStart start=new LifecycleEvents.RunStart();start.runId="custom";start.seed=9384928394L;start.customSeedText="private seed";start.customRules=selected.preferenceText();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomScoreSnapshot.forNewRun(selected).recordTo(start);assertTrue(LifecycleEvents.emitRunStart(w,0,false,start));assertTrue(w.close(false));byte[] before=Files.readAllBytes(file.toPath());
  ByteArrayOutputStream output=new ByteArrayOutputStream();ArchiveExporter.Report exported=ArchiveExporter.export(root,output,"test",1);assertEquals(1,exported.redactedRunLogs);assertArrayEquals(before,Files.readAllBytes(file.toPath()));File imported=tmp.newFolder("dest");ArchiveImporter.Report report=ArchiveImporter.importArchive(imported,new ByteArrayInputStream(output.toByteArray()));assertTrue(report.summary(),report.ok);ReadResult raw=RunLogReader.read(new File(imported,"runs/custom.apdlog"));LifecycleEvents.RunStart restored=LifecycleEvents.decodeRunStart(raw.events.get(0),raw);assertEquals(0,restored.seed);assertEquals("",restored.customSeedText);assertEquals(selected.preferenceText(),restored.customRules);assertEquals(2,restored.customScoreVersion);assertEquals(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomRuleMultipliers.VERSION,restored.customScoreCalculation);RunFacts f=RunFactsBuilder.fromLog(raw,"custom");assertEquals(60,f.numOr(CustomRuleFacts.key(CustomDungeonRules.Slider.UPGRADE_SCROLLS.name()),-1));assertFalse(f.isKnown(CustomRuleFacts.GENERATED_UPGRADES));
 }
}
