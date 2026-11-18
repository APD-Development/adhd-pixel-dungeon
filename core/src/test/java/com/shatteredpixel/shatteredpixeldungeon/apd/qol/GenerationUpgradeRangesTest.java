/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Slime;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ParchmentScrap;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.*;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class GenerationUpgradeRangesTest {
    private Hero oldHero; private Level oldLevel; private int oldDepth, oldBranch;
    private Bundle oldRules, oldGenerator;
    @Before public void setup() {
        HeadlessTestSupport.start(); oldHero = Dungeon.hero; oldLevel = Dungeon.level; oldDepth = Dungeon.depth; oldBranch = Dungeon.branch;
        oldRules = new Bundle(); Dungeon.apdStoreCustomRules(oldRules); rules(100);
        oldGenerator = new Bundle(); Generator.storeInBundle(oldGenerator); Generator.fullReset();
        Dungeon.hero = new Hero(); Dungeon.depth = 8; Dungeon.branch = 0;
        ItemOriginsTest.RoomsLevel level = new ItemOriginsTest.RoomsLevel(); level.setSize(8, 8); level.roomType(new ArmoryRoom()); Dungeon.level = level;
    }
    @After public void cleanup() { Dungeon.hero = oldHero; Dungeon.level = oldLevel; Dungeon.depth = oldDepth; Dungeon.branch = oldBranch; Dungeon.apdRestoreCustomRules(oldRules); Generator.restoreFromBundle(oldGenerator); }
    private void rules(int equipment) { Bundle b = new Bundle(), r = new Bundle(); CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.EQUIPMENT, equipment).store(r); b.put("apd_custom_rules", r); Dungeon.apdRestoreCustomRules(b); }
    private <T extends Item> T found(T item) { item.levelKnown = false; item.cursedKnown = false; ItemOrigins.record(item, new ItemOrigins.Origin(8, 0, 0, "ArmoryRoom")); return item; }
    private void range(Item item, int min, int max) { assertEquals(new GenerationUpgradeRanges.Range(min,max), item.apdGenerationRange); }
    private ItemOrigins.Origin origin(String rule) { return new ItemOrigins.Origin(8,0,0,"CryptRoom",rule); }
    @Test public void allHiddenLevelsFromSamePrizeRulesProduceExactlyTheSameRange() {
        String expected = null;
        for (int hidden = 0; hidden <= 3; hidden++) {
            Sword sword = found(new Sword()); sword.level(hidden); sword.cursed = hidden % 2 == 0;
            GenerationUpgradeRanges.optionalBonus(sword); range(sword, 0, 3);
            String text = GenerationUpgradeRanges.describe(sword); assertTrue(text, text.contains("Possible upgrade level: +0 to +3"));
            if (expected == null) expected = text; else assertEquals(expected, text);
        }
    }
    public static class NoSecretAccessSword extends Sword {
        @Override public int level() { throw new AssertionError("Hidden level was read"); }
        @Override public int buffedLvl() { throw new AssertionError("Hidden buffed level was read"); }
    }
    @Test public void rangeCalculationAndDescriptionNeverReadAnyHiddenLevelAccessor() {
        Sword sword = found(new NoSecretAccessSword()); GenerationUpgradeRanges.baseline(sword); GenerationUpgradeRanges.optionalBonus(sword);
        assertTrue(GenerationUpgradeRanges.describe(sword).contains("+0 to +3"));
        sword.apdGenerationRange = null; GenerationUpgradeRanges.recover(sword, origin("normal")); range(sword,0,2);
        sword.apdGenerationRange = null; GenerationUpgradeRanges.recover(sword, origin("room_bonus_unknown_context")); range(sword,0,3);
    }
    @Test public void ordinaryWeaponArmorRingAndWandFactoriesRecordTheirActualSupport() {
        for (int n=0;n<100;n++) for (Item item:new Item[]{new Sword().random(),new LeatherArmor().random(),new RingOfForce().random(),new WandOfMagicMissile().random()}) range(item,0,2);
    }
    @Test public void artifactFactoryIsFixedZeroRatherThanUniversalZeroToThree() { CloakOfShadows item=new CloakOfShadows();item.random();range(item,0,0); }
    @Test public void ordinaryAndSpecialPrizeSourcesDiffer() { Sword ordinary=found(new Sword());GenerationUpgradeRanges.baseline(ordinary);Sword special=found(new Sword());GenerationUpgradeRanges.optionalBonus(special);assertFalse(GenerationUpgradeRanges.describe(ordinary).equals(GenerationUpgradeRanges.describe(special))); }
    @Test public void curseStateCannotNarrowCryptSupport() { Sword a=found(new Sword()),b=found(new Sword());a.cursed=true;b.cursed=false;GenerationUpgradeRanges.curseConditionalBonus(a);GenerationUpgradeRanges.curseConditionalBonus(b);range(a,0,3);assertEquals(a.apdGenerationRange,b.apdGenerationRange); }
    @Test public void independentParchmentConditionCanGuaranteeAnExtraUpgradeWithoutReadingPrize() { ParchmentScrap parchment=new ParchmentScrap();parchment.level(3);Dungeon.hero.belongings.backpack.items.add(parchment);Sword s=found(new NoSecretAccessSword());GenerationUpgradeRanges.curseConditionalBonus(s);range(s,1,3); }
    @Test public void equipmentExtrasAreIncludedInWholeRoomUnion() { ParchmentScrap parchment=new ParchmentScrap();parchment.level(3);Dungeon.hero.belongings.backpack.items.add(parchment);Sword s=new NoSecretAccessSword();ItemOrigins.Origin origin=new ItemOrigins.Origin(8,0,GenerationEvents.C_TOMB,"CryptRoom");rules(100);GenerationUpgradeRanges.finalRoomSupport(s,origin);range(s,1,3);rules(200);GenerationUpgradeRanges.finalRoomSupport(s,origin);range(s,0,3); }
    @Test public void historicalMissingConditionsUseUnionAndNeverCurrentTrinketLevel() { ParchmentScrap p=new ParchmentScrap();p.level(3);Dungeon.hero.belongings.backpack.items.add(p);Sword s=found(new Sword());GenerationUpgradeRanges.recover(s,origin("room_bonus_unknown_context"));range(s,0,3); }
    @Test public void historicalQuestRulesAreDistinct() { Sword ghost=found(new Sword());GenerationUpgradeRanges.recover(ghost,origin("ghost"));range(ghost,0,3);WandOfMagicMissile wand=found(new WandOfMagicMissile());GenerationUpgradeRanges.recover(wand,origin("wandmaker"));range(wand,1,3);Sword shop=found(new Sword());GenerationUpgradeRanges.recover(shop,origin("shop"));range(shop,0,0); }
    @Test public void impRewardUsesPublicBaseTierAndNotHiddenLevel() { Longsword four=found(new Longsword());Greatsword five=found(new Greatsword());four.level(0);five.level(99);GenerationUpgradeRanges.impReward(four);GenerationUpgradeRanges.impReward(five);range(four,3,5);range(five,2,4); }
    @Test public void actualSlimeLootResetsItsRangeToFixedZero() { Item loot=new Slime().createLoot();range(loot,0,0); }
    @Test public void zeroOnlyMimicUpgradeDoesNotInventPlusThree() { Sword gold=found(new Sword());GenerationUpgradeRanges.baseline(gold);GenerationUpgradeRanges.minimumBonus(gold,false);range(gold,0,2);GenerationUpgradeRanges.minimumBonus(gold,true);range(gold,1,2); }
    @Test public void actualBlacksmithRewardFactoryRecordsZeroToThreeForEveryRoll() { for(int n=0;n<30;n++){Blacksmith.Quest.generateRewards(false);for(Item item:Blacksmith.Quest.smithRewards)range(item,0,3);} }
    @Test public void actualSpecialRoomFactoriesNeverReturnNullAndAlwaysUseWholeSourceRange() throws Exception {
        Random.pushGenerator(71940);try {
            for(Class<?> room:Arrays.asList(PoolRoom.class,TrapsRoom.class,SentryRoom.class)) {
                java.lang.reflect.Method prize=room.getDeclaredMethod("prize",Level.class);prize.setAccessible(true);
                Set<Integer> rolled=new HashSet<>();
                for(int n=0;n<300;n++){Item item=(Item)prize.invoke(null,Dungeon.level);assertNotNull(item);range(item,0,3);rolled.add(item.level());}
                assertEquals(new HashSet<>(Arrays.asList(0,1,2,3)),rolled);
            }
        }finally{Random.popGenerator();}
    }
    @Test public void actualCryptFactoryUsesSameRangeAcrossDifferentHiddenRolls() throws Exception {
        java.lang.reflect.Method prize=CryptRoom.class.getDeclaredMethod("prize",Level.class);prize.setAccessible(true);Set<Integer> rolled=new HashSet<>();
        Random.pushGenerator(12003);try {for(int n=0;n<500;n++){Item item=(Item)prize.invoke(null,Dungeon.level);range(item,0,3);rolled.add(item.level());}}finally{Random.popGenerator();}
        assertEquals(new HashSet<>(Arrays.asList(0,1,2,3)),rolled);
    }
    @Test public void identificationHidesBothRangeAndOriginWithoutDestroyingMetadata() { Sword s=found(new Sword());GenerationUpgradeRanges.optionalBonus(s);assertFalse(GenerationUpgradeRanges.describe(s).isEmpty());s.identify(false);assertEquals("",GenerationUpgradeRanges.describe(s));assertEquals("",ItemOrigins.describe(s));range(s,0,3); }
    @Test public void laterPlayerUpgradesDoNotRewriteOriginalGenerationSupport() { Sword s=found(new Sword());GenerationUpgradeRanges.baseline(s);s.upgrade(10);range(s,0,2);assertTrue(GenerationUpgradeRanges.describe(s).contains("Before any later upgrades")); }
    @Test public void fullItemSaveLoadPreservesPossibilityRangeSeparatelyFromActualLevel() throws Exception { Sword s=found(new Sword());s.level(2);GenerationUpgradeRanges.optionalBonus(s);Bundle b=new Bundle();b.put("item",s);ByteArrayOutputStream out=new ByteArrayOutputStream();Bundle.write(b,out);Item restored=(Item)Bundle.read(new ByteArrayInputStream(out.toByteArray())).get("item");range(restored,0,3);assertEquals(2,restored.level());assertEquals(GenerationUpgradeRanges.describe(s),GenerationUpgradeRanges.describe(restored)); }
    @Test public void olderAndMalformedItemsDoNotInventARange() { Sword s=found(new Sword());Bundle old=new Bundle();old.put("quantity",1);s.restoreFromBundle(old);assertNull(s.apdGenerationRange);Bundle malformed=new Bundle();malformed.put("apd_item_upgrade_range_v1",new int[]{3,0});GenerationUpgradeRanges.restore(s,malformed);assertNull(s.apdGenerationRange); }
    @Test public void unknownOrUnrecordedSourcesOmitRangeEvenIfRandomBaselineSurvived() { Sword s=found(new Sword());GenerationUpgradeRanges.baseline(s);s.apdFoundOrigin=new ItemOrigins.Origin(8,0,ItemOrigins.PICKUP,"");assertEquals("",GenerationUpgradeRanges.describe(s));s.apdFoundOrigin=null;assertEquals("",GenerationUpgradeRanges.describe(s)); }
    @Test public void mergeTakesUnionAndNeverNarrowsToActualSecretResults() { Sword a=found(new Sword()),b=found(new Sword());GenerationUpgradeRanges.set(a,0,2);GenerationUpgradeRanges.set(b,1,3);GenerationUpgradeRanges.merge(a,b);range(a,0,3);b.apdGenerationRange=null;GenerationUpgradeRanges.merge(a,b);assertNull(a.apdGenerationRange); }
    @Test public void splitAndDuplicateKeepOriginalGenerationSupport() { ThrowingKnife knife=new ThrowingKnife();knife.quantity(3);GenerationUpgradeRanges.set(knife,0,3);Item copy=knife.duplicate(),split=knife.split(1);range(copy,0,3);range(split,0,3); }
    @Test public void historicalRuleProjectionIgnoresAllHiddenItemTruth() { GenerationEvents.ItemGenerated g=new GenerationEvents.ItemGenerated();g.room="CryptRoom";g.container=GenerationEvents.C_TOMB;String expected=GenerationUpgradeRanges.historicalRule(g);for(int hidden=0;hidden<10;hidden++){g.trueLevel=hidden;g.cursed=hidden%2==0;g.enchant="secret"+hidden;assertEquals(expected,GenerationUpgradeRanges.historicalRule(g));} }
    @Test public void recoveredRangeCanBeEnrichedWithoutChangingAlreadySavedOrigin() { Sword s=found(new Sword());ItemOrigins.Origin before=s.apdFoundOrigin;ItemOrigins.record(s,new ItemOrigins.Origin(8,0,0,"ArmoryRoom","normal"));assertEquals(before,s.apdFoundOrigin);range(s,0,2); }
    @Test public void productionRangeCodeContainsNoUpgradeOrHiddenPropertyReads() throws Exception {
        java.nio.file.Path source=java.nio.file.Paths.get(System.getProperty("apd.game.src")).resolve("apd/qol/GenerationUpgradeRanges.java");
        String code=java.nio.file.Files.readString(source);
        for(String forbidden:new String[]{".level(",".trueLevel(",".buffedLvl(",".cursed", ".enchantment"})assertFalse(forbidden,code.contains(forbidden));
    }
    @Test public void zeroDropSourceIncludesCustomEquipmentExtrasWithoutLookingAtTheActualDrop() {
        Sword s=new NoSecretAccessSword();Slime mob=new Slime();rules(100);GenerationUpgradeRanges.finalMobSupport(s,mob);range(s,0,0);
        rules(200);GenerationUpgradeRanges.finalMobSupport(s,mob);range(s,0,2);
    }
    @Test public void remainsAndUnspecifiedCraftingCannotBorrowOrdinaryGeneratorSupport() { GenerationEvents.ItemGenerated source=new GenerationEvents.ItemGenerated();source.container=GenerationEvents.C_REMAINS;source.drawKind=GenerationEvents.DRAW_CATEGORY;assertEquals("",GenerationUpgradeRanges.historicalRule(source));Sword s=found(new Sword());GenerationUpgradeRanges.recover(s,origin("unrecognised"));assertNull(s.apdGenerationRange); }
}
