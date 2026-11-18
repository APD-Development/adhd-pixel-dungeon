package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultSentry;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultCircleRoom;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class VaultCircleTimingTest {
    private Level previousLevel;
    private float previousPlayback;
    private Method act;
    private static class Board extends Level {
        @Override protected boolean build(){return true;}
        @Override protected void createMobs(){}
        @Override protected void createItems(){}
        @Override public void updateFieldOfView(Char who,boolean[] fov){}
    }
    private static class SilentSprite extends CharSprite {
        @Override public void hideAlert(){}
        @Override public void hideLost(){}
    }
    private static class SavedVault extends VaultLevel {
        void room(Room room){rooms=new ArrayList<>();rooms.add(room);}
    }
    @Before public void isolate()throws Exception{
        HeadlessTestSupport.start();previousLevel=Dungeon.level;
        Field f=PlaybackGroup.class.getDeclaredField("speed");f.setAccessible(true);previousPlayback=f.getFloat(null);
        act=VaultSentry.class.getDeclaredMethod("act");act.setAccessible(true);
    }
    @After public void restore()throws Exception{
        Dungeon.level=previousLevel;
        Field f=PlaybackGroup.class.getDeclaredField("speed");f.setAccessible(true);f.setFloat(null,previousPlayback);
        if(previousLevel!=null)PathFinder.setMapSize(previousLevel.width(),previousLevel.height());
    }
    private VaultCircleRoom room(){VaultCircleRoom r=new VaultCircleRoom();r.set(1,1,11,11);return r;}
    private void empty(Level b){
        b.setSize(15,15);b.mobs=new java.util.HashSet<>();b.heaps=new com.watabou.utils.SparseArray<>();
        b.blobs=new java.util.HashMap<>();b.plants=new com.watabou.utils.SparseArray<>();
        b.traps=new com.watabou.utils.SparseArray<>();b.transitions=new ArrayList<>();
        b.customTiles=new ArrayList<>();b.customTerrain=new ArrayList<>();b.customWalls=new ArrayList<>();
    }
    private Board board(){Board b=new Board();empty(b);Dungeon.level=b;return b;}
    private VaultSentry generated(long seed){
        Board b=board();Random.pushGenerator(seed);
        try{room().paint(b);}finally{Random.popGenerator();}
        assertEquals(1,b.mobs.size());VaultSentry s=(VaultSentry)b.mobs.iterator().next();s.sprite=new SilentSprite();return s;
    }
    private void tick(VaultSentry sentry)throws Exception{assertEquals(Boolean.TRUE,act.invoke(sentry));}
    @Test public void everyGeneratedCirclePatternUsesTheUpstreamTwoTurnInterval(){
        java.util.Set<Float> widths=new java.util.HashSet<>();
        for(long seed=0;seed<128;seed++){
            VaultSentry s=generated(seed);widths.add(s.scanWidth);
            assertEquals("seed "+seed,2,s.afterScanCooldown);
            assertEquals(1,s.scansAfterCooldown);
        }
        assertEquals("preserve existing APD patterns",3,widths.size());
    }
    @Test public void actualScanDirectionAdvancesEveryOtherTurnAtEveryPlaybackSpeed()throws Exception{
        for(int speed=0;speed<3;speed++){
            PlaybackGroup.speed(speed);VaultSentry s=generated(42);int scans=0;
            float start=s.cooldown();
            for(int turn=1;turn<=32;turn++){
                tick(s);if(turn%2==1)scans++;
                assertEquals("turn "+turn+" playback "+speed,scans%s.scanDirs.length,s.scanDirIdx);
                assertEquals(start+turn,s.cooldown(),0.00001f);
            }
        }
    }
    @Test public void oldSavedVaultRepairsOnlyTheCircleIntervalWithoutResettingPhase()throws Exception{
        VaultCircleRoom r=room();SavedVault old=new SavedVault();empty(old);old.room(r);Dungeon.level=old;
        Random.pushGenerator(42);try{r.paint(old);}finally{Random.popGenerator();}
        VaultSentry s=(VaultSentry)old.mobs.iterator().next();
        s.afterScanCooldown=1;s.curCooldown=1;s.scanDirIdx=3;
        Bundle saved=new Bundle();old.storeInBundle(saved);
        SavedVault restored=new SavedVault();restored.restoreFromBundle(saved);
        VaultSentry back=(VaultSentry)restored.mobs.iterator().next();
        assertEquals(2,back.afterScanCooldown);assertEquals(1,back.curCooldown);assertEquals(3,back.scanDirIdx);
        assertEquals(s.pos,back.pos);assertEquals(s.scanWidth,back.scanWidth,0f);
        assertEquals(s.scanDirs.length,back.scanDirs.length);
        for(int i=0;i<s.scanDirs.length;i++)assertArrayEquals(s.scanDirs[i],back.scanDirs[i]);
    }
    @Test public void repairIsIdempotentAndConsumesNoRng()throws Exception{
        VaultSentry s=generated(6);s.afterScanCooldown=1;s.scanDirIdx=2;s.curCooldown=1;
        VaultCircleRoom r=room();
        long expected;Random.pushGenerator(271828);try{expected=Random.Long();}finally{Random.popGenerator();}
        Random.pushGenerator(271828);
        try{r.onLevelLoad(Dungeon.level);r.onLevelLoad(Dungeon.level);assertEquals(expected,Random.Long());}
        finally{Random.popGenerator();}
        assertEquals(2,s.afterScanCooldown);assertEquals(2,s.scanDirIdx);assertEquals(1,s.curCooldown);
    }
    @Test public void otherSentinelsAndExplicitlyDifferentIntervalsAreUntouched(){
        Board b=board();VaultCircleRoom r=room();
        VaultSentry outside=new VaultSentry();outside.pos=16;outside.afterScanCooldown=1;b.mobs.add(outside);
        VaultSentry different=new VaultSentry();different.pos=96;different.afterScanCooldown=4;b.mobs.add(different);
        r.onLevelLoad(b);assertEquals(1,outside.afterScanCooldown);assertEquals(4,different.afterScanCooldown);
        different.afterScanCooldown=1;different.scansAfterCooldown=3;r.onLevelLoad(b);
        assertEquals(1,different.afterScanCooldown);assertEquals(3,different.scansAfterCooldown);
    }
}
