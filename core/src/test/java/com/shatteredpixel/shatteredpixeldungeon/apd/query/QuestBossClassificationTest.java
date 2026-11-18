package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental;
import com.watabou.utils.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

public class QuestBossClassificationTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    private static final String[] QUEST_BOSSES={"actors.mobs.CrystalSpire","actors.mobs.GnollGeomancer","actors.mobs.FungalCore","actors.mobs.quest.vault.VaultBossElemental"};
    @Test public void recordedClassificationMatchesActualQuestBossPropertiesWithoutDrawingRng() {
        HeadlessTestSupport.start();Random.pushGenerator(871);
        try {
            for(Mob mob:new Mob[]{new CrystalSpire(),new GnollGeomancer(),new FungalCore(),new VaultBossElemental(),new Goo()}) {
                assertTrue(Char.hasProp(mob,Char.Property.BOSS));
                assertTrue(mob.getClass().getName(),BossClasses.isBoss(mob.getClass().getName()));
            }
            for(Mob mob:new Mob[]{new Rat(),new Pylon()})assertEquals(Char.hasProp(mob,Char.Property.BOSS),BossClasses.isBoss(mob.getClass().getName()));
        } finally {Random.popGenerator();}
        Random.pushGenerator(99);long expected;try{expected=Random.Long();}finally{Random.popGenerator();}
        Random.pushGenerator(99);try{for(String name:QUEST_BOSSES)BossClasses.isBoss(name);assertEquals(expected,Random.Long());}finally{Random.popGenerator();}
    }
    private File log(File root,String id,boolean finished) throws Exception {
        File runs=new File(root,"runs");runs.mkdirs();File file=new File(runs,id+".apdlog");
        RunLogWriter writer=RunLogWriter.create(file,TelemetryVersion.CURRENT,id,"test",1);
        LifecycleEvents.RunStart start=new LifecycleEvents.RunStart();start.runId=id;start.heroClass="WARRIOR";start.rulesetId="rules";
        assertTrue(LifecycleEvents.emitRunStart(writer,0,false,start));
        long clock=1;
        for(String name:QUEST_BOSSES) {
            CombatEvents.Attack attack=new CombatEvents.Attack();attack.attacker.kind=CombatEvents.KIND_HERO;
            attack.attacker.name="you";attack.defender.kind=CombatEvents.KIND_ENEMY;attack.defender.name=name;attack.hit=true;
            assertTrue(CombatEvents.emitAttack(writer,clock,attack));
            CombatEvents.AttackDetail detail=new CombatEvents.AttackDetail();detail.attackerClass="Hero";detail.defenderClass=name;detail.hit=true;detail.publicEmitted=true;
            assertTrue(CombatEvents.emitAttackDetail(writer,clock++,detail));
        }
        if(finished)assertTrue(LifecycleEvents.emitRunEnd(writer,clock,new LifecycleEvents.RunEnd()));
        writer.close();return file;
    }
    private static void assertBosses(Catalog cat,String id,boolean finished) {
        Dataset enemies=cat.dataset(Datasets.ENEMIES);int seen=0;
        for(Row row:enemies.rows)if(row.runId.equals(id)) {
            seen++;int col=enemies.def.indexOf("enemy.boss");
            if(finished){assertTrue(row.isKnown(col));assertEquals(row.id,1,row.num(col));}
            else assertFalse("Unfinished true-class-derived boss status remains unknown",row.isKnown(col));
        }
        assertEquals(4,seen);
        ViewDef bosses=cat.view("bosses");
        QueryResult result=QueryEngine.run(enemies,bosses.instanceQuery().withScope(bosses.instanceQuery().scope.withRules(Scope.Rules.ALL,"").withUnknownRuleset(true)),"rules");
        assertEquals(4,result.rows.size());for(Row row:result.rows)assertTrue(row.finished);
    }
    @Test public void actualRecordedQuestBossesRemainInFinishedBossTablesOnColdAndWarmLoads() throws Exception {
        File root=tmp.newFolder();File finished=log(root,"finished",true),live=log(root,"live",false);
        byte[] a=Files.readAllBytes(finished.toPath()),b=Files.readAllBytes(live.toPath());
        DatasetStore store=new DatasetStore(root);
        for(int pass=0;pass<2;pass++) {
            Catalog cat=store.load();assertBosses(cat,"finished",true);assertBosses(cat,"live",false);
            if(pass==1)assertEquals(2,cat.logsCached);
            assertArrayEquals(a,Files.readAllBytes(finished.toPath()));assertArrayEquals(b,Files.readAllBytes(live.toPath()));
        }
    }
    @Test public void oldBuilderTenCacheIsRejectedAndRebuiltWithoutChangingRawEvidence() throws Exception {
        assertEquals("Classification changes must invalidate old derived rows",18,RowBuilders.BUILDER_VERSION);
        File root=tmp.newFolder();File file=log(root,"finished",true);byte[] original=Files.readAllBytes(file.toPath());
        DatasetStore store=new DatasetStore(root);assertBosses(store.load(),"finished",true);
        File cache=new File(store.rowsDir(),"runs-finished.apdr");byte[] bytes=com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore.plainCache(Files.readAllBytes(cache.toPath()));
        Map<String,DatasetDef> defs=new LinkedHashMap<>();for(Dataset dataset:store.load().datasets())defs.put(dataset.def.id,dataset.def);
        LogRows oldRows=RowCodec.decode(bytes,file.length(),defs);assertNotNull(oldRows);
        for(Row row:oldRows.rows.get(Datasets.ENEMIES))row.setNum(defs.get(Datasets.ENEMIES).indexOf("enemy.boss"),0,Fact.PROV_RECORDED);
        bytes=RowCodec.encode(oldRows,file.length(),defs);
        assertEquals(18,bytes[5]);bytes[5]=10;
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-4);
        java.nio.ByteBuffer.wrap(bytes,bytes.length-4,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt((int)crc.getValue());
        Files.write(cache.toPath(),bytes);
        Catalog rebuilt=store.load();assertEquals(0,rebuilt.logsCached);assertEquals(1,rebuilt.logsRebuilt);assertBosses(rebuilt,"finished",true);
        assertEquals(1,store.load().logsCached);assertArrayEquals(original,Files.readAllBytes(file.toPath()));
    }
}


