package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpear;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class BlacksmithRewardVisibilityTest {
    private ArrayList<Item> previous;
    @Before public void start(){HeadlessTestSupport.start();previous=Blacksmith.Quest.smithRewards;}
    @After public void restore(){Blacksmith.Quest.smithRewards=previous;}
    @Test public void everyOfferedLevelIsRevealedWithoutChangingTheReward(){
        for(int level=0;level<=3;level++){
            ArrayList<Item> offers=new ArrayList<>();offers.add(new Sword());offers.add(new LeatherArmor());offers.add(new ThrowingSpear());
            for(Item item:offers){item.level(level);item.levelKnown=false;item.cursedKnown=false;}
            Blacksmith.Quest.smithRewards=offers;Blacksmith.Quest.revealOfferedLevels();
            assertSame(offers,Blacksmith.Quest.smithRewards);
            for(int i=0;i<offers.size();i++){
                Item item=offers.get(i);assertSame(item,Blacksmith.Quest.smithRewards.get(i));
                assertEquals(level,item.level());assertEquals(level,item.visiblyUpgraded());assertTrue(item.levelKnown);
                assertFalse(item.cursedKnown);assertFalse(item.isIdentified());
            }
        }
    }
    @Test public void revealingDoesNotConsumeRngOrChangeFavorAndRewardCount(){
        Blacksmith.Quest.smithRewards=new ArrayList<>();Blacksmith.Quest.smithRewards.add(new Sword());
        int favor=Blacksmith.Quest.favor,smiths=Blacksmith.Quest.smiths;int expected;
        Random.pushGenerator(445);try{expected=Random.Int();}finally{Random.popGenerator();}
        Random.pushGenerator(445);try{Blacksmith.Quest.revealOfferedLevels();assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
        assertEquals(favor,Blacksmith.Quest.favor);assertEquals(smiths,Blacksmith.Quest.smiths);assertEquals(1,Blacksmith.Quest.smithRewards.size());
    }
    @Test public void deferredEnchantAndGlyphRemainHiddenAndUnchanged(){
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment oldEnchant=Blacksmith.Quest.smithEnchant;
        com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph oldGlyph=Blacksmith.Quest.smithGlyph;
        try{
            Blazing enchant=new Blazing();Flow glyph=new Flow();Blacksmith.Quest.smithEnchant=enchant;Blacksmith.Quest.smithGlyph=glyph;
            Sword sword=new Sword();LeatherArmor armor=new LeatherArmor();
            Blacksmith.Quest.smithRewards=new ArrayList<>();Blacksmith.Quest.smithRewards.add(sword);Blacksmith.Quest.smithRewards.add(armor);
            Blacksmith.Quest.revealOfferedLevels();
            assertSame(enchant,Blacksmith.Quest.smithEnchant);assertSame(glyph,Blacksmith.Quest.smithGlyph);
            assertNull(sword.enchantment);assertNull(armor.glyph);assertFalse(sword.cursedKnown);assertFalse(armor.cursedKnown);
        }finally{Blacksmith.Quest.smithEnchant=oldEnchant;Blacksmith.Quest.smithGlyph=oldGlyph;}
    }
    @Test public void offeredLevelSurvivesExistingItemSerialization(){
        Sword sword=new Sword();sword.level(2);sword.levelKnown=false;sword.cursedKnown=false;
        Blacksmith.Quest.smithRewards=new ArrayList<>();Blacksmith.Quest.smithRewards.add(sword);Blacksmith.Quest.revealOfferedLevels();
        Bundle b=new Bundle();sword.storeInBundle(b);Sword restored=new Sword();restored.restoreFromBundle(b);
        assertEquals(2,restored.visiblyUpgraded());assertEquals(sword.level(),restored.level());assertFalse(restored.cursedKnown);
    }
    @Test public void noOrEmptyOffersAreHarmlessAndNotGenerated(){
        Blacksmith.Quest.smithRewards=null;Blacksmith.Quest.revealOfferedLevels();assertNull(Blacksmith.Quest.smithRewards);
        Blacksmith.Quest.smithRewards=new ArrayList<>();Blacksmith.Quest.revealOfferedLevels();assertTrue(Blacksmith.Quest.smithRewards.isEmpty());
    }
}
