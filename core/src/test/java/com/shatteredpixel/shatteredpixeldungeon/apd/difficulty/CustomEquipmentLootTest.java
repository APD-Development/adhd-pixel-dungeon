package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundle;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
public class CustomEquipmentLootTest {
    static class Quiet extends ItemSprite {@Override public void drop(){}}
    static class Drops extends Level {
        final List<Item> dropped=new ArrayList<>();
        protected boolean build(){return true;}protected void createMobs(){}protected void createItems(){}
        @Override public Heap drop(Item item,int cell){dropped.add(item);Heap heap=new Heap();heap.drop(item);heap.sprite=new Quiet();return heap;}
    }
    static class Ordinary extends Mob {
        Ordinary(){lootChance=1;maxLvl=100;loot=Generator.Category.WEAPON;}
        @Override public Item createLoot(){return loot instanceof Item?(Item)loot:new Dagger();}
        void existing(Item item){loot=item;}
    }
    private CustomDungeonRules prefs;private HeroClass selected;private boolean intro,recording;private int difficulty,challenges;
    @Before public void setup(){HeadlessTestSupport.start();prefs=SPDSettings.apdCustomRules();selected=GamesInProgress.selectedClass;
        intro=SPDSettings.intro();recording=SPDSettings.apdRecording();difficulty=SPDSettings.apdDifficulty();challenges=SPDSettings.challenges();
        SPDSettings.intro(false);SPDSettings.apdRecording(false);SPDSettings.apdDifficulty(3);SPDSettings.challenges(0);}
    @After public void cleanup(){SPDSettings.apdCustomRules(prefs);GamesInProgress.selectedClass=selected;SPDSettings.intro(intro);SPDSettings.apdRecording(recording);
        SPDSettings.apdDifficulty(difficulty);SPDSettings.challenges(challenges);Dungeon.level=null;Dungeon.apdRestoreCustomRules(new Bundle());}
    private Drops init(int percent){SPDSettings.apdCustomRules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.EQUIPMENT,percent));
        GamesInProgress.selectedClass=HeroClass.WARRIOR;Dungeon.seed=8123;Dungeon.customSeedText="";Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.init();
        Dungeon.depth=1;Drops level=new Drops();level.setSize(7,7);Dungeon.level=level;return level;}
    @Test public void actualOrdinaryMonsterDropPathExpandsOnlyFreshGeneratedEquipment(){
        Drops level=init(200);Ordinary mob=new Ordinary();mob.pos=24;mob.rollToDropLoot();
        assertEquals(2,level.dropped.size());assertNotSame(level.dropped.get(0),level.dropped.get(1));
        assertTrue(level.dropped.get(0) instanceof Dagger);assertTrue(level.dropped.get(1) instanceof Dagger);
    }
    @Test public void existingLootAndThiefStolenPlayerItemsAreNeverRemovedOrMultiplied(){
        for(int percent:new int[]{25,200}){
            Drops level=init(percent);Dagger owned=new Dagger();Ordinary mob=new Ordinary();mob.pos=24;mob.existing(owned);mob.rollToDropLoot();
            assertEquals(1,level.dropped.size());assertSame(owned,level.dropped.get(0));
            level.dropped.clear();Thief thief=new Thief();thief.pos=24;thief.item=owned;thief.rollToDropLoot();
            int count=0;for(Item item:level.dropped)if(item==owned)count++;
            assertEquals(1,count);assertNull(thief.item);
        }
    }
}
