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
package com.shatteredpixel.shatteredpixeldungeon.apd;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.GrassDewPickupTest;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Obfuscation;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.*;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;
public class MechanicsExplanationTest {
 private final java.util.Map<java.lang.reflect.Field,Object> badgeState=new java.util.LinkedHashMap<>();
 private com.watabou.utils.Bundle oldRules;private Object oldNotes;private int oldChallenges;private Hero oldHero,hero;private com.shatteredpixel.shatteredpixeldungeon.levels.Level oldLevel;private MailArmor armor;
 @Before public void setup()throws Exception{HeadlessTestSupport.start();for(String key:new String[]{"global","local","saveNeeded"}){java.lang.reflect.Field f=Badges.class.getDeclaredField(key);f.setAccessible(true);Object prior=f.get(null);badgeState.put(f,prior);f.set(null,key.equals("saveNeeded")?false:prior==null?new java.util.HashSet<>():new java.util.HashSet<>((java.util.Set<?>)prior));}java.lang.reflect.Field notes=com.shatteredpixel.shatteredpixeldungeon.journal.Notes.class.getDeclaredField("records");notes.setAccessible(true);oldNotes=notes.get(null);com.shatteredpixel.shatteredpixeldungeon.journal.Notes.reset();oldChallenges=Dungeon.challenges;Dungeon.challenges=0;oldRules=new com.watabou.utils.Bundle();Dungeon.apdStoreCustomRules(oldRules);Dungeon.apdRestoreCustomRules(new com.watabou.utils.Bundle());oldHero=Dungeon.hero;oldLevel=Dungeon.level;Dungeon.level=new GrassDewPickupTest.TestLevel();Dungeon.level.setSize(7,7);hero=new Hero();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);hero.pos=24;hero.HP=hero.HT=20;Dungeon.hero=hero;armor=new MailArmor();armor.levelKnown=armor.cursedKnown=true;armor.inscribe(new Obfuscation());hero.belongings.armor=armor;}
 @After public void restore()throws Exception{for(java.util.Map.Entry<java.lang.reflect.Field,Object> e:badgeState.entrySet())e.getKey().set(null,e.getValue());Dungeon.hero=oldHero;Dungeon.level=oldLevel;Dungeon.challenges=oldChallenges;Dungeon.apdRestoreCustomRules(oldRules);java.lang.reflect.Field notes=com.shatteredpixel.shatteredpixeldungeon.journal.Notes.class.getDeclaredField("records");notes.setAccessible(true);notes.set(null,oldNotes);}
 private RingOfArcana ring(int level){RingOfArcana ring=new RingOfArcana();ring.level(level);ring.anonymize();ring.levelKnown=ring.cursedKnown=true;hero.belongings.ring=ring;ring.activate(hero);return ring;}
 public static class BountyLootProbe extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob {public BountyLootProbe(float chance){lootChance=chance;}}
 @Test public void mealChargeTextUsesSingularForOneChargeAndUncountedDamage() {
  hero.heroClass=HeroClass.DUELIST;
  String focused=TalentExplanation.details(Talent.FOCUSED_MEAL,2);
  assertTrue(focused,focused.contains("_1 weapon-ability charge_"));
  assertTrue(TalentExplanation.details(Talent.FOCUSED_MEAL,1).contains("_0.667 weapon-ability charges_"));
  hero.heroClass=HeroClass.CLERIC;
  String enlightening=TalentExplanation.details(Talent.ENLIGHTENING_MEAL,2);
  assertTrue(enlightening,enlightening.contains("_1 Tome charge_"));
  assertTrue(TalentExplanation.details(Talent.ENLIGHTENING_MEAL,1).contains("_0.667 Tome charges_"));
  hero.heroClass=HeroClass.WARRIOR;hero.lvl=1;
  String alternate=TalentExplanation.details(Talent.FOCUSED_MEAL,2,true);
  assertTrue(alternate,alternate.contains("At your current level: _+1 damage_."));
 }
 public static class CombinedLethalityTarget extends PlainStatusTarget {
  int deaths;
  CombinedLethalityTarget(com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property trait) {
   if (trait != null) properties.add(trait);
  }
  @Override public void die(Object cause) { deaths++; }
 }
 @Test public void combinedLethalityTextMatchesRealInclusiveThresholdAndExclusions() throws Exception {
  hero.sprite=quietSprite();
  hero.STR=20;
  java.util.Arrays.fill(Dungeon.level.heroFOV,false);
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword first=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword copy=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();
  java.lang.reflect.Method used=com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.class.getDeclaredMethod("afterAbilityUsed",Hero.class);
  used.setAccessible(true);
  com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property[] traits={null,
   com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.BOSS,
   com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.MINIBOSS};
  for(int rank=1;rank<=3;rank++) {
   hero.talents.get(2).put(Talent.COMBINED_LETHALITY,rank);
   for(boolean sameWeapon:new boolean[]{false,true}) for(int relation=0;relation<4;relation++) {
    hero.belongings.weapon=sameWeapon?first:copy;
    CombinedLethalityTarget target=new CombinedLethalityTarget(relation<3?traits[relation]:null);
    target.HT=150;
    target.pos=hero.pos+1;
    target.alignment=relation==3?hero.alignment:com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;
    Buff.detach(hero,Talent.CombinedLethalityAbilityTracker.class);
    target.HP=150;
    Random.pushGenerator(43110);
    try { assertTrue(hero.attack(target)); } finally { Random.popGenerator(); }
    int ordinaryDamage=150-target.HP;
    assertTrue(ordinaryDamage>0);
    for(int offset=-1;offset<=1;offset++) {
     int remaining=20*rank+offset;
     Buff.detach(hero,Talent.CombinedLethalityAbilityTracker.class);
     used.invoke(first,hero);
     Talent.CombinedLethalityAbilityTracker tracker=hero.buff(Talent.CombinedLethalityAbilityTracker.class);
     assertNotNull(tracker);
     assertSame(first,tracker.weapon);
     float time=hero.cooldown();
     TalentExplanation.details(Talent.COMBINED_LETHALITY,rank);
     assertSame(tracker,hero.buff(Talent.CombinedLethalityAbilityTracker.class));
     assertSame(first,tracker.weapon);
     assertEquals(time,hero.cooldown(),0);
     target.HP=remaining+ordinaryDamage;
     target.deaths=0;
     Random.pushGenerator(43110);
     try { assertTrue(hero.attack(target)); } finally { Random.popGenerator(); }
     boolean executes=!sameWeapon&&relation==0&&offset<=0;
     String context="rank="+rank+", sameWeapon="+sameWeapon+", relation="+relation+", remaining="+remaining;
     assertEquals(context,executes?0:remaining,target.HP);
     assertEquals(context,executes?1:0,target.deaths);
     assertEquals(context,sameWeapon,hero.buff(Talent.CombinedLethalityAbilityTracker.class)!=null);
    }
   }
  }
  String normal=Talent.COMBINED_LETHALITY.desc();
  assertTrue(normal,normal.contains("at or below"));
  assertTrue(normal,normal.contains("Bosses and minibosses"));
  assertTrue(normal,normal.contains("about 13.33%"));
  assertTrue(normal,normal.contains("about 26.67%"));
  assertTrue(normal,normal.contains("40%"));
 }
 @Test public void swiftEquipExplanationMatchesActualFreeUsesCooldownSaveAndCursedBlock()throws Exception{
  hero.sprite=quietSprite();java.util.Set<com.shatteredpixel.shatteredpixeldungeon.actors.Actor> original=new java.util.HashSet<>(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all());
  try{for(int rank=1;rank<=2;rank++){
   Buff.detach(hero,Talent.SwiftEquipCooldown.class);hero.talents.get(1).put(Talent.SWIFT_EQUIP,rank);hero.belongings.weapon=null;hero.belongings.backpack.items.clear();
   com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword first=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword(),second=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword(),third=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();
   first.identify();second.identify();third.identify();hero.belongings.backpack.items.add(first);hero.belongings.backpack.items.add(second);hero.belongings.backpack.items.add(third);
   float before=hero.cooldown();assertTrue(first.doEquip(hero));assertEquals(before,hero.cooldown(),0);Talent.SwiftEquipCooldown cooldown=hero.buff(Talent.SwiftEquipCooldown.class);assertNotNull(cooldown);assertEquals(20,cooldown.visualcooldown(),0);assertEquals(rank==2,cooldown.hasSecondUse());
   com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();cooldown.storeInBundle(saved);Talent.SwiftEquipCooldown loaded=new Talent.SwiftEquipCooldown();loaded.restoreFromBundle(saved);assertEquals(cooldown.visualcooldown(),loaded.visualcooldown(),0);assertEquals(cooldown.hasSecondUse(),loaded.hasSecondUse());
   String text=TalentExplanation.details(Talent.SWIFT_EQUIP,rank);assertTrue(text,text.contains("20 turns"));assertFalse(text.contains("19-unit"));for(int i=0;i<20;i++)assertEquals(text,TalentExplanation.details(Talent.SWIFT_EQUIP,rank));assertEquals(rank==2,cooldown.hasSecondUse());
   first.cursed=true;assertFalse(second.doEquip(hero));assertSame(first,hero.belongings.weapon);assertEquals(before,hero.cooldown(),0);assertEquals(rank==2,cooldown.hasSecondUse());first.cursed=false;
   assertTrue(second.doEquip(hero));float afterSecond=hero.cooldown();assertEquals(rank==2?before:before+2,afterSecond,0);assertFalse(cooldown.hasSecondUse());
   assertTrue(third.doEquip(hero));assertEquals(afterSecond+2,hero.cooldown(),0);assertSame(cooldown,hero.buff(Talent.SwiftEquipCooldown.class));cooldown.act();assertNull(hero.buff(Talent.SwiftEquipCooldown.class));
  }}finally{for(com.shatteredpixel.shatteredpixeldungeon.actors.Actor actor:new java.util.ArrayList<>(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all()))if(!original.contains(actor))com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(actor);}
 }
 @Test public void variedChargeExplanationMatchesActualDifferentTypePairsAndSameTypeCopies()throws Exception{
  hero.sprite=quietSprite();java.lang.reflect.Method used=com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.class.getDeclaredMethod("afterAbilityUsed",Hero.class);used.setAccessible(true);
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword(),copy=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HandAxe axe=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HandAxe();
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.Charger charger=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.Charger.class);
  for(int rank=1;rank<=3;rank++){Buff.detach(hero,Talent.VariedChargeTracker.class);hero.talents.get(2).put(Talent.VARIED_CHARGE,rank);charger.charges=0;charger.partialCharge=0;
   used.invoke(sword,hero);used.invoke(copy,hero);assertEquals(0,charger.charges+charger.partialCharge,0);assertNotNull(hero.buff(Talent.VariedChargeTracker.class));used.invoke(axe,hero);assertEquals(rank/6f,charger.charges+charger.partialCharge,.00001f);assertNull(hero.buff(Talent.VariedChargeTracker.class));
   used.invoke(sword,hero);assertEquals(rank/6f,charger.charges+charger.partialCharge,.00001f);assertNotNull(hero.buff(Talent.VariedChargeTracker.class));assertTrue(TalentExplanation.details(Talent.VARIED_CHARGE,rank).contains("_"+ObfuscationExplanation.number(rank/6f)+" weapon-ability charges_"));
  }
 }
 @Test public void searingLightAndAggressiveBarrierDescriptionsMatchRealAttackAndAbilityHandlers()throws Exception{
  hero.sprite=quietSprite();hero.STR=20;hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();java.util.Arrays.fill(Dungeon.level.heroFOV,false);
  for(int rank=1;rank<=2;rank++)for(int seed=0;seed<20;seed++){
   PlainStatusTarget target=new PlainStatusTarget(){@Override public int defenseSkill(com.shatteredpixel.shatteredpixeldungeon.actors.Char enemy){return 0;}};target.HP=target.HT=1000;target.pos=hero.pos+1;target.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;
   hero.talents.get(0).put(Talent.SEARING_LIGHT,0);Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.Illuminated.class);Random.pushGenerator(seed);try{assertTrue(hero.attack(target));}finally{Random.popGenerator();}int base=1000-target.HP;assertNull(target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.Illuminated.class));
   target.HP=1000;hero.talents.get(0).put(Talent.SEARING_LIGHT,rank);Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.Illuminated.class);Random.pushGenerator(seed);try{assertTrue(hero.attack(target));}finally{Random.popGenerator();}assertEquals(base+1+2*rank,1000-target.HP);assertTrue(TalentExplanation.details(Talent.SEARING_LIGHT,rank).contains("_"+(1+2*rank)+" damage_"));
  }
  hero.heroClass=HeroClass.DUELIST;java.lang.reflect.Method before=com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.class.getDeclaredMethod("beforeAbilityUsed",Hero.class,com.shatteredpixel.shatteredpixeldungeon.actors.Char.class);before.setAccessible(true);
  for(int rank=1;rank<=2;rank++)for(int health:new int[]{9,10,11}){Buff.detach(hero,Barrier.class);hero.HP=health;hero.HT=20;hero.talents.get(0).put(Talent.AGGRESSIVE_BARRIER,rank);before.invoke(hero.belongings.weapon,hero,null);assertEquals(health<=10?1+2*rank:0,hero.shielding());if(health<=10){hero.buff(Barrier.class).setShield(20);before.invoke(hero.belongings.weapon,hero,null);assertEquals(20,hero.shielding());}assertTrue(TalentExplanation.details(Talent.AGGRESSIVE_BARRIER,rank).contains("_"+(1+2*rank)+" shield points_"));}
  hero.belongings.abilityWeapon=null;
 }
 @Test public void bountyHunterDetailsMatchActualAdditiveLootBonusAndPreparationBoundaries(){
  com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring previous=hero.belongings.ring;
  try{for(boolean withWealth:new boolean[]{false,true}){
   if(withWealth){com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth wealth=new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth();wealth.level(2);hero.belongings.ring=wealth;wealth.activate(hero);}
   float ordinary=com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth.dropChanceMultiplier(hero);Preparation prep=Buff.affect(hero,Preparation.class);Buff.affect(hero,Talent.BountyHunterTracker.class,5);int[] turns={1,3,5,9};float[] bonus={.02f,.04f,.08f,.16f};
   for(int rank=1;rank<=3;rank++){hero.talents.get(2).put(Talent.BOUNTY_HUNTER,rank);String text=TalentExplanation.details(Talent.BOUNTY_HUNTER,rank);assertTrue(text,text.contains((2*rank)+"%, "+(4*rank)+"%, "+(8*rank)+"%, or "+(16*rank)+"%"));for(int tier=0;tier<4;tier++){com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();prep.storeInBundle(state);state.put("turnsInvis",turns[tier]);prep.restoreFromBundle(state);assertEquals(tier+1,prep.attackLevel());assertEquals(.25f*(ordinary+bonus[tier]*rank),new BountyLootProbe(.25f).lootChance(),.000001f);assertEquals(0,new BountyLootProbe(0).lootChance(),0);}}
   Buff.detach(hero,Talent.BountyHunterTracker.class);assertEquals(.25f*ordinary,new BountyLootProbe(.25f).lootChance(),.000001f);prep.detach();Buff.affect(hero,Talent.BountyHunterTracker.class,5);assertEquals(.25f*ordinary,new BountyLootProbe(.25f).lootChance(),.000001f);Buff.detach(hero,Talent.BountyHunterTracker.class);
  }}finally{Buff.detach(hero,Preparation.class);Buff.detach(hero,Talent.BountyHunterTracker.class);Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth.Wealth.class);hero.belongings.ring=previous;}
 }
 @Test public void emptyStaffExplainsItsMissingWandWithoutChangingStateOrActions(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff empty=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff();
  com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();empty.storeInBundle(before);String text=empty.info();empty.storeInBundle(after);
  assertTrue(text,text.contains("Imbue a wand to let this staff cast spells."));assertTrue(empty.actions(hero).contains("IMBUE"));assertFalse(empty.actions(hero).contains("ZAP"));assertNull(empty.wandClass());assertEquals(before.toString(),after.toString());
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff imbued=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile());
  assertFalse(imbued.info().contains("Imbue a wand to let this staff cast spells."));assertTrue(imbued.actions(hero).contains("ZAP"));assertSame(com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile.class,imbued.wandClass());
 }
 @Test public void plantDescriptionWithoutAHeroDoesNotPresentAStaleFloorCalculation(){
  int depth=Dungeon.depth;Dungeon.depth=20;Dungeon.hero=null;try{String earth=PlantExplanation.details(com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.class,null),poison=PlantExplanation.details(com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss.class,null);assertTrue(earth.contains("floor((scaling floor + 5) / 2)"));assertTrue(poison.contains("5 + round(2 × scaling floor / 3)"));assertTrue(earth.contains("requires an active run"));assertTrue(poison.contains("requires an active run"));assertFalse(earth.contains("currently"));assertFalse(poison.contains("currently"));}finally{Dungeon.depth=depth;Dungeon.hero=hero;}
 }
 @Test public void allWandDescriptionsPreserveInfusionResinChargeAndKnowledgeState()throws Exception{
  int inspected=0;for(Class<?> type:com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.WAND.classes)for(int base:new int[]{0,5,10})for(int resin:new int[]{0,1})for(boolean active:new boolean[]{false,true})for(boolean known:new boolean[]{false,true}){
   com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)type.getDeclaredConstructor().newInstance();wand.cursed=true;wand.curseInfusionBonus=true;wand.resinBonus=resin;wand.level(base);wand.cursed=active;wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=known;int capacity=wand.maxCharges;com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();wand.storeInBundle(before);Random.pushGenerator(311);int next=Random.Int();Random.popGenerator();Random.pushGenerator(311);try{String info=wand.info();MechanicsDetails.item(wand,info);assertEquals(next,Random.Int());}finally{Random.popGenerator();}wand.storeInBundle(after);assertEquals(type.getSimpleName()+" level="+base+" resin="+resin+" active="+active+" known="+known,before.toString(),after.toString());assertEquals(capacity,wand.maxCharges);inspected++;
  }assertEquals(312,inspected);
 }
 private void assertWandInfoPreservesInfusionCleanup(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand){
  wand.cursed=true;wand.curseInfusionBonus=true;wand.level(5);wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.cursed=false;int capacity=wand.maxCharges;com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();wand.storeInBundle(before);String visible=wand.info();MechanicsDetails.item(wand,visible);wand.storeInBundle(after);assertEquals("description must not perform the gameplay curse-cleanup query",before.toString(),after.toString());assertTrue(wand.curseInfusionBonus);assertEquals(capacity,wand.maxCharges);assertEquals("the real gameplay query still performs its normal cleanup",5,wand.level());assertFalse(wand.curseInfusionBonus);
 }
 @Test public void wardingInfoDoesNotPerformInfusionCleanup(){assertWandInfoPreservesInfusionCleanup(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding());}
 @Test public void regrowthInfoDoesNotPerformInfusionCleanup(){assertWandInfoPreservesInfusionCleanup(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth());}
 @Test public void identifiedRegrowthDescriptionWithoutAHeroWithholdsDepletionInsteadOfCrashing(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;Dungeon.hero=null;try{String description=wand.statsDesc();assertNotNull(description);assertFalse(description.contains("After another"));assertFalse(wand.info().isEmpty());}finally{Dungeon.hero=hero;}
 }
 public static class DescriptionTransmutation extends com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation {public boolean eligible(com.shatteredpixel.shatteredpixeldungeon.items.Item item){return usableOnItem(item);}}
 @Test public void transmutationDescriptionMatchesActualEligibleItemsAndWeaponPropertyTransfer(){
  DescriptionTransmutation scroll=new DescriptionTransmutation();scroll.anonymize();assertFalse(scroll.eligible(new MailArmor()));assertFalse(scroll.eligible(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart()));assertFalse(scroll.eligible(new com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew()));assertFalse(scroll.eligible(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight()));assertFalse(scroll.eligible(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows()));
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword original=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();original.level(4);original.levelKnown=original.cursedKnown=false;original.augment=com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.SPEED;original.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic());assertTrue(scroll.eligible(original));
  com.watabou.utils.Bundle generator=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.items.Generator.storeInBundle(generator);Random.pushGenerator(33018);
  try{com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon changed=(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon)com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation.changeItem(original);assertNotEquals(original.getClass(),changed.getClass());assertEquals(original.tier,changed.tier);assertEquals(4,changed.trueLevel());assertSame(original.enchantment,changed.enchantment);assertEquals(original.augment,changed.augment);assertFalse(changed.levelKnown);assertFalse(changed.cursedKnown);assertTrue(scroll.info(),scroll.info().contains("Armor, plain darts, brews, elixirs and unique artifacts cannot be changed"));assertFalse(scroll.info().contains("glyphs"));}finally{Random.popGenerator();com.shatteredpixel.shatteredpixeldungeon.items.Generator.restoreFromBundle(generator);}
 }
 @Test public void aquaBrewDescriptionMatchesActualFloodTrapRemovalAndFireClear(){
  descriptionSightArena();int center=hero.pos+3,trapCell=center+1,wall=center-1;
  com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(trapCell,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_TRAP);Dungeon.level.traps.put(trapCell,new com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap().set(trapCell));com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(wall,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL);
  com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire fire=new com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire();fire.seed(Dungeon.level,center,4);Dungeon.level.blobs.put(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire.class,fire);
  com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew brew=new com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew();assertFalse(brew.actions(hero).contains("DRINK"));assertEquals("THROW",brew.defaultAction());assertTrue(brew.info(),brew.info().contains("including you and allies"));
  Random.pushGenerator(993);try{brew.shatter(center);}finally{Random.popGenerator();}
  assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WATER,Dungeon.level.map[center]);assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WATER,Dungeon.level.map[trapCell]);assertNull(Dungeon.level.traps.get(trapCell));assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL,Dungeon.level.map[wall]);assertEquals(0,fire.cur[center]);
 }
 @Test public void amuletDescriptionMatchesItsActualActionsBeforeAndDuringAscension(){
  com.shatteredpixel.shatteredpixeldungeon.items.Amulet amulet=new com.shatteredpixel.shatteredpixeldungeon.items.Amulet();
  assertTrue(amulet.actions(hero).contains("END"));assertTrue(amulet.desc(),amulet.desc().contains("claim victory now"));
  Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge.class);
  assertTrue(amulet.actions(hero).isEmpty());assertTrue(amulet.desc(),amulet.desc().contains("cannot activate or discard"));assertFalse(amulet.desc().contains("claim victory now"));
  String before=savedDescriptionHero();amulet.info();assertEquals(before,savedDescriptionHero());
 }
 @Test public void sharpshootingDurabilityTextDoesNotClaimExtraUsesOnNeverWearingItems(){
  RingOfSharpshooting ring=new RingOfSharpshooting();ring.level(0);ring.anonymize();ring.levelKnown=ring.cursedKnown=true;hero.belongings.ring=ring;ring.activate(hero);
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart();assertEquals(0,dart.previewDurabilityPerUse(),0);assertFalse(dart.info().contains("Sharpshooting durability"));
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife knife=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife();knife.levelKnown=knife.cursedKnown=true;assertTrue(knife.previewDurabilityPerUse()>0);assertTrue(knife.info(),knife.info().contains("Sharpshooting durability"));
  knife.level(30);assertEquals(0,knife.previewDurabilityPerUse(),0);assertFalse(knife.info().contains("Sharpshooting durability"));
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart tip=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart();assertTrue(tip.previewDurabilityPerUse()>0);assertTrue(tip.info(),tip.info().contains("Sharpshooting durability"));
 }
 @Test public void sungrassDescriptionsRespectZeroHealingForBothHeroBranchesButNotOtherCreatures()throws Exception{
  descriptionSightArena();hero.sprite=quietSprite();hero.HP=20;hero.HT=100;java.util.Arrays.fill(Dungeon.level.heroFOV,false);
  com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);
  com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass plant=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass();plant.pos=hero.pos;com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed seed=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed();
  for(HeroSubClass kind:new HeroSubClass[]{HeroSubClass.NONE,HeroSubClass.WARDEN}){hero.subClass=kind;plant.activate(hero);assertTrue(seed.desc(),seed.desc().contains("prevent Sungrass"));assertTrue(plant.desc().contains("prevent Sungrass"));String before=savedDescriptionHero();seed.info();assertEquals(before,savedDescriptionHero());if(kind==HeroSubClass.NONE){com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health healing=hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health.class);assertTrue(healing.desc().contains("prevent Sungrass"));for(int i=0;i<6;i++)healing.act();healing.detach();}else{Healing healing=hero.buff(Healing.class);for(int i=0;i<6;i++)healing.act();healing.detach();}assertEquals(20,hero.HP);}
  PlainStatusTarget other=new PlainStatusTarget();other.HP=20;other.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health npc=Buff.affect(other,com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health.class);npc.boost(100);assertFalse(npc.desc().contains("prevent Sungrass"));for(int i=0;i<6;i++)npc.act();assertTrue(other.HP>20);npc.detach();Dungeon.hero=null;try{assertTrue(seed.desc(),seed.desc().contains("Gradually restores"));}finally{Dungeon.hero=hero;}
 }
 @Test public void cursedEquipmentRemovalTextMatchesActualTemporaryMagicImmunityException()throws Exception{
  hero.sprite=quietSprite();
  for(com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem item:new com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword(),new MailArmor(),new RingOfHaste()}){
   item.levelKnown=item.cursedKnown=true;item.cursed=true;
   if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon)hero.belongings.weapon=(com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon)item;
   else if(item instanceof Armor)hero.belongings.armor=(Armor)item;
   else{((Ring)item).anonymize();hero.belongings.ring=(Ring)item;}
   assertTrue(item.info(),item.info().contains("Magic immunity temporarily lets you take it off"));float time=hero.cooldown();assertFalse(item.doUnequip(hero,false,false));assertEquals(time,hero.cooldown(),0);assertTrue(item.isEquipped(hero));
   Buff.affect(hero,MagicImmune.class);assertTrue(item.doUnequip(hero,false,false));assertFalse(item.isEquipped(hero));assertTrue("Suppression permits removal without curing the curse",item.cursed);Buff.detach(hero,MagicImmune.class);
  }
 }
 @Test public void barrenLandSeedTextMatchesActualNoGrowthWithoutChangingChallengeRules(){
  descriptionSightArena();Dungeon.challenges|=com.shatteredpixel.shatteredpixeldungeon.Challenges.NO_HERBALISM;com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Seed seed=new com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Seed();assertTrue(seed.info(),seed.info().contains("Barren Land prevents this seed from growing"));assertNull(Dungeon.level.plant(seed,hero.pos));assertNull(Dungeon.level.plants.get(hero.pos));
 }
 @Test public void dartTippingSelectorRejectsUnsupportedSeedsAndKeepsEveryRealRecipe()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart();java.lang.reflect.Field field=dart.getClass().getDeclaredField("itemSelector");field.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector selector=(com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector)field.get(dart);
  assertFalse("Blandfruit has no tipped-dart result and must not be offered",selector.itemSelectable(new com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed()));
  for(java.util.Map.Entry<Class<? extends com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed>,Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.TippedDart>> recipe:com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.TippedDart.types.entrySet()){
   com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed seed=recipe.getKey().getDeclaredConstructor().newInstance();assertTrue(seed.getClass().getSimpleName(),selector.itemSelectable(seed));com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.TippedDart result=com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.TippedDart.getTipped(seed,2);assertEquals(recipe.getValue(),result.getClass());assertEquals(2,result.quantity());
  }
 }
 @Test public void dartTippingDescriptionsAllowRealMultipleUseCoatingsAndPlainDartsDoNotWearOut(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart();
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart tip=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart();
  assertEquals(0,dart.previewDurabilityPerUse(),0);assertTrue(dart.info(),dart.info().contains("do not wear out"));
  hero.talents.get(2).put(Talent.DURABLE_TIPS,2);float wear=tip.previewDurabilityPerUse();assertTrue(wear>0&&wear<100);
  String prompt=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(dart,"tip_desc",tip.name());assertTrue(prompt.contains("up to two darts"));assertTrue(prompt.contains("last longer"));assertFalse(prompt.contains("single-use"));
  for(java.util.Map<Talent,Integer> tier:hero.talents)tier.remove(Talent.DURABLE_TIPS);RingOfSharpshooting ring=new RingOfSharpshooting();ring.level(30);ring.anonymize();ring.levelKnown=ring.cursedKnown=true;hero.belongings.ring=ring;ring.activate(hero);
  assertEquals(0,tip.previewDurabilityPerUse(),0);assertTrue(tip.info(),tip.info().contains("With your current bonuses, these tips do not wear out"));
 }
 @Test public void allDartInspectorsUseTheEquippedBowAndWithholdItsUnknownLevel()throws Exception{
  java.lang.reflect.Field field=com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.class.getDeclaredField("bow");field.setAccessible(true);Object original=field.get(null);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow pending=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow(),equipped=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow();pending.level(8);equipped.level(3);equipped.levelKnown=equipped.cursedKnown=true;hero.belongings.weapon=equipped;
  RingOfSharpshooting aim=new RingOfSharpshooting();aim.level(2);aim.anonymize();aim.levelKnown=aim.cursedKnown=true;hero.belongings.ring=aim;aim.activate(hero);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow.ChargedShot.class);
  try{for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart dart:new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart()}){dart.levelKnown=dart.cursedKnown=true;equipped.levelKnown=equipped.cursedKnown=true;equipped.level(3);field.set(null,equipped);int minimum=dart.min(),maximum=dart.max();String expected=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon.class,"stats_known",dart.tier,dart.augment.damageFactor(minimum),dart.augment.damageFactor(maximum),dart.STRReq());field.set(null,pending);String visible=dart.info();assertTrue(visible,visible.contains(expected));assertSame(pending,field.get(null));String rings=com.shatteredpixel.shatteredpixeldungeon.apd.qol.RingItemEffects.describe(dart);assertTrue("ring contribution stays in the ordinary item text",visible.contains(rings.trim()));assertFalse(MechanicsDetails.item(dart,visible).isEmpty());assertSame(pending,field.get(null));
   equipped.levelKnown=equipped.cursedKnown=false;String previous=null;for(int hidden:new int[]{0,7}){equipped.level(hidden);String before=savedDescriptionHero();String text=dart.info()+MechanicsDetails.item(dart,dart.info());if(previous!=null)assertEquals("unknown crossbow upgrade must not leak through dart damage or tip wear",previous,text);previous=text;assertEquals(before,savedDescriptionHero());assertSame(pending,field.get(null));}
  }}finally{field.set(null,original);}
 }
 @Test public void dartInspectionContextIsThreadIsolatedAndClearsAfterFormatterFailure()throws Exception{
  descriptionSightArena();java.lang.reflect.Field field=com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.class.getDeclaredField("bow");field.setAccessible(true);Object original=field.get(null);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow pending=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow(),equipped=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow();pending.level(8);equipped.level(3);equipped.levelKnown=equipped.cursedKnown=true;hero.belongings.weapon=equipped;field.set(null,pending);com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart();
  try{try{com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->{assertEquals(equipped.dartMin(),dart.min(0));java.util.concurrent.atomic.AtomicInteger other=new java.util.concurrent.atomic.AtomicInteger(-1);Thread actor=new Thread(()->other.set(dart.min(0)));actor.start();try{actor.join(10000);}catch(InterruptedException error){throw new RuntimeException(error);}assertFalse(actor.isAlive());assertEquals(pending.dartMin(),other.get());throw new IllegalStateException("formatter fixture");});fail("formatter exception expected");}catch(IllegalStateException expected){assertEquals("formatter fixture",expected.getMessage());}assertSame(pending,field.get(null));assertEquals(pending.dartMin(),dart.min(0));hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();assertEquals(1,(int)com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->dart.min(0)));assertSame(pending,field.get(null));hero.belongings.weapon=equipped;dart.throwPos(hero,hero.pos+2);assertSame("real throw preparation still refreshes the actual crossbow",equipped,field.get(null));}finally{field.set(null,original);}
 }
 @Test public void inspectingADartMustPreserveThePendingShotsCrossbow()throws Exception{
  java.lang.reflect.Field field=com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.class.getDeclaredField("bow");field.setAccessible(true);Object original=field.get(null);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow pending=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow();pending.level(5);hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();field.set(null,pending);
  try{com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart();dart.info();assertSame("inspecting inventory cannot replace the crossbow retained by a pending shot",pending,field.get(null));}finally{field.set(null,original);}
 }
 @Test public void brokenSealCapacityDescriptionMatchesRealTierAndAlternateTalentRules(){
  com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal seal=new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal();armor.levelKnown=false;
  for(int tier=1;tier<=5;tier++)for(int rank=0;rank<=2;rank++){armor.tier=tier;hero.talents.get(0).put(Talent.IRON_WILL,rank);String prior=null;for(int level:new int[]{-2,0,7}){armor.level(level);int actual=seal.maxShield(tier,level);String text=UtilityItemExplanation.details(seal,hero);assertTrue(text.contains("capacity would be "+actual+" shield points"));if(prior!=null)assertEquals("unknown upgrade levels do not change a tier-only capacity",prior,text);prior=text;}}
  hero.heroClass=HeroClass.ROGUE;hero.talents.get(0).put(Talent.IRON_WILL,2);com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield alternate=new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield();alternate.target=hero;assertEquals(2,alternate.maxShield());assertTrue(UtilityItemExplanation.details(seal,hero).contains("alternate Iron Will capacity is 2 shield points"));assertNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield.class));
 }
 @Test public void brokenPotDescriptionDoesNotRetargetItsBeeButRealPickupAndDropDo(){
  descriptionSightArena();com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bee bee=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bee();bee.pos=hero.pos+1;bee.spawn(4);bee.setPotInfo(hero.pos,null);com.shatteredpixel.shatteredpixeldungeon.items.Honeypot.ShatteredPot pot=new com.shatteredpixel.shatteredpixeldungeon.items.Honeypot.ShatteredPot();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(hero);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(bee);
  try{com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();bee.storeInBundle(before);String text=UtilityItemExplanation.details(pot,hero);assertTrue(text.contains("does not guarantee friendship"));bee.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(-1,bee.potHolderID());pot.pickupPot(hero);assertEquals(hero.id(),bee.potHolderID());assertEquals(-1,bee.potPos());pot.dropPot(hero,hero.pos+2);assertEquals(-1,bee.potHolderID());assertEquals(hero.pos+2,bee.potPos());pot.destroyPot(hero.pos+2);assertEquals(-1,bee.potHolderID());assertEquals(-1,bee.potPos());}finally{com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(bee);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(hero);}
 }
 @Test public void seedAndPlantExplanationsUseTheSameSpeciesWithoutTriggeringOrRolling()throws Exception{
  for(HeroSubClass subclass:new HeroSubClass[]{HeroSubClass.NONE,HeroSubClass.WARDEN}){hero.subClass=subclass;String before=savedDescriptionHero();Random.pushGenerator(288);int next=Random.Int();Random.popGenerator();Random.pushGenerator(288);try{for(String name:new String[]{"Sungrass","Earthroot","Blindweed","Firebloom","Icecap","Swiftthistle","Starflower","Fadeleaf","Mageroyal","Sorrowmoss","Stormvine","Rotberry","BlandfruitBush"}){Class<?> type=Class.forName("com.shatteredpixel.shatteredpixeldungeon.plants."+name);com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed seed=(com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed)Class.forName(type.getName()+"$Seed").getDeclaredConstructor().newInstance();assertSame(type,seed.plantType());String detail=PlantExplanation.details(seed.plantType(),hero);assertFalse(name,detail.isEmpty());assertTrue(MechanicsDetails.item(seed,seed.info()).contains(detail));assertEquals(1,seed.quantity());}assertEquals(next,Random.Int());assertEquals(before,savedDescriptionHero());}finally{Random.popGenerator();}}
 }
 @Test public void plantDescriptionsMatchActualSungrassEarthrootAndPoisonBranches(){
  descriptionSightArena();Dungeon.level.heroFOV=new boolean[Dungeon.level.length()];hero.HT=100;hero.lvl=6;
  com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass sun=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass();sun.pos=hero.pos;sun.activate(hero);sun.activate(hero);assertEquals(200,hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health.class).remainingPool());assertTrue(PlantExplanation.details(sun.getClass(),hero).contains("Adds the recipient's maximum HP"));hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health.class).detach();
  com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot earth=new com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot();earth.pos=hero.pos;earth.activate(hero);assertEquals(100,hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Armor.class).remainingReserve());hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Armor.class).detach();
  com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss sorrow=new com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss();sorrow.pos=hero.pos;sorrow.activate(hero);int duration=5+Math.round(2*Dungeon.scalingDepth()/3f);assertEquals(duration,hero.buff(Poison.class).apdRemaining(),0);assertTrue(PlantExplanation.details(sorrow.getClass(),hero).contains("currently "+duration+"."));hero.buff(Poison.class).detach();
  hero.subClass=HeroSubClass.WARDEN;sun.activate(hero);assertEquals(100,hero.buff(Healing.class).remainingHealingPool());assertEquals(1,hero.buff(Healing.class).flatHealing());earth.activate(hero);assertEquals(11,hero.buff(Barkskin.class).level());assertEquals(5,hero.buff(Barkskin.class).decayInterval());assertTrue(PlantExplanation.details(earth.getClass(),hero).contains("currently 11 damage-blocking points"));
 }
 @Test public void blindweedAndStormvineDescriptionsMatchBothActualSubclassEffects()throws Exception{
  hero=new DescriptionMonkHero();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);Dungeon.hero=hero;descriptionSightArena();hero.sprite=quietSprite();java.util.Arrays.fill(Dungeon.level.heroFOV,false);
  for(HeroSubClass subclass:new HeroSubClass[]{HeroSubClass.NONE,HeroSubClass.WARDEN}){
   hero.subClass=subclass;com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed blind=new com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed();blind.pos=hero.pos;java.util.Arrays.fill(Dungeon.level.heroFOV,false);blind.activate(hero);
   if(subclass==HeroSubClass.WARDEN){assertNull(hero.buff(Blindness.class));assertNull(hero.buff(Cripple.class));assertEquals(Invisibility.DURATION/2,hero.buff(Invisibility.class).cooldown(),.00001f);assertTrue(blind.desc().contains("invisible instead"));Buff.detach(hero,Invisibility.class);}else{assertEquals(Blindness.DURATION,hero.buff(Blindness.class).cooldown(),.00001f);assertEquals(Cripple.DURATION,hero.buff(Cripple.class).cooldown(),.00001f);Buff.detach(hero,Blindness.class);Buff.detach(hero,Cripple.class);}
   com.shatteredpixel.shatteredpixeldungeon.plants.Stormvine storm=new com.shatteredpixel.shatteredpixeldungeon.plants.Stormvine();storm.pos=hero.pos;java.util.Arrays.fill(Dungeon.level.heroFOV,false);storm.activate(hero);
   if(subclass==HeroSubClass.WARDEN){assertNull(hero.buff(Vertigo.class));assertTrue(hero.flying);assertEquals(Levitation.DURATION/2,hero.buff(Levitation.class).cooldown(),.00001f);assertTrue(storm.desc().contains("levitates instead"));com.watabou.noosa.Game previousGame=com.watabou.noosa.Game.instance;java.lang.reflect.Field f=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");f.setAccessible(true);Object unsafe=f.get(null);com.watabou.noosa.Game.instance=(com.watabou.noosa.Game)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.watabou.noosa.Game.class);try{Buff.detach(hero,Levitation.class);}finally{com.watabou.noosa.Game.instance=previousGame;}}else{assertEquals(Vertigo.DURATION,hero.buff(Vertigo.class).cooldown(),.00001f);Buff.detach(hero,Vertigo.class);}
  }
 }
 @Test public void mageroyalCuresWithoutHealingAndAddsCloudProtectionOnlyForWarden()throws Exception{
  hero.sprite=quietSprite();hero.HP=5;hero.HT=20;com.shatteredpixel.shatteredpixeldungeon.plants.Mageroyal plant=new com.shatteredpixel.shatteredpixeldungeon.plants.Mageroyal();plant.pos=hero.pos;
  for(HeroSubClass subclass:new HeroSubClass[]{HeroSubClass.NONE,HeroSubClass.WARDEN}){
   hero.subClass=subclass;Buff.affect(hero,Weakness.class,10);Buff.affect(hero,Cripple.class,10);plant.activate(hero);assertNull(hero.buff(Weakness.class));assertNull(hero.buff(Cripple.class));assertEquals(5,hero.HP);assertTrue(plant.desc().contains("Does not restore health"));
   if(subclass==HeroSubClass.WARDEN){assertEquals(BlobImmunity.DURATION/2,hero.buff(BlobImmunity.class).cooldown(),.00001f);Buff.detach(hero,BlobImmunity.class);}else assertNull(hero.buff(BlobImmunity.class));
  }
 }
 @Test public void blandfruitBushSeedWarnsThatRawFruitCannotBeEatenAndActualAttemptIsFree(){
  com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed seed=new com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed();assertTrue(seed.info(),seed.info().contains("cannot be eaten raw"));
  com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit fruit=new com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit();assertTrue(fruit.collect());Hunger hunger=Buff.affect(hero,Hunger.class);com.watabou.utils.Bundle food=new com.watabou.utils.Bundle();hunger.storeInBundle(food);food.put("level",300f);hunger.restoreFromBundle(food);float time=hero.cooldown();fruit.execute(hero,com.shatteredpixel.shatteredpixeldungeon.items.food.Food.AC_EAT);assertEquals(300,hunger.hungerPoints(),0);assertEquals(time,hero.cooldown(),0);assertTrue(hero.belongings.contains(fruit));assertEquals(1,fruit.quantity());assertNull(fruit.defaultAction());
 }
 @Test public void timeBubbleDetailsPreserveFractionalActionBudgetAcrossSaveLoad(){
  com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle.TimeBubble bubble=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle.TimeBubble.class);bubble.reset();assertEquals(7,bubble.remainingActionTime(),0);float time=hero.cooldown();hero.spend(1.25f);assertEquals(time,hero.cooldown(),0);assertEquals(5.75f,bubble.remainingActionTime(),0);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();bubble.storeInBundle(before);assertTrue(BuffExplanation.details(bubble).contains("5.75 turns"));bubble.storeInBundle(after);assertEquals(before.toString(),after.toString());com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle.TimeBubble loaded=new com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle.TimeBubble();loaded.restoreFromBundle(before);assertEquals(5.75f,loaded.remainingActionTime(),0);
 }
 @Test public void rawMeatPoisonExplanationLabelsTheActualDurationWithoutApplyingIt(){
  int seed=-1;for(int candidate=0;candidate<100;candidate++){Random.pushGenerator(candidate);int outcome=Random.Int(5);Random.popGenerator();if(outcome==2){seed=candidate;break;}}assertTrue(seed>=0);
  for(int health:new int[]{20,51,100}){hero.HT=health;String text=FoodExplanation.specialEffect(new com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat(),hero);assertTrue(text.contains("being poisoned for "+(health/5)+" turns"));assertFalse(text.contains("Poison with strength"));assertNull(hero.buff(Poison.class));Random.pushGenerator(seed);try{com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat.effect(hero);}finally{Random.popGenerator();}Poison poison=hero.buff(Poison.class);assertNotNull(poison);assertEquals(health/5,poison.apdRemaining(),0);poison.detach();}
 }
 @Test public void poisonDartDurationExplanationMatchesItsActualOnHitHandler()throws Exception{
  int oldDepth=Dungeon.depth;java.lang.reflect.Field bow=com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.class.getDeclaredField("bow");bow.setAccessible(true);Object oldBow=bow.get(null);bow.set(null,null);
  try{for(int depth:new int[]{2,8,17,26}){Dungeon.depth=depth;com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart();com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat rat=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();rat.pos=hero.pos+1;int duration=3+Dungeon.scalingDepth()/2;String text=MissileExplanation.details(dart,hero);assertTrue(text.contains("currently "+duration+" units"));assertNull(rat.buff(Poison.class));dart.proc(hero,rat,0);assertEquals(duration,rat.buff(Poison.class).apdRemaining(),0);rat.buff(Poison.class).detach();}}finally{Dungeon.depth=oldDepth;bow.set(null,oldBow);}
 }
 @Test public void lunarPastyPreviewUsesItsActualReducedMainMealWithoutMutatingTheFood()throws Exception{
  java.lang.reflect.Field cached=com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.class.getDeclaredField("cached");cached.setAccessible(true);Object original=cached.get(null);
  try{cached.set(null,com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.LUNAR_NEW_YEAR);com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty meal=new com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty();assertEquals(Hunger.STARVING,meal.energy,0);assertEquals(Hunger.HUNGRY,FoodExplanation.nutritionBeforeHorn(meal),0);assertTrue(FoodExplanation.summary(meal).contains("Refills _300 hunger_"));assertEquals(Hunger.STARVING,meal.energy,0);Dungeon.challenges=Challenges.NO_FOOD;assertEquals(Hunger.HUNGRY/3,FoodExplanation.nutritionBeforeHorn(meal),0);}finally{cached.set(null,original);}
 }
 @Test public void specialFoodInspectionKeepsBerryProgressAndRandomStateWhileScalingLiveOffers(){
  com.shatteredpixel.shatteredpixeldungeon.items.food.Food[] foods={new com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio(),new com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat(),new com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat(),new com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie(),new com.shatteredpixel.shatteredpixeldungeon.items.food.Berry(),new com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation(),new com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty()};
  Buff.count(hero,com.shatteredpixel.shatteredpixeldungeon.items.food.Berry.SeedCounter.class,1);String before=savedDescriptionHero();
  Random.pushGenerator(281);int next=Random.Int();Random.popGenerator();Random.pushGenerator(281);try{for(com.shatteredpixel.shatteredpixeldungeon.items.food.Food food:foods)assertFalse(food.getClass().getSimpleName(),FoodExplanation.specialEffect(food,hero).isEmpty());assertEquals(next,Random.Int());assertEquals(before,savedDescriptionHero());}finally{Random.popGenerator();}
  for(int health:new int[]{20,51,100}){hero.HT=health;String text=FoodExplanation.specialEffect(foods[0],hero);assertTrue(text.contains("healing for "+(health/4)+" HP"));assertTrue(text.contains("20%"));assertNull(hero.buff(Healing.class));assertNull(hero.buff(Invisibility.class));}
 }
 @Test public void rotDartPreviewRetainsItsFiveUseOverrideAtEveryLevel(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart();
  for(int level:new int[]{-2,0,1,3,8}){assertEquals(20f,dart.durabilityPerUse(level),0);assertEquals(dart.durabilityPerUse(level),dart.previewDurabilityPerUse(level),0);}
 }
 @Test public void infusionAndAugmentationExplanationsMatchActualLevelAndTimingRules(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();
  for(int base:new int[]{0,1,5,6,11,12}){sword.curseInfusionBonus=false;sword.level(base);sword.cursed=true;sword.curseInfusionBonus=true;armor.curseInfusionBonus=false;armor.level(base);armor.cursed=true;armor.curseInfusionBonus=true;wand.curseInfusionBonus=false;wand.level(base);wand.cursed=true;wand.curseInfusionBonus=true;assertEquals(base+1+base/6,sword.level());assertEquals(base+1+base/6,armor.level());assertEquals(base+1+base/6,wand.level());}
  assertEquals(.7f,com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.SPEED.damageFactor(1f),0);assertEquals(2/3f,com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.SPEED.delayFactor(1),0);assertEquals(1.5f,com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.DAMAGE.damageFactor(1f),0);assertEquals(5/3f,com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.DAMAGE.delayFactor(1),0);
  for(int level:new int[]{0,3,8}){assertEquals(2*(2+level),Armor.Augment.EVASION.evasionFactor(level));assertEquals(-(2+level),Armor.Augment.EVASION.defenseFactor(level));assertEquals(-2*(2+level),Armor.Augment.DEFENSE.evasionFactor(level));assertEquals(2+level,Armor.Augment.DEFENSE.defenseFactor(level));}
  assertTrue(CraftingEffectExplanation.details(new com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion()).contains("integer division of base level by 6"));
 }
 @Test public void craftingExplanationDoesNotSpendAnIntuitionUseOrReclaimedTrap(){
  com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition.IntuitionUseTracker intuition=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition.IntuitionUseTracker.class);com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap.ReclaimedTrap trap=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap.ReclaimedTrap.class);com.watabou.utils.Bundle captured=new com.watabou.utils.Bundle();captured.put("trap",com.shatteredpixel.shatteredpixeldungeon.levels.traps.GeyserTrap.class);trap.restoreFromBundle(captured);String before=savedDescriptionHero();Random.pushGenerator(274);int next=Random.Int();Random.popGenerator();Random.pushGenerator(274);try{CraftingEffectExplanation.details(new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition());CraftingEffectExplanation.details(new com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap());assertEquals(next,Random.Int());assertEquals(before,savedDescriptionHero());assertSame(intuition,hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition.IntuitionUseTracker.class));com.watabou.utils.Bundle retained=new com.watabou.utils.Bundle();trap.storeInBundle(retained);assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.traps.GeyserTrap.class,retained.getClass("trap"));}finally{Random.popGenerator();}
 }
 @Test public void summonElementalDescriptionsFollowItsSavedVariantWithoutRollingOrSummoning(){
  Class[] variants={com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.AllyNewBornElemental.class,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.FireElemental.class,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.FrostElemental.class,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.ShockElemental.class,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.ChaosElemental.class};
  String[] effects={"no ranged attack","50% chance","one-in-three","including you and your allies","random cursed wand effects"};
  for(int i=0;i<variants.length;i++){
   com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental spell=new com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental();com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();spell.storeInBundle(state);state.put("summon_class",variants[i]);spell.restoreFromBundle(state);
   Random.pushGenerator(81129);int next=Random.Int();Random.popGenerator();Random.pushGenerator(81129);
   try{String text=spell.info();assertTrue(text,text.contains(effects[i]));assertTrue(text.contains("Recalling still takes a turn"));com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();spell.storeInBundle(saved);com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental copy=new com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental();copy.restoreFromBundle(saved);assertEquals(text,copy.info());assertEquals(next,Random.Int());}finally{Random.popGenerator();}
  }
 }
 @Test public void detectMagicRevealsAnExistingCurseWithoutIdentifyingTheUpgrade()throws Exception{
  hero.sprite=new com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite(){@Override public void operate(int cell){}};com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.level(3);sword.levelKnown=sword.cursedKnown=false;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Displacing());
  com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDetectMagic stone=new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDetectMagic();stone.anonymize();
  java.lang.reflect.Field user=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser");user.setAccessible(true);Object previous=user.get(null);user.set(null,hero);
  java.lang.reflect.Method apply=stone.getClass().getDeclaredMethod("onItemSelected",com.shatteredpixel.shatteredpixeldungeon.items.Item.class);apply.setAccessible(true);
  try{assertFalse(sword.name().toLowerCase(java.util.Locale.ROOT).contains("displacing"));apply.invoke(stone,sword);assertTrue(sword.cursedKnown);assertFalse(sword.levelKnown);assertEquals(3,sword.trueLevel());assertTrue(sword.name().toLowerCase(java.util.Locale.ROOT).contains("displacing"));assertTrue(stone.desc().contains("Does not identify its upgrade level"));}finally{user.set(null,previous);}
 }
 @Test public void noisemakerRequiresOccupancyOfItsOwnTileRatherThanAnAdjacentCreature()throws Exception{
  class ObservedNoisemaker extends com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker {int explodedAt=-1;@Override public void explode(int cell){explodedAt=cell;}}
  Dungeon.level.heaps=new com.watabou.utils.SparseArray<>();ObservedNoisemaker bomb=new ObservedNoisemaker();com.shatteredpixel.shatteredpixeldungeon.items.Heap heap=new com.shatteredpixel.shatteredpixeldungeon.items.Heap();heap.pos=hero.pos+1;heap.items.add(bomb);Dungeon.level.heaps.put(heap.pos,heap);
  com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker.NoisemakerFuse fuse=new com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker.NoisemakerFuse();bomb.fuse=fuse.ignite(bomb);
  java.lang.reflect.Method act=fuse.getClass().getDeclaredMethod("act");act.setAccessible(true);java.lang.reflect.Field left=fuse.getClass().getDeclaredField("left");left.setAccessible(true);left.setInt(fuse,5);
  com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(hero);
  try{act.invoke(fuse);assertEquals(-1,bomb.explodedAt);act.invoke(fuse);assertEquals("adjacent hero must not trigger the armed bomb",-1,bomb.explodedAt);assertFalse(bomb.doPickUp(hero,heap.pos));hero.pos=heap.pos;act.invoke(fuse);assertEquals(heap.pos,bomb.explodedAt);assertTrue(bomb.desc().contains("steps onto its tile"));assertFalse(bomb.desc().contains("Picking it up before"));}finally{com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(hero);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(fuse);}
 }
 @Test public void swiftthistleWardenDescriptionMatchesActualMovementSpeed()throws Exception{
  Dungeon.level.mobs=new java.util.HashSet<>();hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.WARDEN;hero.STR=50;hero.sprite=quietSprite();float normal=hero.speed();new com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle().activate(hero);assertNotNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste.class));assertEquals(normal*3,hero.speed(),.0001f);assertTrue(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle.class,"warden_desc").contains("tripled movement speed"));
 }
 @Test public void regrowthBombWarnsAboutTheCurrentHealingRestrictions(){
  com.shatteredpixel.shatteredpixeldungeon.items.bombs.RegrowthBomb bomb=new com.shatteredpixel.shatteredpixeldungeon.items.bombs.RegrowthBomb();assertFalse(bomb.desc().contains("Pharmacophobia"));Dungeon.challenges=com.shatteredpixel.shatteredpixeldungeon.Challenges.NO_HEALING;assertTrue(bomb.desc().contains("poisons you instead of healing"));Dungeon.challenges=0;
  com.watabou.utils.Bundle state=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);state.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(state);assertTrue(bomb.desc().contains("disable healing"));Dungeon.hero=null;assertFalse(bomb.desc().contains("current rules"));
 }
 @Test public void armedBombDescriptionsDoNotAdvanceOrRemoveTheirFuse()throws Exception{
  for(String type:new String[]{"Bomb","ArcaneBomb","HolyBomb","RegrowthBomb","FlashBangBomb","Firebomb","FrostBomb","ShrapnelBomb","Noisemaker","SmokeBomb","WoollyBomb"}){
   com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb bomb=(com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.bombs."+type).getDeclaredConstructor().newInstance();bomb.fuse=new com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb.Fuse().ignite(bomb);com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb.Fuse fuse=bomb.fuse;com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();bomb.storeInBundle(before);Random.pushGenerator(313);int next=Random.Int();Random.popGenerator();Random.pushGenerator(313);try{String text=BombExplanation.details(bomb);assertTrue(type,text.contains("range input is "+bomb.previewExplosionRange()+" tiles"));assertEquals(next,Random.Int());}finally{Random.popGenerator();}bomb.storeInBundle(after);assertEquals(type,before.toString(),after.toString());assertSame(type,fuse,bomb.fuse);
  }
 }
 @Test public void utilityDescriptionsUseLiveContainerCapacityWithoutChangingInventory(){
  com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster holster=new com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster();int capacity=holster.capacity();String before=savedDescriptionHero();String text=UtilityItemExplanation.details(holster,hero);assertTrue(text.contains("capacity: "+capacity+" item slots"));assertTrue(text.contains("0.85"));assertTrue(text.contains("1.2"));assertEquals(before,savedDescriptionHero());assertEquals(capacity,holster.capacity());
  com.shatteredpixel.shatteredpixeldungeon.items.Ankh ankh=new com.shatteredpixel.shatteredpixeldungeon.items.Ankh();hero.HP=0;float time=hero.cooldown();int items=hero.belongings.backpack.items.size();UtilityItemExplanation.details(ankh,hero);assertEquals(0,hero.HP);assertFalse(ankh.isBlessed());assertEquals(time,hero.cooldown(),0);assertEquals(items,hero.belongings.backpack.items.size());
 }
 @Test public void scrollDamageDetailsMatchGuardedExecutionFormulasWithoutReadingHiddenTargetHp(){
  descriptionSightArena();hero.HT=100;hero.HP=50;
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat visible=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat(),ally=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat(),hidden=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();visible.pos=hero.pos+1;ally.pos=hero.pos+2;ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;hidden.pos=hero.pos+3;Dungeon.level.mobs.add(visible);Dungeon.level.mobs.add(ally);Dungeon.level.mobs.add(hidden);Dungeon.level.heroFOV[visible.pos]=true;Dungeon.level.heroFOV[ally.pos]=true;Dungeon.level.heroFOV[hidden.pos]=false;
  com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast blast=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast();blast.anonymize();String text=ScrollExplanation.details(blast,hero);assertTrue(text.contains("2 creatures you can see"));assertTrue(text.contains("at most 41 base damage"));visible.HP=1;ally.HP=10000;hidden.HP=0;assertEquals(text,ScrollExplanation.details(blast,hero));
  assertEquals(50,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast.baseSelfDamage(100,0));assertEquals(45,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast.baseSelfDamage(100,1));assertEquals(41,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast.baseSelfDamage(100,2));
  assertEquals(0,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution.powerForHealth(100,100),0);assertEquals(2.225f,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution.powerForHealth(50,100),.000001f);assertEquals(4,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution.powerForHealth(0,100),0);assertEquals(10,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution.baseDamageForHealth(100,100,0));assertEquals(100,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution.baseDamageForHealth(100,100,4));
 }
 @Test public void psionicPreviewDoesNotRevealInvisibleCreaturesOrDisguisedMimics(){
  descriptionSightArena();hero.HT=100;com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast scroll=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast();scroll.anonymize();String empty=ScrollExplanation.details(scroll,hero);
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat invisible=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();invisible.pos=hero.pos+1;invisible.invisible=1;Dungeon.level.heroFOV[invisible.pos]=true;Dungeon.level.mobs.add(invisible);assertEquals("unknown invisible creature must not alter the preview",empty,ScrollExplanation.details(scroll,hero));
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic mimic=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic();mimic.pos=hero.pos+2;Dungeon.level.heroFOV[mimic.pos]=true;Dungeon.level.mobs.add(mimic);assertEquals("a disguised chest must not alter the preview",empty,ScrollExplanation.details(scroll,hero));
  invisible.invisible=0;String one=ScrollExplanation.details(scroll,hero);assertFalse(empty.equals(one));assertTrue(one,one.contains("45"));mimic.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;String two=ScrollExplanation.details(scroll,hero);assertFalse(one.equals(two));assertTrue(two,two.contains("41"));
 }
 public static class DescriptionBlastHero extends DescriptionMonkHero {int selfDamage;@Override public void damage(int amount,Object source){selfDamage+=amount;}}
 public static class DescriptionBlastMob extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob {int received;@Override public void damage(int amount,Object source){received+=amount;}}
 @Test public void actualBlastScrollsKeepHiddenCreatureTargetsAndOriginalDamageFormulas()throws Exception{
  hero=new DescriptionBlastHero();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);Dungeon.hero=hero;hero.HP=500;hero.HT=1000;hero.sprite=quietSprite();descriptionSightArena();java.util.Arrays.fill(Dungeon.level.heroFOV,false);
  java.lang.reflect.Field user=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser");user.setAccessible(true);Object oldUser=user.get(null);user.set(null,hero);
  try{
   java.util.List<DescriptionBlastMob> targets=new java.util.ArrayList<>();for(int i=0;i<3;i++){DescriptionBlastMob mob=new DescriptionBlastMob();mob.HP=70;mob.HT=120;mob.pos=hero.pos+i+1;mob.sprite=quietSprite();mob.alignment=i==1?com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY:com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;mob.invisible=i==2?1:0;Dungeon.level.mobs.add(mob);Dungeon.level.heroFOV[mob.pos]=true;targets.add(mob);}
   com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution retribution=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution();retribution.anonymize();assertTrue(retribution.collect());retribution.doRead();int expected=com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution.baseDamageForHealth(70,120,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution.powerForHealth(500,1000));for(DescriptionBlastMob mob:targets){assertEquals(expected,mob.received);mob.received=0;assertNotNull(mob.buff(Blindness.class));}
   Buff.detach(hero,Blindness.class);Buff.detach(hero,Weakness.class);for(DescriptionBlastMob mob:targets)Dungeon.level.heroFOV[mob.pos]=true;
   com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast blast=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast();blast.anonymize();assertTrue(blast.collect());int known=ScrollExplanation.visibleCreatureCount();assertEquals(2,known);blast.doRead();for(DescriptionBlastMob mob:targets)assertEquals(95,mob.received);int actual=com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast.baseSelfDamage(1000,3);assertEquals(actual,((DescriptionBlastHero)hero).selfDamage);assertTrue(actual<=com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast.baseSelfDamage(1000,known));
  }finally{user.set(null,oldUser);}
 }
 @Test public void scrollExplanationsPreserveFloorKnowledgeAndSpellResources()throws Exception{
  String[] regular={"Identify","Upgrade","Recharging","Lullaby","Terror","Rage","MirrorImage","MagicMapping","Transmutation","Teleportation","Retribution","RemoveCurse","Gathering"};String[] exotic={"MysticalEnergy","AntiMagic","Foresight","Dread","PrismaticImage","Divination","Enchantment","Metamorphosis","Passage","Forwarding","SirensSong","Challenge","PsionicBlast"};
  for(String[] names:new String[][]{regular,exotic})for(String name:names){String path="com.shatteredpixel.shatteredpixeldungeon.items.scrolls."+(names==exotic?"exotic.":"")+"ScrollOf"+name;com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll scroll=(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll)Class.forName(path).getDeclaredConstructor().newInstance();scroll.anonymize();String before=savedDescriptionHero();boolean[] map=Dungeon.level.mapped.clone();int[] terrain=Dungeon.level.map.clone();Random.pushGenerator(142);int next=Random.Int();Random.popGenerator();Random.pushGenerator(142);try{assertTrue(name,ScrollExplanation.details(scroll,hero).contains(name.equals("Retribution")?"_Damage_":name.equals("PsionicBlast")?"_Blast damage_":"Known scroll effect"));assertEquals(next,Random.Int());}finally{Random.popGenerator();}assertEquals(name,before,savedDescriptionHero());assertArrayEquals(map,Dungeon.level.mapped);assertArrayEquals(terrain,Dungeon.level.map);}
 }
 @Test public void mightElixirDetailsTrackActualLevelStepDecayAndSaveState(){
  hero.lvl=13;com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost boost=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost.class);boost.reset();int full=boost.boost();
  for(int remaining=5;remaining>=1;remaining--){assertEquals(remaining,boost.remainingLevelSteps());assertEquals(Math.round(remaining*full/5f),boost.boost());assertTrue(boost.desc(),boost.desc().contains("_"+boost.boost()+" HP_"));assertTrue(boost.desc().contains("Level-ups remaining: _"+remaining+"_"));String text=BuffExplanation.details(boost);assertTrue(text.contains("Level-ups remaining: _"+remaining+" of 5_"));com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();boost.storeInBundle(saved);com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost restored=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost();restored.target=hero;restored.restoreFromBundle(saved);assertEquals(text,BuffExplanation.details(restored));boost.onLevelUp();}
  assertNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost.class));
 }
 @Test public void aquaticElixirDetailsMatchActualPoolSpendPauseAndSave()throws Exception{
  hero.sprite=quietSprite();hero.HP=10;hero.HT=100;com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation elixir=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation();elixir.apply(hero);
  com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing healing=hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing.class);assertEquals(150,healing.remainingHealingPool());Dungeon.level.water[hero.pos]=true;healing.act();assertEquals(12,hero.HP);assertEquals(148,healing.remainingHealingPool());Dungeon.level.water[hero.pos]=false;healing.act();assertEquals(12,hero.HP);assertEquals(148,healing.remainingHealingPool());Dungeon.level.water[hero.pos]=true;hero.flying=true;healing.act();assertEquals(148,healing.remainingHealingPool());healing.set(100);assertEquals(148,healing.remainingHealingPool());
  String text=BuffExplanation.details(healing);assertTrue(text.contains("reserve: 148 HP"));assertTrue(healing.desc().contains("_148 HP_"));assertTrue(healing.desc().contains("flying"));com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();healing.storeInBundle(saved);com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing restored=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing();restored.restoreFromBundle(saved);assertEquals(text,BuffExplanation.details(restored));
 }
 @Test public void mightElixirPreviewMatchesActualFreshBoostWhenOtherEffectsChangeMaximumHp(){
  hero.lvl=15;hero.HT=300;com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost boost=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost();boost.target=hero;boost.reset();int actual=boost.boost();
  String text=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight().desc();assertTrue(text,text.contains("_"+actual+" maximum HP_"));
 }
 @Test public void knownPotionDescriptionsMatchActualHealingPoolAndBarrierOffers()throws Exception{
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing healing=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing();healing.anonymize();com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding shielding=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding();shielding.anonymize();
  for(int max:new int[]{20,47,100,151}){hero.HT=max;hero.HP=1;Healing previous=hero.buff(Healing.class);if(previous!=null)previous.detach();com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing.heal(hero);assertEquals((int)(.8f*max+14),hero.buff(Healing.class).remainingHealingPool());assertTrue(PotionExplanation.details(healing,hero).contains((int)(.8f*max+14)+" base HP"));
   Barrier shield=Buff.affect(hero,Barrier.class);shield.setShield(1000);shielding.apply(hero);assertEquals(1000,shield.shielding());shield.detach();shielding.apply(hero);assertEquals((int)(.6f*max+10),hero.buff(Barrier.class).shielding());assertTrue(PotionExplanation.details(shielding,hero).contains((int)(.6f*max+10)+" shield points"));assertTrue(shielding.desc(),shielding.desc().contains("_"+hero.buff(Barrier.class).shielding()+" shielding_"));hero.buff(Barrier.class).detach();
  }
 }
 @Test public void knownHasteAndInvisibilityPotionDetailsMatchActualProlonging()throws Exception{
  hero.sprite=quietSprite();
  com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste haste=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste();haste.anonymize();haste.apply(hero);assertEquals(Haste.DURATION,hero.buff(Haste.class).cooldown(),0);Buff.prolong(hero,Haste.class,Haste.DURATION+10);haste.apply(hero);assertEquals(Haste.DURATION+10,hero.buff(Haste.class).cooldown(),0);assertTrue(PotionExplanation.details(haste,hero).contains(ObfuscationExplanation.number(Haste.DURATION)+" game-time units"));
  com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility invisible=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility();invisible.anonymize();invisible.apply(hero);assertEquals(Invisibility.DURATION,hero.buff(Invisibility.class).cooldown(),0);assertTrue(PotionExplanation.details(invisible,hero).contains(ObfuscationExplanation.number(Invisibility.DURATION)+" game-time units"));
 }
 @Test public void potionDetailsDoNotRevealUnknownTypesOrSpendHealingResources(){
  com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing unknown=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing(){@Override public boolean isKnown(){return false;}};assertEquals("",PotionExplanation.details(unknown,hero));
  com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing known=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing();known.anonymize();hero.apdCustomHealingFraction=.7f;String before=savedDescriptionHero();Random.pushGenerator(982);int next=Random.Int();Random.popGenerator();Random.pushGenerator(982);try{PotionExplanation.details(known,hero);assertEquals(next,Random.Int());assertEquals(before,savedDescriptionHero());assertEquals(.7f,hero.apdCustomHealingFraction,0);}finally{Random.popGenerator();}
 }
 @Test public void trinketDetailsUseCurrentKnownLevelsWithoutAdvancingSelectorsOrRandomness()throws Exception{
  for(String type:new String[]{"EyeOfNewt","FerretTuft","SaltCube","VialOfBlood","ExoticCrystals","RatSkull","MimicTooth","ParchmentScrap","PetrifiedSeed","ThirteenLeafClover","DimensionalSundial","CrackedSpyglass","MossyClump","TrapMechanism","ChaoticCenser","WondrousResin","ShardOfOblivion"}){
   com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket trinket=(com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.trinkets."+type).getDeclaredConstructor().newInstance();trinket.levelKnown=trinket.cursedKnown=true;
   for(int level=0;level<=3;level++){trinket.level(level);String heroBefore=savedDescriptionHero();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();trinket.storeInBundle(before);Random.pushGenerator(721);int next=Random.Int();Random.popGenerator();Random.pushGenerator(721);try{String text=TrinketExplanation.details(trinket);assertTrue(type,text.contains("Trinket level: "+level));assertEquals(type,next,Random.Int());}finally{Random.popGenerator();}trinket.storeInBundle(after);assertEquals(type,before.toString(),after.toString());assertEquals(type,heroBefore,savedDescriptionHero());}
   trinket.levelKnown=false;trinket.level(0);String zero=TrinketExplanation.details(trinket);trinket.level(3);assertEquals(type,zero,TrinketExplanation.details(trinket));
  }
 }
 @Test public void trapMechanismDetailsMatchActualFloorWideRevealApportionment()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrapMechanism mechanism=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrapMechanism();mechanism.identify();hero.belongings.backpack.items.add(mechanism);
  java.lang.reflect.Method paint=com.shatteredpixel.shatteredpixeldungeon.levels.painters.RegularPainter.class.getDeclaredMethod("paintTraps",com.shatteredpixel.shatteredpixeldungeon.levels.Level.class,java.util.ArrayList.class);paint.setAccessible(true);
  for(int level=0;level<=3;level++)for(boolean themed:new boolean[]{false,true}){
   mechanism.level(level);descriptionSightArena();for(int i=0;i<33;i++){Dungeon.level.map[i]=Dungeon.level.map[32*33+i]=Dungeon.level.map[i*33]=Dungeon.level.map[i*33+32]=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL;}Dungeon.level.buildFlagMaps();
   Dungeon.level.feeling=themed?com.shatteredpixel.shatteredpixeldungeon.levels.Level.Feeling.TRAPS:com.shatteredpixel.shatteredpixeldungeon.levels.Level.Feeling.NONE;
   com.shatteredpixel.shatteredpixeldungeon.levels.painters.SewerPainter painter=new com.shatteredpixel.shatteredpixeldungeon.levels.painters.SewerPainter();painter.setTraps(20,new Class<?>[]{com.shatteredpixel.shatteredpixeldungeon.levels.traps.ToxicTrap.class},new float[]{1});Random.pushGenerator(904+level);try{paint.invoke(painter,Dungeon.level,new java.util.ArrayList<>());}finally{Random.popGenerator();}
   int visible=0;for(com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap trap:Dungeon.level.traps.valueList())if(trap.visible)visible++;
   assertEquals(themed?100:20,Dungeon.level.traps.valueList().size());assertEquals((themed?80:0)+2*(level+1),visible);
   String text=TrinketExplanation.details(mechanism);assertTrue(text,text.contains("spread across the floor"));assertTrue(text,text.contains("hidden-door searching are unchanged"));
  }
 }
 @Test public void censerAndSpyglassDetailsRetainActualIntervalsAndWholePlusFractionCounts(){
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser censer=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser();censer.identify();com.shatteredpixel.shatteredpixeldungeon.items.trinkets.CrackedSpyglass spyglass=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.CrackedSpyglass();spyglass.identify();
  for(int level=0;level<=3;level++){censer.level(level);spyglass.level(level);assertTrue(TrinketExplanation.details(censer).contains("_"+com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser.averageTurnsUntilGas(level)+" turns_"));assertTrue(censer.statsDesc().contains("random gas"));censer.levelKnown=false;assertTrue(censer.statsDesc().startsWith("At +0:"));censer.levelKnown=true;float extra=com.shatteredpixel.shatteredpixeldungeon.items.trinkets.CrackedSpyglass.extraLootChance(level);String detail=TrinketExplanation.details(spyglass);assertTrue(detail.contains("_"+(int)extra+" hidden item"));assertTrue(detail.contains(ObfuscationExplanation.number(100*(extra-(int)extra))+"%"));}
 }
 public static class ShardLootDescriptionMob extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob {public ShardLootDescriptionMob(){lootChance=.2f;}}
 @Test public void shardDetailsFollowActualCappedLootBonusAndDoNotReadHiddenUpgrades(){
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion shard=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion();shard.identify();hero.belongings.backpack.items.add(shard);ShardLootDescriptionMob mob=new ShardLootDescriptionMob();
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();hero.belongings.weapon=sword;hero.belongings.ring=null;hero.belongings.misc=null;
  for(int level=0;level<=3;level++)for(int worn=0;worn<=2;worn++)for(int uses=0;uses<=2;uses++){
   shard.level(level);sword.levelKnown=sword.cursedKnown=worn<1;armor.levelKnown=armor.cursedKnown=worn<2;Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.WandUseTracker.class);Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.ThrownUseTracker.class);if(uses>=1)Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.WandUseTracker.class,50);if(uses>=2)Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.ThrownUseTracker.class,50);
   int counted=Math.min(level+1,worn+uses);assertEquals(.2f*(1+.2f*counted),mob.lootChance(),.00001f);String text=TrinketExplanation.details(shard);assertTrue(text,text.contains("Your current Shard bonus: _"+ObfuscationExplanation.number(20*counted)+"%_"));sword.level(0);String before=TrinketExplanation.details(shard);sword.level(9);assertEquals(before,TrinketExplanation.details(shard));
  }
  shard.levelKnown=false;assertTrue(shard.statsDesc().startsWith("At +0,"));
 }
 @Test public void ordinaryClericDescriptionsAlreadyContainOneLiveCostForAllSpellsAndRanks(){
  hero.heroClass=HeroClass.CLERIC;Talent.initClassTalents(hero);descriptionSightArena();int checked=0;
  for(HeroSubClass subclass:new HeroSubClass[]{HeroSubClass.NONE,HeroSubClass.PRIEST,HeroSubClass.PALADIN})for(int rank:new int[]{0,1,4}){
   hero.subClass=subclass;for(java.util.LinkedHashMap<Talent,Integer> tier:hero.talents)tier.replaceAll((talent,value)->Math.min(rank,talent.maxPoints()));
   for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell spell:com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell.getAllSpells()){
    String before=savedDescriptionHero();Random.pushGenerator(387);int next=Random.Int();Random.popGenerator();Random.pushGenerator(387);try{String text=spell.desc();assertEquals(spell.getClass().getSimpleName(),1,text.split("Charge cost:",-1).length-1);assertTrue(text,text.endsWith("Charge cost: "+(int)spell.chargeUse(hero)));assertEquals(next,Random.Int());assertEquals(before,savedDescriptionHero());}finally{Random.popGenerator();}checked++;
   }
  }assertEquals(243,checked);
 }
 @Test public void eyeOfNewtDescriptionMatchesActualRoundedVisionRange(){
  descriptionSightArena();hero.viewDistance=8;com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt eye=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt();eye.cursedKnown=true;hero.belongings.backpack.items.add(eye);
  for(int level=0;level<=3;level++){eye.level(level);boolean[] fov=new boolean[Dungeon.level.length()];Dungeon.level.updateFieldOfView(hero,fov);int radius=Math.round(8*com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt.visionRangeMultiplier(level));assertTrue(fov[hero.pos+radius]);assertFalse(fov[hero.pos+radius+1]);assertTrue(TrinketExplanation.details(eye).contains("_"+(2+level)+" tiles_"));}
 }
 @Test public void eyeOfNewtSensesThroughWallsButNotBeyondRangeOrStealthyMimics()throws Exception{
  descriptionSightArena();hero.viewDistance=8;com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt eye=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt();eye.cursedKnown=true;hero.belongings.backpack.items.add(eye);for(int y=1;y<32;y++)com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(y*33+17,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL);
  java.lang.reflect.Field stealthy=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic.class.getDeclaredField("stealthy");stealthy.setAccessible(true);
  for(int level=0;level<=3;level++){
   eye.level(level);int range=com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt.mindVisionRange(level);assertTrue(eye.info().contains("Stealthy mimics evade this sensing"));
   for(int offset:new int[]{range,range+1}){Dungeon.level.mobs.clear();com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat mob=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();mob.pos=hero.pos+offset;Dungeon.level.mobs.add(mob);boolean[] view=new boolean[Dungeon.level.length()];Dungeon.level.updateFieldOfView(hero,view);assertEquals(offset<=range,view[mob.pos]);}
   Dungeon.level.mobs.clear();com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic mimic=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic();mimic.pos=hero.pos+range;stealthy.setBoolean(mimic,true);Dungeon.level.mobs.add(mimic);boolean[] view=new boolean[Dungeon.level.length()];Dungeon.level.updateFieldOfView(hero,view);assertFalse(view[mimic.pos]);
  }
 }
 @Test public void saltCubeTextMatchesActualHungerAndLockedFloorRegeneration(){
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube cube=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube();cube.levelKnown=cube.cursedKnown=true;hero.belongings.backpack.items.add(cube);Hunger hunger=Buff.affect(hero,Hunger.class);boolean intro=com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro();com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro(false);
  try{for(int level=0;level<=3;level++)for(boolean locked:new boolean[]{false,true}){
   cube.level(level);hero.HP=1;hero.HT=1000;hero.apdCustomHealingFraction=0;Dungeon.level.locked=locked;Buff.detach(hero,LockedFloor.class);if(locked)Buff.affect(hero,LockedFloor.class);com.watabou.utils.Bundle food=new com.watabou.utils.Bundle();hunger.storeInBundle(food);food.put("level",0f);hunger.restoreFromBundle(food);
   Regeneration regen=new Regeneration();regen.target=hero;for(int tick=0;tick<10;tick++){hunger.act();regen.act();}com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();regen.storeInBundle(saved);float total=hero.HP-1+saved.getFloat(Regeneration.PARTIAL_REGEN);assertEquals(locked?1:com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube.healthRegenMultiplier(level),total,.00001f);assertEquals(locked?0:10*com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube.hungerGainMultiplier(level),hunger.hungerPoints(),.00001f);assertTrue(cube.info().contains("penalty pauses on locked floors"));assertTrue(TrinketExplanation.details(cube).contains("penalty pauses on locked floors"));
  }}finally{com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro(intro);}
 }
 @Test public void cloverDescribesIndependentDamageRollsRatherThanWholeAttackOutcomes(){
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ThirteenLeafClover clover=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ThirteenLeafClover();clover.levelKnown=clover.cursedKnown=true;hero.belongings.backpack.items.add(clover);
  for(int level=0;level<=3;level++){
   clover.level(level);float chance=com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ThirteenLeafClover.alterHeroDamageChance(level);String text=clover.info();assertTrue(text,text.contains("weapon and many magical damage rolls"));assertTrue(text.contains("_"+Math.round(60*chance)+"%_"));assertTrue(text.contains("_"+Math.round(40*chance)+"%_"));assertFalse(text.contains("of your attacks"));
   for(int seed=0;seed<100;seed++){
    Random.pushGenerator(seed);int expected=Random.Float()<chance?(Random.Float()<.6f?17:3):Random.NormalIntRange(3,17);int expectedNext=Random.Int();Random.popGenerator();
    Random.pushGenerator(seed);try{assertEquals(expected,Hero.heroDamageIntRange(3,17));assertEquals(expectedNext,Random.Int());}finally{Random.popGenerator();}
   }
  }
  clover.levelKnown=false;clover.level(0);String unknown=clover.info();clover.level(3);assertEquals(unknown,clover.info());
 }
 @Test public void ratSkullDetailsPreserveHalfBonusForActualArmoredStatueGeneration(){
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull skull=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull();skull.levelKnown=skull.cursedKnown=true;hero.belongings.backpack.items.add(skull);com.watabou.utils.Bundle generation=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.items.Generator.storeInBundle(generation);
  try{for(int level=0;level<=3;level++){
   skull.level(level);float factor=com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull.exoticChanceMultiplier(level),halfBonusFactor=1+(factor-1)/2f;String text=TrinketExplanation.details(skull);assertTrue(text,text.contains("only the increase above the ordinary chance is halved"));assertTrue(text.contains("effective factor at this level is "+ObfuscationExplanation.number(halfBonusFactor)));
   for(int seed=0;seed<100;seed++){
    Random.pushGenerator(seed);boolean expected=Random.Float()<.1f*halfBonusFactor;Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(expected,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue.random(false) instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue);}finally{Random.popGenerator();}
   }
  }}finally{com.shatteredpixel.shatteredpixeldungeon.items.Generator.restoreFromBundle(generation);}
 }
 @Test public void ferretTuftDescriptionMatchesActualContestForEnemyAndHeroAllies(){
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft tuft=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft();tuft.cursedKnown=true;hero.belongings.backpack.items.add(tuft);ContestScoreTarget attacker=new ContestScoreTarget(),defender=new ContestScoreTarget();
  for(int level=0;level<=3;level++){tuft.level(level);float factor=com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft.evasionMultiplier(level);
   for(int seed=0;seed<40;seed++){Random.pushGenerator(seed);float attack=Random.Float(attacker.attackSkill(defender)),defense=Random.Float(defender.defenseSkill(attacker))*factor;Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(attack>=defense,com.shatteredpixel.shatteredpixeldungeon.actors.Char.hit(attacker,defender,1,false));}finally{Random.popGenerator();}}
   assertTrue(TrinketExplanation.details(tuft).contains("your allies and your enemies"));
  }
 }
 @Test public void inspectingTippedDartDoesNotConsumePendingImpactPosition()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart();dart.levelKnown=dart.cursedKnown=true;
  java.lang.reflect.Field pending=com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.TippedDart.class.getDeclaredField("targetPos");pending.setAccessible(true);int previous=pending.getInt(null);
  try{pending.setInt(null,hero.pos);dart.info();MechanicsDetails.item(dart,dart.info());assertEquals("description must preserve the pending gameplay impact",hero.pos,pending.getInt(null));float preview=dart.previewDurabilityPerUse();assertEquals(hero.pos,pending.getInt(null));assertEquals(preview,dart.durabilityPerUse(),0);assertEquals("gameplay calculation still consumes its impact marker",-1,pending.getInt(null));}finally{pending.setInt(null,previous);}
 }
 @Test public void missileDescriptionsTrackBleedingModifiersAndPreserveUnknownRingPrivacy(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Tomahawk axe=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Tomahawk();axe.levelKnown=axe.cursedKnown=true;
  RingOfSharpshooting aim=new RingOfSharpshooting();aim.anonymize();aim.levelKnown=aim.cursedKnown=true;hero.belongings.ring=aim;aim.activate(hero);
  for(int level:new int[]{0,3,8})for(int bonus:new int[]{0,2,5}){axe.level(level);aim.level(bonus);int damageLevel=axe.buffedLvl()+RingOfSharpshooting.levelDamageBonus(hero);String text=MissileExplanation.details(axe,hero);assertTrue(text.contains(ObfuscationExplanation.number(axe.augment.damageFactor(axe.minBleed(damageLevel)))+"–"+ObfuscationExplanation.number(axe.augment.damageFactor(axe.maxBleed(damageLevel)))+" points"));}
  aim.levelKnown=false;aim.level(0);String zero=MissileExplanation.details(axe,hero),ordinary=axe.statsInfo();aim.level(9);assertEquals(zero,MissileExplanation.details(axe,hero));assertEquals("Ordinary bleeding description must not expose unknown ring upgrades",ordinary,axe.statsInfo());
 }
 public static class CapturedAbilitySprite extends QuietMovementSprite {
  public com.watabou.utils.Callback offered;
  @Override public synchronized void attack(int cell,com.watabou.utils.Callback callback){offered=callback;}
  @Override public void jump(int from,int to,float height,float duration,com.watabou.utils.Callback callback){offered=callback;}
 }
 @Test public void weaponAbilityAddedAmountsMatchActualDeferredAttackInputs()throws Exception{
  descriptionSightArena();hero.STR=30;
  java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);CapturedAbilitySprite sprite=(CapturedAbilitySprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,CapturedAbilitySprite.class);hero.sprite=sprite;
  PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();target.HP=target.HT=100;com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(target);
  try{
   for(String type:new String[]{"WornShortsword","Shortsword","Sword","Longsword","Greatsword","Cudgel","HandAxe","Mace","BattleAxe","WarHammer","Spear","Glaive","Rapier","Katana","Gloves","Sai","Gauntlet","Sickle","WarScythe"}){
    com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon weapon=(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee."+type).getDeclaredConstructor().newInstance();weapon.levelKnown=weapon.cursedKnown=true;hero.belongings.weapon=weapon;
    java.lang.reflect.Method ability=weapon.getClass().getDeclaredMethod("duelistAbility",Hero.class,Integer.class);ability.setAccessible(true);
    for(int level:new int[]{0,1,3,8})for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment augment:com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.values()){
     weapon.level(level);weapon.augment=augment;target.pos=hero.pos+((type.equals("Spear")||type.equals("Glaive")||type.equals("Rapier")||type.equals("Katana"))?2:1);Dungeon.level.heroFOV[target.pos]=true;sprite.offered=null;ability.invoke(weapon,hero,target.pos);assertNotNull(type,sprite.offered);
     Integer actual=null;for(java.lang.reflect.Field field:sprite.offered.getClass().getDeclaredFields())if(field.getType()==int.class&&(field.getName().toLowerCase(java.util.Locale.ROOT).contains("boost"))){field.setAccessible(true);actual=field.getInt(sprite.offered);}
     assertNotNull(type+" deferred added input",actual);String text=WeaponAbilityExplanation.details(weapon,hero);assertTrue(type+" "+actual+" "+text,text.contains("amount: "+actual+" damage points")||text.contains("defenses: "+actual+" points"));
    }
   }
  }finally{com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(target);}
 }
 private String savedDescriptionHero(){com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();hero.storeInBundle(saved);return saved.toString();}
 @Test public void weaponAbilityBreakdownsCoverEveryImplementedFamilyWithoutSpendingState()throws Exception{
  String[] types={"WornShortsword","Shortsword","Sword","Longsword","Greatsword","Cudgel","HandAxe","Mace","BattleAxe","WarHammer","Spear","Glaive","Rapier","Katana","Gloves","Sai","Gauntlet","Greataxe","Flail","Dagger","Dirk","AssassinsBlade","Sickle","WarScythe","RoundShield","Greatshield","Quarterstaff","Scimitar","RunicBlade","Whip","Crossbow"};
  for(String type:types){com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon weapon=(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee."+type).getDeclaredConstructor().newInstance();weapon.levelKnown=weapon.cursedKnown=true;weapon.level(3);
   com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();weapon.storeInBundle(before);String heroBefore=savedDescriptionHero();Random.pushGenerator(187);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(187);try{String text=WeaponAbilityExplanation.details(weapon,hero);assertTrue(type,text.contains("Duelist weapon ability"));assertEquals(type,expected,Random.Int());}finally{Random.popGenerator();}weapon.storeInBundle(after);assertEquals(type,before.toString(),after.toString());assertEquals(type,heroBefore,savedDescriptionHero());
   weapon.levelKnown=false;weapon.level(0);String zero=WeaponAbilityExplanation.details(weapon,hero);weapon.level(9);assertEquals(type,zero,WeaponAbilityExplanation.details(weapon,hero));
  }
 }
 @Test public void stanceAbilityDescriptionsAgreeWithActualDefenseAccuracyAndAttackSpeed(){
  hero.STR=20;com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar();hero.belongings.weapon=sword;sword.levelKnown=sword.cursedKnown=true;
  PlainStatusTarget target=new PlainStatusTarget();int baseDefense=hero.defenseSkill(target),baseAccuracy=hero.attackSkill(target);
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff.DefensiveStance defense=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff.DefensiveStance.class,5);assertEquals(3*baseDefense,hero.defenseSkill(target));defense.detach();
  float delay=sword.delayFactor(hero);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar.SwordDance dance=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar.SwordDance.class,5);assertEquals(delay/1.6f,sword.delayFactor(hero),.000001f);assertEquals(Math.round(baseAccuracy*1.5f),hero.attackSkill(target));dance.detach();
  assertTrue(WeaponAbilityExplanation.details(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff(),hero).contains("score by 3"));assertTrue(WeaponAbilityExplanation.details(sword,hero).contains("adds 0.6"));
 }
 @Test public void abilityChargeExplanationDoesNotConsumeCleaveOrSpinBenefits(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail flail=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail();
  assertEquals(1,sword.abilityChargeUse(hero,null),0);assertEquals(1,flail.abilityChargeUse(hero,null),0);
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword.CleaveTracker cleave=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword.CleaveTracker.class,4);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail.SpinAbilityTracker spin=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail.SpinAbilityTracker.class,3);spin.spins=2;
  String before=savedDescriptionHero();WeaponAbilityExplanation.details(sword,hero);WeaponAbilityExplanation.details(flail,hero);assertEquals(before,savedDescriptionHero());assertEquals(0,sword.abilityChargeUse(hero,null),0);assertEquals(0,flail.abilityChargeUse(hero,null),0);assertSame(cleave,hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword.CleaveTracker.class));assertEquals(2,spin.spins);
 }
 @Test public void protectionDecayDescriptionsMatchActualUpdateAndSaveRestore(){
  for(int interval:new int[]{1,3,5}){
   Barkskin bark=Buff.affect(hero,Barkskin.class);bark.set(8,interval);float before=bark.cooldown();String text=BuffExplanation.details(bark);assertTrue(text.contains(interval+" turns apart"));assertEquals(8,bark.level());assertEquals(before,bark.cooldown(),0);bark.act();assertEquals(7,bark.level());assertEquals(before+interval,bark.cooldown(),0);com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();bark.storeInBundle(saved);Barkskin loaded=new Barkskin();loaded.restoreFromBundle(saved);assertEquals(interval,loaded.decayInterval());bark.detach();
   ArcaneArmor arcane=Buff.affect(hero,ArcaneArmor.class);arcane.set(8,interval);before=arcane.cooldown();assertTrue(BuffExplanation.details(arcane).contains(interval+" turns apart"));assertEquals(8,arcane.level());arcane.act();assertEquals(7,arcane.level());assertEquals(before+interval,arcane.cooldown(),0);arcane.storeInBundle(saved);ArcaneArmor restored=new ArcaneArmor();restored.restoreFromBundle(saved);assertEquals(interval,restored.decayInterval());arcane.detach();
  }
 }
 @Test public void remainingStatusDetailsUseSavedPublicStateWithoutAdvancingIt(){
  PinCushion pins=Buff.affect(hero,PinCushion.class);com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife knife=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife();knife.quantity(3);pins.stick(knife);assertTrue(BuffExplanation.details(pins).contains("quantity: 3"));assertEquals(3,pins.getStuckItems().get(0).quantity());
  SwarmIntelTracker swarm=new SwarmIntelTracker();com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();swarm.storeInBundle(saved);saved.put("alert_range",7);swarm.restoreFromBundle(saved);assertTrue(BuffExplanation.details(swarm).contains("7 tiles"));assertEquals(7,swarm.observedAlertRange());
  GravityChaosTracker gravity=new GravityChaosTracker();gravity.left=11;gravity.positiveOnly=true;Random.pushGenerator(97);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(97);try{String text=BuffExplanation.details(gravity);assertTrue(text.contains("count: 11"));assertTrue(text.contains("excludes allied"));assertEquals(expected,Random.Int());assertEquals(11,gravity.left);}finally{Random.popGenerator();}
 }
 @Test public void alternateLightTalentsMatchDistinctActualChargingChannels(){
  for(int rank=1;rank<=3;rank++){
   hero.talents.get(2).put(Talent.LIGHT_CLOAK,rank);hero.talents.get(2).put(Talent.LIGHT_READING,0);
   assertEquals(1+.2f*rank/3f,RingOfEnergy.artifactChargeMultiplier(hero),.000001f);assertEquals(1,RingOfEnergy.wandChargeMultiplier(hero),0);
   hero.talents.get(2).put(Talent.LIGHT_CLOAK,0);hero.talents.get(2).put(Talent.LIGHT_READING,rank);
   assertEquals(1+.2f*rank/3f,RingOfEnergy.wandChargeMultiplier(hero),.000001f);assertEquals(1,RingOfEnergy.artifactChargeMultiplier(hero),0);
   assertTrue(TalentExplanation.details(Talent.LIGHT_CLOAK,rank,true).contains("artifact charging factor"));assertTrue(TalentExplanation.details(Talent.LIGHT_READING,rank,true).contains("wand charging factor"));
  }
 }
 @Test public void unidentifiedTwinUpgradeSourceMustNotChangeKnownWeaponInspection(){
  hero.heroClass=HeroClass.DUELIST;hero.subClass=HeroSubClass.CHAMPION;hero.talents.get(2).put(Talent.TWIN_UPGRADES,3);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword known=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();known.level(1);known.levelKnown=known.cursedKnown=true;com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword hidden=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword();hidden.levelKnown=hidden.cursedKnown=false;hero.belongings.weapon=known;hero.belongings.secondWep=hidden;
  hidden.level(0);String zero=known.info();hidden.level(8);assertEquals("Twin Upgrades must not expose the companion's hidden level",zero,known.info());assertEquals("real combat still borrows the hidden upgrade",8,known.buffedLvl());hidden.levelKnown=hidden.cursedKnown=true;assertFalse(zero.equals(known.info()));assertFalse(known.info().contains("bonus from your unidentified weapon"));
 }
 @Test public void unidentifiedTwinUpgradeSourceMustNotChangeKnownCrossbowDartInspection(){
  hero.heroClass=HeroClass.DUELIST;hero.subClass=HeroSubClass.CHAMPION;hero.talents.get(2).put(Talent.TWIN_UPGRADES,3);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow();bow.level(1);bow.levelKnown=bow.cursedKnown=true;com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword hidden=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword();hidden.levelKnown=hidden.cursedKnown=false;hero.belongings.weapon=bow;hero.belongings.secondWep=hidden;com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart();
  hidden.level(0);String zero=dart.info();hidden.level(8);assertEquals("Dart preview must not expose the companion's hidden level",zero,dart.info());assertEquals(8,bow.buffedLvl());
 }
 @Test public void hiddenTwinUpgradeCannotChangePassiveBlockOrEnchantmentExplanations(){
  hero.heroClass=HeroClass.DUELIST;hero.subClass=HeroSubClass.CHAMPION;com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword source=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword();source.levelKnown=source.cursedKnown=false;hero.belongings.secondWep=source;
  for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon item:new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow()})for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment augment:com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.values())for(int rank=1;rank<=3;rank++){
   hero.talents.get(2).put(Talent.TWIN_UPGRADES,rank);hero.belongings.weapon=item;item.level(1);item.levelKnown=item.cursedKnown=true;item.augment=augment;item.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking());source.level(0);String info=item.info(),normal=EnchantmentExplanation.playerDescription(item,hero),detail=EnchantmentExplanation.describe(item,hero,true),block=WeaponExplanation.summary(item,hero),all=MechanicsDetails.item(item,info);source.level(8);
   assertEquals(info,item.info());assertEquals(normal,EnchantmentExplanation.playerDescription(item,hero));assertEquals(detail,EnchantmentExplanation.describe(item,hero,true));assertEquals(block,WeaponExplanation.summary(item,hero));assertEquals(all,MechanicsDetails.item(item,item.info()));assertEquals(item.tier+3-rank<=source.tier?8:1,item.buffedLvl());
  }
 }
 @Test public void twinInspectionScopeDoesNotAffectAnotherThreadOrSurviveExceptions()throws Exception{
  hero.talents.get(2).put(Talent.TWIN_UPGRADES,3);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword item=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();item.level(1);item.levelKnown=item.cursedKnown=true;com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword source=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword();source.level(8);source.levelKnown=false;hero.belongings.weapon=item;hero.belongings.secondWep=source;java.util.concurrent.atomic.AtomicInteger actorLevel=new java.util.concurrent.atomic.AtomicInteger(-99);String before=savedDescriptionHero();
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->{assertEquals(1,item.buffedLvl());Thread actor=new Thread(()->actorLevel.set(item.buffedLvl()));actor.start();try{actor.join(5000);}catch(InterruptedException e){throw new RuntimeException(e);}assertFalse(actor.isAlive());return null;});assertEquals(8,actorLevel.get());
  try{com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->{item.info();throw new IllegalStateException("fixture");});fail();}catch(IllegalStateException expected){assertEquals("fixture",expected.getMessage());}assertFalse(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspectionActive());assertEquals(8,item.buffedLvl());assertEquals(before,savedDescriptionHero());
 }
 @Test public void twinUpgradeDetailsMatchActualTierAndEffectiveLevelRules(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword low=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe high=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe();hero.belongings.weapon=low;hero.belongings.secondWep=high;low.level(1);high.level(7);
  for(int rank=1;rank<=3;rank++)for(int gap=0;gap<=3;gap++){
   hero.talents.get(2).put(Talent.TWIN_UPGRADES,rank);high.tier=low.tier+gap;
   assertEquals(gap>=3-rank?7:1,low.buffedLvl());assertTrue(TalentExplanation.details(Talent.TWIN_UPGRADES,rank).contains("required tier gap is "+(3-rank)));
  }
 }
 @Test public void alternateWeaponRechargeDetailsMatchActualDamageRounding(){
  String normal=Talent.WEAPON_RECHARGING.desc(true);assertTrue(normal,normal.contains("_2.5%_"));assertFalse(normal,normal.contains("+5%"));assertFalse(normal,normal.contains("+7.5%"));
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();hero.belongings.weapon=sword;hero.STR=10;
  for(int rank=1;rank<=2;rank++)for(int seed=0;seed<30;seed++){
   hero.talents.get(1).put(Talent.WEAPON_RECHARGING,0);Random.pushGenerator(seed);int base=hero.damageRoll();Random.popGenerator();hero.talents.get(1).put(Talent.WEAPON_RECHARGING,rank);Recharging effect=Buff.affect(hero,Recharging.class,5);
   Random.pushGenerator(seed);try{assertEquals(Math.round(base*1.025f+.025f*rank),hero.damageRoll());}finally{Random.popGenerator();effect.detach();}
   assertTrue(TalentExplanation.details(Talent.WEAPON_RECHARGING,rank,true).contains("1.025"));
  }
 }
 @Test public void inscribedStealthDetailsMatchActualAdditiveDuration()throws Exception{
  hero.sprite=quietSprite();
  for(int rank=1;rank<=2;rank++)for(float factor:new float[]{.5f,1f,2f}){
   Invisibility old=hero.buff(Invisibility.class);if(old!=null)old.detach();hero.talents.get(1).put(Talent.INSCRIBED_STEALTH,rank);
   Invisibility effect=Buff.affect(hero,Invisibility.class,7);float before=effect.cooldown();
   Talent.onScrollUsed(hero,hero.pos,factor,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify.class);
   assertEquals(before+factor*(1+2*rank),effect.cooldown(),.0001f);
   assertTrue(TalentExplanation.details(Talent.INSCRIBED_STEALTH,rank).contains((1+2*rank)+" game-time units"));
  }
 }
 @Test public void assassinTalentDetailsUseLiveTierTablesWithoutQueryingEnemyHealth(){
  for(int rank=1;rank<=3;rank++){
   hero.talents.get(2).put(Talent.ENHANCED_LETHALITY,rank);hero.talents.get(2).put(Talent.ASSASSINS_REACH,rank);
   for(Talent talent:new Talent[]{Talent.ENHANCED_LETHALITY,Talent.ASSASSINS_REACH}){
    String text=TalentExplanation.details(talent,rank);
    for(Preparation.AttackLevel tier:Preparation.AttackLevel.values())assertTrue(text.contains("below "+ObfuscationExplanation.number(100*tier.KOThreshold())+"% HP, "+tier.blinkDistance()+" tiles"));
   }
  }
 }
 @Test public void durableTipsDetailsMatchActualUnroundedWearRatio(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart();
  hero.talents.get(2).put(Talent.DURABLE_TIPS,0);float base=dart.durabilityPerUse(0);
  for(int rank=1;rank<=3;rank++){hero.talents.get(2).put(Talent.DURABLE_TIPS,rank);float expected=100f/Math.round(100f/((base-.001f)/(1+rank)))+.001f;assertEquals(expected,dart.durabilityPerUse(0),.0001f);assertTrue(TalentExplanation.details(Talent.DURABLE_TIPS,rank).contains("= "+(1+rank)+"."));}
 }
 @Test public void soulTalentsMatchActualPhysicalRestorationAndHungerUnits()throws Exception{
  hero.sprite=quietSprite();Hunger hunger=Buff.affect(hero,Hunger.class);
  for(int rank=1;rank<=3;rank++)for(boolean heroAttack:new boolean[]{false,true}){
   hero.talents.get(2).put(Talent.SOUL_SIPHON,rank);hero.talents.get(2).put(Talent.SOUL_EATER,rank);hero.HP=1;
   com.watabou.utils.Bundle food=new com.watabou.utils.Bundle();hunger.storeInBundle(food);food.put("level",200f);hunger.restoreFromBundle(food);float oldHunger=hunger.hungerPoints();
   com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat victim=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();victim.HP=victim.HT=10;victim.pos=hero.pos+1;victim.sprite=quietSprite();victim.state=victim.HUNTING;Buff.affect(victim,SoulMark.class);Buff.affect(victim,Barrier.class).setShield(2);
   com.shatteredpixel.shatteredpixeldungeon.actors.Char attacker=heroAttack?hero:new PlainStatusTarget();attacker.pos=hero.pos;
   victim.defenseProc(attacker,20);int input=heroAttack?12:Math.round(12*.4f*rank/3f);
   assertEquals(1+(int)Math.ceil(input*.4f),hero.HP);assertEquals(oldHunger-input*rank/3f,hunger.hungerPoints(),.0001f);
   assertTrue(TalentExplanation.details(Talent.SOUL_SIPHON,rank).contains(ObfuscationExplanation.number(100*.4f*rank/3f)+"%"));
   assertTrue(TalentExplanation.details(Talent.SOUL_EATER,rank).contains((10*rank)+"% chance"));
  }
 }
 @Test public void sealTalentDetailsMatchCapacityAndGlyphEligibility(){
  com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal seal=new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal();
  for(int rank=1;rank<=2;rank++){
   hero.talents.get(0).put(Talent.IRON_WILL,rank);hero.talents.get(1).put(Talent.RUNIC_TRANSFERENCE,rank);
   for(int tier=1;tier<=5;tier++)for(int level:new int[]{0,3,9})assertEquals(3+2*tier+rank,seal.maxShield(tier,level));
   seal.setGlyph(new Obfuscation());assertTrue(seal.canTransferGlyph());
   seal.setGlyph(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Affection());assertEquals(rank==2,seal.canTransferGlyph());
   seal.setGlyph(null);assertFalse(seal.canTransferGlyph());
   assertTrue(TalentExplanation.details(Talent.IRON_WILL,rank).contains(rank+" shield points"));
   assertTrue(TalentExplanation.details(Talent.RUNIC_TRANSFERENCE,rank,true).contains("from +"+(5+rank)));
  }
 }
 @Test public void allyWarpTalentDetailsMatchActualInteractionRange(){
  descriptionSightArena();com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat ally=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;
  for(int rank=1;rank<=3;rank++){
   hero.talents.get(2).put(Talent.ALLY_WARP,rank);
   for(int distance=2;distance<=8;distance++){ally.pos=hero.pos+distance;assertEquals(distance<=2*rank,ally.canInteract(hero));}
   assertTrue(TalentExplanation.details(Talent.ALLY_WARP,rank).contains((2*rank)+" tiles"));
  }
 }
 @Test public void inspectingRingEffectsDoesNotActivateEmergencyBerserk()throws Exception{
  hero.sprite=quietSprite();hero.talents.get(0).put(Talent.DEATHLESS_FURY,1);
  Berserk rage=Buff.affect(hero,Berserk.class);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();rage.storeInBundle(before);before.put("power",1.2f);rage.restoreFromBundle(before);hero.HP=0;
  com.shatteredpixel.shatteredpixeldungeon.apd.qol.RingItemEffects.describe(armor);
  com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();rage.storeInBundle(after);
  assertEquals("NORMAL",after.getString("state"));assertEquals(0,rage.shielding());
  assertFalse(rage.activeForInspection());assertTrue(hero.isAlive());assertTrue(rage.activeForInspection());
  rage.storeInBundle(before);com.shatteredpixel.shatteredpixeldungeon.apd.qol.RingItemEffects.describe(armor);rage.storeInBundle(after);
  assertEquals(before.getString("state"),after.getString("state"));assertEquals(before.getFloat("levelrecovery"),after.getFloat("levelrecovery"),0);

 }
 @Test public void armorProtectionPreviewMatchesActualHeroRollsAcrossStrengthAugmentsAndChallenge(){
  for(int challenge:new int[]{0,Challenges.NO_ARMOR})for(Armor.Augment augment:Armor.Augment.values())for(int level:new int[]{-3,0,3,8})for(int strength:new int[]{6,10,20}){
   Dungeon.challenges=challenge;armor.augment=augment;armor.level(level);hero.STR=strength;
   int missing=Math.max(0,armor.STRReq()-hero.STR());
   String detail=ArmorExplanation.details(armor,hero);
   assertTrue(detail,detail.contains("protection-roll penalty: "+2*missing+" points"));
   for(int seed=0;seed<10;seed++){
    Random.pushGenerator(seed);int expected;try{Random.NormalIntRange(0,0);expected=Math.max(0,Random.NormalIntRange(armor.DRMin(),armor.DRMax())-2*missing);}finally{Random.popGenerator();}
    Random.pushGenerator(seed);try{assertEquals(expected,hero.drRoll());}finally{Random.popGenerator();}
   }
   assertEquals((float)(10/Math.pow(1.5,missing))+augment.evasionFactor(armor.buffedLvl()),armor.evasionFactor(hero,10),.00001f);
   assertEquals((float)(1/Math.pow(1.2,missing)),armor.speedFactor(hero,1),.00001f);
  }
 }
 @Test public void armorPreviewUsesKnownMightWithoutTreatingEnhancedRingsAsStrength(){
  armor.level(3);hero.STR=10;RingOfMight might=new RingOfMight();might.level(1);might.anonymize();might.levelKnown=might.cursedKnown=true;hero.belongings.ring=might;might.activate(hero);
  Buff.affect(hero,EnhancedRings.class);assertEquals(Integer.valueOf(hero.STR()),ArmorExplanation.knownStrength(hero));
  assertTrue(ArmorExplanation.details(armor,hero).contains("Known strength: "+hero.STR()));
  might.levelKnown=false;String unknown=ArmorExplanation.details(armor,hero);might.level(9);assertEquals(unknown,ArmorExplanation.details(armor,hero));assertTrue(unknown.contains("withheld"));
 }
 @Test public void armorUnknownUpgradeDetailsDoNotChangeWithHiddenLevel(){
  armor.levelKnown=false;armor.level(0);String detail=ArmorExplanation.details(armor,hero);armor.level(9);
  assertEquals(detail,ArmorExplanation.details(armor,hero));assertTrue(detail.contains("stay hidden"));
 }
 @Test public void armorRequirementPreviewSeparatesDegradeFromOwnLevelAndMastery(){
  armor.level(6);armor.masteryPotionBonus=true;int requirement=armor.STRReq();Buff.affect(hero,Degrade.class);
  assertEquals(requirement,armor.STRReq());assertTrue(ArmorExplanation.details(armor,hero).contains("strength requirement: "+requirement+" points"));
  assertTrue(ArmorExplanation.details(armor,hero).contains("effective armor level: "+armor.buffedLvl()));
 }
 @Test public void normalWeaponTimingMatchesActualDelayWithoutConsumingFreeAttack(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon[] weapons={new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sai(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife()};
  RingOfFuror furor=new RingOfFuror();furor.level(2);furor.anonymize();furor.levelKnown=furor.cursedKnown=true;hero.belongings.ring=furor;furor.activate(hero);
  Talent.LethalMomentumTracker free=Buff.affect(hero,Talent.LethalMomentumTracker.class);
  for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon weapon:weapons)for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment augment:com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.values())for(int strength:new int[]{6,10,20})for(boolean dancing:new boolean[]{false,true}){
   weapon.levelKnown=weapon.cursedKnown=true;weapon.level(3);weapon.augment=augment;hero.STR=strength;
   if(dancing)Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar.SwordDance.class);else if(hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar.SwordDance.class)!=null)hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar.SwordDance.class).detach();
   assertEquals(weapon.delayFactor(hero),WeaponExplanation.normalAttackDelay(weapon,hero),0);
   String shownTime=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",weapon.delayFactor(hero));assertTrue(WeaponExplanation.summary(weapon,hero).contains(shownTime+(shownTime.equals("1")?" turn":" turns")+" per attack"));
   WeaponExplanation.details(weapon,hero);assertSame(free,hero.buff(Talent.LethalMomentumTracker.class));
  }
  hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();String momentum=TalentExplanation.details(Talent.LETHAL_MOMENTUM,2);assertTrue(momentum,momentum.contains("killing action itself"));assertSame(free,hero.buff(Talent.LethalMomentumTracker.class));assertEquals(0,hero.attackDelay(),0);assertNull(hero.buff(Talent.LethalMomentumTracker.class));assertEquals(hero.belongings.weapon.delayFactor(hero),hero.attackDelay(),0);
 }
 @Test public void ordinarySwordSummaryIsShortAndKeepsLiveTiming(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();
  sword.levelKnown=true;
  String summary=WeaponExplanation.summary(sword,hero);
  assertEquals("Attack time: _"+com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",sword.delayFactor(hero))+" turns per attack_.",summary);
  assertTrue(summary.length()<100);
  assertFalse(summary.contains("attack score"));
  assertFalse(summary.contains("strength requirement"));
 }
 @Test public void weaponTimingHidesUnknownLevelsAndRingEffectsAndDoesNotPretendBowIsOrdinary(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive();weapon.levelKnown=false;String hidden=WeaponExplanation.details(weapon,hero);weapon.level(8);assertEquals(hidden,WeaponExplanation.details(weapon,hero));assertNull(WeaponExplanation.normalAttackDelay(weapon,hero));
  weapon.levelKnown=true;RingOfFuror furor=new RingOfFuror();furor.levelKnown=false;hero.belongings.ring=furor;String unknown=WeaponExplanation.details(weapon,hero);furor.level(8);assertEquals(unknown,WeaponExplanation.details(weapon,hero));assertNull(WeaponExplanation.normalAttackDelay(weapon,hero));
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow();assertNull(WeaponExplanation.normalAttackDelay(bow,hero));assertTrue(WeaponExplanation.details(bow,hero).contains("Sniper-shot timing"));
 }
 public static class QuietScrySprite extends QuietMovementSprite {int lastZap=-1;@Override public void zap(int cell){lastZap=cell;}}
 @Test public void conciseTalismanRangeAndCostMatchActualEmptyFloorScrying()throws Exception{
  hero=new DescriptionMonkHero();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);hero.HP=hero.HT=20;Dungeon.hero=hero;descriptionSightArena();java.util.Arrays.fill(Dungeon.level.mapped,true);
  java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);QuietScrySprite sprite=(QuietScrySprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,QuietScrySprite.class);hero.sprite=sprite;
  java.lang.reflect.Field user=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser"),charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge"),partial=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("partialCharge");user.setAccessible(true);charge.setAccessible(true);partial.setAccessible(true);Object oldUser=user.get(null);user.set(null,hero);
  try{for(int level:new int[]{0,5,10})for(int stored:new int[]{6,25,100})for(int distance:new int[]{1,4,10}){
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight();item.level(level);item.levelKnown=item.cursedKnown=true;charge.setInt(item,stored);partial.setFloat(item,.3f);float time=hero.cooldown();int calls=((DescriptionMonkHero)hero).nextCalls;sprite.lastZap=-1;String saved=savedDescriptionHero();String detail=TalismanExplanation.details(item);assertEquals(saved,savedDescriptionHero());
   item.scry.onSelect(hero.pos);assertEquals(time,hero.cooldown(),0);assertEquals(stored,item.currentCharges());assertEquals(-1,sprite.lastZap);
   item.scry.onSelect(hero.pos+distance);assertEquals(time+1,hero.cooldown(),0);assertEquals(calls+1,((DescriptionMonkHero)hero).nextCalls);assertTrue(sprite.lastZap>hero.pos);float actualDistance=Dungeon.level.trueDistance(hero.pos,sprite.lastZap);assertTrue(actualDistance<=Math.max(2,Math.min(5+2*level,(stored-3)/1.08f)));
   float nominal=3+actualDistance*1.08f;int expectedCharge=(int)(stored-nominal);float expectedPartial=.3f-(actualDistance*1.08f)%1;if(expectedPartial<0&&expectedCharge>0){expectedPartial++;expectedCharge--;}while(expectedCharge<0){expectedCharge++;expectedPartial--;}assertEquals(expectedCharge,item.currentCharges());assertEquals(expectedPartial,partial.getFloat(item),.00001f);assertTrue(detail.contains("3 + 1.08 × d"));
  }}finally{user.set(null,oldUser);}
 }
 @Test public void talismanActualWarningChecksOnlyVisibleSearchableNearbyHiddenTraps()throws Exception{
  hero=new DescriptionMonkHero();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);hero.HP=hero.HT=20;Dungeon.hero=hero;descriptionSightArena();
  for(int state=0;state<8;state++){
   java.util.Arrays.fill(Dungeon.level.heroFOV,false);Dungeon.level.traps.clear();java.util.Arrays.fill(Dungeon.level.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);Dungeon.level.buildFlagMaps();Buff.detach(hero,MagicImmune.class);
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight();item.levelKnown=item.cursedKnown=true;item.cursed=state==6;if(state==7)Buff.affect(hero,MagicImmune.class);int cell=hero.pos+(state==1?4:3);int tile=state==3?com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.TRAP:state==5?com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_DOOR:com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_TRAP;com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(cell,tile);com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap trap=new com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap();trap.set(cell);trap.canBeSearched=state!=4;Dungeon.level.traps.put(cell,trap);Dungeon.level.heroFOV[cell]=state!=2;
   com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();item.storeInBundle(before);TalismanExplanation.details(item);com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();item.storeInBundle(after);assertEquals(before.toString(),after.toString());com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight.Foresight passive=item.new Foresight();passive.target=hero;passive.checkAwareness();com.watabou.utils.Bundle actual=new com.watabou.utils.Bundle();item.storeInBundle(actual);assertEquals("warning state"+state,state==0,actual.getBoolean("warn"));assertEquals(tile,Dungeon.level.map[cell]);
  }
 }
 public static class DescriptionPlantTrigger extends com.shatteredpixel.shatteredpixeldungeon.plants.Plant {
  @Override public void wither(){}
  @Override public void activate(com.shatteredpixel.shatteredpixeldungeon.actors.Char target){}
 }
 @Test public void plantTalentWordingMatchesActualProtectionAndDecay(){
  hero=new DescriptionMonkHero();hero.heroClass=HeroClass.HUNTRESS;hero.HP=hero.HT=20;Talent.initClassTalents(hero);Dungeon.hero=hero;descriptionSightArena();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(hero);
  try{
   for(int rank=1;rank<=3;rank++)for(int level:new int[]{1,2,7,20}){
    Buff.detach(hero,Barkskin.class);hero.talents.get(0).put(Talent.NATURES_AID,0);hero.talents.get(0).put(Talent.BARKSKIN,rank);hero.lvl=level;
    DescriptionPlantTrigger plant=new DescriptionPlantTrigger();plant.pos=hero.pos;plant.trigger();
    Barkskin bark=hero.buff(Barkskin.class);int protection=level*rank/3;assertEquals(protection,bark.level());assertEquals(1,bark.decayInterval());
    String explanation=MechanicsDetails.talent(Talent.BARKSKIN,rank,"");assertTrue(explanation,explanation.contains(protection==0?"No extra protection":"0–"+protection+" damage protection"));
    if(protection>1){float before=bark.cooldown();bark.act();assertEquals(protection-1,bark.level());assertEquals(1,bark.cooldown()-before,0);}
   }
   for(int rank=1;rank<=2;rank++){
    Buff.detach(hero,Barkskin.class);hero.talents.get(0).put(Talent.BARKSKIN,0);hero.talents.get(0).put(Talent.NATURES_AID,rank);
    DescriptionPlantTrigger plant=new DescriptionPlantTrigger();plant.pos=hero.pos+1;Dungeon.level.heroFOV[plant.pos]=true;plant.trigger();
    Barkskin bark=hero.buff(Barkskin.class);assertEquals(2,bark.level());assertEquals(1+2*rank,bark.decayInterval());assertTrue(MechanicsDetails.talent(Talent.NATURES_AID,rank,"").contains("_"+(1+2*rank)+" turns_"));
    float before=bark.cooldown();bark.act();assertEquals(1,bark.level());assertEquals(1+2*rank,bark.cooldown()-before,0);
   }
   assertTrue(Talent.BARKSKIN.desc().contains("one third"));assertFalse(Talent.BARKSKIN.desc().contains("steppingon"));
  }finally{com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(hero);}
 }
 @Test public void followupWordingMatchesRealTargetSwitchAndOneHitBonus(){
  for(int rank=1;rank<=2;rank++){
   hero.talents.get(0).put(Talent.FOLLOWUP_STRIKE,rank);Buff.detach(hero,Talent.FollowupStrikeTracker.class);
   PlainStatusTarget first=new PlainStatusTarget(),second=new PlainStatusTarget();first.alignment=second.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;
   hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife();
   assertEquals(10,Talent.onAttackProc(hero,first,10));assertEquals(first.id(),hero.buff(Talent.FollowupStrikeTracker.class).object);
   assertEquals(10,Talent.onAttackProc(hero,second,10));assertEquals(second.id(),hero.buff(Talent.FollowupStrikeTracker.class).object);
   hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger();
   assertEquals(10,Talent.onAttackProc(hero,first,10));assertNotNull(hero.buff(Talent.FollowupStrikeTracker.class));
   assertEquals(11+rank,Talent.onAttackProc(hero,second,10));assertNull(hero.buff(Talent.FollowupStrikeTracker.class));assertEquals(10,Talent.onAttackProc(hero,second,10));
   assertTrue(MechanicsDetails.talent(Talent.FOLLOWUP_STRIKE,rank,"").contains("_"+(1+rank)+" damage_"));
  }
 }
 private void descriptionSightArena(){
  Dungeon.level.blobs=new java.util.HashMap<>();Dungeon.level.mobs=new java.util.HashSet<>();Dungeon.level.heaps=new com.watabou.utils.SparseArray<>();Dungeon.level.traps=new com.watabou.utils.SparseArray<>();Dungeon.level.plants=new com.watabou.utils.SparseArray<>();Dungeon.level.customTiles=new java.util.ArrayList<>();Dungeon.level.customWalls=new java.util.ArrayList<>();Dungeon.level.setSize(33,33);java.util.Arrays.fill(Dungeon.level.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);Dungeon.level.buildFlagMaps();Dungeon.level.discoverable=new boolean[Dungeon.level.length()];java.util.Arrays.fill(Dungeon.level.discoverable,true);hero.pos=16*33+16;hero.viewDistance=8;
 }
 @Test public void blindnessExplanationMatchesActualLocalSightAndIndependentSensing(){
  descriptionSightArena();boolean[] view=new boolean[Dungeon.level.length()];Blindness blind=Buff.affect(hero,Blindness.class,8);Dungeon.level.updateFieldOfView(hero,view);assertTrue(view[hero.pos+1]);assertFalse(view[hero.pos+5]);
  MindVision sense=Buff.affect(hero,MindVision.class,8);sense.distance=3;int secret=hero.pos+2;com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(secret,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_DOOR);Dungeon.level.updateFieldOfView(hero,view);assertTrue(view[hero.pos+3]);assertFalse(view[hero.pos+5]);assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_DOOR,Dungeon.level.map[secret]);
  assertTrue(MechanicsDetails.buff(blind,blind.desc()).contains("maximum, not adding"));assertSame(blind,hero.buff(Blindness.class));assertEquals(3,sense.distance);
 }
 public static class DescriptionAmokedRat extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat {public com.shatteredpixel.shatteredpixeldungeon.actors.Char chooseNow(){return chooseEnemy();}public void rememberEnemy(com.shatteredpixel.shatteredpixeldungeon.actors.Char target){enemy=target;}}
 @Test public void cleansingDescriptionMatchesActualCuresHungerAndStatusExceptions(){
  Hunger appetite=Buff.affect(hero,Hunger.class);appetite.affectHunger(-Hunger.HUNGRY,false);assertEquals(Hunger.HUNGRY,appetite.hungerPoints(),0);Buff.affect(hero,Weakness.class,5);Buff.affect(hero,Cripple.class,5);com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.cleanse(hero);assertNull(hero.buff(Weakness.class));assertNull(hero.buff(Cripple.class));assertEquals(0,appetite.hungerPoints(),0);com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.Cleanse protection=hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.Cleanse.class);assertEquals(5,protection.cooldown(),0);Buff.affect(hero,Daze.class,3);assertNull(hero.buff(Daze.class));Buff.affect(hero,LostInventory.class);assertNotNull(hero.buff(LostInventory.class));assertTrue(protection.desc().contains("most harmful status effects"));assertFalse(protection.desc().contains("all harmful"));
  PlainStatusTarget subject=new PlainStatusTarget();NegativeAllyFixture relation=Buff.affect(subject,NegativeAllyFixture.class);com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.cleanse(subject);assertSame(relation,subject.buff(NegativeAllyFixture.class));
 }
 @Test public void staminaPlainDescriptionMatchesMovementAndUnchangedOtherActionTimes()throws Exception{
  hero.sprite=quietSprite();float movement=hero.speed(),attack=hero.attackDelay();Stamina stamina=Buff.prolong(hero,Stamina.class,Stamina.DURATION);assertEquals(movement*1.5f,hero.speed(),.000001f);assertEquals(attack,hero.attackDelay(),0);float start=hero.cooldown();hero.spend(1);assertEquals(1,hero.cooldown()-start,0);assertTrue(stamina.desc().contains("50% faster"));assertFalse(stamina.desc().contains("unending"));
 }
 @Test public void masteryDescriptionMatchesActualPickerAndSavedStrengthReduction()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMastery potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMastery();java.lang.reflect.Field field=potion.getClass().getDeclaredField("itemSelector");field.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector selector=(com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector)field.get(potion);
  assertFalse(selector.itemSelectable(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow()));assertFalse(selector.itemSelectable(new RingOfMight()));assertFalse(selector.itemSelectable(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile()));
  for(com.shatteredpixel.shatteredpixeldungeon.items.Item item:new com.shatteredpixel.shatteredpixeldungeon.items.Item[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart(),new MailArmor()}){
   assertTrue(selector.itemSelectable(item));if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon){com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon weapon=(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon)item;int original=weapon.STRReq();weapon.masteryPotionBonus=true;assertEquals(original-2,weapon.STRReq());assertTrue(((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon)weapon.duplicate()).masteryPotionBonus);}else{Armor suit=(Armor)item;int original=suit.STRReq();suit.masteryPotionBonus=true;assertEquals(original-2,suit.STRReq());assertTrue(((Armor)suit.duplicate()).masteryPotionBonus);}assertFalse(selector.itemSelectable(item));
  }
 }
 @Test public void divineInspirationBonusIsTwoOncePerTierAndRequiresActualUnlocks(){
  for(int tier=1;tier<=4;tier++){
   hero.subClass=HeroSubClass.BERSERKER;Talent.initSubclassTalents(hero);hero.armorAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap();Talent.initArmorTalents(hero);int unlock=Talent.tierLevelThresholds[tier]-1;hero.lvl=unlock;int baseline=hero.talentPointsAvailable(tier);
   com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration.DivineInspirationTracker tracker=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration.DivineInspirationTracker.class);tracker.setBoosted(tier);assertEquals(2,hero.bonusTalentPoints(tier));assertEquals(baseline+2,hero.talentPointsAvailable(tier));tracker.setBoosted(tier);assertEquals(baseline+2,hero.talentPointsAvailable(tier));
   com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();tracker.storeInBundle(saved);com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration.DivineInspirationTracker restored=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration.DivineInspirationTracker();restored.restoreFromBundle(saved);for(int candidate=1;candidate<=4;candidate++)assertEquals(candidate==tier,restored.isBoosted(candidate));
   if(unlock>1){hero.lvl=unlock-1;assertEquals(0,hero.bonusTalentPoints(tier));assertEquals(0,hero.talentPointsAvailable(tier));}hero.lvl=30;if(tier==3){hero.subClass=HeroSubClass.NONE;assertEquals(0,hero.bonusTalentPoints(tier));}if(tier==4){hero.armorAbility=null;assertEquals(0,hero.bonusTalentPoints(tier));}tracker.detach();
  }
  com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration();potion.anonymize();assertTrue(potion.desc().contains("2 extra talent points"));assertTrue(potion.desc().contains("only once"));assertTrue(potion.desc().contains("must be unlocked"));
 }
 @Test public void aggressionDescriptionMatchesActualAlertVisibilityAndSleepingRules(){
  descriptionSightArena();DescriptionAmokedRat subject=new DescriptionAmokedRat();subject.pos=hero.pos+1;subject.fieldOfView=new boolean[Dungeon.level.length()];java.util.Arrays.fill(subject.fieldOfView,true);com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat marked=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();marked.pos=hero.pos+3;Dungeon.level.mobs.add(subject);Dungeon.level.mobs.add(marked);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(marked);
  com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression.Aggression status=Buff.affect(marked,com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression.Aggression.class,20);
  try{
   subject.state=subject.SLEEPING;assertSame(hero,subject.chooseNow());assertSame(subject.SLEEPING,subject.state);subject.rememberEnemy(null);subject.state=subject.WANDERING;assertSame(marked,subject.chooseNow());assertSame(subject.HUNTING,subject.state);
   subject.rememberEnemy(null);subject.state=subject.WANDERING;subject.fieldOfView[marked.pos]=false;assertSame(hero,subject.chooseNow());assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression().info().contains("alert enemies"));assertTrue(status.desc().contains("Alert enemies"));
  }finally{status.detach();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(marked);}
 }
 @Test public void amokExplanationMatchesActualTargetGroupPriorityAndVisibility(){
  descriptionSightArena();int center=hero.pos;hero.pos=center+2;DescriptionAmokedRat subject=new DescriptionAmokedRat();subject.pos=center;subject.fieldOfView=new boolean[Dungeon.level.length()];java.util.Arrays.fill(subject.fieldOfView,true);Dungeon.level.mobs.add(subject);
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat enemy=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();enemy.pos=center+8;Dungeon.level.mobs.add(enemy);
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat ally=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;ally.pos=center+1;Dungeon.level.mobs.add(ally);
  Amok rage=Buff.affect(subject,Amok.class,8);String detail=MechanicsDetails.buff(rage,rage.desc());assertTrue(detail.contains("does not choose randomly between friends and foes"));assertSame(enemy,subject.chooseNow());enemy.invisible=1;assertSame(ally,subject.chooseNow());ally.invisible=1;assertSame(hero,subject.chooseNow());hero.invisible=1;assertNull(subject.chooseNow());hero.invisible=0;ally.invisible=0;enemy.invisible=0;subject.fieldOfView[enemy.pos]=false;assertSame(ally,subject.chooseNow());assertEquals(com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY,subject.alignment);
 }
 @Test public void rageTalentNumbersMatchActualCapsAndAdditiveEnchantmentPower()throws Exception{
  Berserk rage=Buff.affect(hero,Berserk.class);java.lang.reflect.Field power=Berserk.class.getDeclaredField("power");power.setAccessible(true);ring(2);
  for(int rank=1;rank<=3;rank++){
   hero.talents.get(0).put(Talent.ENDLESS_RAGE,rank);hero.talents.get(0).put(Talent.ENRAGED_CATALYST,rank);power.setFloat(rage,0);rage.damage(hero.HT*40);
   assertEquals(1f+.1667f*rank,rage.rageFraction(),0);assertTrue(TalentExplanation.details(Talent.ENDLESS_RAGE,rank).contains(ObfuscationExplanation.number(100*rage.rageFraction())+"%"));
   for(float stored:new float[]{.25f,.8f,1.5f}){
    power.setFloat(rage,stored);float expected=RingOfArcana.enchantPowerMultiplier(hero)+(Math.min(1,stored)*.15f)*rank;
    assertEquals(expected,com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.genericProcChanceMultiplierReadOnly(hero),0);
    com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();rage.storeInBundle(before);String text=TalentExplanation.details(Talent.ENRAGED_CATALYST,rank);assertTrue(text.contains("added after Arcana"));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();rage.storeInBundle(after);assertEquals(before.toString(),after.toString());
   }
  }
 }
 @Test public void cleaveExplanationMatchesActualKillWindowAndOrdinaryHitMinimum(){
  for(int rank=1;rank<=3;rank++){
   hero.talents.get(0).put(Talent.CLEAVE,rank);PlainStatusTarget victim=new PlainStatusTarget();victim.HP=0;Combo kill=new Combo();kill.target=hero;kill.hit(victim);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();kill.storeInBundle(state);assertEquals(15f+15f*rank,state.getFloat("combotime"),0);assertTrue(TalentExplanation.details(Talent.CLEAVE,rank).contains(ObfuscationExplanation.number(state.getFloat("combotime"))+" game-time units"));
   victim.HP=100;Combo ordinary=new Combo();ordinary.target=hero;ordinary.hit(victim);ordinary.storeInBundle(state);assertEquals(5,state.getFloat("combotime"),0);
  }
 }
 @Test public void lethalDefenseExplanationMatchesActualRechargeCreditFloorAndActivation(){
  for(int rank=1;rank<=3;rank++){
   com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield seal=new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield();seal.target=hero;com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();seal.storeInBundle(state);state.put("cooldown",120);seal.restoreFromBundle(state);seal.reduceCooldown(rank/3f);seal.storeInBundle(state);assertEquals(120-50*rank,state.getInt("cooldown"));assertTrue(TalentExplanation.details(Talent.LETHAL_DEFENSE,rank).contains((50*rank)+" turns"));
   for(int i=0;i<20;i++)seal.reduceCooldown(rank/3f);seal.storeInBundle(state);assertEquals(-150,state.getInt("cooldown"));seal.activate();seal.storeInBundle(state);assertEquals(0,state.getInt("cooldown"));
  }
 }
 @Test public void comboDetailsMatchActualDecayAndPreserveInactiveHoldFastAndSavedState(){
  for(int rank=0;rank<=3;rank++)for(boolean samePosition:new boolean[]{false,true}){
   hero.talents.get(0).put(Talent.HOLD_FAST,rank);HoldFast hold=Buff.affect(hero,HoldFast.class);hold.pos=samePosition?hero.pos:hero.pos+1;
   Combo combo=new Combo();combo.target=hero;com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();combo.storeInBundle(state);state.put("combotime",10f);state.put("count",8);combo.restoreFromBundle(state);
   com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();combo.storeInBundle(before);String text=MechanicsDetails.buff(combo,combo.desc());assertTrue(text.contains("Time left: 10 turns at the normal decay rate"));assertTrue(text.contains("2 times normal damage"));assertSame(hold,hero.buff(HoldFast.class));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();combo.storeInBundle(after);assertEquals(before.toString(),after.toString());
   float expected=!samePosition||rank==0?1:rank==1?.5f:rank==2?.25f:0;assertTrue(text.contains("reduces the countdown by "+ObfuscationExplanation.number(expected)));combo.act();assertEquals(10-expected,combo.remainingComboTime(),0);if(hero.buff(HoldFast.class)!=null)hero.buff(HoldFast.class).detach();
  }
 }
 public static class DescriptionHungerHero extends Hero {int requestedDamage;@Override public void damage(int amount,Object source){requestedDamage+=amount;}}
 @Test public void hungerDamagePreviewMatchesActualStrictReserveThresholdAndDoesNotAdvanceState(){
  boolean intro=SPDSettings.intro();SPDSettings.intro(false);try{
   for(int maxHp:new int[]{20,100,1000,2500})for(float reserve:new float[]{0,.5f,1,1.25f}){
    DescriptionHungerHero target=new DescriptionHungerHero();target.HP=target.HT=maxHp;Hunger hunger=new Hunger();hunger.target=target;com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();hunger.storeInBundle(state);state.put("level",Hunger.STARVING);state.put("partialDamage",reserve);hunger.restoreFromBundle(state);
    com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();hunger.storeInBundle(before);int request=hunger.nextStarvationInput();String details=BuffExplanation.details(hunger);assertTrue(details.contains("Next unpaused starving turn: "+request+" base damage"));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();hunger.storeInBundle(after);assertEquals(before.toString(),after.toString());hunger.act();assertEquals(request,target.requestedDamage);assertEquals(reserve+maxHp/1000f-request,hunger.starvationDamageProgress(),0);
   }
  }finally{SPDSettings.intro(intro);}
 }
 @Test public void hungerExplanationMatchesFoodSatisfactionAndSaveLoadWithoutInventingReserve(){
  Hunger hunger=new Hunger();hunger.target=hero;com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();hunger.storeInBundle(state);state.put("level",450f);hunger.restoreFromBundle(state);hunger.satisfy(300);assertEquals(150,hunger.hungerPoints(),0);hunger.satisfy(300);assertEquals(0,hunger.hungerPoints(),0);assertEquals(0,hunger.nextStarvationInput());hunger.storeInBundle(state);Hunger loaded=new Hunger();loaded.target=hero;loaded.restoreFromBundle(state);assertEquals(BuffExplanation.details(hunger),BuffExplanation.details(loaded));assertTrue(BuffExplanation.details(loaded).contains("starving at 450 points"));
 }
 @Test public void foodNutritionDescriptionsMatchRealSatisfactionAcrossChallengeAndMealTypes()throws Exception{
  java.lang.reflect.Method satisfy=com.shatteredpixel.shatteredpixeldungeon.items.food.Food.class.getDeclaredMethod("satisfy",Hero.class);satisfy.setAccessible(true);
  com.shatteredpixel.shatteredpixeldungeon.items.food.Food[] meals={new com.shatteredpixel.shatteredpixeldungeon.items.food.Food(),new com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation(),new com.shatteredpixel.shatteredpixeldungeon.items.food.ChargrilledMeat()};
  for(int challenge:new int[]{0,Challenges.NO_FOOD})for(com.shatteredpixel.shatteredpixeldungeon.items.food.Food food:meals){
   Dungeon.challenges=challenge;Hunger hunger=Buff.affect(hero,Hunger.class);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();hunger.storeInBundle(state);state.put("level",450f);hunger.restoreFromBundle(state);float offered=FoodExplanation.nutritionBeforeHorn(food);String info=food.info();assertTrue(info,info.contains("Refills _"+ObfuscationExplanation.number(offered)+" hunger_"));assertEquals(450,hunger.hungerPoints(),0);satisfy.invoke(food,hero);assertEquals(Math.max(0,450-offered),hunger.hungerPoints(),0);
  }
 }
 @Test public void ordinaryMealsAreConciseAndSpecialFoodsExplainTheirActualRisks(){
  com.shatteredpixel.shatteredpixeldungeon.items.food.ChargrilledMeat steak=new com.shatteredpixel.shatteredpixeldungeon.items.food.ChargrilledMeat();
  String plain=steak.info();assertTrue(plain.contains("Refills _150 hunger_"));assertTrue(plain.contains("3 turns"));
  assertFalse(plain.contains("that many HP"));assertFalse(plain.contains("game-time units"));assertFalse(plain.contains("fully-fed limit"));
  assertTrue(plain.length()<300);
  String raw=new com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat().info();assertTrue(raw.contains("20%"));assertTrue(raw.contains("being poisoned for "+(hero.HT/5)+" turns"));
  String frozen=new com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio().info();assertTrue(frozen.contains("20% each"));assertTrue(frozen.contains("healing for "+(hero.HT/4)+" HP"));
  assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.food.Berry().info().contains("Every second berry"));
  assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation().info().contains("adds 1 charge"));
 }
 @Test public void foodTimePreviewFollowsExistingMealTalentsAndDoesNotRollOrEat(){
  com.shatteredpixel.shatteredpixeldungeon.items.food.Food food=new com.shatteredpixel.shatteredpixeldungeon.items.food.Food();com.shatteredpixel.shatteredpixeldungeon.items.food.Berry berry=new com.shatteredpixel.shatteredpixeldungeon.items.food.Berry();com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation supply=new com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation();assertEquals(3,food.previewEatingTime(),0);assertEquals(1,berry.previewEatingTime(),0);assertEquals(1,supply.previewEatingTime(),0);hero.talents.get(0).put(Talent.IRON_STOMACH,1);assertEquals(1,food.previewEatingTime(),0);assertEquals(0,berry.previewEatingTime(),0);assertEquals(0,supply.previewEatingTime(),0);
  Random.pushGenerator(128);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(128);float time=hero.cooldown();try{for(int i=0;i<20;i++){food.info();berry.info();supply.info();FoodExplanation.details(new com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat());}assertEquals(expected,Random.Int());}finally{Random.popGenerator();}assertEquals(time,hero.cooldown(),0);assertEquals(1,food.quantity());assertNull(hero.buff(Hunger.class));
  com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit raw=new com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit();assertTrue(FoodExplanation.summary(raw).contains("cannot be eaten"));assertFalse(FoodExplanation.summary(raw).contains("450 hunger"));
 }
 @Test public void momentumDescriptionMatchesActualRunPauseCooldownAndArmorContribution(){
  hero.lvl=9;hero.STR=armor.STRReq()+3;hero.talents.get(0).put(Talent.EVASIVE_ARMOR,2);
  for(int rank=0;rank<=3;rank++)for(int invisible:new int[]{0,1}){
   hero.talents.get(0).put(Talent.SPEEDY_STEALTH,rank);hero.invisible=invisible;Momentum momentum=Buff.affect(hero,Momentum.class);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();momentum.storeInBundle(state);state.put("freerun_turns",3);state.put("freerun_CD",20);momentum.restoreFromBundle(state);
   com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();momentum.storeInBundle(before);String text=BuffExplanation.details(momentum);assertTrue(text.contains("Freerunning turns left: _3_"));assertTrue(text.contains("Walking speed from this effect: _2× normal_"));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();momentum.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(10,momentum.evasionBonus(9,3));assertEquals(20,armor.evasionFactor(hero,10),0);momentum.act();assertEquals(invisible>0&&rank>=2?3:2,momentum.remainingRunUpdates());assertEquals(19,momentum.remainingCooldownUpdates());
  }
 }
 @Test public void momentumStationaryLossAndInvisibleSpeedDoNotPromiseFreeRunEvasion(){
  hero.talents.get(0).put(Talent.SPEEDY_STEALTH,3);hero.invisible=1;Momentum momentum=new Momentum();momentum.target=hero;com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();momentum.storeInBundle(state);state.put("stacks",10);state.put("freerun_CD",3);momentum.restoreFromBundle(state);assertEquals(2,momentum.speedMultiplier(),0);assertEquals(0,momentum.evasionBonus(20,10));assertTrue(BuffExplanation.details(momentum).contains("without granting freerunning's extra evasion"));momentum.act();assertEquals(7,momentum.storedStacks());assertEquals(2,momentum.remainingCooldownUpdates());
 }
 public static class DescriptionShadows extends Shadows {boolean ended;@Override public void detach(){ended=true;}}
 @Test public void shadowsExplanationTracksRefreshReplacementExpiryAndAdjacentEnemy(){
  descriptionSightArena();DescriptionShadows shadows=new DescriptionShadows();shadows.target=hero;shadows.prolong();shadows.prolong();assertEquals(2,shadows.remainingShadowUpdates(),0);String text=BuffExplanation.details(shadows);assertTrue(text.contains("two thirds"));assertTrue(text.contains("Refresh countdown: 2 updates"));assertFalse("Shadowmeld uses its own refresh timer",MechanicsDetails.buff(shadows,shadows.desc()).contains("_Duration remaining_"));assertEquals(2,shadows.remainingShadowUpdates(),0);shadows.act();assertEquals(1,shadows.remainingShadowUpdates(),0);assertFalse(shadows.ended);shadows.act();assertTrue(shadows.ended);
  shadows.ended=false;shadows.prolong();com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat enemy=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();enemy.pos=hero.pos+1;Dungeon.level.mobs.add(enemy);shadows.act();assertTrue(shadows.ended);
 }
 @Test public void ascentPressurePreviewMatchesActualDamageReserveAndBossException(){
  descriptionSightArena();int depth=Dungeon.depth;try{
   for(int floor:new int[]{4,5})for(float pressure:new float[]{7.5f,8,8.5f,10})for(float reserve:new float[]{0,.75f}){
    Dungeon.depth=floor;DescriptionHungerHero target=new DescriptionHungerHero();target.HP=target.HT=100;target.pos=hero.pos;Dungeon.hero=target;AscensionChallenge ascent=Buff.affect(target,AscensionChallenge.class);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();ascent.storeInBundle(state);state.put(AscensionChallenge.STACKS,pressure);state.put(AscensionChallenge.DAMAGE,reserve);ascent.restoreFromBundle(state);
    com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();ascent.storeInBundle(before);int request=ascent.nextPressureInput();assertTrue(BuffExplanation.details(ascent).contains("Next turn: "+request+" damage points"));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();ascent.storeInBundle(after);assertEquals(before.toString(),after.toString());ascent.act();assertEquals(request,target.requestedDamage);assertEquals(pressure<8||floor==5?0:reserve+(pressure-4)/4f-request,ascent.damageProgress(),0);ascent.detach();
   }
  }finally{Dungeon.depth=depth;Dungeon.hero=hero;}
 }
 @Test public void ascentMovementThresholdsMatchActualEnemyStateAndHeroRateCap(){
  AscensionChallenge ascent=Buff.affect(hero,AscensionChallenge.class);com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat enemy=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();
  for(float pressure:new float[]{0,2,3.5f,4,5.5f,6,8}){
   ascent.storeInBundle(state);state.put(AscensionChallenge.STACKS,pressure);ascent.restoreFromBundle(state);enemy.state=enemy.WANDERING;assertEquals(pressure>=4?2:1,AscensionChallenge.enemySpeedModifier(enemy),0);enemy.state=enemy.HUNTING;assertEquals(1,AscensionChallenge.enemySpeedModifier(enemy),0);enemy.state=enemy.FLEEING;assertEquals(1,AscensionChallenge.enemySpeedModifier(enemy),0);
   for(float speed:new float[]{.5f,1,3})assertEquals(pressure>=6?Math.min(speed/2,1):speed,AscensionChallenge.modifyHeroSpeed(speed),0);
  }
 }
 @Test public void ascentTakedownPressureCreditsMatchActualTypeRulesAndExclusions(){
  AscensionChallenge ascent=Buff.affect(hero,AscensionChallenge.class);com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob[] enemies={new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat(),new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon(),new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul(),new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha()};float[] credits={1,.5f,.5f,0};com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();
  for(int i=0;i<enemies.length;i++){ascent.storeInBundle(state);state.put(AscensionChallenge.STACKS,2f);ascent.restoreFromBundle(state);AscensionChallenge.processEnemyKill(enemies[i]);assertEquals(2-credits[i],ascent.pressure(),0);}
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat blocked=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();Buff.affect(blocked,AscensionChallenge.AscensionBuffBlocker.class);float before=ascent.pressure();AscensionChallenge.processEnemyKill(blocked);assertEquals(before,ascent.pressure(),0);
 }
 @Test public void monkAbilityDescriptionsDoNotCreateAnEnergyBuffJustByInspection(){
  assertNull(hero.buff(MonkEnergy.class));StringBuilder offenders=new StringBuilder();for(MonkEnergy.MonkAbility ability:MonkEnergy.MonkAbility.abilities){ability.desc();MonkEnergy created=hero.buff(MonkEnergy.class);if(created!=null){offenders.append(ability.getClass().getSimpleName()).append(";");created.detach();}}
  assertEquals("inspection must not attach a gameplay resource", "",offenders.toString());
 }
 @Test public void monkDescriptionUsesExistingEmpowermentStateAndPreservesEnergyAndRng(){
  MonkEnergy energy=Buff.affect(hero,MonkEnergy.class);hero.talents.get(0).put(Talent.MONASTIC_VIGOR,2);energy.energy=9.25f;com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();energy.storeInBundle(before);Random.pushGenerator(427);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(427);try{for(MonkEnergy.MonkAbility ability:MonkEnergy.MonkAbility.abilities)assertTrue(ability.desc().length()>0);assertTrue(BuffExplanation.details(energy).contains("_9.25/10_"));assertEquals(expected,Random.Int());}finally{Random.popGenerator();}com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();energy.storeInBundle(after);assertEquals(before.toString(),after.toString());assertTrue(energy.abilitiesEmpowered(hero));energy.energy=1;assertFalse(energy.abilitiesEmpowered(hero));assertFalse(new MonkEnergy.MonkAbility.Flurry().desc().contains("enchantment"));
 }
 @Test public void monkCapacityAndEnergyGainsMatchDescriptionWithAbilityOverflowAndSpending(){
  MonkEnergy energy=Buff.affect(hero,MonkEnergy.class);for(int level:new int[]{1,10,20,30}){hero.lvl=level;assertEquals(Math.max(10,5+level/2),energy.energyCap());assertTrue(BuffExplanation.details(energy).contains("/"+energy.energyCap()+"_"));}
  energy.energy=0;energy.gainEnergy(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat());assertEquals(1,energy.energy,0);energy.gainEnergy(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul());assertEquals(1.5f,energy.energy,0);energy.energy=energy.energyCap();energy.gainEnergy(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat());assertEquals(energy.energyCap(),energy.energy,0);
  MonkEnergy.MonkAbility.UnarmedAbilityTracker tracker=Buff.affect(hero,MonkEnergy.MonkAbility.UnarmedAbilityTracker.class);energy.gainEnergy(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat());assertEquals(energy.energyCap()+1,energy.energy,0);energy.abilityUsed(new MonkEnergy.MonkAbility.Flurry());assertEquals(energy.energyCap(),energy.energy,0);tracker.detach();energy.energy=3.5f;energy.act();assertEquals(3.5f,energy.energy,0);
 }
 @Test public void monkEquipmentEnergyBonusesMatchActualAdditiveTierAndStanceRules(){
  MonkEnergy energy=Buff.affect(hero,MonkEnergy.class);hero.belongings.armor=new ClothArmor();hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword();com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat enemy=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();
  for(int rank=1;rank<=3;rank++){hero.talents.get(0).put(Talent.UNENCUMBERED_SPIRIT,rank);energy.energy=0;energy.gainEnergy(enemy);assertEquals(rank==1?2:rank==2?2.5f:3,energy.energy,0);}
  RingOfForce.BrawlersStance stance=Buff.affect(hero,RingOfForce.BrawlersStance.class);stance.active=true;energy.energy=0;energy.gainEnergy(enemy);assertEquals(2,energy.energy,0);hero.belongings.armor=null;energy.energy=0;energy.gainEnergy(enemy);assertEquals(1,energy.energy,0);assertTrue(BuffExplanation.details(energy).contains("Empty equipment slots give no bonus"));
 }
 @Test public void monkUnarmedDamagePreviewDoesNotRevealHiddenMightLevel(){
  RingOfMight might=new RingOfMight();might.levelKnown=false;hero.belongings.ring=might;might.activate(hero);MonkEnergy.MonkAbility[] abilities={new MonkEnergy.MonkAbility.Flurry(),new MonkEnergy.MonkAbility.DragonKick()};for(MonkEnergy.MonkAbility ability:abilities){might.level(0);String hidden=ability.desc();might.level(9);assertEquals(hidden,ability.desc());assertTrue(hidden.contains("preview is unavailable"));assertNull(hero.buff(MonkEnergy.class));}
  might.anonymize();might.levelKnown=might.cursedKnown=true;assertTrue(new MonkEnergy.MonkAbility.Flurry().desc().contains(Integer.toString(Math.round(1.5f*(hero.STR()-8)))));assertTrue(new MonkEnergy.MonkAbility.DragonKick().desc().contains(Integer.toString(6*(hero.STR()-8))));
 }
 @Test public void monkLowStrengthDescriptionEndpointsMatchActualBasicUnarmedRolls(){
  Buff.affect(hero,MonkEnergy.MonkAbility.UnarmedAbilityTracker.class);
  for(int strength:new int[]{4,8,9,12}){hero.STR=strength;int maximum=Math.max(1,strength-8);String flurry=new MonkEnergy.MonkAbility.Flurry().desc(),kick=new MonkEnergy.MonkAbility.DragonKick().desc();assertTrue(flurry.contains("2-"+Math.round(1.5f*maximum)+" base damage"));assertTrue(kick.contains("6-"+(6*maximum)+" base damage"));
   Random.pushGenerator(121);try{for(int i=0;i<100;i++){int rolled=RingOfForce.damageRoll(hero);assertTrue(rolled>=1&&rolled<=maximum);assertTrue(Math.round(1.5f*rolled)>=2&&Math.round(1.5f*rolled)<=Math.round(1.5f*maximum));}}finally{Random.popGenerator();}
  }
 }
 @Test public void monkPrimaryDetailsCoverAllFiveAbilitiesAndPreserveStateAndRng(){
  MonkEnergy energy=Buff.affect(hero,MonkEnergy.class);energy.energy=9;hero.talents.get(0).put(Talent.MONASTIC_VIGOR,2);hero.HP=11;com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();energy.storeInBundle(before);Random.pushGenerator(13);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(13);try{String text=MonkAbilityExplanation.details(energy,hero);for(String name:new String[]{"_Flurry:","_Focus:","_Dash:","_Dragon Kick:","_Meditate:"})assertTrue(text.contains(name));assertTrue(text.contains("current range: 8 tiles"));assertTrue(text.contains("currently 2 HP before"));assertEquals(expected,Random.Int());}finally{Random.popGenerator();}com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();energy.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(11,hero.HP);assertNull(hero.buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class));
 }
 public static class DescriptionMonkHero extends Hero {int nextCalls;@Override public void next(){nextCalls++;}@Override public void interrupt(){}}
 @Test public void meditationDetailsMatchActualEmpoweredOnlyHealingResistanceTimeAndDelayedRecharge()throws Exception{
  java.util.HashSet<com.shatteredpixel.shatteredpixeldungeon.actors.Actor> original=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all();
  try{for(boolean empowered:new boolean[]{false,true})for(boolean disabled:new boolean[]{false,true}){
   DescriptionMonkHero target=new DescriptionMonkHero();target.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(target);target.talents.get(0).put(Talent.MONASTIC_VIGOR,2);target.HP=11;target.HT=20;target.sprite=quietSprite();Dungeon.hero=target;
   com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,disabled?0:100).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);
   MonkEnergy energy=Buff.affect(target,MonkEnergy.class);energy.energy=empowered?9:6;String text=MonkAbilityExplanation.details(energy,target);assertTrue(text.contains("Only empowered meditation"));assertEquals(disabled,text.contains("current rules prevent this healing"));float before=target.cooldown();new MonkEnergy.MonkAbility.Meditate().doAbility(target,null);
   assertEquals(5,target.cooldown()-before,0);assertEquals(empowered?4:1,energy.energy,0);assertEquals(empowered,target.buff(MonkEnergy.MonkAbility.Meditate.MeditateResistance.class)!=null);Healing healing=target.buff(Healing.class);assertEquals(empowered,healing!=null);if(healing!=null){assertEquals(2,healing.remainingHealingPool());healing.act();healing.act();assertEquals(disabled?11:13,target.HP);}else assertEquals(11,target.HP);
   assertNull(target.buff(Recharging.class));assertNull(target.buff(ArtifactRecharge.class));boolean delayed=false;
   for(com.shatteredpixel.shatteredpixeldungeon.actors.Actor actor:com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all())if(!original.contains(actor)&&actor.getClass().getEnclosingClass()==MonkEnergy.MonkAbility.Meditate.class){assertEquals(target.cooldown()-1,actor.cooldown(),0);java.lang.reflect.Method act=actor.getClass().getDeclaredMethod("act");act.setAccessible(true);assertTrue((Boolean)act.invoke(actor));delayed=true;}
   assertTrue(delayed);assertEquals(8,target.buff(Recharging.class).cooldown(),0);assertEquals(8,target.buff(ArtifactRecharge.class).left(),0);assertFalse(target.buff(ArtifactRecharge.class).ignoreHornOfPlenty);
   for(com.shatteredpixel.shatteredpixeldungeon.actors.Actor actor:com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all())if(!original.contains(actor))com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(actor);
  }}finally{for(com.shatteredpixel.shatteredpixeldungeon.actors.Actor actor:com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all())if(!original.contains(actor))com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(actor);Dungeon.hero=hero;}
 }
 @Test public void monkFocusActualSetupTimeEnergyAndReadyDefenseMatchExplanation(){
  try{for(boolean empowered:new boolean[]{false,true}){DescriptionMonkHero target=new DescriptionMonkHero();target.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(target);target.talents.get(0).put(Talent.MONASTIC_VIGOR,2);target.HP=target.HT=20;Dungeon.hero=target;MonkEnergy energy=Buff.affect(target,MonkEnergy.class);energy.energy=empowered?9:3;float start=target.cooldown();new MonkEnergy.MonkAbility.Focus().doAbility(target,null);assertEquals(empowered?0:1,target.cooldown()-start,0);assertEquals(empowered?7:1,energy.energy,0);assertEquals(1,target.nextCalls);assertNotNull(target.buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class));assertFalse(com.shatteredpixel.shatteredpixeldungeon.actors.Char.hit(new PlainStatusTarget(),target,false));target.defenseVerb();assertNull(target.buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class));}}
  finally{Dungeon.hero=hero;}
 }
 @Test public void monasticVigorDescriptionMatchesActualEightyPercentBoundary(){
  hero.talents.get(0).put(Talent.MONASTIC_VIGOR,2);MonkEnergy energy=Buff.affect(hero,MonkEnergy.class);energy.energy=8;assertEquals(10,energy.energyCap());assertFalse("preserve the actual original float comparison",energy.abilitiesEmpowered(hero));assertFalse("the default must not promise empowerment at exactly80%",Talent.MONASTIC_VIGOR.desc().contains("80% or more energy"));
 }
 @Test public void monasticVigorDetailedStatusMatchesExactOriginalRatioAndBoundary(){
  MonkEnergy energy=Buff.affect(hero,MonkEnergy.class);float[] nominal={1,.8f,.6f};for(int rank=1;rank<=3;rank++){hero.talents.get(0).put(Talent.MONASTIC_VIGOR,rank);energy.energy=10*nominal[rank-1];assertEquals(rank!=2,energy.abilitiesEmpowered(hero));String text=TalentExplanation.details(Talent.MONASTIC_VIGOR,rank);assertTrue(text.contains("comparison ratio: "+Float.toString(1.2f-.2f*rank)));assertTrue(text.contains("empowered now: "+(rank==2?"no":"yes")));energy.energy=Math.nextUp(energy.energy);assertTrue(energy.abilitiesEmpowered(hero));}
 }
 @Test public void combinedEnergyActualPairHandlersRefundOnceInEitherOrderAfterSaveLoad()throws Exception{
  java.lang.reflect.Method weaponUsed=com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.class.getDeclaredMethod("afterAbilityUsed",Hero.class);weaponUsed.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword();MonkEnergy energy=Buff.affect(hero,MonkEnergy.class);
  for(int rank=1;rank<=3;rank++)for(boolean weaponFirst:new boolean[]{false,true}){
   hero.talents.get(0).put(Talent.COMBINED_ENERGY,rank);if(hero.buff(Talent.CombinedEnergyAbilityTracker.class)!=null)hero.buff(Talent.CombinedEnergyAbilityTracker.class).detach();energy.energy=10;MonkEnergy.MonkAbility ability=rank==1?new MonkEnergy.MonkAbility.DragonKick():rank==2?new MonkEnergy.MonkAbility.Dash():new MonkEnergy.MonkAbility.Focus();if(weaponFirst)weaponUsed.invoke(weapon,hero);else energy.abilityUsed(ability);
   Talent.CombinedEnergyAbilityTracker first=hero.buff(Talent.CombinedEnergyAbilityTracker.class);assertNotNull(first);com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();first.storeInBundle(saved);first.detach();Talent.CombinedEnergyAbilityTracker loaded=new Talent.CombinedEnergyAbilityTracker();loaded.restoreFromBundle(saved);loaded.attachTo(hero);
   if(weaponFirst)energy.abilityUsed(ability);else weaponUsed.invoke(weapon,hero);assertEquals(10-ability.energyCost()+1,energy.energy,0);assertNull(hero.buff(Talent.CombinedEnergyAbilityTracker.class));assertTrue(TalentExplanation.details(Talent.COMBINED_ENERGY,rank).contains("at least "+(5-rank)+" energy"));
   energy.energy=10;weaponUsed.invoke(weapon,hero);energy.abilityUsed(new MonkEnergy.MonkAbility.Flurry());assertEquals(9,energy.energy,0);assertNotNull(hero.buff(Talent.CombinedEnergyAbilityTracker.class));hero.buff(Talent.CombinedEnergyAbilityTracker.class).detach();
  }
 }
 @Test public void weaponPassiveBlockDescriptionMatchesActualDefenseRollAndMissingStrength(){
  hero.belongings.armor=null;com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon[] weapons={new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield()};
  for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon weapon:weapons)for(int level:new int[]{-6,-3,0,3,9})for(int strength:new int[]{6,12,30}){
   weapon.levelKnown=weapon.cursedKnown=true;weapon.level(level);hero.STR=strength;hero.belongings.weapon=weapon;int missing=Math.max(0,weapon.STRReq()-strength),maximum=weapon.defenseFactor(hero);assertTrue(WeaponExplanation.blockSummary(weapon,hero).contains("0–"+Math.max(0,maximum-2*missing)+" damage"));
   for(int seed=0;seed<10;seed++){Random.pushGenerator(seed);int expected;try{Random.NormalIntRange(0,0);expected=Math.max(0,Random.NormalIntRange(0,maximum)-2*missing);}finally{Random.popGenerator();}Random.pushGenerator(seed);try{assertEquals(expected,hero.drRoll());}finally{Random.popGenerator();}}
  }
  RingOfForce force=new RingOfForce();hero.belongings.ring=force;force.activate(hero);RingOfForce.BrawlersStance stance=Buff.affect(hero,RingOfForce.BrawlersStance.class);stance.active=true;assertEquals(0,hero.drRoll());assertTrue(WeaponExplanation.blockDetails(weapons[4],hero).contains("Unarmed fighting rules can disable"));
 }
 @Test public void defensiveWeaponDescriptionsHaveOneAccurateBlockStatementWithoutEmptyParagraphs(){
  for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon weapon:new com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield()})for(boolean known:new boolean[]{false,true}){
   weapon.levelKnown=known;weapon.cursedKnown=true;hero.STR=5;String hidden=null;for(int level:new int[]{0,3,9}){weapon.level(level);String text=weapon.info();assertTrue(text,text.contains(WeaponExplanation.blockSummary(weapon,hero)));assertEquals(text,1,text.split("while wielded normally",-1).length-1);assertFalse(text,text.contains("This weapon blocks"));assertFalse(text,text.contains("\n\n\n"));if(!known){if(hidden!=null)assertEquals(hidden,text);hidden=text;}}
  }
 }
 @Test public void passiveBlockDescriptionDoesNotLeakUnknownWeaponOrMightUpgrades(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield shield=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield();shield.levelKnown=false;String unknown=WeaponExplanation.blockDetails(shield,hero);shield.level(9);assertEquals(unknown,WeaponExplanation.blockDetails(shield,hero));shield.levelKnown=true;RingOfMight might=new RingOfMight();might.levelKnown=false;hero.belongings.ring=might;might.activate(hero);String hiddenRing=WeaponExplanation.blockDetails(shield,hero);might.level(9);assertEquals(hiddenRing,WeaponExplanation.blockDetails(shield,hero));assertTrue(hiddenRing.contains("Personal penalties are withheld"));
 }
 @Test public void scrollEmpowerDetailsMatchActualUseCleanupAndMagicChargePriority()throws Exception{
  java.lang.reflect.Field current=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser");current.setAccessible(true);Object previous=current.get(null);try{
   DescriptionMonkHero target=new DescriptionMonkHero();target.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(target);target.HP=target.HT=20;Dungeon.hero=target;current.set(null,target);ScrollEmpower empower=Buff.affect(target,ScrollEmpower.class);empower.reset(3);empower.reset(1);assertEquals(3,empower.remainingUses());
   com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile source=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();source.level(5);source.levelKnown=source.cursedKnown=source.curChargeKnown=true;source.charge(target);com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile.MagicCharge magic=Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile.MagicCharge.class);magic.setup(source);
   com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.charge(target);wand.curCharges=2;java.lang.reflect.Field marker=magic.getClass().getDeclaredField("wandJustApplied");marker.setAccessible(true);for(int i=0;i<20;i++){assertTrue(BuffExplanation.details(empower).contains("Wand casts left: 3"));assertEquals("Source5 plus current Scroll Empower2",7,magic.level());assertTrue(magic.desc(),magic.desc().contains("+"+magic.level()));assertTrue(magic.desc(),magic.desc().contains("one zap"));}assertSame(source,marker.get(magic));assertEquals(3,empower.remainingUses());wand.wandUsed();assertNull(target.buff(com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile.MagicCharge.class));assertEquals(3,empower.remainingUses());wand.wandUsed();assertEquals(2,empower.remainingUses());assertEquals(2,target.nextCalls);
   com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();empower.storeInBundle(saved);ScrollEmpower loaded=new ScrollEmpower();loaded.restoreFromBundle(saved);assertEquals(2,loaded.remainingUses());assertEquals(BuffExplanation.details(empower),BuffExplanation.details(loaded));
  }finally{current.set(null,previous);Dungeon.hero=hero;}
 }
 @Test public void holdFastDetailsReportStalePositionWithoutDetachingOrRollingProtection(){
  hero.talents.get(0).put(Talent.HOLD_FAST,2);HoldFast hold=Buff.affect(hero,HoldFast.class);hold.pos=hero.pos;String active=BuffExplanation.details(hold);assertTrue(active.contains("currently active"));hero.pos++;Random.pushGenerator(715);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(715);try{for(int i=0;i<20;i++)assertTrue(BuffExplanation.details(hold).contains("currently inactive"));assertEquals(expected,Random.Int());}finally{Random.popGenerator();}assertSame(hold,hero.buff(HoldFast.class));hold.act();assertNull(hero.buff(HoldFast.class));
 }
 @Test public void heroicEnergyDescriptionFactorsPreserveActualChargeCostIncludingLegacyDefault(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap leap=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap();for(int rank=0;rank<=5;rank++){hero.talents.get(0).put(Talent.HEROIC_ENERGY,rank);float factor=rank==0?1:rank==2?.77f:rank==3?.68f:rank==4?.6f:.88f;assertEquals(leap.baseChargePoints()*factor,leap.chargeUse(hero),0);if(rank>0)assertTrue(TalentExplanation.details(Talent.HEROIC_ENERGY,rank).contains("by "+ObfuscationExplanation.number(factor)));}
 }
 @Test public void deathlessRecoveryDescriptionUsesLevelProgressAndDoesNotActivateBerserk(){
  Berserk rage=Buff.affect(hero,Berserk.class);for(int rank=1;rank<=3;rank++){hero.talents.get(0).put(Talent.DEATHLESS_FURY,rank);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();rage.storeInBundle(state);state.put("state","RECOVERING");state.put("levelrecovery",Berserk.baseLevelRecovery(rank));state.put("turn_recovery",0);rage.restoreFromBundle(state);String details=TalentExplanation.details(Talent.DEATHLESS_FURY,rank);assertTrue(details.contains((4-rank)+" hero-level equivalents"));int shield=rage.shielding();rage.recover(.25f);rage.storeInBundle(state);assertEquals(4-rank-.25f,state.getFloat("levelrecovery"),0);assertEquals(shield,rage.shielding());rage.recover(10);rage.storeInBundle(state);assertEquals(0,state.getFloat("levelrecovery"),0);assertEquals("NORMAL",state.getString("state"));}
 }
 @Test public void holdFastNormalTextMatchesActualClassCapsAndStaysPure(){
  Object[][] cases={
   {HeroClass.WARRIOR,1,null,"1–2",1,2},
   {HeroClass.WARRIOR,2,null,"2–4",2,4},
   {HeroClass.WARRIOR,3,new MailArmor(),"3–6",3,6},
   {HeroClass.MAGE,2,new LeatherArmor(),"2",2,2},
   {HeroClass.MAGE,3,new MailArmor().level(2),"3–5",3,5},
   {HeroClass.MAGE,2,null,"0",0,0},
   {HeroClass.MAGE,2,new ClothArmor().level(-3),"penalty",-2,-2}
  };
  for(Object[] example:cases){
   hero.heroClass=(HeroClass)example[0];int rank=(int)example[1];hero.talents.get(0).put(Talent.HOLD_FAST,rank);hero.belongings.armor=(Armor)example[2];if(hero.belongings.armor!=null)hero.belongings.armor.levelKnown=true;
   HoldFast hold=Buff.affect(hero,HoldFast.class);hold.pos=hero.pos;
   Random.pushGenerator(867);long next=Random.Long();Random.popGenerator();Random.pushGenerator(867);String normal;try{normal=hold.desc();assertEquals(next,Random.Long());}finally{Random.popGenerator();}
   String plain=normal.replace("_","");
   String expected=example[3].equals("penalty")?"reduce protection by 2":example[4].equals(example[5])?"Extra armor protection: "+example[3]+" damage":"Blocks "+example[3]+" extra damage";
   assertTrue(normal,plain.contains(expected));
   assertTrue(normal,normal.contains(rank==1?"fade at half speed":rank==2?"fade at quarter speed":"stop fading"));
   int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;Random.pushGenerator(337);try{for(int roll=0;roll<1000;roll++){int value=hold.armorBonus();min=Math.min(min,value);max=Math.max(max,value);}}finally{Random.popGenerator();}assertEquals(example[4],min);assertEquals(example[5],max);
   assertEquals(1f-(25+25*rank)/100f,HoldFast.buffDecayFactor(hero),0);
   hold.pos=hero.pos+1;com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();hold.storeInBundle(before);assertTrue(hold.desc().contains("You moved"));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();hold.storeInBundle(after);assertEquals(before.toString(),after.toString());assertSame(hold,hero.buff(HoldFast.class));hold.detach();
  }
  hero.heroClass=HeroClass.MAGE;hero.talents.get(0).put(Talent.HOLD_FAST,2);hero.belongings.armor=armor;armor.levelKnown=false;armor.level(0);HoldFast unknown=Buff.affect(hero,HoldFast.class);unknown.pos=hero.pos;String first=unknown.desc();assertTrue(first,first.contains("Identify your armor"));armor.level(9);assertEquals(first,unknown.desc());unknown.detach();
 }
 @Test public void evasiveArmorDescriptionMatchesActualHeroDefensePathAndHidesUnknownArmor(){
  hero.lvl=9;armor.level(3);hero.STR=armor.STRReq()+4;PlainStatusTarget attacker=new PlainStatusTarget();float baseline=hero.defenseSkill(attacker);Momentum momentum=Buff.affect(hero,Momentum.class);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();momentum.storeInBundle(state);state.put("freerun_turns",3);momentum.restoreFromBundle(state);
  for(int rank=1;rank<=3;rank++){hero.talents.get(0).put(Talent.EVASIVE_ARMOR,rank);assertEquals(baseline+4+4*rank,hero.defenseSkill(attacker),0);assertTrue(TalentExplanation.details(Talent.EVASIVE_ARMOR,rank).contains((4*rank)+" score points while freerunning"));}
  armor.levelKnown=false;armor.level(0);String hidden=TalentExplanation.details(Talent.EVASIVE_ARMOR,3);armor.level(9);assertEquals(hidden,TalentExplanation.details(Talent.EVASIVE_ARMOR,3));hero.belongings.armor=null;assertEquals(baseline,hero.defenseSkill(attacker),0);assertTrue(TalentExplanation.details(Talent.EVASIVE_ARMOR,3).contains("contribution is zero"));
 }
 @Test public void currentArmorTalentPanelsReuseAllNineteenReviewedAbilityContextsWithoutMutation()throws Exception{
  hero.belongings.weapon=describedStaff(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile(),3);java.util.List<com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility> abilities=new java.util.ArrayList<>();for(HeroClass type:HeroClass.values())java.util.Collections.addAll(abilities,type.armorAbilities());abilities.add(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify());int linked=0,slots=0;Random.pushGenerator(333);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(333);try{
   for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility ability:abilities){hero.armorAbility=ability;hero.talents.get(3).clear();for(Talent talent:ability.talents())hero.talents.get(3).put(talent,2);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();hero.storeInBundle(before);
    for(Talent talent:ability.talents())if(talent!=Talent.HEROIC_ENERGY){String text=TalentExplanation.details(talent,2);assertFalse(ability.getClass().getSimpleName()+"/"+talent,text.isEmpty());if(talent!=Talent.WILD_POWER){assertTrue(text.startsWith("_Current armor-ability rules"));assertTrue(text.endsWith(AbilityExplanation.effects(ability,hero)));linked++;}slots++;}
    com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();hero.storeInBundle(after);assertEquals(ability.getClass().getSimpleName(),before.toString(),after.toString());
   }assertEquals(19,abilities.size());assertEquals(57,slots);assertEquals(56,linked);assertEquals(expected,Random.Int());
  }finally{Random.popGenerator();}
 }
 @Test public void armorTalentBindingUsesCurrentUpgradeRanksAndAvoidsUnrelatedOrAlternateContext(){
  hero.armorAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap();hero.talents.get(3).put(Talent.BODY_SLAM,1);String first=TalentExplanation.details(Talent.BODY_SLAM,1);hero.talents.get(3).put(Talent.BODY_SLAM,4);assertNotEquals(first,TalentExplanation.details(Talent.BODY_SLAM,4));assertEquals("",TalentExplanation.details(Talent.SWIFT_SPIRIT,2));assertEquals("",TalentExplanation.details(Talent.BODY_SLAM,2,true));assertEquals("",TalentExplanation.details(Talent.BODY_SLAM,0));hero.armorAbility=null;assertEquals("",TalentExplanation.details(Talent.BODY_SLAM,2));
 }
 @Test public void clericTalentBindingsMatchActualLearnedSpellListAndPreserveStateAndRng(){
  descriptionSightArena();hero.heroClass=HeroClass.CLERIC;Talent.initClassTalents(hero);int linked=0;Random.pushGenerator(238);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(238);try{
   for(Talent talent:Talent.values()){com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell spell=SpellExplanation.spellForTalent(talent);if(spell==null)continue;for(java.util.LinkedHashMap<Talent,Integer> tier:hero.talents)tier.clear();hero.talents.get(0).put(talent,1);boolean found=false;for(int tier=1;tier<=4;tier++)found|=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell.getSpellList(hero,tier).contains(spell);assertTrue(talent.name(),found);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();hero.storeInBundle(before);String text=MechanicsDetails.talent(talent,1,talent.desc());assertTrue(text.contains("_Spell details_"));assertTrue(text.contains(SpellExplanation.details(spell,hero)));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();hero.storeInBundle(after);assertEquals(talent.name(),before.toString(),after.toString());hero.talents.get(0).put(talent,0);for(int tier=1;tier<=4;tier++)assertFalse(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell.getSpellList(hero,tier).contains(spell));linked++;}
   assertEquals(22,linked);assertEquals(expected,Random.Int());
  }finally{Random.popGenerator();}
 }
 @Test public void spellTalentBindingsRespectClassAlternateContextAndCurrentRankChanges(){
  hero.talents.get(0).put(Talent.HOLY_INTUITION,1);assertEquals("",SpellExplanation.talentDetails(Talent.HOLY_INTUITION,hero));hero.heroClass=HeroClass.CLERIC;String first=MechanicsDetails.talent(Talent.HOLY_INTUITION,1,Talent.HOLY_INTUITION.desc());assertTrue(first.contains("Tome charge cost: _3_"));hero.talents.get(0).put(Talent.HOLY_INTUITION,2);String next=MechanicsDetails.talent(Talent.HOLY_INTUITION,2,Talent.HOLY_INTUITION.desc());assertTrue(next.contains("Tome charge cost: _2_"));assertNotEquals(first,next);assertFalse(MechanicsDetails.talent(Talent.HOLY_INTUITION,2,Talent.HOLY_INTUITION.desc(),true).contains("_Spell details_"));
  hero.armorAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity();hero.talents.get(3).put(Talent.BODY_FORM,2);String body=MechanicsDetails.talent(Talent.BODY_FORM,2,Talent.BODY_FORM.desc());assertTrue(body.contains("_Current armor-ability rules"));assertTrue(body.contains("_Spell details_"));
 }
 @Test public void enhancedComboDescriptionMatchesActualParryRetentionBoundaries(){
  Combo combo=Buff.affect(hero,Combo.class);for(int rank=1;rank<=3;rank++)for(int count:new int[]{6,7,8,9,10}){hero.talents.get(2).put(Talent.ENHANCED_COMBO,rank);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();combo.storeInBundle(state);state.put("count",count);state.put("combotime",5f);combo.restoreFromBundle(state);Combo.ParryTracker parry=Buff.affect(hero,Combo.ParryTracker.class);String text=TalentExplanation.details(Talent.ENHANCED_COMBO,rank);assertTrue(text.contains("Current recorded combo: "+count));assertTrue(text.contains("would be "+(1+count/3)+" tiles"));assertFalse(parry.parried);hero.defenseVerb();assertTrue(parry.parried);assertEquals(rank>=2&&count>=9,hero.buff(Combo.ParryTracker.class)!=null);if(hero.buff(Combo.ParryTracker.class)!=null)hero.buff(Combo.ParryTracker.class).detach();assertSame(combo,hero.buff(Combo.class));}
 }
 @Test public void mealRechargeAndHasteDescriptionsMatchActualDurationsReplacementAndHornFlags()throws Exception{
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty();for(int rank=1;rank<=2;rank++){
   for(Buff b:new Buff[]{hero.buff(Recharging.class),hero.buff(ArtifactRecharge.class),hero.buff(Haste.class)})if(b!=null)b.detach();hero.talents.get(0).put(Talent.ENERGIZING_MEAL,rank);hero.talents.get(0).put(Talent.MYSTICAL_MEAL,rank);hero.talents.get(0).put(Talent.INVIGORATING_MEAL,rank);Talent.onFoodEaten(hero,1,null);Recharging wand=hero.buff(Recharging.class);ArtifactRecharge artifact=hero.buff(ArtifactRecharge.class);Haste haste=hero.buff(Haste.class);assertEquals(2+3*rank,wand.cooldown(),.0001f);assertEquals(1+2*rank,artifact.left(),0);assertEquals(.67f+rank,haste.cooldown(),.0001f);assertFalse(artifact.ignoreHornOfPlenty);assertTrue(TalentExplanation.details(Talent.ENERGIZING_MEAL,rank).contains("_"+(2+3*rank)+" turns of wand Recharging_"));assertTrue(TalentExplanation.details(Talent.MYSTICAL_MEAL,rank).contains("_"+(1+2*rank)+" turns of Artifact Recharging_"));
   Talent.onFoodEaten(hero,1,horn);assertFalse("equal offer preserves an existing different source",artifact.ignoreHornOfPlenty);artifact.detach();Talent.onFoodEaten(hero,1,horn);artifact=hero.buff(ArtifactRecharge.class);assertTrue("new Horn-sourced offer excludes Horn",artifact.ignoreHornOfPlenty);Talent.onFoodEaten(hero,1,null);assertTrue("equal ordinary offer does not replace the flag",artifact.ignoreHornOfPlenty);Buff.prolong(hero,Haste.class,20);Talent.onFoodEaten(hero,1,null);assertEquals(20,haste.cooldown(),.0001f);
  }
 }
 @Test public void satiatedSpellMealBenefitMatchesActualCastCleanupAndAlternateShieldDelay()throws Exception{
  descriptionSightArena();hero.sprite=quietSprite();for(int rank=1;rank<=2;rank++){
   hero.heroClass=HeroClass.CLERIC;hero.talents.get(0).put(Talent.SATIATED_SPELLS,rank);if(hero.buff(Barrier.class)!=null)hero.buff(Barrier.class).detach();Talent.onFoodEaten(hero,1,null);assertNotNull(hero.buff(Talent.SatiatedSpellsTracker.class));com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome();hero.belongings.artifact=tome;tome.directCharge(100);int charges=tome.currentCharges();com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.INSTANCE.onSpellCast(tome,hero);assertNull(hero.buff(Talent.SatiatedSpellsTracker.class));assertEquals(1+2*rank,hero.buff(Barrier.class).shielding());assertEquals(charges-1,tome.currentCharges());
   Talent.onFoodEaten(hero,1,null);com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.seed(hero.pos+1,20,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.LightWall.class);com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.INSTANCE.onCast(null,hero);assertNotNull("free wall cleanup does not consume the meal benefit",hero.buff(Talent.SatiatedSpellsTracker.class));hero.buff(Talent.SatiatedSpellsTracker.class).detach();
   hero.heroClass=HeroClass.WARRIOR;hero.buff(Barrier.class).detach();Talent.onFoodEaten(hero,1,null);Barrier barrier=hero.buff(Barrier.class);assertEquals(1+2*rank,barrier.shielding());assertTrue(barrier.cooldown()>=10);barrier.delay(20);float delay=barrier.cooldown();Talent.onFoodEaten(hero,1,null);assertEquals(delay,barrier.cooldown(),0);barrier.setShield(10+2*rank);Talent.onFoodEaten(hero,1,null);assertEquals(10+2*rank,barrier.shielding());assertEquals(delay,barrier.cooldown(),0);assertTrue(TalentExplanation.details(Talent.SATIATED_SPELLS,rank).contains("larger Barrier is left unchanged"));
  }
 }
 @Test public void farsightDescriptionMatchesActualRoundedFieldOfViewCapAndBlindness(){
  descriptionSightArena();boolean[] view=new boolean[Dungeon.level.length()];for(int rank=1;rank<=3;rank++){hero.talents.get(0).put(Talent.FARSIGHT,rank);Dungeon.level.updateFieldOfView(hero,view);int radius=Math.round(hero.viewDistance*(1+.25f*rank));assertTrue(view[hero.pos+radius]);assertFalse(view[hero.pos+radius+1]);String text=TalentExplanation.details(Talent.FARSIGHT,rank);assertTrue(text.contains(ObfuscationExplanation.number(1+.25f*rank)));assertTrue(text.contains("caps that radius at 20 tiles"));}
  Dungeon.level.setSize(49,49);java.util.Arrays.fill(Dungeon.level.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);Dungeon.level.buildFlagMaps();Dungeon.level.discoverable=new boolean[Dungeon.level.length()];java.util.Arrays.fill(Dungeon.level.discoverable,true);hero.pos=24*49+24;hero.viewDistance=20;view=new boolean[Dungeon.level.length()];Dungeon.level.updateFieldOfView(hero,view);assertTrue(view[hero.pos+20]);assertFalse(view[hero.pos+21]);Buff.affect(hero,Blindness.class,5);Dungeon.level.updateFieldOfView(hero,view);assertFalse(view[hero.pos+2]);assertTrue(TalentExplanation.details(Talent.FARSIGHT,3).contains("disable this normal distant-sight path"));
 }
 @Test public void obfuscationDetectionDetailsMatchActualSleepingAndWanderingChecks()throws Exception{
  descriptionSightArena();com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat observer=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();
  java.lang.reflect.Method sleeping=observer.SLEEPING.getClass().getDeclaredMethod("detectionChance",com.shatteredpixel.shatteredpixeldungeon.actors.Char.class),wandering=observer.WANDERING.getClass().getDeclaredMethod("detectionChance",com.shatteredpixel.shatteredpixeldungeon.actors.Char.class);sleeping.setAccessible(true);wandering.setAccessible(true);
  for(int level:new int[]{0,3,9})for(int ringLevel:new int[]{0,3}){
   armor.level(level);RingOfArcana arcana=ring(ringLevel);String details=ObfuscationExplanation.details(armor,hero);assertTrue(details.contains("distance in tiles + your stealth points"));assertTrue(details.contains("distance in tiles / 2 + your stealth points"));assertTrue(details.contains("separate enemy checks"));assertTrue(details.contains("Alerts, damage, invisibility"));
   for(int distance:new int[]{1,2,5}){
    observer.pos=hero.pos+distance;float stealth=hero.stealth();assertEquals(Obfuscation.stealthBoost(hero,hero.glyphLevel(Obfuscation.class)),stealth,0);
    assertEquals(1f/(observer.distance(hero)+stealth),(Float)sleeping.invoke(observer.SLEEPING,hero),.000001f);assertEquals(1f/(observer.distance(hero)/2f+stealth),(Float)wandering.invoke(observer.WANDERING,hero),.000001f);
   }
   for(Ring.RingBuff buff:new java.util.ArrayList<>(hero.buffs(Ring.RingBuff.class)))buff.detach();hero.belongings.ring=null;
  }
 }
 @Test public void mailPlusThreeShowsPracticalDetectionChanceWithoutTechnicalPanel(){armor.level(3);RingOfArcana ring=ring(3);float actual=Obfuscation.stealthBoost(hero,hero.glyphLevel(Obfuscation.class));String text=armor.info();assertTrue(text,text.contains("sleeping enemy 3 tiles away"));assertTrue(text,text.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.#",100f/(3f+actual))+"%"));assertFalse(text,text.contains("2.35 stealth"));assertFalse(text,text.contains("Effective armor level:"));assertEquals("",MechanicsDetails.itemCalculations(armor));}
 @Test public void upgradesAndArcanaRefreshBothReadableValuesAndDetailedResults(){armor.level(0);RingOfArcana ring=ring(0);String first=ObfuscationExplanation.details(armor,hero);armor.level(6);ring.upgrade();String next=ObfuscationExplanation.details(armor,hero);assertNotEquals(first,next);assertTrue(next,next.contains(ObfuscationExplanation.number(Obfuscation.stealthBoost(hero,hero.glyphLevel(Obfuscation.class)))));assertTrue(next,next.contains("3 stealth points"));}
 @Test public void enhancedRingsAndProtectiveAuraMatchLiveCalculation(){armor.level(3);ring(1);Buff.affect(hero,EnhancedRings.class);Buff.affect(hero,AuraOfProtection.AuraBuff.class);float actual=Obfuscation.stealthBoost(hero,hero.glyphLevel(Obfuscation.class));assertTrue(ObfuscationExplanation.summary(armor,hero).contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.#",100f/(3f+actual))+"%"));assertEquals("",MechanicsDetails.itemCalculations(armor));}
 @Test public void unknownArmorDoesNotRevealItsHiddenUpgradeThroughEitherSection(){ring(3);armor.levelKnown=false;armor.level(0);String plain=ObfuscationExplanation.summary(armor,hero),detail=ObfuscationExplanation.details(armor,hero);armor.level(9);assertEquals(plain,ObfuscationExplanation.summary(armor,hero));assertEquals(detail,ObfuscationExplanation.details(armor,hero));assertTrue(plain.contains("unaware enemies"));assertFalse(plain.contains("sleeping enemy 3 tiles away"));assertEquals("",MechanicsDetails.itemCalculations(armor));}
 @Test public void unknownRingDoesNotRevealIdentityOrLevelViaTheNumericalPreview(){armor.level(3);RingOfArcana ring=ring(0);ring.levelKnown=false;String first=ObfuscationExplanation.summary(armor,hero);ring.level(9);assertEquals(first,ObfuscationExplanation.summary(armor,hero));RingOfHaste other=new RingOfHaste();other.levelKnown=false;hero.belongings.ring=other;assertEquals(first,ObfuscationExplanation.summary(armor,hero));assertFalse(first.contains("3.812"));}
 @Test public void magicImmunityExplainsSuppressionRatherThanClaimingAnActiveBenefit(){armor.level(3);ring(2);Buff.affect(hero,MagicImmune.class);assertEquals(0,Obfuscation.stealthBoost(hero,hero.glyphLevel(Obfuscation.class)),0);assertTrue(ObfuscationExplanation.summary(armor,hero).contains("disables this glyph"));assertFalse(ObfuscationExplanation.summary(armor,hero).contains("2 stealth points"));}
 @Test public void negativeArmorLevelsMatchTheActualGlyphLevelSentinel(){for(int level=-5;level<=0;level++){armor.level(level);float expected=Obfuscation.stealthBoost(hero,hero.glyphLevel(Obfuscation.class));assertEquals(expected,ObfuscationExplanation.base(Math.max(-1,armor.buffedLvl())),0);if(level<0)assertTrue(ObfuscationExplanation.summary(armor,hero).contains("adds no stealth"));}}
 @Test public void detectionDetailsDistinguishConditionsAndDoNotInventAFixedPercentage(){String details=ObfuscationExplanation.details(armor,hero);assertTrue(details.contains("standard sleeping"));assertTrue(details.contains("standard wandering"));assertTrue(details.contains("distance in tiles / 2"));assertTrue(details.contains("visibility"));assertTrue(details.contains("No fixed detection percentage"));}
 @Test public void repeatedExplanationQueriesPreserveRngTimeHealthAndInventory(){armor.level(3);RingOfArcana ring=ring(2);float time=hero.cooldown();int hp=hero.HP,level=ring.level(),quantity=armor.quantity();Random.pushGenerator(991);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(991);try{for(int i=0;i<100;i++){armor.info();MechanicsDetails.item(armor,armor.info());}assertEquals(expected,Random.Int());}finally{Random.popGenerator();}assertEquals(time,hero.cooldown(),0);assertEquals(hp,hero.HP);assertEquals(level,ring.level());assertEquals(quantity,armor.quantity());}
 @Test public void reviewedBuffDetailsKeepSingularCountdownsReadableWithoutChangingState(){
  Poison poison=new Poison();poison.set(1);ArtifactRecharge recharge=new ArtifactRecharge().set(1);
  Combo combo=new Combo();com.watabou.utils.Bundle b=new com.watabou.utils.Bundle();combo.storeInBundle(b);b.put("combotime",1f);combo.restoreFromBundle(b);
  for(Buff effect:new Buff[]{poison,recharge,combo}){com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();effect.storeInBundle(before);String text=MechanicsDetails.buff(effect,"");assertTrue(text,text.contains("1 turn"));assertFalse(text,text.contains("1 turns"));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();effect.storeInBundle(after);assertEquals(before.toString(),after.toString());}
 }
 @Test public void shieldingAndDurationDetailsReadLivePublicState(){Barrier barrier=new Barrier();barrier.setShield(17);assertTrue(MechanicsDetails.buff(barrier,"shielding for several turns").contains("17 shield points"));barrier.setShield(23);assertTrue(MechanicsDetails.buff(barrier,"shielding").contains("23 shield points"));}
 @Test public void barrierDecayExplanationTracksProgressAndDoesNotDetachInactiveHoldFast(){
  Barrier barrier=new Barrier();barrier.setShield(10);barrier.attachTo(hero);HoldFast hold=Buff.affect(hero,HoldFast.class);hold.pos=hero.pos+1;
  com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();barrier.storeInBundle(before);String text=MechanicsDetails.buff(barrier,barrier.desc());assertTrue(text,text.contains("adds 50% each turn"));assertSame("reading does not invoke the mutating HoldFast helper",hold,hero.buff(HoldFast.class));
  com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();barrier.storeInBundle(after);assertEquals(before.toString(),after.toString());
  barrier.act();assertEquals(.5f,barrier.decayProgress(),0);assertTrue(MechanicsDetails.buff(barrier,barrier.desc()).contains("50%"));barrier.act();assertEquals(9,barrier.shielding());
 }
 @Test public void poisonDetailsReadLiveScheduledDamageWithoutChangingSavedState(){
  Poison poison=new Poison();poison.set(9);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();poison.storeInBundle(before);String details=MechanicsDetails.buff(poison,poison.desc());assertTrue(details,details.contains(poison.apdNextInput()+" damage points"));assertTrue(details.contains("9 turns"));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();poison.storeInBundle(after);assertEquals(before.toString(),after.toString());
 }
 @Test public void protectiveBuffDescriptionsDefineDamageUnitsAndDoNotClaimPercentReduction(){
  Barkskin bark=new Barkskin();bark.set(7,1);assertTrue(bark.desc(),bark.desc().contains("_0–7 damage_"));assertTrue(bark.desc().contains("only the strongest"));
  ArcaneArmor arcane=new ArcaneArmor();arcane.set(9,1);assertTrue(arcane.desc(),arcane.desc().contains("_0–9 damage_"));
 }
 @Test public void conciseDefensiveBuffDescriptionsFollowActualDecayAndSaveRoundTrip(){
  for(int interval:new int[]{1,5}){
   Barkskin bark=Buff.append(hero,Barkskin.class);bark.set(7,interval);String before=savedDescriptionHero();assertTrue(bark.desc().contains("_0–7 damage_"));assertEquals(before,savedDescriptionHero());bark.act();assertEquals(6,bark.level());assertTrue(bark.desc().contains("_0–6 damage_"));com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();bark.storeInBundle(saved);Barkskin copy=new Barkskin();copy.restoreFromBundle(saved);assertEquals(bark.desc(),copy.desc());assertEquals(interval,copy.decayInterval());bark.detach();
   ArcaneArmor arcane=Buff.affect(hero,ArcaneArmor.class);arcane.set(9,interval);before=savedDescriptionHero();assertTrue(arcane.desc().contains("_0–9 damage_"));assertEquals(before,savedDescriptionHero());arcane.act();assertEquals(8,arcane.level());assertTrue(arcane.desc().contains("_0–8 damage_"));saved=new com.watabou.utils.Bundle();arcane.storeInBundle(saved);ArcaneArmor restored=new ArcaneArmor();restored.restoreFromBundle(saved);assertEquals(arcane.desc(),restored.desc());assertEquals(interval,restored.decayInterval());arcane.detach();
  }
 }
 @Test public void liveContestPercentDescriptionsPreserveAllExistingTranslationPlaceholders(){
  com.shatteredpixel.shatteredpixeldungeon.messages.Languages previous=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang();
  try{for(com.shatteredpixel.shatteredpixeldungeon.messages.Languages language:com.shatteredpixel.shatteredpixeldungeon.messages.Languages.values()){
   com.shatteredpixel.shatteredpixeldungeon.messages.Messages.setup(language);
   for(Buff buff:new Buff[]{new Bless(),new Hex(),new Daze()}){String text=buff.desc();assertFalse(language+" "+buff.getClass().getSimpleName()+": "+text,text.isEmpty()||text.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.NO_TEXT_FOUND)||text.contains("!!!")||text.contains("%1$s")||text.contains("%2$s"));}
  }}finally{com.shatteredpixel.shatteredpixeldungeon.messages.Messages.setup(previous);}
 }
 @Test public void blessHexAndDazeExplainScoresInsteadOfDirectHitProbability(){
  for(Buff buff:new Buff[]{new Bless(),new Hex(),new Daze()}){String desc=buff.desc();assertTrue(desc,desc.contains("accuracy and evasion"));assertTrue(desc,desc.contains("final chances depend on your enemy"));}
 }
 @Test public void ordinaryAccuracyExplanationDoesNotConfuseScoresWithHitPercentages(){String text=MechanicsDetails.glossary("accuracy and evasion");assertTrue(text.contains("not hit/dodge percentages"));assertTrue(text.contains("1 − evasion / (2 × accuracy)"));assertTrue(text.contains("accuracy / (2 × evasion)"));}
 @Test public void identifiedRingDetailsFollowLiveUpgradeAndTemporaryBonuses(){
  RingOfAccuracy r=new RingOfAccuracy();r.anonymize();r.levelKnown=r.cursedKnown=true;r.level(0);hero.belongings.ring=r;r.activate(hero);
  String first=RingExplanation.details(r);assertTrue(first.contains(ObfuscationExplanation.number(RingOfAccuracy.accuracyMultiplier(hero))));
  r.upgrade();Buff.affect(hero,EnhancedRings.class);String next=RingExplanation.details(r);
  assertNotEquals(first,next);assertTrue(next,next.contains("× "+ObfuscationExplanation.number(RingOfAccuracy.accuracyMultiplier(hero))));
 }
 @Test public void unidentifiedRingDetailsDoNotRevealTypeOrHiddenLevel(){
  Ring[] rings={new RingOfAccuracy(),new RingOfArcana(),new RingOfForce(),new RingOfTenacity()};String expected=null;
  for(Ring r:rings){r.levelKnown=false;for(int level:new int[]{0,3,9}){r.level(level);String actual=RingExplanation.details(r);if(expected==null)expected=actual;assertEquals(expected,actual);}}
 }
 @Test public void tenacityUpgradePreviewStatesMoreOrLessDamageInsteadOfNegativeResistance(){
  RingOfTenacity ring=new RingOfTenacity();ring.anonymize();ring.levelKnown=ring.cursedKnown=true;
  for(boolean cursed:new boolean[]{false,true})for(int level:new int[]{0,1,3,6}){
   ring.cursed=cursed;ring.level(level);int bonus=ring.soloBonus();float factor=RingOfTenacity.damageMultiplier(bonus,1);String preview=ring.upgradeStat1(level);String amount=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("#.##",Math.abs(100*(1-factor)));assertEquals(amount+"% "+(factor>1?"more":"less"),preview);assertFalse(preview.startsWith("-"));
  }
 }
 @Test public void tenacityDetailsUseCurrentHealthAndDoNotTreatMaximumAsCurrent(){
  RingOfTenacity r=new RingOfTenacity();r.anonymize();r.levelKnown=r.cursedKnown=true;r.level(3);hero.belongings.ring=r;r.activate(hero);hero.HP=hero.HT;
  String full=RingExplanation.details(r);hero.HP=hero.HT/2;String half=RingExplanation.details(r);
  assertNotEquals(full,half);assertTrue(half,half.contains("× "+ObfuscationExplanation.number(RingOfTenacity.damageMultiplier(hero))));assertTrue(half.contains("not your reduction on every hit"));
 }
 @Test public void tenacityRetainedDetailsShowCurrentProtectionOrCurseAcrossHealthAndBonuses(){
  RingOfTenacity ring=new RingOfTenacity();ring.anonymize();ring.levelKnown=ring.cursedKnown=true;hero.belongings.ring=ring;ring.activate(hero);
  for(boolean cursed:new boolean[]{false,true})for(int level:new int[]{0,3,8})for(int hp:new int[]{1,hero.HT/2,hero.HT}){
   ring.cursed=cursed;ring.level(level);hero.HP=hp;String before=savedDescriptionHero(),text=RingExplanation.details(ring);float actual=RingOfTenacity.damageMultiplier(hero);String amount=ObfuscationExplanation.number(Math.abs(100*(1-actual)))+"% "+(actual>1?"more":"less")+" damage";assertTrue(text,text.contains(amount));assertTrue(text.contains("0.85^(ring value × fraction of HP missing)"));assertTrue(text,text.contains(ring.soloBuffedBonus()>0?"Damage reduction grows":ring.soloBuffedBonus()<0?"Extra damage taken grows":"currently has no effect on damage"));assertFalse(text.contains("Cursed rings increase damage instead"));if(ring.soloBuffedBonus()==0)assertFalse(text.contains("only near zero HP"));assertEquals(before,savedDescriptionHero());
  }
 }
 @Test public void forceRetainedStanceBonusMatchesActualSeededDamageDifference(){
  RingOfForce ring=new RingOfForce();ring.anonymize();ring.levelKnown=ring.cursedKnown=true;hero.belongings.ring=ring;ring.activate(hero);RingOfForce.BrawlersStance stance=Buff.affect(hero,RingOfForce.BrawlersStance.class);
  for(int strength:new int[]{8,10,18,22})for(boolean cursed:new boolean[]{false,true})for(int level:new int[]{0,3,8}){
   hero.STR=strength;ring.cursed=cursed;ring.level(level);stance.active=false;Random.pushGenerator(47);int ordinary;try{ordinary=RingOfForce.damageRoll(hero);}finally{Random.popGenerator();}stance.active=true;Random.pushGenerator(47);int boosted;try{boosted=RingOfForce.damageRoll(hero);}finally{Random.popGenerator();}String before=savedDescriptionHero(),text=RingExplanation.details(ring);assertTrue(text,text.contains("_"+(boosted-ordinary)+" extra damage_"));assertEquals(before,savedDescriptionHero());
  }
 }
 @Test public void magicImmuneRingDetailsDoNotClaimAnActiveBonus(){RingOfHaste r=new RingOfHaste();r.anonymize();r.levelKnown=r.cursedKnown=true;Buff.affect(hero,MagicImmune.class);assertTrue(RingExplanation.details(r).contains("not active benefits"));}
 @Test public void tinyPositiveMultipliersNeverDisplayAsZero(){assertNotEquals("0",ObfuscationExplanation.number(.00001f));assertFalse(ObfuscationExplanation.number(Float.POSITIVE_INFINITY).contains("Infinity"));}
 @Test public void forceKnownArmedBonusKeepsEnhancedRingsWhenUnknownStrengthWithholdsUnarmedRange(){
  RingOfForce force=new RingOfForce();force.anonymize();force.level(3);force.levelKnown=force.cursedKnown=true;hero.belongings.ring=force;force.activate(hero);RingOfMight unknown=new RingOfMight();unknown.levelKnown=false;hero.belongings.misc=unknown;Buff.affect(hero,EnhancedRings.class);
  String text=force.info();assertTrue(text,text.contains("Melee weapon damage bonus: _"+RingOfForce.armedDamageBonus(hero)+"_."));assertTrue(text.contains("Identify your equipped rings to see unarmed damage"));unknown.level(9);assertEquals(text,force.info());assertEquals(5,RingOfForce.armedDamageBonus(hero));
 }
 @Test public void forceDetailsUseActualStrengthTierAndCombatEndpointsWithoutRolling(){
  RingOfForce r=new RingOfForce();r.anonymize();r.levelKnown=r.cursedKnown=true;r.level(3);hero.belongings.ring=r;r.activate(hero);
  for(int strength:new int[]{8,10,18,22}){hero.STR=strength;String detail=RingExplanation.details(r);int b=r.soloBuffedBonus();assertTrue(detail,detail.contains(RingOfForce.unarmedMinimum(hero.STR(),b)+"–"+RingOfForce.unarmedMaximum(hero.STR(),b)+" damage points"));
   Random.pushGenerator(35);try{for(int j=0;j<50;j++){int actual=RingOfForce.damageRoll(hero);assertTrue(actual>=RingOfForce.unarmedMinimum(hero.STR(),b));assertTrue(actual<=RingOfForce.unarmedMaximum(hero.STR(),b));}}finally{Random.popGenerator();}
  }
 }
 @Test public void stylusDetailsFollowActualPoolWeightsAndActionCostWithoutRolling(){
  com.shatteredpixel.shatteredpixeldungeon.items.Stylus stylus=new com.shatteredpixel.shatteredpixeldungeon.items.Stylus();
  float[] weights=Armor.Glyph.typeChances;float[] saved=weights.clone();
  try{
   Random.pushGenerator(214);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(214);
   try{String ordinary=UtilityItemExplanation.details(stylus,hero);assertTrue(ordinary.contains(ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.items.Stylus.inscriptionTime())+" game-time units"));assertTrue(ordinary.contains("common 50%"));assertTrue(ordinary.contains("uncommon 40%"));assertTrue(ordinary.contains("rare 10%"));assertTrue(ordinary.contains("12.5% per glyph"));assertTrue(ordinary.contains("not a glyph's chance to trigger"));assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
   weights[0]=weights[1]=0;weights[2]=100;
   String changed=UtilityItemExplanation.details(stylus,hero);assertTrue(changed.contains("rare 100%"));
   Random.pushGenerator(216);try{for(int draw=0;draw<100;draw++){Armor.Glyph actual=Armor.Glyph.random();assertTrue(java.util.Arrays.asList(Armor.Glyph.rare).contains(actual.getClass()));}}finally{Random.popGenerator();}
  }finally{System.arraycopy(saved,0,weights,0,weights.length);}
 }
 @Test public void ordinaryInscriptionExcludesExistingGlyphWithoutChangingArmorLevel(){
  Armor target=new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor();target.level(3);target.levelKnown=target.cursedKnown=true;
  Random.pushGenerator(217);
  try{for(int draw=0;draw<100;draw++){Class<?> previous=target.glyph==null?null:target.glyph.getClass();target.inscribe();assertNotEquals(previous,target.glyph.getClass());assertEquals(3,target.level());}}finally{Random.popGenerator();}
  String text=UtilityItemExplanation.details(new com.shatteredpixel.shatteredpixeldungeon.items.Stylus(),hero);assertTrue(text.contains("excludes the selected armor's existing glyph"));assertTrue(text.contains("does not raise armor upgrade level"));
 }
 @Test public void torchDetailsExplainActualAdditiveLightWithoutAdvancingIt(){
  descriptionSightArena();
  com.shatteredpixel.shatteredpixeldungeon.items.Torch torch=new com.shatteredpixel.shatteredpixeldungeon.items.Torch();
  for(int base:new int[]{2,8}){Dungeon.level.viewDistance=base;hero.viewDistance=base;Light light=Buff.affect(hero,Light.class,30);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();light.storeInBundle(before);String heroBefore=savedDescriptionHero();String text=UtilityItemExplanation.details(torch,hero);assertTrue(text,text.contains("adds "+ObfuscationExplanation.number(Light.DURATION)+" game-time units"));assertTrue(text,text.contains("Repeated torches extend that time"));light.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(heroBefore,savedDescriptionHero());float old=light.cooldown();assertSame(light,Buff.affect(hero,Light.class,Light.DURATION));assertEquals(old+Light.DURATION*hero.resist(Light.class),light.cooldown(),.0001f);assertEquals(Math.max(base,Light.DISTANCE),hero.viewDistance);light.detach();assertEquals(base,hero.viewDistance);}
 }
 @Test public void recoveryDescriptionsCannotRestoreInventoryOrReviveTheHero(){
  Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory.class);com.shatteredpixel.shatteredpixeldungeon.items.LostBackpack pack=new com.shatteredpixel.shatteredpixeldungeon.items.LostBackpack();com.shatteredpixel.shatteredpixeldungeon.items.Ankh ankh=new com.shatteredpixel.shatteredpixeldungeon.items.Ankh();hero.HP=1;String heroBefore=savedDescriptionHero();
  Random.pushGenerator(434);int next=Random.Int();Random.popGenerator();Random.pushGenerator(434);try{String recovery=UtilityItemExplanation.details(pack,hero);assertTrue(recovery,recovery.contains("base pickup time is "+ObfuscationExplanation.number(pack.pickupDelay())+" game-time unit"));String revival=UtilityItemExplanation.details(ankh,hero);assertTrue(revival,revival.contains("two item slots"));assertTrue(revival,revival.contains("locked floor is regenerated"));assertTrue(revival,revival.contains("Floor means round down"));assertEquals(next,Random.Int());}finally{Random.popGenerator();}
  assertEquals(heroBefore,savedDescriptionHero());assertEquals(1,hero.HP);assertNotNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory.class));assertFalse(ankh.isBlessed());
 }
 @Test public void progressionItemDetailsExplainRealTimeAndChargeWithoutChoosingAnything(){
  for(com.shatteredpixel.shatteredpixeldungeon.items.Item item:new com.shatteredpixel.shatteredpixeldungeon.items.Item[]{new com.shatteredpixel.shatteredpixeldungeon.items.TengusMask(),new com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown()}){
   String heroBefore=savedDescriptionHero();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();item.storeInBundle(before);Random.pushGenerator(430);int next=Random.Int();Random.popGenerator();Random.pushGenerator(430);
   try{String text=UtilityItemExplanation.details(item,hero);assertTrue(text,text.contains(ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.TICK)+" game-time unit"));if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown){assertTrue(text,text.contains("starts with "+ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor.initialUpgradeCharge())+" charge points"));assertTrue(text,text.contains("unknown upgrade level stays unknown"));}assertEquals(next,Random.Int());}finally{Random.popGenerator();}
   item.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(heroBefore,savedDescriptionHero());assertEquals(UtilityItemExplanation.details(item,hero),UtilityItemExplanation.details(item,null));
  }
 }
 @Test public void crownInitialChargeAndPropertyExplanationMatchRealClassArmorConversion(){
  int cases=0;for(HeroClass type:HeroClass.values())for(int upgrade:new int[]{0,7})for(boolean known:new boolean[]{false,true})for(boolean cursed:new boolean[]{false,true}){
   hero.heroClass=type;Armor original=new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor();original.level(upgrade);original.levelKnown=known;original.cursedKnown=false;original.cursed=cursed;original.augment=Armor.Augment.EVASION;original.inscribe(new Obfuscation());original.glyphHardened=true;original.masteryPotionBonus=true;
   com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor converted=com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor.upgrade(hero,original);
   assertEquals(type.name().substring(0,1)+type.name().substring(1).toLowerCase(java.util.Locale.ROOT)+"Armor",converted.getClass().getSimpleName());assertEquals(com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor.initialUpgradeCharge(),converted.charge,0f);assertEquals(50f,converted.charge,0f);assertEquals(upgrade,converted.trueLevel());assertEquals(original.tier,converted.tier);assertSame(original.glyph,converted.glyph);assertEquals(original.augment,converted.augment);assertTrue(converted.glyphHardened);assertTrue(converted.masteryPotionBonus);assertEquals(cursed,converted.cursed);assertEquals(known,converted.levelKnown);assertTrue(converted.cursedKnown);cases++;
  }assertEquals(HeroClass.values().length*8,cases);
 }
 public static class LightningDescriptionSprite extends QuietMovementSprite {
  @Override public com.watabou.noosa.particles.Emitter centerEmitter(){return new com.watabou.noosa.particles.Emitter(){@Override public void burst(com.watabou.noosa.particles.Emitter.Factory factory,int quantity){}};}
  @Override public void flash(){}
 }
 public static class LightningDamageTarget extends PlainStatusTarget {int received;@Override public void damage(int amount,Object source){received+=amount;}}
 public static class LightningDamageCaster extends Hero {int received;@Override public void damage(int amount,Object source){received+=amount;}}
 public static class FixedLightningDescriptionProbe extends com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning {@Override public int damageRoll(){return 20;}@Override protected void wandProc(com.shatteredpixel.shatteredpixeldungeon.actors.Char target,int charges){}}
 public static class FixedFrostDescriptionProbe extends com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost {int checks;@Override public int damageRoll(){return 20;}@Override protected void wandProc(com.shatteredpixel.shatteredpixeldungeon.actors.Char target,int charges){checks++;}}
 @Test public void frostDescriptionNamesOnlyItsOwnDamageReductionAndFrozenImmunity()throws Exception{
  descriptionSightArena();hero.sprite=quietSprite();LightningDamageTarget enemy=new LightningDamageTarget();enemy.sprite=quietSprite();enemy.pos=hero.pos+1;com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(enemy);FixedFrostDescriptionProbe wand=new FixedFrostDescriptionProbe();wand.level(3);wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;java.lang.reflect.Field current=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser");current.setAccessible(true);Object previous=current.get(null);
  try{current.set(null,hero);for(int turns:new int[]{0,3,10,20})for(boolean water:new boolean[]{false,true}){
   Buff.detach(enemy,Chill.class);Buff.detach(enemy,Frost.class);enemy.received=0;Dungeon.level.water[enemy.pos]=water;if(turns>0)Buff.affect(enemy,Chill.class,turns);int checks=wand.checks;wand.onZap(new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos,enemy.pos,wand.collisionProperties(enemy.pos)));assertEquals(Math.round(20*(float)Math.pow(.9333f,Math.min(10,turns))),enemy.received);assertEquals(checks+1,wand.checks);assertEquals(turns+(water?4:2)+3,enemy.buff(Chill.class).cooldown(),0);
  }Buff.detach(enemy,Chill.class);Buff.affect(enemy,Frost.class,5);enemy.received=0;int checks=wand.checks;wand.onZap(new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos,enemy.pos,wand.collisionProperties(enemy.pos)));assertEquals(0,enemy.received);assertEquals(checks,wand.checks);assertNull(enemy.buff(Chill.class));assertTrue(wand.statsDesc(),wand.statsDesc().contains("This wand deals less damage to chilled targets"));
  }finally{current.set(null,previous);Dungeon.level.water[enemy.pos]=false;Buff.detach(enemy,Chill.class);Buff.detach(enemy,Frost.class);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(enemy);}
 }
 @Test public void lightningDescriptionMatchesActualAllyFilterWaterAndCasterDamage()throws Exception{
  descriptionSightArena();
  java.lang.reflect.Field current=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser");current.setAccessible(true);Object previous=current.get(null);java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);
  LightningDescriptionSprite sprite=(LightningDescriptionSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,LightningDescriptionSprite.class);
  LightningDamageCaster caster=new LightningDamageCaster();caster.HP=caster.HT=100;caster.sprite=sprite;LightningDamageTarget enemy=new LightningDamageTarget();enemy.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;enemy.sprite=sprite;LightningDamageTarget ally=new LightningDamageTarget();ally.alignment=caster.alignment;ally.sprite=sprite;
  FixedLightningDescriptionProbe wand=new FixedLightningDescriptionProbe();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;java.lang.reflect.Field affected=com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning.class.getDeclaredField("affected");affected.setAccessible(true);
  int base=-1;for(int y=1;y<31&&base<0;y++)for(int x=1;x<29;x++){int cell=y*33+x;if(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell)==null&&com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell+1)==null&&com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell+2)==null){base=cell;break;}}assertTrue("Free three-cell arena",base>=0);caster.pos=base;enemy.pos=base+1;ally.pos=base+2;com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(caster);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(enemy);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(ally);
  try{current.set(null,caster);
   for(int scenario=0;scenario<4;scenario++){
    caster.received=enemy.received=ally.received=0;enemy.pos=base+(scenario==1?2:1);ally.pos=base+(scenario==1?1:2);Dungeon.level.water[base+1]=scenario==2;int primary=scenario==1?ally.pos:enemy.pos;
    if(scenario==3)Buff.affect(caster,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning.LightningCharge.class);
    affected.set(wand,new java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.actors.Char>(java.util.Arrays.asList(enemy,ally,caster)));
    wand.onZap(new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(caster.pos,primary,wand.collisionProperties(primary)));
    assertEquals("direct ally vs collateral scenario"+scenario,scenario==1?12:0,ally.received);assertEquals("water/target count scenario"+scenario,scenario==1?12:scenario>=2?20:14,enemy.received);assertEquals("caster half/charge scenario"+scenario,scenario==3?0:scenario==2?10:scenario==1?6:7,caster.received);
    Buff.detach(caster,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning.LightningCharge.class);
   }
   assertTrue(wand.statsDesc(),wand.statsDesc().contains("allies are safe unless directly targeted"));assertTrue(wand.statsDesc().contains("half damage"));assertTrue(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning.class,"bmage_desc").contains(ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning.LightningCharge.DURATION)+" turns"));
  }finally{Buff.detach(caster,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning.LightningCharge.class);current.set(null,previous);Dungeon.level.water[base+1]=false;com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(caster);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(enemy);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(ally);}
 }
 @Test public void unidentifiedWandExamplesLabelPublicZeroStatsWithoutUsingHiddenLevels()throws Exception{
  for(String name:new String[]{"BlastWave","Corrosion","Corruption","Disintegration","Fireblast","Frost","Lightning","LivingEarth","MagicMissile","PrismaticLight","Regrowth","Transfusion","Warding"}){
   com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOf"+name).getConstructor().newInstance();String baseline=null;
   for(int hidden:new int[]{0,3,8}){
    wand.level(hidden);wand.levelKnown=false;wand.cursedKnown=wand.curChargeKnown=true;wand.curCharges=2;com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();wand.storeInBundle(before);String text=wand.info();wand.storeInBundle(after);assertEquals(name,before.toString(),after.toString());
    assertEquals(name,!name.equals("Corruption")&&!name.equals("Regrowth"),text.contains("Unupgraded example (+0):"));if(baseline!=null)assertEquals(name+" hidden level must not affect example",baseline,text);baseline=text;
   }
   if(name.equals("Fireblast")){wand.curChargeKnown=false;assertFalse(wand.info().contains("Unupgraded example (+0):"));assertTrue(wand.info().contains("Identify this wand"));}
   if(name.equals("Transfusion")){Dungeon.hero=null;try{assertFalse(wand.info().contains("Unupgraded example (+0):"));}finally{Dungeon.hero=hero;}}
  }
 }
 @Test public void everyWandStatDescriptionKeepsUnknownChargesPrivate() throws Exception {
  hero.heroClass=HeroClass.MAGE;Talent.initClassTalents(hero);hero.talents.get(1).put(Talent.DESPERATE_POWER,2);java.util.List<String> leaked=new java.util.ArrayList<>();
  for(String name:new String[]{"BlastWave","Corrosion","Corruption","Disintegration","Fireblast","Frost","Lightning","LivingEarth","MagicMissile","PrismaticLight","Regrowth","Transfusion","Warding"}){
   com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOf"+name).getConstructor().newInstance();wand.level(2);wand.levelKnown=wand.cursedKnown=true;wand.curChargeKnown=false;hero.belongings.backpack.items.add(wand);wand.charge(hero);
   try{wand.curCharges=1;String first=wand.statsDesc(),firstInfo=wand.info();wand.curCharges=4;if(!first.equals(wand.statsDesc()))leaked.add(name);assertEquals(name+" ordinary description must preserve unknown charge privacy",firstInfo,wand.info());if(name.equals("Fireblast")||name.equals("Regrowth")){assertTrue(name+" still explains its basic effect without known charges",firstInfo.contains("cone"));wand.curChargeKnown=true;String known=wand.statsDesc();wand.curCharges=1;assertNotEquals(name+" known charge-dependent values should return",known,wand.statsDesc());}}finally{wand.stopCharging();hero.belongings.backpack.items.remove(wand);}
  }
  assertEquals("Unidentified charge counts must not change visible effect descriptions",java.util.Collections.emptyList(),leaked);
 }
 @Test public void wandConditionalPreviewCannotRevealAnUnknownChargeCount(){
  hero.heroClass=HeroClass.MAGE;Talent.initClassTalents(hero);hero.talents.get(1).put(Talent.DESPERATE_POWER,2);
  com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.levelKnown=wand.cursedKnown=true;wand.curChargeKnown=false;hero.belongings.backpack.items.add(wand);wand.charge(hero);
  try{wand.curCharges=1;String single=wand.statsDesc(),badge=wand.previewUpgradeRange();int[] range=wand.previewEffectiveLevels();wand.curCharges=2;assertEquals("A conditional damage readout must not reveal the hidden last charge",single,wand.statsDesc());assertEquals(badge,wand.previewUpgradeRange());assertArrayEquals(range,wand.previewEffectiveLevels());assertArrayEquals(new int[]{0,2},range);
   wand.curChargeKnown=true;wand.curCharges=1;assertArrayEquals(new int[]{2,2},wand.previewEffectiveLevels());assertEquals(2,wand.buffedLvl());wand.curCharges=2;assertArrayEquals(new int[]{0,0},wand.previewEffectiveLevels());assertEquals(0,wand.buffedLvl());
  }finally{wand.stopCharging();hero.belongings.backpack.items.remove(wand);}
 }
 @Test public void keyDescriptionsDistinguishLockTypesAndRecordedFloorWithoutUsingKeys() throws Exception {
  String[] names={"GoldenKey","CrystalKey","IronKey","WornKey"},targets={"a locked chest","a crystal chest or crystal door","an ordinary locked door","a locked exit"};
  for(int index=0;index<names.length;index++){
   com.shatteredpixel.shatteredpixeldungeon.items.keys.Key key=(com.shatteredpixel.shatteredpixeldungeon.items.keys.Key)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.keys."+names[index]).getConstructor(int.class).newInstance(8);com.shatteredpixel.shatteredpixeldungeon.journal.Notes.add(key);int count=com.shatteredpixel.shatteredpixeldungeon.journal.Notes.keyCount(key);String heroBefore=savedDescriptionHero();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();key.storeInBundle(before);
   Random.pushGenerator(424);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(424);try{String text=UtilityItemExplanation.details(key,hero);assertTrue(text,text.contains("opens "+targets[index]));assertTrue(text,text.contains("belongs to Floor 8"));assertTrue(key.info(),key.info().contains("matching dungeon floor"));assertTrue(key.info(),key.info().contains("Floor 8"));assertTrue(text,text.contains(ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.items.keys.Key.TIME_TO_UNLOCK)+" turn"));assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
   key.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(heroBefore,savedDescriptionHero());assertEquals(count,com.shatteredpixel.shatteredpixeldungeon.journal.Notes.keyCount(key));
   com.shatteredpixel.shatteredpixeldungeon.items.keys.Key restored=(com.shatteredpixel.shatteredpixeldungeon.items.keys.Key)key.getClass().getConstructor().newInstance();restored.restoreFromBundle(before);assertEquals(UtilityItemExplanation.details(key,hero),UtilityItemExplanation.details(restored,hero));
  }
 }
 @Test public void keyFloorMatchingDescriptionAgreesWithRealJournalConsumption(){
  com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey key=new com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey(8);com.shatteredpixel.shatteredpixeldungeon.journal.Notes.add(key);
  assertFalse(com.shatteredpixel.shatteredpixeldungeon.journal.Notes.remove(new com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey(9)));assertFalse(com.shatteredpixel.shatteredpixeldungeon.journal.Notes.remove(new com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey(8)));assertEquals(1,com.shatteredpixel.shatteredpixeldungeon.journal.Notes.keyCount(key));assertTrue(com.shatteredpixel.shatteredpixeldungeon.journal.Notes.remove(key));assertEquals(0,com.shatteredpixel.shatteredpixeldungeon.journal.Notes.keyCount(key));
  String catalog=UtilityItemExplanation.details(new com.shatteredpixel.shatteredpixeldungeon.items.keys.CrystalKey(),null);assertTrue(catalog,catalog.contains("No floor is recorded"));assertFalse(catalog,catalog.contains("Floor 0"));
 }
 @Test public void sealShieldDefaultTooltipDistinguishesReadyWaitingAndSavedCredit(){
  com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield shield=new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield();shield.target=hero;
  String ready=shield.desc();assertTrue(ready,ready.startsWith("Ready."));assertFalse(ready,ready.contains("recharging"));
  com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();shield.storeInBundle(saved);saved.put("cooldown",37);shield.restoreFromBundle(saved);String waiting=shield.desc();assertTrue(waiting,waiting.contains("37 turns"));assertTrue(waiting,waiting.contains("Recharging"));
  saved.put("cooldown",-37);shield.restoreFromBundle(saved);String credit=shield.desc();assertTrue(credit,credit.contains("37 turns"));assertTrue(credit,credit.contains("next recharge is shortened"));
  saved.put("cooldown",37);shield.restoreFromBundle(saved);shield.incShield(2);String active=shield.desc();assertTrue(active,active.contains("Shield remaining: 2"));assertTrue(active,active.contains("Recharge time left: 37 turns"));assertFalse(active,active.contains("max of 50"));
 }
 @Test public void sealShieldActivationExplanationMatchesOddHpThresholdAndHungerExclusion() throws Exception {
  hero.sprite=quietSprite();hero.damageInterrupt=false;hero.heroClass=HeroClass.ROGUE;hero.talents.get(0).put(Talent.IRON_WILL,2);hero.HT=21;armor.affixSeal(new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal());String ordinary=armor.info();assertTrue(ordinary,ordinary.contains("When ready, the attached seal grants _"+armor.checkSeal().maxShield(armor.tier,armor.level())+" shielding_"));assertTrue(ordinary,ordinary.contains("half health or lower"));assertTrue(ordinary,ordinary.contains("Starvation does not trigger it"));
  for(int startingHp:new int[]{10,11,12})for(boolean hunger:new boolean[]{false,true}){
   Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield.class);hero.HP=startingHp;com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield shield=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield.class);
   String text=BuffExplanation.details(shield);assertTrue(text,text.contains("floor(maximum HP / 2)"));assertTrue(text,text.contains("non-hunger damage"));assertEquals(0,shield.remainingCooldown());
   hero.damage(1,hunger?new Hunger():Object.class);assertEquals("HP="+startingHp+", hunger="+hunger,!hunger&&startingHp<=11,shield.coolingDown());
  }
 }
 @Test public void sealShieldDetailsTrackRealCooldownRefundAndPreserveInspection() throws Exception {
  hero.heroClass=HeroClass.ROGUE;hero.talents.get(0).put(Talent.IRON_WILL,2);
  com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield shield=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield.class);shield.activate();assertEquals(2,shield.shielding());assertEquals(150,shield.remainingCooldown());
  com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();shield.storeInBundle(before);String heroBefore=savedDescriptionHero();Random.pushGenerator(418);int next=Random.Int();Random.popGenerator();Random.pushGenerator(418);
  try{String text=BuffExplanation.details(shield);assertTrue(text,text.contains("Current shield: 2 points"));assertFalse(text,text.contains("_Current shield_"));assertTrue(text,text.contains("Recharge remaining: 150"));assertTrue(text,text.contains("Next activation: 2 shield points"));assertEquals(next,Random.Int());}finally{Random.popGenerator();}
  shield.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(heroBefore,savedDescriptionHero());
  shield.act();assertEquals(149,shield.remainingCooldown());assertEquals(1,shield.enemyFreeProgress(),0);assertTrue(BuffExplanation.details(shield).contains("Enemy-free decay progress: 1 of 5"));for(int tick=0;tick<4;tick++)shield.act();assertEquals(0,shield.shielding());assertEquals(70,shield.remainingCooldown());assertTrue(BuffExplanation.details(shield).contains("Recharge remaining: 70"));
  shield.reduceCooldown(1);assertEquals(-80,shield.remainingCooldown());String credit=BuffExplanation.details(shield);assertTrue(credit,credit.contains("Stored cooldown reduction: 80 turns"));shield.activate();assertEquals(70,shield.remainingCooldown());assertEquals(2,shield.shielding());
 }
 @Test public void sealShieldDescriptionDoesNotAdvancePausedCooldownOrOutOfPositionHoldFast() throws Exception {
  hero.heroClass=HeroClass.ROGUE;hero.talents.get(0).put(Talent.IRON_WILL,2);hero.talents.get(2).put(Talent.HOLD_FAST,1);
  com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield shield=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield.class);shield.activate();com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor lock=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor.class);lock.removeTime(lock.remainingGraceTime());
  com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast hold=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast.class);hold.pos=hero.pos+1;BuffExplanation.details(shield);assertSame(hold,hero.buff(hold.getClass()));assertEquals(0,shield.enemyFreeProgress(),0);assertEquals(150,shield.remainingCooldown());
  hold.pos=hero.pos;shield.act();assertEquals(.5f,shield.enemyFreeProgress(),0);assertEquals(150,shield.remainingCooldown());
  com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();shield.storeInBundle(saved);com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield restored=new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield();restored.restoreFromBundle(saved);restored.target=hero;assertEquals(BuffExplanation.details(shield),BuffExplanation.details(restored));Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor.class);shield.act();assertEquals(149,shield.remainingCooldown());
 }
 @Test public void dewExplanationUsesLiveHpStorageAndShieldCapWithoutSpendingAnything() throws Exception {
  com.shatteredpixel.shatteredpixeldungeon.items.Waterskin skin=new com.shatteredpixel.shatteredpixeldungeon.items.Waterskin();skin.fill();hero.HP=13;hero.HT=53;hero.apdCustomHealingFraction=.75f;hero.talents.get(2).put(Talent.SHIELDING_DEW,2);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class).setShield(7);
  String heroBefore=savedDescriptionHero();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();skin.storeInBundle(before);
  Random.pushGenerator(416);int next=Random.Int();Random.popGenerator();Random.pushGenerator(416);
  try{String text=UtilityItemExplanation.details(skin,hero);assertTrue(text,text.contains("contains 20 of 20 drops"));assertTrue(text,text.contains("One drop's base value: 3 HP"));assertTrue(text,text.contains("five drops together: 13 HP"));assertTrue(text,text.contains("Shielding Dew cap: 21 Barrier points; current Barrier: 7 points"));assertEquals(next,Random.Int());}finally{Random.popGenerator();}
  skin.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(heroBefore,savedDescriptionHero());assertEquals(.75f,hero.apdCustomHealingFraction,0f);hero.HT=100;assertTrue(UtilityItemExplanation.details(skin,hero).contains("five drops together: 25 HP"));assertTrue(UtilityItemExplanation.details(skin,null).contains("require a loaded character"));skin.empty();assertTrue(UtilityItemExplanation.details(skin,hero).contains("contains 0 of 20 drops"));
 }
 public static class CursedHealingDisplaySprite extends QuietMovementSprite {
  String healing;
  @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){if(icon==com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING)healing=text;}
  @Override public com.watabou.noosa.particles.Emitter emitter(){return new com.watabou.noosa.particles.Emitter(){@Override public void burst(com.watabou.noosa.particles.Emitter.Factory factory,int quantity){}@Override public void start(com.watabou.noosa.particles.Emitter.Factory factory,float interval,int quantity){}};}
 }
 @Test public void heartyMealHealingNumberMatchesRulesAndPreservesOtherMealBenefits()throws Exception{
  java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);CursedHealingDisplaySprite sprite=(CursedHealingDisplaySprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,CursedHealingDisplaySprite.class);hero.sprite=sprite;hero.HT=100;hero.talents.get(0).put(Talent.STRENGTHENING_MEAL,1);
  for(int rank:new int[]{1,2})for(int percent:new int[]{0,25,100,200})for(int startingHp:new int[]{10,33,34}){
   com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,percent).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);hero.talents.get(0).put(Talent.HEARTY_MEAL,rank);hero.HP=startingHp;hero.apdCustomHealingFraction=.75f;sprite.healing=null;Buff.detach(hero,PhysicalEmpower.class);float time=hero.cooldown();com.shatteredpixel.shatteredpixeldungeon.items.food.Food food=new com.shatteredpixel.shatteredpixeldungeon.items.food.Food();
   Talent.onFoodEaten(hero,Hunger.HUNGRY,food);boolean qualifies=startingHp<33.4f;int restored=qualifies&&percent>0?(int)Math.floor((2+2*rank)*percent/100f+.75f):0;assertEquals("rank="+rank+",percent="+percent+",HP="+startingHp,startingHp+restored,hero.HP);assertEquals("Displayed healing must match actual HP gain",qualifies?Integer.toString(restored):null,sprite.healing);assertEquals(time,hero.cooldown(),0);assertEquals(1,food.quantity());assertEquals(3,hero.buff(PhysicalEmpower.class).dmgBoost);assertEquals(2,hero.buff(PhysicalEmpower.class).left);
  }
 }
 @Test public void cursedHealthTransferFloatingHealingMatchesActualCustomRules()throws Exception{
  descriptionSightArena();int oldDepth=Dungeon.depth;java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);CursedHealingDisplaySprite sprite=(CursedHealingDisplaySprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,CursedHealingDisplaySprite.class);hero.sprite=sprite;hero.HT=100;LightningDamageTarget enemy=new LightningDamageTarget();enemy.sprite=sprite;enemy.pos=hero.pos+1;com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(enemy);
  try{Dungeon.depth=10;for(int percentage:new int[]{0,25,100,200}){com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,percentage).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);hero.HP=10;hero.apdCustomHealingFraction=.75f;sprite.healing=null;enemy.received=0;assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand.HealthTransfer().effect(null,hero,new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos,enemy.pos,com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE),true));assertEquals("percentage"+percentage,Integer.toString(hero.HP-10),sprite.healing);assertEquals(20,enemy.received);}
  }finally{Dungeon.depth=oldDepth;com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(enemy);}
 }
 @Test public void zeroHealingFoodDescriptionsPreserveOtherActualMealBenefits()throws Exception{
  hero.sprite=quietSprite();hero.HT=100;hero.HP=10;com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);
  com.shatteredpixel.shatteredpixeldungeon.items.food.Food[] meals={new com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio(),new com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat(),new com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie(),new com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation()};String before=savedDescriptionHero();for(com.shatteredpixel.shatteredpixeldungeon.items.food.Food meal:meals){String text=FoodExplanation.specialEffect(meal,hero);assertTrue(text,text.contains("rules"));assertFalse(text,text.contains("healing for 25 HP"));assertFalse(text,text.contains("Restores 5 HP"));}assertEquals(before,savedDescriptionHero());
  int seed=0;for(;seed<1000;seed++){Random.pushGenerator(seed);int outcome=Random.Int(5);Random.popGenerator();if(outcome==3)break;}assertTrue(seed<1000);Random.pushGenerator(seed);try{com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio.effect(hero);}finally{Random.popGenerator();}assertEquals(10,hero.HP);
  com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat.effect(hero);assertEquals(10,hero.HP);assertNotNull(hero.buff(Invisibility.class));assertNotNull(hero.buff(Barkskin.class));
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows cloak=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows();hero.belongings.artifact=cloak;java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge");charge.setAccessible(true);charge.setInt(cloak,0);new DescriptionSupplyRation().satisfyNow(hero);assertEquals(10,hero.HP);assertEquals(1,cloak.currentCharges());
 }
 @Test public void zeroHealingDewDescriptionsMatchActualConsumptionShieldingAndStorage()throws Exception{
  hero.sprite=quietSprite();hero.HT=100;hero.HP=80;com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop dew=new com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop();com.shatteredpixel.shatteredpixeldungeon.items.Waterskin skin=new com.shatteredpixel.shatteredpixeldungeon.items.Waterskin();for(int i=0;i<20;i++)skin.collectDew(new com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop());assertTrue(skin.info().contains("5%"));
  com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);hero.apdCustomHealingFraction=.75f;
  String heroBefore=savedDescriptionHero();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();skin.storeInBundle(before);assertTrue(dew.desc(),dew.desc().contains("prevent dew from healing"));assertTrue(skin.info(),skin.info().contains("prevent dew healing"));assertTrue(skin.info().contains("Drinking still spends dew"));assertFalse(skin.info().contains("Each drop normally heals"));skin.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(heroBefore,savedDescriptionHero());assertEquals(.75f,hero.apdCustomHealingFraction,0);
  assertTrue(com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop.consumeDew(1,hero,false));assertEquals(80,hero.HP);
  float time=hero.cooldown();boolean confirm=SPDSettings.confirmWaterskin();java.lang.reflect.Field selector=com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector"),user=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser"),item=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curItem");selector.setAccessible(true);user.setAccessible(true);item.setAccessible(true);Object oldSelector=selector.get(null),oldUser=user.get(null),oldItem=item.get(null);java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);selector.set(null,unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.shatteredpixel.shatteredpixeldungeon.apd.qol.WaitingPolicyTest.QuietSelector.class));try{SPDSettings.confirmWaterskin(false);skin.execute(hero,"DRINK");}finally{SPDSettings.confirmWaterskin(confirm);selector.set(null,oldSelector);user.set(null,oldUser);item.set(null,oldItem);}assertEquals(80,hero.HP);assertEquals(16,skin.storedDrops());assertEquals(com.shatteredpixel.shatteredpixeldungeon.items.Waterskin.drinkingTime(),hero.cooldown()-time,0);
  hero.HP=100;hero.talents.get(2).put(Talent.SHIELDING_DEW,1);assertTrue(com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop.consumeDew(1,hero,false));assertEquals(5,hero.buff(Barrier.class).shielding());
  Dungeon.hero=null;try{assertFalse(dew.desc().contains("Your current rules"));assertFalse(skin.info().contains("Your current rules"));}finally{Dungeon.hero=hero;}
 }
 @Test public void dewReadoutSharedValuesMatchRealHealingAndTalentShieldCaps() throws Exception {
  hero.sprite=quietSprite();int comparisons=0;
  for(int maximum:new int[]{20,53,100,201})for(int drops:new int[]{1,2,5,20})for(int rank:new int[]{0,1,2,3}){
   Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class);hero.HT=maximum;hero.HP=maximum;hero.talents.get(2).put(Talent.SHIELDING_DEW,rank);
   int base=Math.round(maximum*.05f*drops),cap=Math.round(maximum*.2f*rank);assertEquals(base,com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop.baseDewEffect(hero,drops));assertEquals(cap,com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop.shieldingDewCap(hero));
   assertTrue(com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop.consumeDew(drops,hero,true));com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier barrier=hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class);assertEquals(Math.min(base,cap),barrier==null?0:barrier.shielding());assertEquals(maximum,hero.HP);
   Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class);hero.talents.get(2).put(Talent.SHIELDING_DEW,0);hero.HP=1;assertTrue(com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop.consumeDew(drops,hero,true));assertEquals(Math.min(maximum,1+base),hero.HP);comparisons++;
  }
  assertEquals(64,comparisons);
 }
 @Test public void armbandUiQuoteMatchesRealChargeBudgetWithoutCleaningUpWand() throws Exception {
  for(int upgrade:new int[]{0,5,10})for(int charges:new int[]{0,1,4}) {
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband armband=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband();armband.level(upgrade);
   java.lang.reflect.Method factory=armband.getClass().getDeclaredMethod("passiveBuff");factory.setAccessible(true);
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband.Thievery effect=(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband.Thievery)factory.invoke(armband);effect.target=hero;
   java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge");charge.setAccessible(true);charge.setInt(armband,charges);
   com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=inactiveInfusionUiFixture();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle(),armBefore=new com.watabou.utils.Bundle(),armAfter=new com.watabou.utils.Bundle();wand.storeInBundle(before);armband.storeInBundle(armBefore);
   int budget=effect.previewChargesToUse(wand);float chance=effect.previewStealChance(wand);wand.storeInBundle(after);armband.storeInBundle(armAfter);assertEquals(before.toString(),after.toString());assertEquals(armBefore.toString(),armAfter.toString());
   int expected=0;float value=0;while(value<75&&expected<charges){value+=10+upgrade/2f;expected++;}assertEquals(expected,budget);assertEquals(Math.min(1f,expected*(10+upgrade/2f)/75),chance,0f);assertEquals(budget,effect.chargesToUse(wand));assertEquals(chance,effect.stealChance(wand),0f);assertFalse(wand.curseInfusionBonus);
  }
 }
 public static class QuietArtifactOperateSprite extends QuietMovementSprite {
  @Override public void operate(int cell,com.watabou.utils.Callback callback){callback.call();}
  @Override public void idle(){}
 }
 @Test public void skeletonKeyDescriptionMatchesActualIronFreeReopeningAndTemporaryWallCosts()throws Exception{
  descriptionSightArena();java.util.Arrays.fill(Dungeon.level.visited,true);java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);hero.sprite=(QuietArtifactOperateSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,QuietArtifactOperateSprite.class);
  int depth=Dungeon.depth,branch=Dungeon.branch;java.lang.reflect.Field user=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser");user.setAccessible(true);Object previous=user.get(null);Dungeon.depth=2;Dungeon.branch=0;user.set(null,hero);
  try{for(int terrain:new int[]{com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.LOCKED_DOOR,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.HERO_LKD_DR,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY}){
   int target=hero.pos+1;com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(target,terrain);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey key=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey();key.levelKnown=key.cursedKnown=true;hero.belongings.misc=key;
   String text=key.desc();assertTrue(text,text.contains("1 charge for Iron"));assertTrue(text,text.contains("2 for Golden"));assertTrue(text,text.contains("5 for Crystal"));assertTrue(text,text.contains("10 turns"));int charge=key.currentCharges();float time=hero.cooldown();key.targeter.onSelect(terrain==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY?hero.pos+3:target);assertEquals(1,hero.cooldown()-time,0);
   if(terrain==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.LOCKED_DOOR){assertEquals(charge-1,key.currentCharges());assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.DOOR,Dungeon.level.map[target]);}
   else if(terrain==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.HERO_LKD_DR){assertEquals(charge,key.currentCharges());assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.DOOR,Dungeon.level.map[target]);}
   else{assertEquals(charge-2,key.currentCharges());com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey.KeyWall wall=(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey.KeyWall)Dungeon.level.blobs.get(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey.KeyWall.class);assertNotNull(wall);assertEquals(30,wall.volume);for(int cell:new int[]{target,target-Dungeon.level.width(),target+Dungeon.level.width()}){assertEquals(10,wall.cur[cell]);assertTrue(Dungeon.level.solid[cell]);}for(int turn=0;turn<10;turn++)wall.act();assertEquals(0,wall.volume);assertFalse(Dungeon.level.solid[target]);}
  }}finally{Dungeon.depth=depth;Dungeon.branch=branch;user.set(null,previous);}
 }
 @Test public void sandalsRejectSeedsWithoutAnExistingAbilityAndKeepAllSupportedSeeds()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature sandals=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature();
  assertFalse("Blandfruit has no footwear cost/color/ability entry",sandals.canUseSeed(new com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed()));
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed invalid=new com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed();assertTrue(invalid.collect());java.lang.reflect.Field selectorField=sandals.getClass().getDeclaredField("itemSelector");selectorField.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector selector=(com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector)selectorField.get(sandals);float time=hero.cooldown();selector.onSelect(invalid);assertEquals(time,hero.cooldown(),0);assertTrue(hero.belongings.contains(invalid));assertTrue(sandals.seeds.isEmpty());assertNull(sandals.curSeedEffect);
  java.lang.reflect.Field field=sandals.getClass().getDeclaredField("seedChargeReqs");field.setAccessible(true);java.util.Map<Class<?>,Integer> costs=(java.util.Map<Class<?>,Integer>)field.get(null);assertEquals(12,costs.size());
  for(Class<?> type:costs.keySet())assertTrue(type.getSimpleName(),sandals.canUseSeed((com.shatteredpixel.shatteredpixeldungeon.items.Item)type.getDeclaredConstructor().newInstance()));
 }
 @Test public void sandalsLegacyUnsupportedSeedCanBeInspectedAndReplacedWithoutChangingItsSave()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature original=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature();original.levelKnown=original.cursedKnown=true;original.curSeedEffect=com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed.class;original.seeds.add(original.curSeedEffect);
  com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();original.storeInBundle(saved);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature restored=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature();restored.restoreFromBundle(saved);hero.belongings.misc=restored;
  java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge");charge.setAccessible(true);charge.setInt(restored,100);
  com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();restored.storeInBundle(before);assertTrue(restored.actions(hero).contains("FEED"));assertFalse(restored.actions(hero).contains("ROOT"));assertNull(restored.glowing());assertFalse(restored.info().contains("Charge needed"));assertTrue(restored.canUseSeed(new com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom.Seed()));restored.storeInBundle(after);assertEquals(before.toString(),after.toString());assertSame(original.curSeedEffect,restored.curSeedEffect);assertEquals(original.seeds,restored.seeds);
  float time=hero.cooldown();java.lang.reflect.Field user=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser");user.setAccessible(true);Object previous=user.get(null);try{user.set(null,hero);restored.cellSelector.onSelect(hero.pos);}finally{user.set(null,previous);}assertEquals(time,hero.cooldown(),0);assertEquals(100,restored.currentCharges());
 }
 @Test public void sandalsGrowthDescriptionMatchesActualSeedConsumptionAndFinalForm()throws Exception{
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature sandals=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature();sandals.levelKnown=sandals.cursedKnown=true;hero.belongings.misc=sandals;
  java.lang.reflect.Field field=sandals.getClass().getDeclaredField("itemSelector");field.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector selector=(com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector)field.get(sandals);
  String[] kinds={"Blindweed","Earthroot","Fadeleaf","Firebloom","Icecap","Mageroyal","Sorrowmoss","Starflower","Stormvine"};int consumed=0;
  for(int stage=0;stage<3;stage++){
   int needed=3+3*stage;
   for(int i=0;i<needed;i++){
    com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed seed=(com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed)Class.forName("com.shatteredpixel.shatteredpixeldungeon.plants."+kinds[i]+"$Seed").getDeclaredConstructor().newInstance();assertTrue(selector.itemSelectable(seed));assertTrue(seed.collect());float before=hero.cooldown();selector.onSelect(seed);consumed++;assertEquals(1,hero.cooldown()-before,0);assertFalse(hero.belongings.contains(seed));assertSame(seed.getClass(),sandals.curSeedEffect);
    if(i+1<needed){assertEquals(stage,sandals.level());assertTrue(sandals.desc(),sandals.desc().contains((i+1)+" of "+needed+" different seed types"));assertFalse(selector.itemSelectable(seed));}else{assertEquals(stage+1,sandals.level());assertTrue(sandals.seeds.isEmpty());assertFalse(sandals.desc().contains("Fed _"));}
   }
  }
  assertEquals(18,consumed);assertTrue(sandals.desc(),sandals.desc().contains("final form"));assertTrue(sandals.desc().contains("visible tile"));
  sandals.seeds.add(com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom.Seed.class);assertFalse("A legacy extra seed list cannot promise another growth at the cap",sandals.desc().contains("next growth"));
  com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();sandals.storeInBundle(before);sandals.info();sandals.storeInBundle(after);assertEquals(before.toString(),after.toString());
 }
 @Test public void hourglassRetainedActionBudgetMatchesActualPartialCostsAndSave()throws Exception{
  float[][] sequences={{0},{1},{1,1},{1,1,.5f},{.5f,.5f,.5f,.5f,.5f},{2.0005f},{2.01f},{4},{4,.5f},{8}};
  int[] expectedCharges={6,6,6,5,5,6,5,5,4,3};
  for(int i=0;i<sequences.length;i++){
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass();item.levelKnown=item.cursedKnown=true;
   java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge");charge.setAccessible(true);charge.setInt(item,6);
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass.timeFreeze freeze=item.new timeFreeze();freeze.target=hero;
   com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();item.storeInBundle(before);String text=MechanicsDetails.itemCalculations(item);item.storeInBundle(after);assertEquals(before.toString(),after.toString());assertTrue(text,text.contains("partial time carries over"));assertTrue(text,text.contains("Traps and plants"));assertFalse(text.contains("rounding tolerance"));
   for(float time:sequences[i])freeze.processTime(time);assertEquals("sequence="+i,expectedCharges[i],item.currentCharges());
   com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();freeze.storeInBundle(saved);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass.timeFreeze restored=item.new timeFreeze();restored.restoreFromBundle(saved);restored.target=hero;assertEquals(freeze.desc(),restored.desc());assertEquals(freeze.iconTextDisplay(),restored.iconTextDisplay());
   int afterFirst=item.currentCharges();freeze.processTime(.25f);int originalCost=afterFirst-item.currentCharges();charge.setInt(item,afterFirst);restored.processTime(.25f);assertEquals(originalCost,afterFirst-item.currentCharges());
  }
 }
 @Test public void toolkitRetainedWarmupTextMatchesActualTimingAndSavedProgress()throws Exception{
  for(int level:new int[]{0,5,10})for(float warm:new float[]{0,50,101}){
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit kit=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit();kit.level(level);kit.levelKnown=kit.cursedKnown=true;
   java.lang.reflect.Field remaining=kit.getClass().getDeclaredField("warmUpDelay");remaining.setAccessible(true);remaining.setFloat(kit,warm);
   java.lang.reflect.Method factory=kit.getClass().getDeclaredMethod("passiveBuff");factory.setAccessible(true);Buff effect=(Buff)factory.invoke(kit);effect.target=hero;
   com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();kit.storeInBundle(before);String text=ArtifactExplanation.details(kit);kit.storeInBundle(after);assertEquals(before.toString(),after.toString());assertTrue(text,text.contains("provides 2 + Toolkit level = "+(2+level)));assertEquals(level==10,text.contains("Maximum upgrade reached"));
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit restored=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit();restored.restoreFromBundle(before);assertEquals(warm,remaining.getFloat(restored),0);
   int expected=warm==0?0:level==10?1:level==0?(warm==101?101:50):(warm==101?26:13),ticks=0;
   while(remaining.getFloat(kit)>0&&ticks<102){effect.act();ticks++;}assertEquals("level="+level+" warm="+warm,expected,ticks);assertTrue(remaining.getFloat(kit)<=0);
  }
 }
 @Test public void toolkitWarmupPausesUnderCurseAndMagicImmunityButKeepsSavedProgress()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit kit=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit();kit.level(5);kit.levelKnown=kit.cursedKnown=true;
  java.lang.reflect.Field remaining=kit.getClass().getDeclaredField("warmUpDelay");remaining.setAccessible(true);remaining.setFloat(kit,50);
  java.lang.reflect.Method factory=kit.getClass().getDeclaredMethod("passiveBuff");factory.setAccessible(true);Buff effect=(Buff)factory.invoke(kit);effect.target=hero;
  kit.cursed=true;effect.act();assertEquals(50,remaining.getFloat(kit),0);kit.cursed=false;Buff.affect(hero,MagicImmune.class);effect.act();assertEquals(50,remaining.getFloat(kit),0);Buff.detach(hero,MagicImmune.class);effect.act();assertEquals(46,remaining.getFloat(kit),0);
 }
 public static class ArmbandDescriptionValueItem extends com.shatteredpixel.shatteredpixeldungeon.items.Item {
  private final int price;
  ArmbandDescriptionValueItem(int price){this.price=price;}
  @Override public int value(){return price;}
 }
 @Test public void armbandRetainedShopDescriptionMatchesActualSuccessAndFailureCosts()throws Exception{
  int successes=0,failures=0;
  for(int charges:new int[]{0,1,4})for(int price:new int[]{15,75,200})for(int seed=0;seed<32;seed++){
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband();item.level(10);item.levelKnown=item.cursedKnown=true;
   java.lang.reflect.Field meter=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge");meter.setAccessible(true);meter.setInt(item,charges);
   java.lang.reflect.Method factory=item.getClass().getDeclaredMethod("passiveBuff");factory.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband.Thievery effect=(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband.Thievery)factory.invoke(item);effect.target=hero;
   ArmbandDescriptionValueItem loot=new ArmbandDescriptionValueItem(price);int used=effect.chargesToUse(loot);float chance=effect.stealChance(loot);float time=hero.cooldown();
   com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();item.storeInBundle(before);Random.pushGenerator(seed);float roll=Random.Float();Random.popGenerator();Random.pushGenerator(seed);
   try{String text=ArtifactExplanation.details(item);assertTrue(text,text.contains("Charges are spent only on success"));assertTrue(text,text.contains("8 turns, or 10"));assertTrue(text,text.contains("× 2"));item.storeInBundle(after);assertEquals(before.toString(),after.toString());boolean result=effect.steal(loot);assertEquals(roll<=chance,result);assertEquals(charges-(result?used:0),item.currentCharges());if(result)successes++;else failures++;}finally{Random.popGenerator();}
   assertEquals(time,hero.cooldown(),0);assertEquals(10,item.level());
  }
  assertTrue(successes>0);assertTrue(failures>0);
 }
 @Test public void wandUiQuotesMatchRealTransactionsWithoutChangingItemOrRandomness() throws Exception {
  int previousDepth= Dungeon.depth;int comparisons=0;
  try { for(String name:new String[]{"BlastWave","Corrosion","Corruption","Disintegration","Fireblast","Frost","Lightning","LivingEarth","MagicMissile","PrismaticLight","Regrowth","Transfusion","Warding"})
   for(int level:new int[]{-1,0,3,6})for(boolean known:new boolean[]{false,true})for(boolean cursed:new boolean[]{false,true})for(boolean infusion:new boolean[]{false,true}) {
    com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOf"+name).getConstructor().newInstance();
    wand.cursed=true;wand.curseInfusionBonus=infusion;wand.level(level);wand.cursed=cursed;wand.levelKnown=known;wand.cursedKnown=true;wand.resinBonus=1;
    com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();wand.storeInBundle(before);
    Random.pushGenerator(407);int expectedRandom=Random.Int();Random.popGenerator();Random.pushGenerator(407);
    int quote;
    try{quote=wand.previewValue();wand.title();wand.buffedVisiblyUpgraded();wand.previewUpgradeRange();Dungeon.depth=16;assertEquals(quote*20,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.previewSellPrice(wand));assertEquals(expectedRandom,Random.Int());}finally{Random.popGenerator();}
    wand.storeInBundle(after);assertEquals(name,before.toString(),after.toString());assertEquals(name,quote,wand.value());
    assertEquals(name,known && !cursed ? false : infusion,wand.curseInfusionBonus);comparisons++;
   }
  }finally{Dungeon.depth=previousDepth;}
  assertEquals(416,comparisons);
 }
 @Test public void wildMagicUiShowsBothPossibleLevelsWithoutIdentifyingUnknownWands(){
  hero.heroClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.MAGE;Talent.initClassTalents(hero);hero.talents.get(3).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.WILD_POWER,1);
  com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.levelKnown=true;hero.belongings.backpack.items.add(wand);wand.charge(hero);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic.WildMagicTracker.class);
  try{assertEquals("+2..+3",wand.previewUpgradeRange());assertEquals(0,wand.buffedVisiblyUpgraded());java.util.Set<Integer> actual=new java.util.HashSet<>();Random.pushGenerator(409);try{for(int draw=0;draw<40;draw++)actual.add(wand.buffedLvl());}finally{Random.popGenerator();}assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(2,3)),actual);wand.levelKnown=false;wand.level(7);assertNull(wand.previewUpgradeRange());assertEquals(0,wand.buffedVisiblyUpgraded());}
  finally{wand.stopCharging();Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic.WildMagicTracker.class);hero.belongings.backpack.items.remove(wand);}
 }
 private com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand inactiveInfusionUiFixture(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.cursed=true;wand.curseInfusionBonus=true;wand.level(0);wand.cursed=false;wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;return wand;
 }
 @Test public void wandUiTitleDoesNotCleanUpTheItem(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=inactiveInfusionUiFixture();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();wand.storeInBundle(before);int capacity=wand.maxCharges;wand.title();wand.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(capacity,wand.maxCharges);
 }
 @Test public void wandUiShopEligibilityDoesNotCleanUpTheItem(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=inactiveInfusionUiFixture();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();wand.storeInBundle(before);assertTrue(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.canSell(wand));wand.storeInBundle(after);assertEquals(before.toString(),after.toString());
 }
 @Test public void wandUiUpgradeBadgeDoesNotRollWildMagic(){
  hero.heroClass=HeroClass.MAGE;Talent.initClassTalents(hero);hero.talents.get(3).put(Talent.WILD_POWER,1);
  com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;hero.belongings.backpack.items.add(wand);wand.charge(hero);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic.WildMagicTracker.class);
  try{Random.pushGenerator(978);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(978);try{wand.buffedVisiblyUpgraded();assertEquals("Drawing an upgrade badge must not advance the cast RNG",expected,Random.Int());}finally{Random.popGenerator();}}finally{wand.stopCharging();Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic.WildMagicTracker.class);}
 }
 @Test public void metalReadoutsMatchActualRecipeAndPreserveKnownWeaponState()throws Exception{
  int compared=0;
  for(Class<?> type:com.shatteredpixel.shatteredpixeldungeon.journal.Catalog.THROWN_WEAPONS.items()){
   if(!com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon.class.isAssignableFrom(type))continue;
   for(int level:new int[]{0,2,5,8})for(float durability:new float[]{25,70,100})for(boolean spareMissing:new boolean[]{false,true}){
    com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon weapon=(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon)type.getDeclaredConstructor().newInstance();weapon.level(level);weapon.levelKnown=weapon.cursedKnown=true;weapon.cursed=false;weapon.quantity(spareMissing?Math.max(1,weapon.defaultQuantity()-1):weapon.defaultQuantity());
    com.watabou.utils.Bundle fixture=new com.watabou.utils.Bundle();weapon.storeInBundle(fixture);fixture.put("durability",durability);weapon.restoreFromBundle(fixture);
    java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> ingredients=new java.util.ArrayList<>();ingredients.add(weapon);com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal.Recipe recipe=new com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal.Recipe();assertTrue(recipe.testIngredients(ingredients));
    float perWeapon=5*(weapon.tier+1);if(weapon.defaultQuantity()!=3)perWeapon=3f/weapon.defaultQuantity();perWeapon*=Math.pow(1.35f,Math.min(5,weapon.level()));float weight=weapon.quantity()-1;weight+=.25f+.0075f*weapon.durabilityLeft();int expected=Math.round(weight*perWeapon);assertEquals(expected,recipe.sampleOutput(ingredients).quantity());assertEquals(3,recipe.cost(ingredients));
    com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();weapon.storeInBundle(before);String heroBefore=savedDescriptionHero();String detail=MetalExplanation.forWeapon(weapon);assertTrue(detail,detail.contains("yields "+expected+" metal units for 3 alchemy energy"));
    if(!(weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart)){float original=5*(weapon.tier+1);original*=Math.pow(1.35f,weapon.level());assertEquals(Float.floatToIntBits(original),Float.floatToIntBits(com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal.repairMetalScale(weapon)));assertTrue(detail.contains(ObfuscationExplanation.number(original)+" metal units"));}
    weapon.storeInBundle(after);assertEquals(type.getSimpleName(),before.toString(),after.toString());assertEquals(heroBefore,savedDescriptionHero());compared++;
   }
  }
  System.out.println("METAL_READOUT_COMPARISONS="+compared);assertTrue(compared>=240);
 }
 @Test public void metalInspectionKeepsHiddenLevelsAndDartImpactPrivateWhileRealRecyclingConsumes(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife();weapon.cursedKnown=true;weapon.levelKnown=false;weapon.level(0);String low=MetalExplanation.forWeapon(weapon);weapon.level(7);assertEquals(low,MetalExplanation.forWeapon(weapon));weapon.levelKnown=true;weapon.cursed=true;assertTrue(MetalExplanation.forWeapon(weapon).contains("ineligible for recycling"));weapon.cursed=false;
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart();dart.levelKnown=dart.cursedKnown=true;
  try{java.lang.reflect.Field pending=com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.TippedDart.class.getDeclaredField("targetPos");pending.setAccessible(true);int previous=pending.getInt(null);try{pending.setInt(null,hero.pos);Random.pushGenerator(972);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(972);try{assertTrue(MetalExplanation.forWeapon(dart).contains("cannot be selected for metal repair"));assertEquals(expected,Random.Int());assertEquals(hero.pos,pending.getInt(null));}finally{Random.popGenerator();}}finally{pending.setInt(null,previous);}}catch(ReflectiveOperationException error){throw new AssertionError(error);}
  java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> inputs=new java.util.ArrayList<>();inputs.add(weapon);com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal.Recipe recipe=new com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal.Recipe();int expected=recipe.sampleOutput(inputs).quantity();
  try{assertEquals(expected,recipe.brew(inputs).quantity());assertEquals(0,weapon.quantity());assertEquals(Integer.valueOf(Integer.MAX_VALUE),hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon.UpgradedSetTracker.class).levelThresholds.get(weapon.setID));}finally{Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon.UpgradedSetTracker.class);}
 }
 @Test public void resinRecipePreviewDoesNotCleanUpOrConsumeTheInputWand(){
  hero.talents.get(1).put(Talent.WAND_PRESERVATION,2);
  com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.cursed=true;wand.curseInfusionBonus=true;wand.resinBonus=2;wand.level(6);wand.cursed=false;wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;
  java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> ingredients=new java.util.ArrayList<>();ingredients.add(wand);com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin.Recipe recipe=new com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin.Recipe();assertTrue(recipe.testIngredients(ingredients));
  com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();wand.storeInBundle(before);int capacity=wand.maxCharges;String heroBefore=savedDescriptionHero();
  Random.pushGenerator(419);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(419);
  try{assertEquals(16,recipe.sampleOutput(ingredients).quantity());assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
  wand.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(capacity,wand.maxCharges);assertEquals(heroBefore,savedDescriptionHero());assertEquals(1,wand.quantity());
 }
 @Test public void resinPreviewAndActualConversionAgreeAcrossWandTypesAndPreservationContexts()throws Exception{
  int checked=0;
  for(Class<?> type:com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.WAND.classes)for(int base:new int[]{0,2,6})for(int resin:new int[]{0,2})for(boolean infusion:new boolean[]{false,true})for(HeroClass heroClass:new HeroClass[]{HeroClass.WARRIOR,HeroClass.MAGE})for(int rank:new int[]{0,2})for(boolean known:new boolean[]{false,true}){
   hero.heroClass=heroClass;hero.talents.get(1).put(Talent.WAND_PRESERVATION,rank);
   com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)type.getDeclaredConstructor().newInstance();wand.cursed=true;wand.curseInfusionBonus=infusion;wand.resinBonus=resin;wand.level(base);wand.cursed=false;wand.levelKnown=known;wand.cursedKnown=wand.curChargeKnown=true;
   java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> inputs=new java.util.ArrayList<>();inputs.add(wand);com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin.Recipe recipe=new com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin.Recipe();assertTrue(recipe.testIngredients(inputs));assertEquals(5,recipe.cost(inputs));
   com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();wand.storeInBundle(before);int expected=2*(base+1)+(heroClass==HeroClass.MAGE?0:rank);assertEquals(known?expected:1,recipe.sampleOutput(inputs).quantity());
   wand.levelKnown=false;assertEquals(1,recipe.sampleOutput(inputs).quantity());wand.levelKnown=known;wand.storeInBundle(after);assertEquals(type.getSimpleName(),before.toString(),after.toString());
   assertEquals(expected,recipe.brew(inputs).quantity());assertEquals(0,wand.quantity());assertFalse("Real conversion retains its original inactive-infusion cleanup",wand.curseInfusionBonus);checked++;
  }
  assertEquals(1248,checked);String text=UtilityItemExplanation.details(new com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin(),hero);assertTrue(text.contains("5 alchemy energy"));assertTrue(text.contains("current extra resin yield from Preservation is 0 resin units"));
 }
 @Test public void holsterRechargeIntervalsMatchActualScheduledChargeProgress(){
  for(float factor:new float[]{com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.normalRechargeScaleFactor(),com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster.HOLSTER_SCALE_FACTOR})for(int missing=1;missing<=9;missing++){
   com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.maxCharges=10;wand.curCharges=10-missing;wand.partialCharge=0;wand.charge(hero,factor);
   try{com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.Charger charger=null;for(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.Charger candidate:hero.buffs(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.Charger.class))if(candidate.wand()==wand)charger=candidate;assertNotNull(charger);
    float original=(float)(10f+40f*Math.pow(factor,missing));assertEquals(Float.floatToIntBits(original),Float.floatToIntBits(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.baselineRechargeTurns(missing,factor)));charger.act();assertEquals(1f/original,wand.partialCharge,.0000001f);assertEquals(10-missing,wand.curCharges);
   }finally{wand.stopCharging();}
  }
 }
 @Test public void holsterStoredWandReadoutsUseKnownDeficitsWithoutChargingOrLeakingLevels(){
  com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster holster=new com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster();holster.owner=hero;hero.belongings.backpack.items.add(holster);
  com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.level(3);wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.curCharges=wand.maxCharges-2;wand.partialCharge=.25f;holster.items.add(wand);
  String before=savedDescriptionHero();String detail=UtilityItemExplanation.details(holster,hero);
  String comparison=ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.baselineRechargeTurns(2,com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.normalRechargeScaleFactor()))+" → "+ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.baselineRechargeTurns(2,com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster.HOLSTER_SCALE_FACTOR));
  assertTrue(detail,detail.contains("2 missing charges"));assertTrue(detail,detail.contains(comparison));assertEquals(before,savedDescriptionHero());assertEquals(.25f,wand.partialCharge,0);
  wand.curCharges=wand.maxCharges;wand.partialCharge=.8f;before=savedDescriptionHero();assertTrue(UtilityItemExplanation.details(holster,hero).contains("full or overcharged"));assertEquals(before,savedDescriptionHero());
  wand.levelKnown=false;wand.level(0);String low=UtilityItemExplanation.details(holster,hero);wand.level(7);assertEquals(low,UtilityItemExplanation.details(holster,hero));
  wand.levelKnown=true;wand.curChargeKnown=false;wand.curCharges=0;String unknownCharges=UtilityItemExplanation.details(holster,hero);wand.curCharges=wand.maxCharges;assertEquals(unknownCharges,UtilityItemExplanation.details(holster,hero));
  Random.pushGenerator(672);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(672);try{UtilityItemExplanation.details(holster,hero);assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
 }
 @Test public void holsterExplanationDistinguishesNormalThrownWearFromSpecialDartRules(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife knife=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife();knife.levelKnown=knife.cursedKnown=true;float damage=knife.max();knife.holster=false;float ordinary=knife.previewDurabilityPerUse();knife.holster=true;float holstered=knife.previewDurabilityPerUse();assertTrue(holstered<ordinary);assertEquals(damage,knife.max(),0);
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart rot=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart();rot.holster=false;float outside=rot.previewDurabilityPerUse();rot.holster=true;assertEquals(outside,rot.previewDurabilityPerUse(),0);assertEquals(20,outside,0);
  String text=UtilityItemExplanation.details(new com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster(),hero);assertTrue(text.contains("20%"));assertTrue(text.contains("Special dart types"));assertTrue(text.contains("whole throws left"));
 }
 @Test public void pickaxeAbilityDetailsMatchActualConditionalDamageInput(){
  com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe pick=new com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe();pick.levelKnown=pick.cursedKnown=true;
  com.shatteredpixel.shatteredpixeldungeon.actors.Char[] qualified={new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab(),new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bee(),new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Swarm(),new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Spinner(),new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Scorpio(),new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100()};
  com.shatteredpixel.shatteredpixeldungeon.actors.Char ordinary=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();
  for(int level:new int[]{0,1,3,8})for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment augment:com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.values()){
   pick.level(level);pick.augment=augment;String detail=WeaponAbilityExplanation.details(pick,hero);int expected=augment.damageFactor(8+2*pick.buffedLvl());
   assertTrue(detail,detail.contains("amount: "+expected+" damage points"));
   for(com.shatteredpixel.shatteredpixeldungeon.actors.Char target:qualified)assertEquals(target.getClass().getSimpleName(),expected,pick.pierceDamageBoost(target));
   assertEquals(0,pick.pierceDamageBoost(ordinary));assertEquals(0,pick.pierceDamageBoost(null));
  }
  String detail=WeaponAbilityExplanation.details(pick,hero);assertTrue(detail.contains("only to inorganic targets"));assertTrue(detail.contains("three scheduled game-time units"));assertTrue(detail.contains("not a universal 100%"));
 }
 @Test public void pickaxeAbilityInspectionKeepsHiddenLevelsAndStatePrivate(){
  com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe pick=new com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe();pick.levelKnown=false;pick.cursedKnown=true;pick.level(0);String low=WeaponAbilityExplanation.details(pick,hero);pick.level(7);assertEquals(low,WeaponAbilityExplanation.details(pick,hero));
  pick.levelKnown=true;com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();pick.storeInBundle(before);String heroBefore=savedDescriptionHero();
  Random.pushGenerator(721);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(721);try{WeaponAbilityExplanation.details(pick,hero);assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
  pick.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(heroBefore,savedDescriptionHero());
 }
 @Test public void chaliceDescriptionDelayMatchesActualRegenerationAcrossLevels(){
  hero.HP=10;
  for(int level:new int[]{0,3,9,10}){
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood chalice=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood();chalice.level(level);chalice.levelKnown=chalice.cursedKnown=true;hero.belongings.artifact=chalice;chalice.activate(hero);
   Regeneration regen=Buff.affect(hero,Regeneration.class);regen.act();com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();regen.storeInBundle(state);float actualDelay=1/state.getFloat(Regeneration.PARTIAL_REGEN);
   assertEquals(10f-(1.33f+level*.667f),actualDelay,.00001f);String detail=ArtifactExplanation.details(chalice);assertTrue(detail,detail.contains(ObfuscationExplanation.number(actualDelay)+" turns per HP"));String summary=ArtifactExplanation.summary(chalice);assertTrue(summary,summary.contains("1 HP every "+ObfuscationExplanation.number(actualDelay)+" turns"));assertTrue(summary.contains("Does not heal while starving"));assertFalse(summary.contains("Pricking upgrades"));assertEquals(level<10,detail.contains("next prick"));assertEquals(level==10,detail.contains("cannot be pricked again"));
   regen.detach();hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood.chaliceRegen.class).detach();
  }
 }
 @Test public void chaliceOrdinaryDescriptionWarnsWhenActualRegenerationIsDisabled()throws Exception{
  hero.sprite=quietSprite();hero.HP=1;com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);
  for(int level:new int[]{0,3,10}){com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood chalice=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood();chalice.level(level);chalice.levelKnown=chalice.cursedKnown=true;hero.belongings.artifact=chalice;chalice.activate(hero);String text=chalice.info();assertTrue(text,text.contains("rules prevent natural healing"));assertEquals(text,level<10,text.contains("upgrades the Chalice"));assertEquals(level<10,chalice.actions(hero).contains("PRICK"));Regeneration regen=Buff.affect(hero,Regeneration.class);for(int turn=0;turn<30;turn++)regen.act();assertEquals(1,hero.HP);regen.detach();hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood.chaliceRegen.class).detach();}
 }
 @Test public void hornDescriptionsPreserveActualPartialMealAndChargedCursedUse()throws Exception{
  hero.sprite=quietSprite();java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge"),hungerLevel=Hunger.class.getDeclaredField("level");charge.setAccessible(true);hungerLevel.setAccessible(true);Hunger hunger=Buff.affect(hero,Hunger.class);int previousFood=Statistics.foodEaten;java.lang.reflect.Field selector=com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector"),user=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser"),item=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curItem");selector.setAccessible(true);user.setAccessible(true);item.setAccessible(true);Object oldSelector=selector.get(null),oldUser=user.get(null),oldItem=item.get(null);java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);selector.set(null,unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.shatteredpixel.shatteredpixeldungeon.apd.qol.WaitingPolicyTest.QuietSelector.class));
  try{for(int challenge:new int[]{0,Challenges.NO_FOOD})for(boolean cursed:new boolean[]{false,true})for(int reserve:new int[]{50,179,181,450}){
   Dungeon.challenges=challenge;com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty();horn.levelKnown=horn.cursedKnown=true;horn.cursed=cursed;hero.belongings.artifact=horn;charge.setInt(horn,2);hungerLevel.setFloat(hunger,reserve);
   String text=horn.info();assertFalse(text,text.contains("eat until you're full"));assertFalse(text.contains("as many as needed"));if(cursed){assertTrue(text.contains("Other food is"));assertTrue(text.contains("stops recharging"));assertFalse(text.contains("cannot be used"));horn.charge(hero,4);assertEquals(2,horn.currentCharges());}
   assertTrue(horn.actions(hero).contains("EAT"));int satiety=challenge==0?90:30,used=Math.min(2,Math.max(1,reserve/satiety));float start=hero.cooldown();horn.execute(hero,"EAT");assertEquals(2-used,horn.currentCharges());assertEquals(Math.max(0,reserve-satiety*used),hunger.hungerPoints(),.00001f);assertEquals(com.shatteredpixel.shatteredpixeldungeon.items.food.Food.TIME_TO_EAT,hero.cooldown()-start,0);
  }}finally{Statistics.foodEaten=previousFood;selector.set(null,oldSelector);user.set(null,oldUser);item.set(null,oldItem);}
 }
 @Test public void featherFallDescriptionMatchesActualProtectedLandingAndExtraTimeCost()throws Exception{
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall();potion.apply(hero);com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall.FeatherBuff feather=hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall.FeatherBuff.class);float before=feather.cooldown();int hp=hero.HP;com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm.heroLand();assertEquals(hp,hero.HP);assertNull(hero.buff(Cripple.class));assertNull(hero.buff(Bleeding.class));assertEquals(before-10,feather.cooldown(),0);assertTrue(potion.info(),potion.info().contains("10 extra turns"));assertTrue(potion.info().contains("50 turns"));
 }
 @Test public void arcaneArmorElixirDoesNotPromiseToKeepHigherStrengthWithShorterLifetime(){
  hero.lvl=10;com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor();ArcaneArmor first=Buff.affect(hero,ArcaneArmor.class);first.set(20,1);potion.apply(hero);assertEquals(10,first.level());assertEquals(80,first.decayInterval());assertTrue(potion.info(),potion.info().contains("does not stack"));assertFalse(potion.info().contains("stronger existing protection is kept"));first.detach();ArcaneArmor longLived=Buff.affect(hero,ArcaneArmor.class);longLived.set(9,200);potion.apply(hero);assertEquals(9,longLived.level());assertEquals(200,longLived.decayInterval());
 }
 @Test public void hornExperienceChargeNumbersMatchActualRecharge(){
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty();horn.levelKnown=horn.cursedKnown=true;hero.belongings.artifact=horn;horn.activate(hero);
  hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty.hornRecharge.class).gainCharge(1);
  com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();horn.storeInBundle(state);assertEquals(1.25f,horn.currentCharges()+state.getFloat("partialcharge"),.0001f);
  assertTrue(ArtifactExplanation.details(horn).contains("1.25 charges per whole hero-level"));assertTrue(ArtifactExplanation.summary(horn).contains("refills _90 hunger_"));
 }
 @Test public void unknownArtifactNumbersStayHiddenAcrossLevelAndChargeChanges(){
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood a=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood();a.levelKnown=false;String detail=ArtifactExplanation.details(a);a.level(9);assertEquals(detail,ArtifactExplanation.details(a));assertEquals("",ArtifactExplanation.summary(a));
 }
 @Test public void capeChargeCoefficientMatchesActualProcWithoutAnActivation(){
  for(int level:new int[]{0,3,6}){com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns cape=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns();cape.level(level);cape.levelKnown=cape.cursedKnown=true;cape.activate(hero);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns.Thorns effect=hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns.Thorns.class);assertEquals(10,effect.proc(10,null,hero));assertEquals((int)(10*(.5+level*.05)),cape.currentCharges());String details=ArtifactExplanation.details(cape);assertTrue(details,details.contains(ObfuscationExplanation.number(.5f+.05f*level)+" charge points per damage point"));effect.detach();}
 }
 @Test public void capePlainReflectionDescriptionMatchesActualAdjacentAndDistantProc(){
  for(boolean adjacent:new boolean[]{false,true})for(int seed=0;seed<24;seed++){
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns cape=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns();cape.level(10);cape.levelKnown=cape.cursedKnown=true;cape.activate(hero);cape.charge(hero,25f);assertTrue(cape.effectCountdown()>0);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns.Thorns thorns=hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns.Thorns.class);LightningDamageTarget enemy=new LightningDamageTarget();enemy.pos=hero.pos+(adjacent?1:2);
   Random.pushGenerator(seed);int blocked=Random.NormalIntRange(0,17);Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(17-blocked,thorns.proc(17,enemy,hero));}finally{Random.popGenerator();}assertEquals(adjacent?blocked:0,enemy.received);assertTrue(thorns.desc(),thorns.desc().contains("from none to all"));assertTrue(thorns.desc(),thorns.desc().contains("attacker is adjacent"));thorns.detach();
  }
 }
 @Test public void slowChillAndSpeedDetailsMatchActualActionTimeAndPriority(){
  Chill chill=Buff.affect(hero,Chill.class,3);float factor=chill.speedFactor(),start=hero.cooldown();String detail=MechanicsDetails.buff(chill,chill.desc());hero.spend(1);assertEquals(1/factor,hero.cooldown()-start,.00001f);assertTrue(detail.contains("Current action speed: "+ObfuscationExplanation.number(100*factor)+"% of normal"));
  Slow slow=Buff.affect(hero,Slow.class,5);start=hero.cooldown();hero.spend(1);assertEquals(2,hero.cooldown()-start,.00001f);assertTrue(MechanicsDetails.buff(slow,slow.desc()).contains("Slow takes priority"));
  Speed speed=Buff.affect(hero,Speed.class,5);assertEquals("Speed",speed.name());assertTrue(speed.desc(),speed.desc().contains("Actions take half their usual time"));assertTrue(speed.desc(),speed.desc().contains("Turns remaining: "+ObfuscationExplanation.number(speed.visualcooldown())));start=hero.cooldown();hero.spend(1);assertEquals(1,hero.cooldown()-start,.00001f);assertTrue(MechanicsDetails.buff(speed,"speed").contains("cancel Slow"));
 }
 public static class QuietMovementSprite extends com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite {@Override public void operate(int cell){}@Override public void updateArmor(){}@Override public com.watabou.noosa.particles.Emitter emitter(){return new com.watabou.noosa.particles.Emitter();} @Override public void add(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.State state){} @Override public void remove(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.State state){} @Override public void sprint(float speed){} @Override public void showStatus(int color,String text,Object...args){} @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}}
 private QuietMovementSprite quietSprite()throws Exception{java.lang.reflect.Field field=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");field.setAccessible(true);Object unsafe=field.get(null);return (QuietMovementSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,QuietMovementSprite.class);}
 @Test public void hasteDetailsAgreeWithActualMovementFactorAndDoNotClaimFasterAttacks()throws Exception{java.lang.reflect.Field field=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");field.setAccessible(true);Object unsafe=field.get(null);hero.sprite=(QuietMovementSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,QuietMovementSprite.class);float before=hero.speed();Haste haste=Buff.affect(hero,Haste.class,5);assertEquals(before*3,hero.speed(),.00001f);assertTrue(MechanicsDetails.buff(haste,haste.desc()).contains("Attacks and recharging are unchanged"));float timer=haste.cooldown();String detail=MechanicsDetails.buff(haste,haste.desc());assertTrue(detail.contains(ObfuscationExplanation.number(haste.visualcooldown())+" turns."));assertFalse(detail.contains("game-time units"));assertFalse(detail.contains("counter adds one"));assertEquals(timer,haste.cooldown(),0);com.watabou.utils.Bundle clock=new com.watabou.utils.Bundle();haste.storeInBundle(clock);clock.put("time",clock.getFloat("time")-haste.cooldown()+.0001f);haste.restoreFromBundle(clock);String rounded=MechanicsDetails.buff(haste,haste.desc());assertTrue(rounded.contains("1 turn."));assertFalse(rounded.contains("1 turns"));assertFalse(new Drowsy().desc().contains("1 turns"));}
 @Test public void protectiveShadowsDetailsMatchActualGainLimitAndStrongerShieldPreservation()throws Exception{
  hero.sprite=quietSprite();hero.invisible=1;
  for(int rank=1;rank<=2;rank++){
   hero.talents.get(0).put(Talent.PROTECTIVE_SHADOWS,rank);Buff.detach(hero,Barrier.class);
   Talent.ProtectiveShadowsTracker tracker=new Talent.ProtectiveShadowsTracker();tracker.target=hero;
   tracker.act();assertEquals(1,hero.buff(Barrier.class).shielding());tracker.act();assertEquals(rank==1?1:2,hero.buff(Barrier.class).shielding());
   for(int i=0;i<20;i++)tracker.act();assertEquals(1+2*rank,hero.buff(Barrier.class).shielding());
   String before=savedDescriptionHero(),text=MechanicsDetails.talent(Talent.PROTECTIVE_SHADOWS,rank,"");assertTrue(text,text.contains((1+2*rank)+" shield points"));assertTrue(text,text.contains(rank==1?"every two turns":"every turn"));assertEquals(before,savedDescriptionHero());
   hero.buff(Barrier.class).setShield(20);hero.buff(Barrier.class).act();assertTrue(hero.buff(Barrier.class).shielding()<20);hero.buff(Barrier.class).setShield(20);tracker.act();assertEquals(20,hero.buff(Barrier.class).shielding());assertEquals(0,hero.buff(Barrier.class).decayProgress(),0);
  }
  assertTrue(MechanicsDetails.talent(Talent.PROTECTIVE_SHADOWS,0,"").contains("No effect until learned"));
 }
 @Test public void talentDetailsRefreshRankAndDistinguishTemporaryFromActualUpgrades(){String first=MechanicsDetails.talent(Talent.ENHANCED_RINGS,1,"");String next=MechanicsDetails.talent(Talent.ENHANCED_RINGS,3,"");assertTrue(first.contains("for 3 turns"));assertTrue(next.contains("for 9 turns"));assertTrue(next.contains("does not increase Might"));assertTrue(MechanicsDetails.talent(Talent.HOLD_FAST,3,"").contains("are paused"));assertTrue(MechanicsDetails.talent(Talent.PROTECTIVE_SHADOWS,2,"").contains("5 shield points"));}
 @Test public void passiveGlyphPreviewUsesInactiveSentinelForAllNegativeArmorLevels(){
  ring(1);Armor.Glyph[] glyphs={new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow(),new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness(),new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Camouflage(),new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic()};
  for(Armor.Glyph glyph:glyphs){armor.inscribe(glyph);for(int level:new int[]{-1,-2,-5}){armor.level(level);assertEquals(-1,hero.glyphLevel(glyph.getClass()));String info=ArcanaInfoDisplay.armor(armor);assertTrue(info,info.contains("does nothing"));}}
 }
 @Test public void swiftnessDescriptionMatchesActualThreeStepPassablePathCondition(){
  descriptionSightArena();Dungeon.level.setSize(15,15);java.util.Arrays.fill(Dungeon.level.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL);hero.pos=7*15+5;for(int x=0;x<=5;x++)Dungeon.level.map[hero.pos+x]=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY;Dungeon.level.buildFlagMaps();
  armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness());armor.level(3);ring(2);com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat enemy=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();enemy.pos=hero.pos+3;com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(enemy);
  try{
   String text=GlyphExplanation.playerDescription(armor,hero);assertTrue(text.contains("3-tile walking path"));float boosted=(1.2f+.04f*3)*Armor.Glyph.genericProcChanceMultiplier(hero);
   assertEquals(1,com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness.speedBoost(hero,3),0);enemy.pos=hero.pos+4;assertEquals(boosted,com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness.speedBoost(hero,3),0);
   enemy.pos=hero.pos+3;for(int blocker:new int[]{com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.TRAP,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.CHASM}){
    com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(hero.pos+2,blocker);assertEquals(boosted,com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness.speedBoost(hero,3),0);assertEquals("Inspection describes the condition without reporting unseen enemy state",text,GlyphExplanation.playerDescription(armor,hero));
   }
   assertEquals(1,com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness.speedBoost(hero,-1),0);
  }finally{com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(enemy);}
 }
 @Test public void flowSummaryMatchesActualMovementContributionWithRingAndAura(){armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow());armor.level(3);ring(2);Buff.affect(hero,AuraOfProtection.AuraBuff.class);Dungeon.level.water[hero.pos]=true;float actual=com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow.speedBoost(hero,hero.glyphLevel(com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow.class));String text=GlyphExplanation.summary(armor,hero);assertTrue(text,text.contains(ObfuscationExplanation.number(actual)+" times normal"));String ordinary=GlyphExplanation.playerDescription(armor,hero);assertTrue(ordinary,ordinary.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",actual)+"×"));}
 @Test public void passiveGlyphArcanaComparisonsMatchActualEffectsAndHideUnknownRings(){
  ring(3);for(int level:new int[]{0,3,8}){
   armor.level(level);armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Camouflage());com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Camouflage.activate(hero,level);int actual=Math.round(hero.buff(Invisibility.class).cooldown());String text=GlyphExplanation.playerDescription(armor,hero);assertTrue(text,text.contains("_"+actual+" turns_"));int base=Math.round(3+level/2f);assertFalse(text,text.contains("Arcana"));Buff.detach(hero,Invisibility.class);
  }
  armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow());armor.level(3);String before=savedDescriptionHero();String text=GlyphExplanation.playerDescription(armor,hero);assertFalse(text,text.contains("Arcana"));assertEquals(before,savedDescriptionHero());hero.belongings.ring.levelKnown=false;assertFalse(GlyphExplanation.playerDescription(armor,hero).contains("without Arcana"));
 }
 @Test public void antiMagicDetailsMatchActualProtectionRollBounds(){armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic());armor.level(3);ring(2);float power=Armor.Glyph.genericProcChanceMultiplier(hero);int min=Math.round(3*power),max=Math.round(7.5f*power);String text=GlyphExplanation.summary(armor,hero);assertTrue(text,text.contains(min+"–"+max+" damage-point"));String ordinary=GlyphExplanation.playerDescription(armor,hero);assertTrue(ordinary,ordinary.contains("_"+min+"–"+max+" damage_"));Random.pushGenerator(45);try{for(int i=0;i<50;i++){int actual=com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic.drRoll(hero,3);assertTrue(actual>=min&&actual<=max);}}finally{Random.popGenerator();}}
 @Test public void glyphCalculationsDoNotRevealUnknownLevelsOrConsumeRng(){armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity());armor.levelKnown=false;armor.level(0);String text=GlyphExplanation.details(armor,hero);armor.level(9);assertEquals(text,GlyphExplanation.details(armor,hero));Random.pushGenerator(45);int next=Random.Int();Random.popGenerator();Random.pushGenerator(45);try{armor.levelKnown=true;for(int i=0;i<100;i++)GlyphExplanation.details(armor,hero);assertEquals(next,Random.Int());}finally{Random.popGenerator();}}
 @Test public void stoneAndViscosityExplainDifferentMechanicsRatherThanUniversalProcChance(){armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Stone());String stone=GlyphExplanation.details(armor,hero);assertTrue(stone.contains("at most 75%"));assertTrue(stone.contains("depending on that attacker"));armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity());String viscosity=GlyphExplanation.details(armor,hero);assertTrue(viscosity.contains("For q > 1"));assertTrue(viscosity.contains("not automatically cancelled"));}
 private static class ChargeObserver extends Belongings {float offered;boolean called;ChargeObserver(Hero hero){super(hero);}@Override public int charge(float amount){called=true;offered=amount;return 0;}}
 @Test public void potentialPreviewMatchesRealActivationBoundaryAndChargeStrength(){
  ChargeObserver observed=new ChargeObserver(hero);hero.belongings=observed;hero.belongings.armor=armor;armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Potential());RingOfArcana arcana=ring(0);
  for(int ringLevel:new int[]{0,4,12}){arcana.level(ringLevel);for(int level:new int[]{0,3,9}){armor.level(level);float chance=(level+1f)/(level+6f)*Armor.Glyph.genericProcChanceMultiplier(hero);String description=GlyphExplanation.summary(armor,hero);assertTrue(description,description.contains(ObfuscationExplanation.number(100*Math.min(1,chance))+"%"));
   for(int seed=0;seed<20;seed++){observed.called=false;observed.offered=0;Random.pushGenerator(seed);float roll=Random.Float();int next=Random.Int();Random.popGenerator();Random.pushGenerator(seed);try{armor.glyph.proc(armor,null,hero,5);assertEquals(roll<chance,observed.called);if(observed.called)assertEquals(Math.max(1,chance),observed.offered,0);assertEquals(next,Random.Int());}finally{Random.popGenerator();}}
  }}
 }
 @Test public void blockingShieldDurationDetailsMatchHoldFastDecayAndNeverDetachIt(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking.BlockBuff block=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking.BlockBuff();block.target=hero;block.setShield(10);HoldFast hold=Buff.affect(hero,HoldFast.class);hold.pos=hero.pos;hero.talents.get(2).put(Talent.HOLD_FAST,1);
  String first=MechanicsDetails.buff(block,block.desc());assertTrue(first,first.contains("reduces it by 0.5 each turn"));block.act();assertEquals(4.5f,block.remainingDecay(),0);
  hero.talents.get(2).put(Talent.HOLD_FAST,3);block.act();assertEquals(4.5f,block.remainingDecay(),0);assertTrue(MechanicsDetails.buff(block,block.desc()).contains("reduces it by 0 each turn"));
  hero.pos++;assertTrue(MechanicsDetails.buff(block,block.desc()).contains("reduces it by 1 each turn"));assertSame(hold,hero.buff(HoldFast.class));
 }
 @Test public void enchantmentPowerMatchesAllActiveContributionsWithoutConsumingThem(){
  ring(2);
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade.RunicSlashTracker slash=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade.RunicSlashTracker.class);slash.boost=4;
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike.DirectedPowerTracker directed=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike.DirectedPowerTracker.class);directed.enchBoost=2;
  Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite.SmiteTracker.class);
  float expected=com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.genericProcChanceMultiplierReadOnly(hero);assertEquals(expected,EnchantmentExplanation.knownPower(hero),0);
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=true;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling());
  Random.pushGenerator(773);int next=Random.Int();Random.popGenerator();Random.pushGenerator(773);try{for(int i=0;i<100;i++)EnchantmentExplanation.describe(sword,hero,true);assertEquals(next,Random.Int());}finally{Random.popGenerator();}
  assertSame(slash,hero.buff(slash.getClass()));assertEquals(4,slash.boost,0);assertSame(directed,hero.buff(directed.getClass()));assertEquals(2,directed.enchBoost,0);
 }
 @Test public void vampiricDescriptionUsesActualCurrentHpChanceAndHealingAmount()throws Exception{
  hero.sprite=quietSprite();ring(0);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=true;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric());com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat target=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();
  for(int seed=0;seed<30;seed++){hero.HP=10;float chance=(.05f+.25f*.5f)*EnchantmentExplanation.knownPower(hero);String description=EnchantmentExplanation.describe(sword,hero,false);assertTrue(description,description.contains(ObfuscationExplanation.number(100*chance)+"%"));Random.pushGenerator(seed);boolean trigger=Random.Float()<chance;Random.popGenerator();Random.pushGenerator(seed);try{sword.enchantment.proc(sword,hero,target,10);}finally{Random.popGenerator();}assertEquals(trigger?15:10,hero.HP);}
  hero.HP=10;String before=EnchantmentExplanation.describe(sword,hero,false);sword.level(9);assertEquals(before,EnchantmentExplanation.describe(sword,hero,false));
 }
 @Test public void vampiricOrdinaryDescriptionUsesCurrentHealingRuleWithoutSpendingFractionalCredit()throws Exception{
  hero.sprite=quietSprite();hero.HT=100;RingOfArcana arcana=ring(0);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=sword.cursedKnown=true;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric());com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat enemy=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();
  for(int percent:new int[]{25,50,100,200})for(int ringLevel:new int[]{0,3,12})for(int damage:new int[]{5,12,20}){
   com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,percent).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);arcana.level(ringLevel);hero.HP=1;hero.apdCustomHealingFraction=.75f;float chance=(.05f+.25f*.99f)*EnchantmentExplanation.knownPower(hero),strength=Math.max(1,chance);String before=savedDescriptionHero(),description=EnchantmentExplanation.playerDescription(sword,hero);String amount=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",50*strength*percent/100f);assertTrue(description,description.contains("_"+amount+"%_"));assertEquals(before,savedDescriptionHero());
   int seed=0;for(;seed<10000;seed++){Random.pushGenerator(seed);boolean passes=Random.Float()<chance;Random.popGenerator();if(passes)break;}assertTrue(seed<10000);int expected=com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.preview(hero,Math.round(damage*.5f*strength));Random.pushGenerator(seed);try{sword.enchantment.proc(sword,hero,enemy,damage);}finally{Random.popGenerator();}assertEquals(percent+"% healing/ring"+ringLevel+"/damage"+damage,expected,hero.HP-1);
  }
 }
 @Test public void elementalStrikeNormalDescriptionNeverNamesAnUnknownWeaponCurse()throws Exception{
  com.watabou.noosa.Game previousGame=com.watabou.noosa.Game.instance;java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);Object game=unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.watabou.noosa.Game.class);java.lang.reflect.Field scene=com.watabou.noosa.Game.class.getDeclaredField("scene");scene.setAccessible(true);scene.set(game,unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class));com.watabou.noosa.Game.instance=(com.watabou.noosa.Game)game;
  try{
   com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=sword.cursedKnown=false;hero.belongings.weapon=sword;com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike();String ordinary=ability.desc();
   for(String name:new String[]{"Annoying","Dazzling","Displacing","Explosive","Friendly","Polarized","Pressurized","Sacrificial","Wayward","Wondrous"}){
    sword.enchant((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses."+name).getDeclaredConstructor().newInstance());sword.cursedKnown=false;for(int hidden:new int[]{-2,0,8}){sword.level(hidden);String before=savedDescriptionHero();assertEquals(name+" hidden curse must match no disclosed enchantment",ordinary,ability.desc());assertEquals(before,savedDescriptionHero());}sword.cursedKnown=true;assertTrue(name,ability.desc().toLowerCase(java.util.Locale.ROOT).contains(sword.enchantment.name().toLowerCase(java.util.Locale.ROOT)));assertNotEquals(ordinary,ability.desc());
   }
  }finally{com.watabou.noosa.Game.instance=previousGame;}
 }
 @Test public void intrinsicReachDescriptionsMatchActualWeaponsAtKnownAndUnknownUpgradeLevels(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon[] weapons={new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip()};
  for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon weapon:weapons)for(boolean known:new boolean[]{false,true})for(int level:new int[]{0,3,8}){weapon.level(level);weapon.levelKnown=known;weapon.cursedKnown=true;String expected="Base reach: _"+weapon.RCH+" tiles_.";assertEquals(expected,weapon.statsInfo());assertTrue(weapon.info(),weapon.info().contains(expected));}
 }
 @Test public void projectingOrdinaryContributionStaysCorrectWithStanceAndAscensionReach(){
  ring(3);RingOfForce force=new RingOfForce();force.anonymize();force.levelKnown=force.cursedKnown=true;hero.belongings.misc=force;force.activate(hero);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip whip=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip();whip.levelKnown=whip.cursedKnown=true;whip.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting());hero.belongings.weapon=whip;com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce.BrawlersStance stance=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce.BrawlersStance.class);
  for(boolean brawling:new boolean[]{false,true})for(boolean ascended:new boolean[]{false,true}){
   stance.active=brawling;if(ascended)Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.class);else Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.class);String before=savedDescriptionHero(),text=whip.info();assertTrue(text,text.contains("Adds up to _2 tiles_ to melee reach."));assertEquals(before,savedDescriptionHero());int intrinsic=(brawling?1:whip.RCH)+(ascended?2:0);assertEquals(intrinsic+2,whip.reachFactor(hero));
  }
 }
 @Test public void crystalOrdinaryDescriptionRetainsVisibleConditionWithoutRevealingExactWear(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=sword.cursedKnown=true;com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal crystal=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal();sword.enchant(crystal);int[] wear={100,80,50,11,10,1};String[] keys={"desc_perfect","desc_fine","desc_flawed","desc_flawed","desc_cracked","desc_cracked"};
  for(int i=0;i<wear.length;i++){
   crystal.setDurability(wear[i]);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();crystal.storeInBundle(before);String text=sword.info();assertTrue(text,text.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(crystal,keys[i])));assertTrue(text,text.contains("Can shatter if worn out"));crystal.storeInBundle(after);assertEquals(before.toString(),after.toString());
  }
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife knife=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife();knife.levelKnown=knife.cursedKnown=true;knife.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal());String text=knife.info();assertTrue(text,text.contains("much less durability"));assertFalse(text,text.contains("Can shatter if worn out"));
 }
 public static class KineticDescriptionTarget extends PlainStatusTarget {@Override public void die(Object cause){}}
 @Test public void kineticOrdinaryArcanaComparisonMatchesRealNewStorageAndExcludesReusedBonus()throws Exception{
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=sword.cursedKnown=true;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic());
  for(int ringLevel:new int[]{-2,0,3}){
   for(com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana.Arcana active:hero.buffs(com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana.Arcana.class))active.detach();ring(ringLevel);float power=com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.genericProcChanceMultiplierReadOnly(hero);String text=sword.info();assertTrue(text,text.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",100*power)+"%"));
   for(int reused:new int[]{0,10}){
    Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic.ConservedDamage.class);if(reused>0)Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic.ConservedDamage.class).setBonus(reused);KineticDescriptionTarget target=new KineticDescriptionTarget();target.HP=5;target.pos=hero.pos+1;target.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;target.sprite=quietSprite();int damage=sword.enchantment.proc(sword,hero,target,20);assertEquals(20+reused,damage);target.damage(damage,hero);assertEquals(Math.round(15*power),hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic.ConservedDamage.class).damageBonus());
   }
  }
 }
 public static class VorpalDescriptionTarget extends PlainStatusTarget {VorpalDescriptionTarget(boolean immune){if(immune)immunities.add(Bleeding.class);}}
 @Test public void vorpalPlainDescriptionMatchesActualDamageReplacementAndImmunity()throws Exception{
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=sword.cursedKnown=true;com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vorpal enchant=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vorpal();sword.enchant(enchant);assertTrue(sword.info(),sword.info().contains("replace a hit's immediate damage with bleeding"));long seed=0;for(;seed<1000;seed++){Random.pushGenerator(seed);float roll=Random.Float();Random.popGenerator();if(roll<.25f)break;}assertTrue(seed<1000);
  for(boolean immune:new boolean[]{false,true}){
   VorpalDescriptionTarget target=new VorpalDescriptionTarget(immune);target.pos=hero.pos+1;target.sprite=quietSprite();target.sprite.scale=new com.watabou.utils.PointF(1,1);Barrier shield=Buff.affect(target,Barrier.class);shield.setShield(7);Random.pushGenerator(seed);try{assertEquals(10,enchant.proc(sword,hero,target,10));target.damage(10,hero);}finally{Random.popGenerator();}
   assertEquals(immune?97:100,target.HP);assertEquals(!immune,target.buff(Bleeding.class)!=null);assertEquals(immune?0:7,target.shielding());Buff.detach(target,Bleeding.class);Buff.detach(target,Barrier.class);Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vorpal.VorpalTracker.class);
  }
 }
 @Test public void vampiricNormalDescriptionMatchesDisabledHealingEvenBeforeUpgradesKnown()throws Exception{
  hero.sprite=quietSprite();hero.HP=10;ring(0);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.cursedKnown=true;sword.levelKnown=false;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric());com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);assertTrue(sword.info(),sword.info().contains("rules prevent this enchantment's healing"));PlainStatusTarget enemy=new PlainStatusTarget();enemy.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;boolean triggered=false;
  for(int seed=0;seed<30;seed++){Random.pushGenerator(seed);triggered|=Random.Float()<(.05f+.25f*.5f)*EnchantmentExplanation.knownPower(hero);Random.popGenerator();Random.pushGenerator(seed);try{sword.enchantment.proc(sword,hero,enemy,10);}finally{Random.popGenerator();}assertEquals(10,hero.HP);}assertTrue(triggered);
 }
 @Test public void spiritPowerPreviewKeepsCachedRingUnchangedEvenWhenItsTypeIsKnown(){
  hero.heroClass=HeroClass.CLERIC;hero.armorAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity();Talent.initClassTalents(hero);Talent.initArmorTalents(hero);hero.talents.get(3).put(Talent.SPIRIT_FORM,2);RingOfArcana spirit=new RingOfArcana();spirit.level(90);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff.class).setEffect(spirit);
  assertTrue(Float.isNaN(EnchantmentExplanation.knownPower(hero)));assertEquals(90,spirit.level());spirit.anonymize();spirit.levelKnown=spirit.cursedKnown=true;assertEquals((float)Math.pow(1.175f,3),EnchantmentExplanation.knownPower(hero),0);assertEquals(90,spirit.level());
 }
 @Test public void spiritBowUsesTheSameConciseEnchantmentDescriptionWithoutLegacyArcanaParagraphs(){
  ring(3);com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow();bow.levelKnown=bow.cursedKnown=true;
  for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment enchantment:new com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vorpal()}){bow.enchant(enchantment);String info=bow.info(),plain=EnchantmentExplanation.playerDescription(bow,hero);assertTrue(info,info.contains(plain));assertEquals("One concise enchantment effect",1,info.split(java.util.regex.Pattern.quote(plain),-1).length-1);assertFalse(info,info.contains("Arcana changes:"));assertFalse(info,info.contains("Arcana: "));assertFalse("No empty weapon-summary paragraph",info.contains("\n\n\n"));}
 }
 @Test public void conciseSpiritBowInspectionKeepsUnknownRingValuesAndOneUseBuffsPrivate(){
  RingOfArcana arcana=ring(0);arcana.levelKnown=false;
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow();bow.levelKnown=bow.cursedKnown=true;bow.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic());
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite.SmiteTracker smite=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite.SmiteTracker.class);String original=bow.info();
  for(int level:new int[]{-2,0,8}){arcana.level(level);String before=savedDescriptionHero();com.watabou.utils.Bundle itemBefore=new com.watabou.utils.Bundle(),itemAfter=new com.watabou.utils.Bundle();bow.storeInBundle(itemBefore);assertEquals(original,bow.info());bow.storeInBundle(itemAfter);assertEquals(itemBefore.toString(),itemAfter.toString());assertEquals(before,savedDescriptionHero());assertSame(smite,hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite.SmiteTracker.class));}
 }
 @Test public void spiritBowProjectingDescribesProjectileRadiusInsteadOfMeleeReach(){com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow();bow.levelKnown=true;bow.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting());String description=EnchantmentExplanation.describe(bow,hero,false);assertTrue(description,description.contains("wall-piercing target radius"));assertFalse(description,description.contains("Adds 1 melee reach"));}
 @Test public void runicSlashReachComparisonRetainsPotentialAbilityPreview(){hero.heroClass=HeroClass.DUELIST;com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade blade=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade();blade.levelKnown=true;blade.level(6);blade.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting());ring(2);float power=EnchantmentExplanation.knownPower(hero);int expected=blade.RCH+com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting.additionalReach(power+blade.runicSlashEnchantBoost(),blade.RCH);String text=EnchantmentExplanation.describe(blade,hero,false);assertTrue(text,text.contains("total base reach would be "+expected+" tiles"));}
 @Test public void everyBuiltInOrdinaryEnchantmentHasReadableAndDetailedExplanations()throws Exception{
  String[] names={"Blazing","Blocking","Blooming","Chilling","Corrupting","Crystal","Eldritch","Elastic","Grim","Kinetic","Lucky","Projecting","Shocking","Unstable","Vampiric","Venomous","Vorpal"};com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=true;
  for(String name:names){sword.enchant((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments."+name).getConstructor().newInstance());String plain=EnchantmentExplanation.describe(sword,hero,false),details=EnchantmentExplanation.describe(sword,hero,true);assertFalse(name,plain.isEmpty());assertTrue(name,details.contains(name+" calculations"));assertFalse(name,plain.contains("NaN"));assertFalse(name,details.contains("Infinity"));}
 }
 @Test public void knownCurseDescriptionsCoverAllBuiltInsWithoutRevealingUnknownCurseIdentity()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=false;
  for(String name:new String[]{"Annoying","Dazzling","Displacing","Explosive","Friendly","Polarized","Pressurized","Sacrificial","Wayward","Wondrous"}){sword.enchant((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses."+name).getConstructor().newInstance());sword.cursedKnown=false;assertEquals(name,"",CurseExplanation.weapon(sword,hero,true));sword.cursedKnown=true;sword.level(0);String first=CurseExplanation.weapon(sword,hero,true);sword.level(9);assertFalse(name,first.isEmpty());assertEquals(name,first,CurseExplanation.weapon(sword,hero,true));}
  armor.levelKnown=false;
  for(String name:new String[]{"AntiEntropy","Bulk","Corrosion","Displacement","Metabolism","Multiplicity","Overgrowth","Stench"}){armor.inscribe((Armor.Glyph)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.armor.curses."+name).getConstructor().newInstance());armor.cursedKnown=false;assertEquals(name,"",CurseExplanation.armor(armor,hero,true));armor.cursedKnown=true;armor.level(0);String first=CurseExplanation.armor(armor,hero,true);armor.level(9);assertFalse(name,first.isEmpty());assertEquals(name,first,CurseExplanation.armor(armor,hero,true));}
 }
 @Test public void polarizedChanceAndDamageFactorMatchRealProcAtDifferentArcanaLevels(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.cursedKnown=true;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Polarized());RingOfArcana arcana=ring(0);
  for(int level:new int[]{0,4,12}){arcana.level(level);float chance=.5f*EnchantmentExplanation.knownPower(hero);String text=CurseExplanation.weapon(sword,hero,true);assertTrue(text,text.contains(ObfuscationExplanation.number(100*Math.min(1,chance))+"%"));for(int seed=0;seed<20;seed++){Random.pushGenerator(seed);boolean trigger=Random.Float()<chance;Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(trigger?15:0,sword.enchantment.proc(sword,hero,null,10));}finally{Random.popGenerator();}}}
 }
 @Test public void activeWaywardClearsWithoutANewChanceRollOrTooltipMutation(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.cursedKnown=true;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Wayward());com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Wayward.WaywardBuff penalty=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Wayward.WaywardBuff.class,10);assertTrue(CurseExplanation.weapon(sword,hero,false).contains("next eligible activation clears"));assertSame(penalty,hero.buff(penalty.getClass()));
  Random.pushGenerator(123);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(123);try{assertEquals(10,sword.enchantment.proc(sword,hero,null,10));assertEquals(expected,Random.Int());}finally{Random.popGenerator();}assertNull(hero.buff(penalty.getClass()));
 }
 @Test public void metabolismPreviewCannotSpendHungerOrHealingFractionalCredit(){
  com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),settings=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,25).store(settings);all.put("apd_custom_rules",settings);Dungeon.apdRestoreCustomRules(all);hero.HP=10;hero.apdCustomHealingFraction=.75f;armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Metabolism());armor.cursedKnown=true;float time=hero.cooldown();
  for(int i=0;i<100;i++){String text=CurseExplanation.armor(armor,hero,true);assertTrue(text,text.contains("40 hunger points"));}
  assertEquals(.75f,hero.apdCustomHealingFraction,0);assertEquals(10,hero.HP);assertEquals(time,hero.cooldown(),0);assertNull(hero.buff(Hunger.class));
 }
 @Test public void sacrificialTwoChecksUseCurrentHealthRatherThanMissingHealth(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.cursedKnown=true;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Sacrificial());
  for(int hp:new int[]{3,16}){hero.HP=hp;float first=.1f*EnchantmentExplanation.knownPower(hero),second=(float)Math.pow(hp/(float)hero.HT,2)*hero.HT/8f;String text=CurseExplanation.weapon(sword,hero,false);assertTrue(text,text.contains(ObfuscationExplanation.number(100*Math.min(1,first)*Math.min(1,second))+"%"));
   for(int seed=0;seed<100;seed++){Buff.detach(hero,Bleeding.class);Random.pushGenerator(seed);boolean trigger=Random.Float()<first&&Random.Float()<second;Random.popGenerator();Random.pushGenerator(seed);try{sword.enchantment.proc(sword,hero,null,10);}finally{Random.popGenerator();}assertEquals(trigger,hero.buff(Bleeding.class)!=null);}
  }
 }
 @Test public void artifactRechargeRemainingTextMatchesActualFractionalChargeAndBlockedCases()throws Exception{
  hero.sprite=quietSprite();java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge"),partial=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("partialCharge");charge.setAccessible(true);partial.setAccessible(true);
  for(boolean horn:new boolean[]{false,true})for(float duration:new float[]{1.5f,ArtifactRecharge.DURATION})for(int state=0;state<8;state++){
   boolean cursed=(state&1)!=0,full=(state&2)!=0,immune=(state&4)!=0;
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact item=horn?new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty():new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows();item.cursed=cursed;item.levelKnown=item.cursedKnown=true;hero.belongings.artifact=item;charge.setInt(item,full?item.maximumCharges():0);partial.setFloat(item,0);item.activate(hero);if(immune)Buff.affect(hero,MagicImmune.class);
   ArtifactRecharge recharge=Buff.affect(hero,ArtifactRecharge.class).set(duration);recharge.ignoreHornOfPlenty=false;com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();recharge.storeInBundle(before);String text=recharge.desc();com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();recharge.storeInBundle(after);assertEquals(before.toString(),after.toString());
   for(int tick=0;tick<Math.ceil(duration)+1;tick++)recharge.act();float actual=item.currentCharges()+partial.getFloat(item),expected=full?item.maximumCharges():cursed||immune?0:Math.min(item.maximumCharges(),.25f*duration);assertEquals((horn?"Horn":"Cloak")+" state"+state,expected,actual,.00001f);assertTrue(text,text.contains("Recharge remaining: "+ObfuscationExplanation.number(duration)+" turns"));assertTrue(recharge.desc().contains("Recharge remaining: 0 turns"));
   Buff.detach(hero,ArtifactRecharge.class);Buff.detach(hero,MagicImmune.class);for(Buff b:hero.buffs())if(b instanceof com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.ArtifactBuff)b.detach();hero.belongings.artifact=null;
  }
  com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMysticalEnergy scroll=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMysticalEnergy();scroll.anonymize();assertTrue(scroll.desc(),scroll.desc().contains("_"+ObfuscationExplanation.number(ArtifactRecharge.DURATION)+" turns_"));
 }
 @Test public void wandRechargeNormalRateMatchesRealPartialTickAndFullOrImmuneCharger()throws Exception{
  for(float duration:new float[]{.5f,1,30})for(boolean full:new boolean[]{false,true})for(boolean immune:new boolean[]{false,true}){
   com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.charge(hero);com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.Charger charger=null;for(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.Charger candidate:hero.buffs(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.Charger.class))if(candidate.wand()==wand)charger=candidate;assertNotNull(charger);if(immune)Buff.affect(hero,MagicImmune.class);int start=full?wand.maxCharges:0;wand.curCharges=start;wand.partialCharge=0;charger.act();float base=wand.curCharges+wand.partialCharge-start;
   wand.curCharges=start;wand.partialCharge=0;Recharging bonus=Buff.prolong(hero,Recharging.class,duration);String text=bonus.desc();charger.act();float actual=wand.curCharges+wand.partialCharge-start-base,expected=full||immune?0:com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.rechargingBonusPerTurn()*Math.min(1,duration);assertEquals(expected,actual,.00001f);assertTrue(text,text.contains("1 extra charge every "+ObfuscationExplanation.number(1f/com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.rechargingBonusPerTurn())+" turns"));
   bonus.detach();Buff.detach(hero,MagicImmune.class);wand.stopCharging();
  }
 }
 public static class DescriptionSupplyRation extends com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation {public void satisfyNow(Hero target){super.satisfy(target);}}
 @Test public void cursedCloakAndTomeTextAllowsExistingDirectFoodChargeButNotOrdinaryRecharge()throws Exception{
  hero.sprite=quietSprite();java.lang.reflect.Field meter=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge");meter.setAccessible(true);
  for(boolean immune:new boolean[]{false,true}){
   if(immune)Buff.affect(hero,MagicImmune.class);
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows cloak=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows();cloak.levelKnown=cloak.cursedKnown=cloak.cursed=true;hero.belongings.artifact=cloak;meter.setInt(cloak,0);assertTrue(cloak.info(),cloak.info().contains("recharge normally"));cloak.charge(hero,4);assertEquals(0,cloak.currentCharges());new DescriptionSupplyRation().satisfyNow(hero);assertEquals(1,cloak.currentCharges());assertFalse(cloak.actions(hero).contains("STEALTH"));
   hero.heroClass=HeroClass.CLERIC;hero.talents.get(0).put(Talent.ENLIGHTENING_MEAL,2);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome();tome.levelKnown=tome.cursedKnown=tome.cursed=true;hero.belongings.artifact=tome;meter.setInt(tome,0);assertTrue(tome.info(),tome.info().contains("recharge normally"));tome.charge(hero,4);assertEquals(0,tome.currentCharges());Talent.onFoodEaten(hero,1,null);assertEquals(1,tome.currentCharges());assertFalse(tome.actions(hero).contains("CAST"));Buff.detach(hero,MagicImmune.class);hero.heroClass=HeroClass.WARRIOR;hero.talents.get(0).remove(Talent.ENLIGHTENING_MEAL);
  }
 }
 @Test public void chainsRetainedLimitsMatchRealExperienceOverflowAndExternalRecharge()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains();item.level(5);item.levelKnown=item.cursedKnown=true;hero.belongings.artifact=item;
  java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge"),partial=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("partialCharge");charge.setAccessible(true);partial.setAccessible(true);charge.setInt(item,15);partial.setFloat(item,0);
  java.lang.reflect.Method factory=item.getClass().getDeclaredMethod("passiveBuff");factory.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains.chainsRecharge effect=(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains.chainsRecharge)factory.invoke(item);effect.target=hero;effect.act();assertEquals(15,item.currentCharges());effect.gainExp(1);effect.act();assertEquals(21,item.currentCharges());effect.gainExp(1);effect.act();assertEquals(25,item.currentCharges());assertTrue(partial.getFloat(item)>0);
  charge.setInt(item,29);partial.setFloat(item,0);item.charge(hero,10);assertEquals(34,item.currentCharges());item.charge(hero,10);assertEquals(34,item.currentCharges());String detail=ArtifactExplanation.details(item);assertTrue(detail,detail.contains("large effect accepted just below it can exceed"));assertTrue(detail,detail.contains("Growth stage: 5 of 5, displayed as +10"));assertFalse(detail.contains("normalized displayed"));
 }
 @Test public void artifactRechargeRatesMatchRealPassiveAndEventHandlers()throws Exception{
  RingOfEnergy energy=new RingOfEnergy();energy.anonymize();energy.level(2);energy.levelKnown=energy.cursedKnown=true;hero.belongings.ring=energy;energy.activate(hero);
  java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge"),partial=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("partialCharge");charge.setAccessible(true);partial.setAccessible(true);
  for(String name:new String[]{"AlchemistsToolkit","HornOfPlenty","MasterThievesArmband","SandalsOfNature","CloakOfShadows","HolyTome","TimekeepersHourglass","UnstableSpellbook","SkeletonKey","LloydsBeacon","TalismanOfForesight","DriedRose","EtherealChains"}){
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact item=(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.artifacts."+name).getConstructor().newInstance();item.levelKnown=item.cursedKnown=true;hero.belongings.artifact=item;charge.setInt(item,0);partial.setFloat(item,0);
   java.lang.reflect.Method factory=item.getClass().getDeclaredMethod("passiveBuff");factory.setAccessible(true);Buff effect=(Buff)factory.invoke(item);effect.target=hero;
   float expected=ArtifactEconomyExplanation.knownGain(item,hero);
   if(name.equals("AlchemistsToolkit")||name.equals("HornOfPlenty")||name.equals("MasterThievesArmband"))effect.getClass().getMethod("gainCharge",float.class).invoke(effect,1f);
   else if(name.equals("SandalsOfNature"))effect.getClass().getMethod("charge").invoke(effect);
   else effect.getClass().getMethod("act").invoke(effect);
   assertEquals(name,expected,item.currentCharges()+partial.getFloat(item),.00001f);assertFalse(name,ArtifactEconomyExplanation.details(item,hero).isEmpty());
  }
 }
 @Test public void unidentifiedRechargeRingCannotBeInferredFromArtifactRate(){
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass();item.levelKnown=item.cursedKnown=true;RingOfEnergy energy=new RingOfEnergy();hero.belongings.ring=energy;energy.levelKnown=false;String first=ArtifactEconomyExplanation.summary(item,hero);assertTrue(Float.isNaN(ArtifactEconomyExplanation.knownGain(item,hero)));hero.belongings.ring=new RingOfHaste();assertEquals(first,ArtifactEconomyExplanation.summary(item,hero));assertTrue(Float.isNaN(ArtifactEconomyExplanation.knownGain(item,hero)));
 }
 @Test public void artifactRatesExplainDifferentEventsAndPausedFullMeters(){
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty();horn.levelKnown=horn.cursedKnown=true;assertTrue(ArtifactEconomyExplanation.summary(horn,hero).contains("full experience level"));
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature sandals=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature();sandals.levelKnown=sandals.cursedKnown=true;assertTrue(ArtifactEconomyExplanation.summary(sandals,hero).contains("Trampling tall grass"));
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows cloak=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows();cloak.levelKnown=cloak.cursedKnown=true;assertTrue(ArtifactEconomyExplanation.summary(cloak,hero).contains("Fully charged"));
 }
 @Test public void cursedArmbandDescriptionMatchesActualGoldChanceAndZeroGoldGuard()throws Exception{
  int prior=Dungeon.gold;try{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband armband=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband();armband.cursed=armband.levelKnown=armband.cursedKnown=true;assertTrue(ArtifactEconomyExplanation.summary(armband,hero).contains("20%"));java.lang.reflect.Method factory=armband.getClass().getDeclaredMethod("passiveBuff");factory.setAccessible(true);Buff effect=(Buff)factory.invoke(armband);effect.target=hero;
   for(int seed=0;seed<100;seed++){Dungeon.gold=10;Random.pushGenerator(seed);boolean loss=Random.Int(5)==0;Random.popGenerator();Random.pushGenerator(seed);try{effect.getClass().getMethod("act").invoke(effect);}finally{Random.popGenerator();}assertEquals(loss?9:10,Dungeon.gold);}
   Dungeon.gold=0;Random.pushGenerator(456);int next=Random.Int();Random.popGenerator();Random.pushGenerator(456);try{effect.getClass().getMethod("act").invoke(effect);assertEquals(next,Random.Int());}finally{Random.popGenerator();}assertEquals(0,Dungeon.gold);
  }finally{Dungeon.gold=prior;}
 }
 @Test public void cursedHourglassDescriptionMatchesExtraTimeChance()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass();item.cursed=item.levelKnown=item.cursedKnown=true;assertTrue(ArtifactEconomyExplanation.summary(item,hero).contains("10%"));java.lang.reflect.Method factory=item.getClass().getDeclaredMethod("passiveBuff");factory.setAccessible(true);Buff effect=(Buff)factory.invoke(item);effect.target=hero;
  for(int seed=0;seed<100;seed++){Random.pushGenerator(seed);boolean delay=Random.Int(10)==0;Random.popGenerator();float before=hero.cooldown();Random.pushGenerator(seed);try{effect.getClass().getMethod("act").invoke(effect);}finally{Random.popGenerator();}assertEquals(delay?1:0,hero.cooldown()-before,0);}
 }
 @Test public void warriorShieldOrdinaryDescriptionKeepsLiveRechargeAndCreditSeparateAcrossSaveLoad()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield shield=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield.class);
  java.lang.reflect.Field cooldown=shield.getClass().getDeclaredField("cooldown");cooldown.setAccessible(true);
  for(int reserve:new int[]{0,12})for(int time:new int[]{-25,0,40}){
   shield.setShield(reserve);cooldown.setInt(shield,time);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();shield.storeInBundle(before);String text=shield.desc();shield.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(time,shield.remainingCooldown());
   assertFalse(text,text.contains("game-time units"));assertFalse(text,text.contains("Signed cooldown"));
   if(reserve>0){assertTrue(text,text.contains("Shield remaining: 12"));assertTrue(text,text.contains("Recharge time left: "+Math.max(0,time)+" turns"));assertTrue(text,text.contains("Saved recharge reduction: "+Math.max(0,-time)+" turns"));}
   else if(time<0)assertTrue(text,text.contains("25 turns"));else if(time>0)assertTrue(text,text.contains("40 turns"));else assertTrue(text,text.startsWith("Ready."));
   com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield loaded=new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield();loaded.restoreFromBundle(before);assertEquals(text,loaded.desc());
  }
 }
 @Test public void flailSpinDescriptionShowsActualCountWithoutChangingChargeOrSavedProgress(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail.SpinAbilityTracker spin=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail.SpinAbilityTracker.class,3);
  for(int count=1;count<=3;count++){spin.spins=count;String before=savedDescriptionHero();String text=spin.desc();assertTrue(text,text.contains("Spins: "+count+"/3"));assertFalse(text,text.contains("%"));assertEquals(before,savedDescriptionHero());assertEquals(count,spin.spins);}
 }
 @Test public void everyCursedArtifactHasSpecificReadOnlyOrdinaryText()throws Exception{
  com.watabou.noosa.Game previousGame=com.watabou.noosa.Game.instance;java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);com.watabou.noosa.Game.instance=(com.watabou.noosa.Game)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.watabou.noosa.Game.class);try{
  String[] names={"TimekeepersHourglass","UnstableSpellbook","MasterThievesArmband","SandalsOfNature","DriedRose","EtherealChains","HornOfPlenty","ChaliceOfBlood","TalismanOfForesight","AlchemistsToolkit","SkeletonKey","CloakOfShadows","HolyTome","LloydsBeacon","CapeOfThorns"};
  for(String name:names){
   com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact item=(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.artifacts."+name).getDeclaredConstructor().newInstance();item.levelKnown=item.cursedKnown=item.cursed=true;hero.belongings.artifact=item;
   com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();item.storeInBundle(before);String heroBefore=savedDescriptionHero();Random.pushGenerator(513);int next=Random.Int();Random.popGenerator();Random.pushGenerator(513);String text;
   try{text=item.info();assertEquals(name,next,Random.Int());}finally{Random.popGenerator();}
   if(name.equals("DriedRose")){
    java.lang.reflect.Field scene=com.watabou.noosa.Game.class.getDeclaredField("scene");scene.setAccessible(true);Object previous=scene.get(com.watabou.noosa.Game.instance);Object gameScene=unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class);
    java.lang.reflect.Field spawned=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.Quest.class.getDeclaredField("spawned");spawned.setAccessible(true);boolean oldSpawned=spawned.getBoolean(null);
    try{spawned.setBoolean(null,false);scene.set(com.watabou.noosa.Game.instance,gameScene);assertTrue(item.info(),item.info().contains("hostile wraith"));}finally{spawned.setBoolean(null,oldSpawned);scene.set(com.watabou.noosa.Game.instance,previous);}
   }
   item.storeInBundle(after);assertEquals(name,before.toString(),after.toString());assertEquals(name,heroBefore,savedDescriptionHero());
   for(String jargon:new String[]{"passive effect is equipped","base action time","no universal cursed-artifact"})assertFalse(name+" "+text,text.contains(jargon));
   if(name.equals("LloydsBeacon")){assertTrue(text.contains("teleport actions still work"));assertTrue(item.actions(hero).contains("ZAP"));assertTrue(item.actions(hero).contains("SET"));}
   if(name.equals("CapeOfThorns"))assertTrue(text.contains("deflection effects still work"));
   if(name.equals("CloakOfShadows")){assertTrue(text.contains("Cannot activate"));assertFalse(item.actions(hero).contains("STEALTH"));}
   if(name.equals("HolyTome")){assertTrue(text.contains("Cannot cast spells"));assertFalse(item.actions(hero).contains("CAST"));}
  }
  }finally{com.watabou.noosa.Game.instance=previousGame;}
 }
 @Test public void cursedChaliceExplainsActualSlowerRegeneration(){
  hero.HP=10;com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood();item.cursed=item.levelKnown=item.cursedKnown=true;hero.belongings.artifact=item;item.activate(hero);Regeneration regen=Buff.affect(hero,Regeneration.class);regen.act();com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();regen.storeInBundle(state);assertEquals(1/15f,state.getFloat(Regeneration.PARTIAL_REGEN),.00001f);assertTrue(item.info().contains("50% longer"));assertTrue(MechanicsDetails.itemCalculations(item).contains("delay by 1.5"));
 }
 @Test public void cursedHornNourishmentTextMatchesRealFoodHandler()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty();horn.cursed=horn.levelKnown=horn.cursedKnown=true;hero.belongings.artifact=horn;horn.activate(hero);Hunger hunger=Buff.affect(hero,Hunger.class);java.lang.reflect.Field level=Hunger.class.getDeclaredField("level");level.setAccessible(true);level.setFloat(hunger,300);com.shatteredpixel.shatteredpixeldungeon.items.food.Food food=new com.shatteredpixel.shatteredpixeldungeon.items.food.Food();food.energy=100;java.lang.reflect.Method satisfy=com.shatteredpixel.shatteredpixeldungeon.items.food.Food.class.getDeclaredMethod("satisfy",Hero.class);satisfy.setAccessible(true);satisfy.invoke(food,hero);assertEquals(300-100*.67f,level.getFloat(hunger),.00001f);assertTrue(horn.info().contains("33% less filling"));assertTrue(MechanicsDetails.itemCalculations(horn).contains("0.67"));level.setFloat(hunger,300);Buff.affect(hero,MagicImmune.class);satisfy.invoke(food,hero);assertEquals(200,level.getFloat(hunger),.00001f);assertTrue(horn.info().contains("unless you have magic immunity"));assertTrue(horn.cursed);
 }
 @Test public void brimstoneBarrierPreviewMatchesActualIgnitionRollAndExistingShieldLimit(){
  armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Brimstone());armor.level(3);ring(2);float power=Armor.Glyph.genericProcChanceMultiplier(hero),gain=2*(power-1);int cap=Math.round(gain*4);
  for(int seed=0;seed<30;seed++){Buff.detach(hero,Barrier.class);Random.pushGenerator(seed);int offered=(int)gain+(Random.Float()<gain%1?1:0);Random.popGenerator();Random.pushGenerator(seed);try{new Burning().reignite(hero,4);}finally{Random.popGenerator();}assertEquals(Math.min(cap,offered),hero.buff(Barrier.class).shielding());}
  Barrier barrier=hero.buff(Barrier.class);barrier.setShield(cap+5);int before=barrier.shielding();assertEquals(0,GlyphExplanation.brimstoneExpectedGain(power,before),0);String description=GlyphExplanation.details(armor,hero);assertTrue(description,description.contains("per attempted Burning application"));new Burning().reignite(hero,4);assertEquals(before,barrier.shielding());
 }
 @Test public void berserkInspectorDoesNotExposeUnknownArmorLevelsOrActivateRage(){
  Berserk rage=Buff.affect(hero,Berserk.class);armor.levelKnown=false;armor.cursedKnown=false;armor.level(0);
  String normal=rage.desc(),expanded=MechanicsDetails.buff(rage,normal);
  for(int hiddenLevel:new int[]{-3,1,3,9}){
   armor.level(hiddenLevel);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();rage.storeInBundle(before);
   assertEquals("Normal rage text must not disclose an unidentified armor level",normal,rage.desc());
   assertEquals("Expanded rage text must not disclose an unidentified armor level",expanded,MechanicsDetails.buff(rage,rage.desc()));
   com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();rage.storeInBundle(after);assertEquals(before.toString(),after.toString());assertFalse(rage.activeForInspection());
  }
  assertTrue(normal,normal.contains("Identify your armor"));
  armor.levelKnown=true;assertTrue(rage.desc().contains("_"+rage.previewShieldBoost()+"_"));
  hero.belongings.armor=null;assertTrue(rage.desc().contains("_"+rage.previewShieldBoost()+"_"));
 }
 @Test public void berserkDescriptionIsReadOnlyWhileActivationKeepsItsRecoveryScaling()throws Exception{
  Berserk rage=Buff.affect(hero,Berserk.class);java.lang.reflect.Field power=Berserk.class.getDeclaredField("power"),levels=Berserk.class.getDeclaredField("levelRecovery"),turns=Berserk.class.getDeclaredField("turnRecovery");for(java.lang.reflect.Field field:new java.lang.reflect.Field[]{power,levels,turns})field.setAccessible(true);power.setFloat(rage,1.3334f);levels.setFloat(rage,2);turns.setInt(rage,100);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();rage.storeInBundle(before);
  int preview=rage.previewShieldBoost();assertTrue(MechanicsDetails.buff(rage,rage.desc()).contains("Rage and damage"));for(int i=0;i<100;i++)rage.desc();com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();rage.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(preview,rage.currentShieldBoost());assertEquals(2*(2-1.3334f),levels.getFloat(rage),.00001f);assertEquals((int)(100*(2-1.3334f)),turns.getInt(rage));
 }
 @Test public void repeatedHealingPotionTextMatchesActualLargerPoolWithoutAddingTotals()throws Exception{
  hero.sprite=quietSprite();hero.HP=1;hero.HT=200;com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing();potion.anonymize();assertTrue(potion.desc().contains("Repeated doses do not add"));
  com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing.heal(hero);Healing heal=hero.buff(Healing.class);int full=(int)(.8f*hero.HT+14);assertEquals(full,heal.remainingHealingPool());int next=heal.nextRequestedHealing();assertTrue(heal.desc(),heal.desc().contains("Next heal: up to "+next+" HP"));com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();heal.storeInBundle(before);heal.desc();com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();heal.storeInBundle(after);assertEquals(before.toString(),after.toString());
  heal.act();int remaining=heal.remainingHealingPool();assertEquals(full-next,remaining);com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing.heal(hero);assertEquals(full,heal.remainingHealingPool());assertNotEquals(full+remaining,heal.remainingHealingPool());
  heal.setHeal(10,.5f,1);assertEquals(full,heal.remainingHealingPool());assertEquals(Math.round(full*.5f)+1,heal.nextRequestedHealing());assertTrue(heal.desc().contains("larger remaining amount"));
 }
 @Test public void healingExplanationShowsRequestedPoolWithoutSpendingModifiedHealingCredit()throws Exception{
  com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),settings=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,25).store(settings);all.put("apd_custom_rules",settings);Dungeon.apdRestoreCustomRules(all);hero.HP=10;hero.apdCustomHealingFraction=.5f;hero.sprite=quietSprite();Healing heal=new Healing();heal.target=hero;heal.setHeal(100,.25f,2);
  for(int i=0;i<100;i++){String text=BuffExplanation.details(heal);assertTrue(text,text.contains("27 HP"));assertTrue(text,text.contains("25% of the remaining pool"));}assertEquals(.5f,hero.apdCustomHealingFraction,0);assertEquals(100,heal.remainingHealingPool());
  heal.act();assertEquals(17,hero.HP);assertEquals(.25f,hero.apdCustomHealingFraction,0);assertEquals(73,heal.remainingHealingPool());assertEquals(20,heal.nextRequestedHealing());
 }
 @Test public void degradationExplanationMatchesActualCurveAndHidesUnidentifiedLevels(){
  Degrade degrade=Buff.affect(hero,Degrade.class,20);armor.levelKnown=false;armor.level(0);String hidden=BuffExplanation.details(degrade);armor.level(9);assertEquals(hidden,BuffExplanation.details(degrade));armor.levelKnown=true;assertEquals(Degrade.reduceLevel(9),armor.buffedLvl());String known=BuffExplanation.details(degrade);assertTrue(known,known.contains("+9 → +"+armor.buffedLvl()));assertTrue(known.contains("rounded to the nearest whole level"));
 }
 public static class StatusDamageObserver extends com.shatteredpixel.shatteredpixeldungeon.actors.Char {int received;public StatusDamageObserver(){HP=HT=100;}@Override public void damage(int amount,Object source){received=amount;}}
 @Test public void corrosionDisplayedBaseDamageTracksRealTickGrowth(){
  int depth=Dungeon.depth,branch=Dungeon.branch;try{
   int[] depths={1,8,23},starts={2,2,12};int[][] expected={{2,2,3,3},{2,3,4,5,6,6},{12,13,13,14}};
   for(int scenario=0;scenario<depths.length;scenario++){
    Dungeon.depth=depths[scenario];Dungeon.branch=0;StatusDamageObserver target=new StatusDamageObserver();Corrosion corrosion=new Corrosion();corrosion.target=target;corrosion.set(8,starts[scenario]);
    for(int amount:expected[scenario]){assertEquals(amount,corrosion.apdNextRaw());String detail=BuffExplanation.details(corrosion);assertTrue(detail.contains(amount+" damage points"));assertTrue(detail.contains("once it reaches "+(Dungeon.scalingDepth()/2+2)));corrosion.act();assertEquals(amount,target.received);}
    if(scenario==1)assertEquals(7,corrosion.apdNextRaw());
   }
  }finally{Dungeon.depth=depth;Dungeon.branch=branch;}
 }
 @Test public void burningExplanationUsesLegitimateRangeWithoutRevealingCachedRandomOutcome(){
  Burning burning=new Burning();burning.reignite(hero,8);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();burning.storeInBundle(state);state.put("next_dmg",2);burning.restoreFromBundle(state);String first=BuffExplanation.details(burning);state.put("next_dmg",3);burning.restoreFromBundle(state);assertEquals(first,BuffExplanation.details(burning));assertTrue(first,first.contains("1–"+burning.apdRandomMaximum()+" damage points"));
 }
 @Test public void wandEmpowerDescriptionMatchesActualAddedDamageAndRollCounter(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();Random.pushGenerator(991);int base=wand.damageRoll(0);Random.popGenerator();WandEmpower empower=Buff.affect(hero,WandEmpower.class);empower.set(3,2);assertTrue(BuffExplanation.details(empower).contains("Adds 3 damage"));Random.pushGenerator(991);try{assertEquals(base+3,wand.damageRoll(0));}finally{Random.popGenerator();}assertEquals(1,empower.left);assertTrue(BuffExplanation.details(empower).contains("next 1 direct wand damage rolls"));
 }
 public static class PlainStatusTarget extends com.shatteredpixel.shatteredpixeldungeon.actors.Char {public PlainStatusTarget(){HP=HT=100;}}
 @Test public void corruptionDescriptionMatchesRealSmallAndFractionalHealthTicks(){
  int[] health={20,100,250};int[][] hits={{0,0,0,0,1,0},{1,1,1,1},{2,3,2,3}};
  for(int caseIndex=0;caseIndex<health.length;caseIndex++){
   PlainStatusTarget target=new PlainStatusTarget();target.HP=target.HT=health[caseIndex];Corruption corruption=Buff.affect(target,Corruption.class);
   try{assertEquals(com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY,target.alignment);String text=BuffExplanation.details(corruption);assertTrue(text.contains("_1% of maximum HP per turn_"));
    for(int damage:hits[caseIndex]){int before=target.HP;float time=corruption.cooldown();corruption.act();assertEquals(damage,before-target.HP);assertEquals(1,corruption.cooldown()-time,0);}
    com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();corruption.storeInBundle(saved);assertEquals(text,BuffExplanation.details(corruption));corruption.storeInBundle(after);assertEquals(saved.toString(),after.toString());
   }finally{corruption.detach();}
  }
 }
 @Test public void doomDescriptionMatchesActualIncomingDamageFactor(){PlainStatusTarget target=new PlainStatusTarget();Doom doom=Buff.affect(target,Doom.class);target.damage(20,new Object());assertEquals(67,target.HP);assertTrue(BuffExplanation.details(doom).contains("_67%_"));}
 @Test public void healingDetailsExposeActualVialLimitAndItsEffectOnNextHeal(){Healing heal=new Healing();heal.setHeal(100,.25f,2);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();heal.storeInBundle(state);state.put("vial_level",0);heal.restoreFromBundle(state);int limit=com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood.maxHealPerTurn(0);assertEquals(Math.min(27,limit),heal.nextRequestedHealing());assertTrue(BuffExplanation.details(heal).contains("Blood Vial limits this to "+limit+" HP per turn"));}

 @Test public void liquidWillpowerExplanationMatchesActualPotionShieldAndMaximumHp()throws Exception{
  hero.sprite=quietSprite();hero.talents.get(1).put(Talent.LIQUID_WILLPOWER,1);
  for(int rank=1;rank<=2;rank++){hero.talents.get(1).put(Talent.LIQUID_WILLPOWER,rank);
   for(int hp:new int[]{20,81,143}){hero.HT=hp;Barrier old=hero.buff(Barrier.class);if(old!=null)old.detach();
    Talent.onPotionUsed(hero,hero.pos,1);int actual=hero.buff(Barrier.class).shielding();
    String text=MechanicsDetails.talent(Talent.LIQUID_WILLPOWER,rank,"");
    assertTrue(text,text.contains("At your current "+hp+" maximum HP: "+actual+" shield points"));
   }
  }
 }
 @Test public void liquidWillpowerKeepsStrongerBarrierAndPreviewDoesNotApplyShield()throws Exception{
  hero.sprite=quietSprite();hero.talents.get(1).put(Talent.LIQUID_WILLPOWER,2);Barrier barrier=Buff.affect(hero,Barrier.class);barrier.setShield(100);
  Talent.onPotionUsed(hero,hero.pos,1);assertEquals(100,barrier.shielding());
  for(int i=0;i<50;i++)MechanicsDetails.talent(Talent.LIQUID_WILLPOWER,2,"");assertEquals(100,barrier.shielding());
 }
 @Test public void inscribedPowerMatchesActualUseCounterAndKeepsLargerApplication(){
  hero.talents.get(1).put(Talent.INSCRIBED_POWER,1);
  for(int rank=1;rank<=2;rank++){hero.talents.get(1).put(Talent.INSCRIBED_POWER,rank);
   Talent.onScrollUsed(hero,hero.pos,1,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify.class);
   assertEquals(Integer.toString(1+rank),hero.buff(ScrollEmpower.class).iconTextDisplay());
   assertTrue(MechanicsDetails.talent(Talent.INSCRIBED_POWER,rank,"").contains((1+rank)+" empowered wand uses"));
  }
  Talent.onScrollUsed(hero,hero.pos,.25f,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify.class);
  assertEquals("3",hero.buff(ScrollEmpower.class).iconTextDisplay());
 }
 @Test public void talentDetailsDoNotInventChargeCountsAndRefreshWithRank(){
  assertTrue(MechanicsDetails.talent(Talent.SHIELD_BATTERY,1,"").contains("4% of maximum HP"));
  assertTrue(MechanicsDetails.talent(Talent.SHIELD_BATTERY,2,"").contains("6% of maximum HP"));
  assertTrue(MechanicsDetails.talent(Talent.LIQUID_NATURE,2,"").contains("up to 6 nearby tiles"));
  assertEquals("",com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.TalentExplanation.details(Talent.SHIELD_BATTERY,0));
 }

 @Test public void accuracyExplanationsNeverConsumeLiquidAgilityUsesOrRandomness(){
  hero.talents.get(1).put(Talent.LIQUID_AGILITY,1);Talent.LiquidAgilACCTracker ready=Buff.affect(hero,Talent.LiquidAgilACCTracker.class,5);ready.uses=2;
  Random.pushGenerator(482);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(482);
  try{for(int i=0;i<100;i++){String text=MechanicsDetails.buff(ready,"");assertTrue(text.contains("2 qualifying attack uses remain"));assertTrue(text.contains("multiplies its accuracy score by 3"));}assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
  assertEquals(2,ready.uses);hero.talents.get(1).put(Talent.LIQUID_AGILITY,2);assertTrue(MechanicsDetails.buff(ready,"").contains("uses guaranteed-hit accuracy"));assertEquals(2,ready.uses);
 }
 @Test public void preciseAssaultExplainsRankMultipliersAndGuaranteedEvasionPriority(){
  assertTrue(MechanicsDetails.talent(Talent.PRECISE_ASSAULT,1,"").contains("2 times"));
  assertTrue(MechanicsDetails.talent(Talent.PRECISE_ASSAULT,2,"").contains("5 times"));
  String third=MechanicsDetails.talent(Talent.PRECISE_ASSAULT,3,"");assertTrue(third.contains("guaranteed-hit accuracy"));assertTrue(third.contains("1.3"));assertTrue(third.contains("Guaranteed evasion wins"));
 }

 private static class TalentExpiryGame extends com.watabou.noosa.Game {
  TalentExpiryGame(){super(com.watabou.noosa.Scene.class,com.watabou.noosa.Game.platform);requestedReset=false;}
 }
 private static class TalentExpiryBoundary extends com.shatteredpixel.shatteredpixeldungeon.actors.Actor {
  boolean reached;
  TalentExpiryBoundary(){actPriority=BUFF_PRIO-1;}
  @Override protected boolean act(){reached=true;remove(this);keepActorThreadAlive=false;return true;}
 }
 /** Runs the real buff scheduler, with the ordinary hero parked beyond the test interval. */
 private static class TalentExpiryClock implements AutoCloseable {
  private final java.util.Map<java.lang.reflect.Field,Object> previous=new java.util.LinkedHashMap<>();
  private final com.watabou.noosa.Game oldGame=com.watabou.noosa.Game.instance;
  private final boolean oldKeepAlive=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.keepActorThreadAlive;
  TalentExpiryClock()throws Exception{
   for(String name:new String[]{"all","chars","ids","current","now","nextID"}){
    java.lang.reflect.Field field=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.class.getDeclaredField(name);field.setAccessible(true);previous.put(field,field.get(null));
    if(name.equals("all")||name.equals("chars"))field.set(null,new java.util.HashSet<>());
    else if(name.equals("ids"))field.set(null,new com.watabou.utils.SparseArray<>());
    else if(name.equals("current"))field.set(null,null);
    else if(name.equals("now"))field.setFloat(null,0);
    else field.setInt(null,1);
   }
   java.lang.reflect.Field scene=com.watabou.noosa.Game.class.getDeclaredField("sceneClass");scene.setAccessible(true);previous.put(scene,scene.get(null));
   new TalentExpiryGame();
  }
  void advanceTo(float deadline)throws Exception{
   assertTrue(deadline>=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now());
   TalentExpiryBoundary boundary=new TalentExpiryBoundary();
   com.shatteredpixel.shatteredpixeldungeon.actors.Actor.addDelayed(boundary,deadline-com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now());
   java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
   Thread worker=new Thread(com.shatteredpixel.shatteredpixeldungeon.actors.Actor::process,"talent-expiry-regression");worker.setDaemon(true);worker.setUncaughtExceptionHandler((thread,error)->failure.set(error));
   com.shatteredpixel.shatteredpixeldungeon.actors.Actor.keepActorThreadAlive=true;worker.start();worker.join(5000);
   if(worker.isAlive()){com.shatteredpixel.shatteredpixeldungeon.actors.Actor.keepActorThreadAlive=false;worker.interrupt();synchronized(worker){worker.notifyAll();}worker.join(1000);}
   assertFalse("Talent expiry scheduler must terminate",worker.isAlive());assertNull("Real scheduler failed",failure.get());assertTrue(boundary.reached);
   assertEquals(deadline,com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now(),0);
  }
  @Override public void close()throws Exception{
   for(java.util.Map.Entry<java.lang.reflect.Field,Object> entry:previous.entrySet())entry.getKey().set(null,entry.getValue());
   com.shatteredpixel.shatteredpixeldungeon.actors.Actor.keepActorThreadAlive=oldKeepAlive;com.watabou.noosa.Game.instance=oldGame;
  }
 }
 @Test public void provokedAngerRealShieldBreakAddsFiveTurnsAndExpiresWithoutAnAttack()throws Exception{
  try(TalentExpiryClock clock=new TalentExpiryClock()){
   hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.addDelayed(hero,10000);
   PlainStatusTarget enemy=new PlainStatusTarget();enemy.HP=enemy.HT=100;
   for(int rank=1;rank<=2;rank++){
    hero.talents.get(0).put(Talent.PROVOKED_ANGER,rank);
    Buff.affect(hero,Barrier.class).setShield(3);
    assertEquals(0,ShieldBuff.processDamage(hero,2,new Object()));assertNull(hero.buff(Talent.ProvokedAngerTracker.class));
    assertEquals(0,ShieldBuff.processDamage(hero,1,new Object()));
    Talent.ProvokedAngerTracker tracker=hero.buff(Talent.ProvokedAngerTracker.class);assertNotNull(tracker);assertEquals(5,tracker.cooldown(),0);
    assertEquals(10+1+2*rank,Talent.onAttackProc(hero,enemy,10));assertNull(hero.buff(Talent.ProvokedAngerTracker.class));assertEquals(10,Talent.onAttackProc(hero,enemy,10));

    Buff.affect(hero,Barrier.class).setShield(1);ShieldBuff.processDamage(hero,1,new Object());
    tracker=hero.buff(Talent.ProvokedAngerTracker.class);float start=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now();
    clock.advanceTo(start+4);assertSame(tracker,hero.buff(Talent.ProvokedAngerTracker.class));assertEquals(1,tracker.cooldown(),0);
    Buff.affect(hero,Barrier.class).setShield(1);ShieldBuff.processDamage(hero,1,new Object());
    assertSame(tracker,hero.buff(Talent.ProvokedAngerTracker.class));assertEquals("Another real break adds five turns to the one remaining",6,tracker.cooldown(),0);
    clock.advanceTo(start+9.75f);assertSame(tracker,hero.buff(Talent.ProvokedAngerTracker.class));assertEquals(.25f,tracker.cooldown(),0);
    clock.advanceTo(start+10);assertNull(hero.buff(Talent.ProvokedAngerTracker.class));assertEquals(10,Talent.onAttackProc(hero,enemy,10));assertEquals(20,hero.HP);
   }
  }
 }
 @Test public void lingeringMagicRealWandUseRefreshesFiveTurnsAndExpiresWithoutAnAttack()throws Exception{
  java.lang.reflect.Field user=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser"),item=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curItem");user.setAccessible(true);item.setAccessible(true);
  Object oldUser=user.get(null),oldItem=item.get(null);boolean oldRefresh=com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.updateItemDisplays;
  try(TalentExpiryClock clock=new TalentExpiryClock()){
   hero.heroClass=HeroClass.MAGE;hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.addDelayed(hero,10000);
   PlainStatusTarget enemy=new PlainStatusTarget();enemy.HP=enemy.HT=100;
   for(int rank=1;rank<=2;rank++)for(boolean imbued:new boolean[]{false,true}){
    hero.talents.get(0).put(Talent.LINGERING_MAGIC,rank);
    com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.maxCharges=10;wand.curCharges=10;
    if(imbued){
     com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff();
     java.lang.reflect.Field embedded=com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff.class.getDeclaredField("wand");embedded.setAccessible(true);embedded.set(staff,wand);hero.belongings.weapon=staff;staff.applyWandChargeBuff(hero);
    }else{hero.belongings.weapon=null;hero.belongings.backpack.items.add(wand);wand.charge(hero);}
    wand.setCurrent(hero);float time=hero.cooldown();wand.wandUsed();assertEquals(9,wand.curCharges);assertEquals(time+1,hero.cooldown(),0);
    Talent.LingeringMagicTracker tracker=hero.buff(Talent.LingeringMagicTracker.class);assertNotNull(tracker);assertEquals(5,tracker.cooldown(),0);
    Random.pushGenerator(44210+rank);int bonus;try{bonus=Talent.onAttackProc(hero,enemy,10)-10;}finally{Random.popGenerator();}
    assertTrue(bonus>=rank&&bonus<=2);assertNull(hero.buff(Talent.LingeringMagicTracker.class));assertEquals(10,Talent.onAttackProc(hero,enemy,10));

    wand.wandUsed();tracker=hero.buff(Talent.LingeringMagicTracker.class);float start=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now();
    clock.advanceTo(start+4);assertSame(tracker,hero.buff(Talent.LingeringMagicTracker.class));assertEquals(1,tracker.cooldown(),0);
    wand.wandUsed();assertSame(tracker,hero.buff(Talent.LingeringMagicTracker.class));assertEquals("Another real zap refreshes five turns without adding the old remainder",5,tracker.cooldown(),0);
    clock.advanceTo(start+8.75f);assertSame(tracker,hero.buff(Talent.LingeringMagicTracker.class));assertEquals(.25f,tracker.cooldown(),0);
    clock.advanceTo(start+9);assertNull(hero.buff(Talent.LingeringMagicTracker.class));assertEquals(10,Talent.onAttackProc(hero,enemy,10));assertEquals(20,hero.HP);
    wand.stopCharging();hero.belongings.backpack.items.remove(wand);hero.belongings.weapon=null;
   }
  }finally{user.set(null,oldUser);item.set(null,oldItem);com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.updateItemDisplays=oldRefresh;}
 }
 @Test public void empoweredStrikeRealStaffZapRefreshesTenTurnsAndExpiresWithoutAStrike()throws Exception{
  java.lang.reflect.Field user=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser"),item=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curItem");user.setAccessible(true);item.setAccessible(true);
  Object oldUser=user.get(null),oldItem=item.get(null);boolean oldRefresh=com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.updateItemDisplays;
  try(TalentExpiryClock clock=new TalentExpiryClock()){
   hero.heroClass=HeroClass.MAGE;hero.subClass=HeroSubClass.BATTLEMAGE;hero.talents=new java.util.ArrayList<>();Talent.initClassTalents(hero);Talent.initSubclassTalents(hero);
   hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.addDelayed(hero,10000);
   PlainStatusTarget enemy=new PlainStatusTarget();enemy.HP=enemy.HT=100;
   for(int rank=1;rank<=3;rank++){
    hero.talents.get(2).put(Talent.EMPOWERED_STRIKE,rank);
    com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost standalone=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost();standalone.levelKnown=standalone.cursedKnown=standalone.curChargeKnown=true;
    hero.belongings.backpack.items.add(standalone);standalone.charge(hero);standalone.setCurrent(hero);
    float time=hero.cooldown();int charges=standalone.curCharges;standalone.wandUsed();
    assertEquals(charges-1,standalone.curCharges);assertEquals(time+1,hero.cooldown(),0);assertNull("A real standalone wand use does not ready a staff strike",hero.buff(Talent.EmpoweredStrikeTracker.class));
    standalone.stopCharging();hero.belongings.backpack.items.remove(standalone);

    com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost();
    com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff(wand);staff.levelKnown=staff.cursedKnown=true;
    hero.belongings.weapon=staff;staff.applyWandChargeBuff(hero);wand.setCurrent(hero);
    time=hero.cooldown();charges=wand.curCharges;wand.wandUsed();
    assertEquals(charges-1,wand.curCharges);assertEquals(time+1,hero.cooldown(),0);
    Talent.EmpoweredStrikeTracker tracker=hero.buff(Talent.EmpoweredStrikeTracker.class);assertNotNull(tracker);assertEquals(10f,tracker.cooldown(),0);
    assertEquals(Math.round(12*(1f+rank/6f)),staff.proc(hero,enemy,12));assertNull("The real staff strike consumes the ready effect",hero.buff(Talent.EmpoweredStrikeTracker.class));assertEquals(12,staff.proc(hero,enemy,12));

    time=hero.cooldown();charges=wand.curCharges;wand.wandUsed();assertEquals(charges-1,wand.curCharges);assertEquals(time+1,hero.cooldown(),0);
    tracker=hero.buff(Talent.EmpoweredStrikeTracker.class);float start=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now();assertNotNull(tracker);assertEquals(10f,tracker.cooldown(),0);
    clock.advanceTo(start+9);assertSame(tracker,hero.buff(Talent.EmpoweredStrikeTracker.class));assertEquals(1f,tracker.cooldown(),0);
    time=hero.cooldown();charges=wand.curCharges;wand.wandUsed();assertEquals(charges-1,wand.curCharges);assertEquals(time+1,hero.cooldown(),0);
    assertSame(tracker,hero.buff(Talent.EmpoweredStrikeTracker.class));assertEquals("Another real staff zap refreshes ten turns without adding the remaining turn",10f,tracker.cooldown(),0);
    clock.advanceTo(start+18.75f);assertSame(tracker,hero.buff(Talent.EmpoweredStrikeTracker.class));assertEquals(.25f,tracker.cooldown(),0);
    clock.advanceTo(start+19);assertNull("The actual scheduler expires unused readiness at the refreshed deadline",hero.buff(Talent.EmpoweredStrikeTracker.class));assertEquals(12,staff.proc(hero,enemy,12));assertEquals(20,hero.HP);
    System.out.println("EMPOWERED_STRIKE_EXPIRY rank="+rank+" initial=10 refresh_remaining=1 refreshed=10 final_before_expiry="+(start+18.75f)+" expired_at="+(start+19)+" natural_expiry=true real_staff_consumption=true standalone_rejected=true");
    wand.stopCharging();hero.belongings.weapon=null;
   }
  }finally{user.set(null,oldUser);item.set(null,oldItem);com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.updateItemDisplays=oldRefresh;}
 }
 @Test public void flatAttackTalentDescriptionsMatchRealProcAndConsumeOnlyActualAttack(){
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat enemy=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();enemy.HP=enemy.HT=20;
  for(int rank=1;rank<=2;rank++){
   hero.talents.get(0).put(Talent.PROVOKED_ANGER,rank);Buff.affect(hero,Talent.ProvokedAngerTracker.class,5);
   String text=MechanicsDetails.talent(Talent.PROVOKED_ANGER,rank,"");assertNotNull(hero.buff(Talent.ProvokedAngerTracker.class));
   int bonus=1+2*rank;assertTrue(text.contains("adds "+bonus+" damage points"));assertEquals(10+bonus,Talent.onAttackProc(hero,enemy,10));assertNull(hero.buff(Talent.ProvokedAngerTracker.class));hero.talents.get(0).remove(Talent.PROVOKED_ANGER);
   hero.talents.get(0).put(Talent.LINGERING_MAGIC,rank);
   for(int seed=0;seed<30;seed++){Buff.affect(hero,Talent.LingeringMagicTracker.class,5);Random.pushGenerator(seed);int actual;try{actual=Talent.onAttackProc(hero,enemy,10)-10;}finally{Random.popGenerator();}assertTrue(actual>=rank&&actual<=2);assertNull(hero.buff(Talent.LingeringMagicTracker.class));}
   assertTrue(MechanicsDetails.talent(Talent.LINGERING_MAGIC,rank,"").contains(rank+"–2 damage points"));hero.talents.get(0).remove(Talent.LINGERING_MAGIC);
  }
 }
 @Test public void deadlyFollowupMatchesActualRoundedMultiplicationAndRetainsMark(){
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat enemy=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();enemy.HP=enemy.HT=20;
  for(int rank=1;rank<=3;rank++){hero.talents.get(2).put(Talent.DEADLY_FOLLOWUP,rank);Talent.DeadlyFollowupTracker mark=Buff.affect(hero,Talent.DeadlyFollowupTracker.class,5);mark.object=enemy.id();
   for(int damage:new int[]{1,7,11,30})assertEquals(Math.round(damage*(1+.1f*rank)),Talent.onAttackProc(hero,enemy,damage));
   String text=MechanicsDetails.talent(Talent.DEADLY_FOLLOWUP,rank,"");assertTrue(text.contains("by "+ObfuscationExplanation.number(1+.1f*rank)));assertTrue(text.contains("one melee hit does not consume"));assertSame(mark,hero.buff(Talent.DeadlyFollowupTracker.class));
  }
 }

 @Test public void growingChampionDetailsFollowActualGrowthAndSavedMultiplier(){
  ChampionEnemy.Growing growing=new ChampionEnemy.Growing();
  for(int tick=0;tick<20;tick++){
   String text=MechanicsDetails.buff(growing,growing.desc());assertTrue(text.contains("Attack damage: "+ObfuscationExplanation.number(100*growing.meleeDamageFactor())+"% of normal"));assertTrue(text.contains("Accuracy and dodge rolls: "+ObfuscationExplanation.number(100*growing.evasionAndAccuracyFactor())+"% of normal"));assertTrue(text.contains("Damage taken: "+ObfuscationExplanation.number(100*growing.damageTakenFactor())+"% of normal"));
   float before=growing.meleeDamageFactor();float time=growing.cooldown();growing.act();assertEquals(before+.01f,growing.meleeDamageFactor(),0);assertEquals(time+4,growing.cooldown(),0);
  }
  com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();growing.storeInBundle(saved);ChampionEnemy.Growing loaded=new ChampionEnemy.Growing();loaded.restoreFromBundle(saved);assertEquals(MechanicsDetails.buff(growing,growing.desc()),MechanicsDetails.buff(loaded,loaded.desc()));
 }
 @Test public void championDetailsUseRealFactorsAndNeverRollOrGrow(){
  ChampionEnemy[] champions={new ChampionEnemy.Blazing(),new ChampionEnemy.Projecting(),new ChampionEnemy.AntiMagic(),new ChampionEnemy.Giant(),new ChampionEnemy.Blessed(),new ChampionEnemy.Growing()};
  Random.pushGenerator(412);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(412);
  try{for(ChampionEnemy champion:champions){float time=champion.cooldown(),factor=champion.meleeDamageFactor();for(int i=0;i<30;i++){String text=MechanicsDetails.buff(champion,champion.desc());assertTrue(text.contains("Accuracy and dodge rolls: "+ObfuscationExplanation.number(100*champion.evasionAndAccuracyFactor())+"% of normal"));assertTrue(text.contains("rounded up"));}assertEquals(time,champion.cooldown(),0);assertEquals(factor,champion.meleeDamageFactor(),0);}assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
 }

 @Test public void championIncomingRoundingExplainsSmallHitsUsingActualDamageHandler(){
  for(int damage:new int[]{1,2,3,7,20}){
   PlainStatusTarget giant=new PlainStatusTarget();ChampionEnemy.Giant effect=Buff.affect(giant,ChampionEnemy.Giant.class);giant.damage(damage,new Object());assertEquals((int)Math.ceil(damage*effect.damageTakenFactor()),100-giant.HP);
   PlainStatusTarget antimagic=new PlainStatusTarget();ChampionEnemy.AntiMagic half=Buff.affect(antimagic,ChampionEnemy.AntiMagic.class);antimagic.damage(damage,new Object());assertEquals((int)Math.ceil(damage*half.damageTakenFactor()),100-antimagic.HP);
  }
 }

 @Test public void wandStatsUnderWildMagicNeverConsumeRandomRoll(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.charge(hero);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic.WildMagicTracker.class);
  Random.pushGenerator(412);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(412);try{for(int i=0;i<30;i++)wand.statsDesc();assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
 }

 @Test public void wandPreviewBoundsMatchActualWildMagicLevelsAcrossRanksAndModifiers(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.charge(hero);wand.curCharges=1;hero.talents.get(1).put(Talent.DESPERATE_POWER,1);Buff.affect(hero,ScrollEmpower.class).reset(3);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic.WildMagicTracker.class);
  for(int rank=0;rank<=4;rank++){hero.talents.get(1).put(Talent.WILD_POWER,rank);for(int level=0;level<=7;level++){wand.level(level);int[] bounds=wand.previewEffectiveLevels();int seenMin=Integer.MAX_VALUE,seenMax=Integer.MIN_VALUE;for(int seed=0;seed<30;seed++){Random.pushGenerator(seed);int actual;try{actual=wand.buffedLvl();}finally{Random.popGenerator();}seenMin=Math.min(seenMin,actual);seenMax=Math.max(seenMax,actual);}assertEquals(seenMin,bounds[0]);assertEquals(seenMax,bounds[1]);}}
 }
 @Test public void unknownWandDetailsCannotRevealLevelsOrCharges(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.levelKnown=false;String first=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.WandExplanation.details(wand);wand.level(12);wand.curCharges=9;assertEquals(first,com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.WandExplanation.details(wand));assertArrayEquals(new int[]{0,0},wand.previewEffectiveLevels());
 }
 @Test public void directWandExplanationsUseActualBoundsAndDoNotSpendEmpower(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.DamageWand[] wands={new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion()};
  WandEmpower empower=Buff.affect(hero,WandEmpower.class);empower.set(3,2);
  for(com.shatteredpixel.shatteredpixeldungeon.items.wands.DamageWand wand:wands){wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;for(int level=0;level<=9;level++){wand.level(level);String text=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.WandExplanation.details(wand);assertTrue(text.contains("Current roll bounds: "+wand.min(level)+"–"+wand.max(level)));assertEquals(wand.min(0)+(wand.min(1)-wand.min(0))*level,wand.min(level));assertEquals(wand.max(0)+(wand.max(1)-wand.max(0))*level,wand.max(level));}assertEquals(2,empower.left);}
 }
 @Test public void staleUncursedInfusionPreviewDoesNotCleanStateOrChangeCharges(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.levelKnown=true;wand.level(3);wand.cursed=false;wand.curseInfusionBonus=true;wand.curCharges=4;assertArrayEquals(new int[]{3,3},wand.previewEffectiveLevels());assertTrue(wand.curseInfusionBonus);assertEquals(4,wand.curCharges);
 }

 @Test public void allNumericalWandInfoPreviewsPreserveRandomnessAndEmpowerUses(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand[] wands={new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion()};
  hero.talents.get(1).put(Talent.WILD_POWER,1);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic.WildMagicTracker.class);WandEmpower empower=Buff.affect(hero,WandEmpower.class);empower.set(3,2);
  for(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand:wands){wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.charge(hero);Random.pushGenerator(312);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(312);try{for(int i=0;i<10;i++){wand.info();MechanicsDetails.item(wand,wand.info());}assertEquals(expected,Random.Int());}finally{Random.popGenerator();}assertEquals(2,empower.left);}
 }
 @Test public void wandPreviewMatchesDegradeInfusionResinAndStrongerMagicCharge(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.levelKnown=true;wand.level(6);wand.cursed=true;wand.curseInfusionBonus=true;wand.resinBonus=2;wand.charge(hero);Buff.affect(hero,Degrade.class);assertEquals(wand.buffedLvl(),wand.previewEffectiveLevels()[0]);assertEquals(wand.buffedLvl(),wand.previewEffectiveLevels()[1]);
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile.MagicCharge charge=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile.MagicCharge.class);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();charge.storeInBundle(state);state.put("level",20);charge.restoreFromBundle(state);assertEquals(wand.buffedLvl(),wand.previewEffectiveLevels()[0]);assertEquals(wand.buffedLvl(),wand.previewEffectiveLevels()[1]);
 }

 @Test public void strongmanUsesBaseStrengthAndDoesNotMultiplyRingContribution(){
  for(int rank=1;rank<=3;rank++){hero.talents.get(2).put(Talent.STRONGMAN,rank);for(int strength=10;strength<=20;strength++){hero.STR=strength;int added=(int)Math.floor(strength*(.03f+.05f*rank));assertEquals(strength+5+added,hero.previewStrength(5));String text=MechanicsDetails.talent(Talent.STRONGMAN,rank,"");assertTrue(text.contains("this rank adds "+added+" strength points"));assertTrue(text.contains("excluding Ring of Might"));}}
 }
 public static class RangeAccuracyDart extends com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart {public float rangeFactor(Hero owner,com.shatteredpixel.shatteredpixeldungeon.actors.Char enemy){return adjacentAccFactor(owner,enemy);}}
 @Test public void pointBlankDetailsMatchActualAdjacentAndDistantFactors(){
  RangeAccuracyDart dart=new RangeAccuracyDart();PlainStatusTarget enemy=new PlainStatusTarget();hero.pos=24;
  for(int rank=1;rank<=3;rank++){hero.talents.get(2).put(Talent.POINT_BLANK,rank);enemy.pos=25;float adjacent=dart.rangeFactor(hero,enemy);assertTrue(MechanicsDetails.talent(Talent.POINT_BLANK,rank,"").contains("by "+ObfuscationExplanation.number(adjacent)));enemy.pos=26;assertEquals(1.5f,dart.rangeFactor(hero,enemy),0);}
 }
 @Test public void empoweredStrikeDamageDetailsMatchActualStaffProcAndPowerFactor(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff();PlainStatusTarget target=new PlainStatusTarget();
  for(int rank=1;rank<=3;rank++){hero.talents.get(2).put(Talent.EMPOWERED_STRIKE,rank);for(int damage:new int[]{1,7,18}){Buff.affect(hero,Talent.EmpoweredStrikeTracker.class);assertEquals(Math.round(damage*(1f+rank/6f)),staff.proc(hero,target,damage));assertNull(hero.buff(Talent.EmpoweredStrikeTracker.class));}
   String text=MechanicsDetails.talent(Talent.EMPOWERED_STRIKE,rank,"");assertTrue(text.contains("by "+ObfuscationExplanation.number(1f+rank/6f)));Buff.affect(hero,Talent.EmpoweredStrikeTracker.class);assertTrue(text.contains("factor of "+ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.procChanceMultiplier(hero))));hero.buff(Talent.EmpoweredStrikeTracker.class).detach();}
 }

 @Test public void adrenalineTimingExplanationMatchesHeroAndMonsterActualPaths()throws Exception{
  hero.sprite=quietSprite();float heroMovement=hero.speed(),heroAttack=hero.attackDelay();Adrenaline effect=Buff.affect(hero,Adrenaline.class,5);assertEquals(heroMovement*2,hero.speed(),.00001f);assertEquals(heroAttack,hero.attackDelay(),0);assertTrue(MechanicsDetails.buff(effect,effect.desc()).contains("hero does not gain"));
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat rat=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();float delay=rat.attackDelay();Buff.affect(rat,Adrenaline.class,5);assertEquals(delay/1.5f,rat.attackDelay(),.00001f);
 }
 @Test public void furyConditionKeepsExactHalfHpAndEndsAboveIt(){
  PlainStatusTarget target=new PlainStatusTarget();target.HP=50;Fury fury=Buff.affect(target,Fury.class);fury.act();assertSame(fury,target.buff(Fury.class));assertTrue(MechanicsDetails.buff(fury,fury.desc()).contains("health is above 50%"));target.HP=51;fury.act();assertNull(target.buff(Fury.class));assertFalse(fury.desc().contains("%%"));
 }
 @Test public void fireImbueDescriptionMatchesActualIgnitionChecksAndDuration()throws Exception{
  FireImbue fire=Buff.affect(hero,FireImbue.class);fire.set(7);fire.extend(3);assertTrue(fire.desc().contains("_50% chance_"));assertTrue(hero.isImmune(Burning.class));assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood().desc().startsWith("For _50 turns_"));assertTrue(MechanicsDetails.buff(fire,fire.desc()).contains("duration: 10 turns"));
  for(int seed=0;seed<20;seed++){PlainStatusTarget enemy=new PlainStatusTarget();enemy.sprite=quietSprite();Random.pushGenerator(seed);boolean expected=Random.Int(2)==0;Random.popGenerator();Random.pushGenerator(seed);try{fire.proc(enemy);}finally{Random.popGenerator();}assertEquals(expected,enemy.buff(Burning.class)!=null);}
 }
 @Test public void frostImbueDescriptionMatchesStackedChillAndActualImmunity()throws Exception{
  PlainStatusTarget enemy=new PlainStatusTarget();enemy.sprite=quietSprite();FrostImbue frost=Buff.affect(hero,FrostImbue.class,8);frost.proc(enemy);float first=enemy.buff(Chill.class).cooldown();frost.proc(enemy);assertEquals(first+3,enemy.buff(Chill.class).cooldown(),.00001f);assertTrue(hero.isImmune(Chill.class));assertTrue(hero.isImmune(Frost.class));assertTrue(frost.desc().contains("_3 turns_"));assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch().desc().startsWith("For _50 turns_"));assertTrue(MechanicsDetails.buff(frost,frost.desc()).contains("3 turns of Chill"));
 }

 @Test public void classArmorShowsCurrentMeterCostAndTrinityDoesNotPromiseOneFixedCost(){
  WarriorArmor armor=new WarriorArmor();hero.belongings.armor=armor;hero.armorAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap();
  for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.HEROIC_ENERGY,rank);float actual=hero.armorAbility.chargeUse(hero);String text=armor.desc();assertTrue(text,text.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(armor,"charge_use",com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("#.##",actual))));assertTrue(text,text.contains("% charge"));assertTrue(AbilityExplanation.summary(hero.armorAbility,hero).contains("out of 100"));}
  hero.armorAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity();assertFalse(armor.desc().contains("Ability cost:"));String text=AbilityExplanation.summary(hero.armorAbility,hero);assertTrue(text,text.contains("depends on the effect"));assertFalse(text.contains("Current cost:"));
 }
 @Test public void armorAbilityExplanationMatchesActualRankAndConditionalCost(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap leap=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap();
  for(int rank=0;rank<=4;rank++){hero.talents.get(2).put(Talent.HEROIC_ENERGY,rank);String text=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.AbilityExplanation.summary(leap,hero);assertTrue(text.contains(ObfuscationExplanation.number(leap.chargeUse(hero))+" charge points"));}
  hero.talents.get(2).put(Talent.DOUBLE_JUMP,2);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap.DoubleJumpTracker.class);assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.AbilityExplanation.details(leap,hero).contains("cost: "+ObfuscationExplanation.number(leap.chargeUse(hero))+" charge points"));hero.armorAbility=leap;assertTrue(MechanicsDetails.item(new WarriorArmor(),"").contains("Current cost: "+ObfuscationExplanation.number(leap.chargeUse(hero))+" charge points"));
 }
 @Test public void abilityDetailsNeverConsumeReadyDiscountOrExposeUnknownRingLevel(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap leap=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap();hero.talents.get(2).put(Talent.DOUBLE_JUMP,1);com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap.DoubleJumpTracker ready=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap.DoubleJumpTracker.class);RingOfEnergy energy=new RingOfEnergy();energy.levelKnown=false;hero.belongings.ring=energy;energy.activate(hero);String first=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.AbilityExplanation.details(leap,hero);energy.level(9);assertEquals(first,com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.AbilityExplanation.details(leap,hero));assertSame(ready,hero.buff(ready.getClass()));
 }

 @Test public void trinityChargeDetailsNeverPresentTheBaseAsAUniversalEffectCost(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity trinity=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity();
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility previous=hero.armorAbility;hero.armorAbility=trinity;
  try{for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.HEROIC_ENERGY,rank);float base=trinity.chargeUse(hero);assertEquals(base,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity.trinityChargeUsePerEffect(com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile.class),0);assertEquals(2*base,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity.trinityChargeUsePerEffect(com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth.class),0);String text=AbilityExplanation.details(trinity,hero);assertTrue(text,text.contains("no single cost for Trinity"));assertFalse(text.contains("Current cost:"));assertFalse(text.contains("Base cost:"));if(rank>0)assertTrue(text,text.contains(ObfuscationExplanation.number(100*(1-com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility.heroicEnergyFactor(rank)))+"% at rank "+rank));}}
  finally{hero.armorAbility=previous;}
 }
 @Test public void armorRechargeDetailsMatchActualChargerAndMeterCap(){
  com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor equipment=new com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor();com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor.Charger charger=equipment.new Charger();charger.target=hero;
  RingOfEnergy energy=new RingOfEnergy();energy.anonymize();energy.levelKnown=energy.cursedKnown=true;hero.belongings.ring=energy;energy.activate(hero);com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap();
  for(int level=0;level<=3;level++){energy.level(level);equipment.charge=0;charger.act();float actual=equipment.charge;assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.AbilityExplanation.details(ability,hero).contains("Current recharge: "+ObfuscationExplanation.number(actual)+" charge points per turn"));equipment.charge=99.99f;charger.act();assertEquals(100,equipment.charge,0);}
 }

 @Test public void paralysisExplanationMatchesActualEscapeRollBeforeShielding(){
  for(int seed=0;seed<30;seed++){PlainStatusTarget target=new PlainStatusTarget();target.pos=24;Paralysis paralysis=Buff.affect(target,Paralysis.class,5);Barrier shield=Buff.affect(target,Barrier.class);shield.setShield(100);
   Random.pushGenerator(seed);boolean breaks=Random.NormalIntRange(0,20)>=Random.NormalIntRange(0,100);Random.popGenerator();Random.pushGenerator(seed);try{target.damage(20,new Object());}finally{Random.popGenerator();}
   assertEquals(breaks,target.buff(Paralysis.class)==null);assertEquals(100,target.HP);assertEquals(20,target.buff(Paralysis.ParalysisResist.class).accumulatedDamage());assertTrue(BuffExplanation.details(paralysis).contains("before shield points"));}
 }
 @Test public void paralysisDetailsHideEnemyCounterAndPreserveHeroCounter(){
  Paralysis heroEffect=Buff.affect(hero,Paralysis.class,5);heroEffect.processDamage(3);Paralysis.ParalysisResist count=hero.buff(Paralysis.ParalysisResist.class);int before=count.accumulatedDamage();for(int i=0;i<30;i++)assertTrue(BuffExplanation.details(heroEffect).contains("counted damage: "+before+" damage points"));assertEquals(before,count.accumulatedDamage());
  PlainStatusTarget enemy=new PlainStatusTarget();Paralysis other=Buff.affect(enemy,Paralysis.class,5);assertFalse(BuffExplanation.details(other).contains("current counted damage"));
 }
 @Test public void magicalSleepExplanationMatchesActualHealingAndFullHpEnding()throws Exception{
  hero.sprite=quietSprite();hero.HP=18;MagicalSleep sleep=Buff.affect(hero,MagicalSleep.class);assertTrue(BuffExplanation.details(sleep).contains("Normally restores 1 HP per turn"));float fraction=hero.apdCustomHealingFraction;for(int i=0;i<30;i++)MechanicsDetails.buff(sleep,sleep.desc());assertEquals(18,hero.HP);assertEquals(fraction,hero.apdCustomHealingFraction,0);sleep.act();assertEquals(19,hero.HP);sleep.act();assertEquals(20,hero.HP);assertNull(hero.buff(MagicalSleep.class));
 }

 @Test public void regrowthAllowanceExplanationMatchesRealFormulaRatherThanStaleComments()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;
  java.lang.reflect.Method actual=wand.getClass().getDeclaredMethod("chargeLimit",int.class);actual.setAccessible(true);
  for(int heroLevel:new int[]{1,10,20,30}){hero.lvl=heroLevel;for(int level=0;level<=10;level++){wand.level(level);int expected=(Integer)actual.invoke(wand,heroLevel);assertEquals(expected,wand.previewChargeLimit(heroLevel));if(expected<10000)assertTrue(wand.statsDesc(),wand.statsDesc().contains("Charges left before plant growth becomes unreliable: _"+expected));assertFalse(wand.statsDesc(),wand.statsDesc().contains("1 charges"));String detail=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.WandExplanation.details(wand);if(level<10)assertTrue(detail,detail.contains("becomes exhausted after "+expected+" charges"));else{assertTrue(detail,detail.contains("exhaustion is effectively disabled"));assertFalse(detail.contains("2147483647"));}}}
 }
 @Test public void regrowthDepletionDetailsFollowSavedCountersAndIgnoreTemporaryLevelForAllowance(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.level(3);int limit=wand.previewChargeLimit(hero.lvl);com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();wand.storeInBundle(saved);saved.put("totChrgUsed",limit);saved.put("chargesOverLimit",3);wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth();wand.restoreFromBundle(saved);
  assertEquals(.8f,wand.previewFurrowedChance(hero.lvl),0);String text=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.WandExplanation.details(wand);assertTrue(text.contains("currently 80%"));wand.charge(hero);Buff.affect(hero,ScrollEmpower.class).reset(3);assertEquals(limit,wand.previewChargeLimit(hero.lvl));assertEquals(.8f,wand.previewFurrowedChance(hero.lvl),0);
 }
 @Test public void corruptionDetailsUseSharedLivePowerAndDoNotClaimOneSuccessChance(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;
  for(int level=0;level<=9;level++){wand.level(level);String text=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.WandExplanation.details(wand);assertTrue(text.contains("Current strength: "+ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption.corruptionPower(level))));assertTrue(text.contains("not the final corruption chance"));assertTrue(text.contains("strictly greater"));}
 }
 @Test public void wardSupportUsesOrdinaryKnownLevelRatherThanTemporaryCastingLevel(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.level(3);wand.charge(hero);Buff.affect(hero,ScrollEmpower.class).reset(3);assertEquals(5,wand.previewEffectiveLevels()[0]);String text=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.WandExplanation.details(wand);assertTrue(text.contains("supports 5 points of wards"));assertTrue(text.contains("Temporary casting levels do not increase"));
 }

 private com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff describedStaff(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand,int level)throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff();staff.level(level);staff.levelKnown=true;wand.level(level);wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.charge(hero);java.lang.reflect.Field field=staff.getClass().getDeclaredField("wand");field.setAccessible(true);field.set(staff,wand);return staff;
 }
 @Test public void transfusionStaffRequiresCharmOwnedByAttackerAndPreparesOnlyHealthExemption()throws Exception{
  hero.subClass=HeroSubClass.BATTLEMAGE;hero.sprite=quietSprite();
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion();
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=describedStaff(wand,3);
  PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();
  for(int owner:new int[]{-1,hero.id()+1,hero.id()}){
   Buff.detach(target,Charm.class);Buff.detach(hero,Barrier.class);
   java.lang.reflect.Field free=wand.getClass().getDeclaredField("freeCharge");free.setAccessible(true);free.setBoolean(wand,false);
   if(owner!=-1)Buff.affect(target,Charm.class,10).object=owner;
   int charges=wand.curCharges;float time=hero.cooldown();wand.onHit(staff,hero,target,10);
   com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();wand.storeInBundle(after);
   assertEquals(owner==hero.id(),after.getBoolean("freecharge"));assertEquals(charges,wand.curCharges);assertEquals(time,hero.cooldown(),0);
   if(owner==hero.id())assertEquals(16,hero.buff(Barrier.class).shielding());else assertNull(hero.buff(Barrier.class));
  }
  assertTrue(staff.info(),staff.info().contains("charmed by you"));assertTrue(staff.info().contains("still uses a charge and a turn"));
  hero.subClass=HeroSubClass.NONE;assertFalse(staff.info().contains("charmed by you"));wand.stopCharging();
 }
 @Test public void wardStaffDescriptionDistinguishesAttackRepairFromSentryHealing()throws Exception{
  hero.subClass=HeroSubClass.BATTLEMAGE;com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding();
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=describedStaff(wand,3);
  assertTrue(staff.info(),staff.info().contains("restore spent attacks"));assertTrue(staff.info().contains("Basic wards cannot be repaired"));
  for(int tier=1;tier<=6;tier++){
   com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding.Ward ward=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding.Ward();ward.tier=tier;ward.totalZaps=4;ward.HP=5;ward.HT=100;
   Random.pushGenerator(789);try{ward.wandHeal(3,1);}finally{Random.popGenerator();}
   if(tier==1){assertEquals(4,ward.totalZaps);assertEquals(5,ward.HP);}else if(tier<=3){assertTrue(ward.totalZaps<4);assertEquals(5,ward.HP);}else{assertEquals(4,ward.totalZaps);assertTrue(ward.HP>5);}
  }
  wand.stopCharging();
 }
 @Test public void staffCorruptionChanceAndDurationMatchActualOnHitChecks()throws Exception{
  hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BATTLEMAGE;hero.talents.get(2).put(Talent.EMPOWERED_STRIKE,2);Buff.affect(hero,Talent.EmpoweredStrikeTracker.class);com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=describedStaff(wand,3);float chance=4f/9f*com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.procChanceMultiplier(hero);String text=staff.mechanicsDetails();assertTrue(text.contains(ObfuscationExplanation.number(100*chance)+"% per eligible hit"));
  for(int seed=0;seed<40;seed++){PlainStatusTarget target=new PlainStatusTarget();Random.pushGenerator(seed);boolean expected=Random.Float()<chance;Random.popGenerator();Random.pushGenerator(seed);try{wand.onHit(staff,hero,target,10);}finally{Random.popGenerator();}assertEquals(expected,target.buff(Amok.class)!=null);if(expected)assertEquals(Math.round(10*Math.max(1,chance)),target.buff(Amok.class).cooldown(),.00001f);}
 }
 @Test public void staffRegrowthPoolMatchesActualTwoRoundingSteps()throws Exception{
  hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BATTLEMAGE;hero.talents.get(2).put(Talent.EMPOWERED_STRIKE,1);Buff.affect(hero,Talent.EmpoweredStrikeTracker.class);Dungeon.level.map[hero.pos]=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.GRASS;PlainStatusTarget target=new PlainStatusTarget();target.pos=25;com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=describedStaff(wand,3);
  for(int damage:new int[]{1,7,18,31}){com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health old=hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health.class);if(old!=null)old.detach();wand.onHit(staff,hero,target,damage);int expected=Math.round(Math.round(damage*5f/9f/2f)*1.5f);assertEquals(Integer.toString(expected),hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health.class).iconTextDisplay());}assertTrue(staff.mechanicsDetails().contains("round again"));assertTrue(staff.info(),staff.info().contains("Stay still"));
  com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);assertTrue(staff.info(),staff.info().contains("rules prevent the Battlemage healing"));hero.HP=10;hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health health=hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health.class);for(int tick=0;tick<3;tick++)health.act();assertEquals(10,hero.HP);
 }
 @Test public void staffPreviewUnderWildMagicNeverConsumesRandomnessOrReadyStrike()throws Exception{
  hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BATTLEMAGE;hero.talents.get(2).put(Talent.EMPOWERED_STRIKE,2);Talent.EmpoweredStrikeTracker ready=Buff.affect(hero,Talent.EmpoweredStrikeTracker.class);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic.WildMagicTracker.class);hero.talents.get(1).put(Talent.WILD_POWER,1);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=describedStaff(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning(),0);
  Random.pushGenerator(67);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(67);try{for(int i=0;i<100;i++){assertTrue(staff.mechanicsDetails().contains("single fixed activation percentage is not shown"));MechanicsDetails.item(staff,staff.info());}assertEquals(expected,Random.Int());}finally{Random.popGenerator();}assertSame(ready,hero.buff(Talent.EmpoweredStrikeTracker.class));
 }
 @Test public void unidentifiedStaffOnHitDetailsDoNotRevealHiddenUpgrade()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=describedStaff(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion(),0);staff.levelKnown=false;String first=staff.mechanicsDetails();staff.level(9);assertEquals(first,staff.mechanicsDetails());assertTrue(first.contains("Identify"));
 }

 @Test public void herbalHealingPreviewMatchesActualRequestsAndDoesNotSpendPool()throws Exception{
  hero.sprite=quietSprite();hero.HP=1;com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health healing=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health.class);healing.boost(100);
  for(int tick=0;tick<12;tick++){int expected=healing.nextUnmodifiedRequest(),pool=healing.remainingPool();float progress=healing.healingProgress();String text=MechanicsDetails.buff(healing,healing.desc());assertTrue(text.contains("next base heal: "+expected+" HP"));assertEquals(pool,healing.remainingPool());assertEquals(progress,healing.healingProgress(),0);hero.HP=1;healing.act();assertEquals(1+expected,hero.HP);assertEquals(pool-expected,healing.remainingPool());}
  int remaining=healing.remainingPool(),next=healing.nextUnmodifiedRequest();float fraction=healing.healingProgress();com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();healing.storeInBundle(saved);healing.detach();com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health loaded=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health();loaded.restoreFromBundle(saved);loaded.attachTo(hero);assertEquals(remaining,loaded.remainingPool());assertEquals(fraction,loaded.healingProgress(),0);assertEquals(next,loaded.nextUnmodifiedRequest());
 }
 public static class FixedPrismaticDescriptionProbe extends com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight {@Override public int damageRoll(){return 20;}}
 public static class PrismaticDescriptionTarget extends LightningDamageTarget {public void traits(int mask){if((mask&1)!=0)properties.add(com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.UNDEAD);if((mask&2)!=0)properties.add(com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.DEMONIC);}}
 @Test public void prismaticNormalTextMatchesRealBonusBlindnessAndStaffSlow()throws Exception{
  descriptionSightArena();java.lang.reflect.Method affect=com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight.class.getDeclaredMethod("affectTarget",com.shatteredpixel.shatteredpixeldungeon.actors.Char.class);affect.setAccessible(true);FixedPrismaticDescriptionProbe wand=new FixedPrismaticDescriptionProbe();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;
  java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);LightningDescriptionSprite sprite=(LightningDescriptionSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,LightningDescriptionSprite.class);
  for(int level:new int[]{0,3,8}){
   wand.level(level);assertTrue(wand.statsDesc(),wand.statsDesc().contains("_"+wand.min(level)+"-"+wand.max(level)+" base damage_"));assertTrue(wand.statsDesc().contains("hidden doors and traps along the beam"));
   for(int mask=0;mask<4;mask++)for(int seed=0;seed<8;seed++){
    PrismaticDescriptionTarget target=new PrismaticDescriptionTarget();target.pos=hero.pos+1;target.sprite=sprite;target.traits(mask);Random.pushGenerator(seed);boolean blind=Random.Int(5+level)>=3;Random.popGenerator();Random.pushGenerator(seed);try{affect.invoke(wand,target);}finally{Random.popGenerator();}
    assertEquals(mask==0?20:Math.round(20*1.333f),target.received);assertEquals(blind,target.buff(Blindness.class)!=null);Buff.detach(target,Blindness.class);
   }
   com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=describedStaff(wand,level);PlainStatusTarget target=new PlainStatusTarget();float normal=target.speed();wand.onHit(staff,hero,target,7);assertEquals(normal*.5f,target.speed(),.00001f);assertEquals(level+1,target.buff(Cripple.class).cooldown(),.00001f);Buff.detach(target,Cripple.class);wand.stopCharging();
  }
 }
 @Test public void rockArmorExplanationMatchesActualSmallHitRoundingAndLimitedReserve(){
  for(int level:new int[]{0,3,8})for(int reserve:new int[]{1,3,11})for(int damage:new int[]{1,2,7,20}){
   com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor armor=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor.class);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();armor.storeInBundle(state);state.put("armor",reserve);state.put("wand_level",level);armor.restoreFromBundle(state);
   String before=savedDescriptionHero();assertTrue(armor.desc(),armor.desc().contains("Reserve: _"+reserve+"_"));assertTrue(armor.desc(),armor.desc().contains("_"+(8+4*level)+"_ reserve"));assertTrue(MechanicsDetails.buff(armor,armor.desc()).contains("Current reserve: "+reserve+" damage-blocking points"));assertEquals(before,savedDescriptionHero());
   int spent=Math.min(reserve,(damage+1)/2);assertEquals(damage-spent,armor.previewAbsorb(damage));assertEquals(damage-spent,armor.absorb(damage));if(reserve<=spent)assertNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor.class));else assertEquals(reserve-spent,armor.remainingReserve());Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor.class);
  }
 }
 @Test public void livingEarthStaffActuallyBuildsReserveOrHealsGuardianAtEachLevel()throws Exception{
  java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);hero.sprite=(LightningDescriptionSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,LightningDescriptionSprite.class);
  hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BATTLEMAGE;Dungeon.level.mobs=new java.util.HashSet<>();
  for(int level:new int[]{0,3,8})for(int rank:new int[]{0,1,2}){
   hero.talents.get(2).put(Talent.EMPOWERED_STRIKE,rank);if(rank>0)Buff.affect(hero,Talent.EmpoweredStrikeTracker.class);else Buff.detach(hero,Talent.EmpoweredStrikeTracker.class);
   com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=describedStaff(wand,level);
   for(int damage:new int[]{1,7,18,31}){
    Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor.class);int expected=Math.round(damage*.33f*(1+rank/2f));wand.onHit(staff,hero,new PlainStatusTarget(),damage);com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor reserve=hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor.class);assertEquals(Math.min(expected,2*(8+4*level)),reserve.remainingReserve());
    com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.EarthGuardian guardian=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.EarthGuardian();guardian.sprite=hero.sprite;guardian.setInfo(hero,level,1);Dungeon.level.mobs.add(guardian);int beforeReserve=reserve.remainingReserve();try{wand.onHit(staff,hero,new PlainStatusTarget(),damage);assertEquals(Math.min(guardian.HT,1+expected),guardian.HP);assertEquals(16+8*level,guardian.HT);assertEquals(beforeReserve,reserve.remainingReserve());}finally{Dungeon.level.mobs.remove(guardian);}
   }
   assertTrue(staff.info(),staff.info().contains("restore your guardian's health"));wand.stopCharging();
  }
 }
 @Test public void earthrootExplanationPreservesItsLastHitRuleRatherThanRockArmorRule(){
  com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Armor earth=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Armor.class);earth.level(1);int limit=earth.perHitLimit();assertTrue(earth.desc().contains("at least 1 more damage"));assertTrue(MechanicsDetails.buff(earth,earth.desc()).contains("Remaining reserve: 1 points"));assertEquals(11-Math.min(11,limit),earth.absorb(11));assertNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Armor.class));
 }

 @Test public void magicalSightDescriptionUsesLiveRadiusAndFieldOfViewDoesNotDiscoverSecret(){
  Dungeon.level.blobs=new java.util.HashMap<>();Dungeon.level.mobs=new java.util.HashSet<>();Dungeon.level.heaps=new com.watabou.utils.SparseArray<>();Dungeon.level.traps=new com.watabou.utils.SparseArray<>();Dungeon.level.plants=new com.watabou.utils.SparseArray<>();Dungeon.level.customTiles=new java.util.ArrayList<>();Dungeon.level.customWalls=new java.util.ArrayList<>();Dungeon.level.setSize(33,33);java.util.Arrays.fill(Dungeon.level.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);Dungeon.level.buildFlagMaps();Dungeon.level.discoverable=new boolean[Dungeon.level.length()];java.util.Arrays.fill(Dungeon.level.discoverable,true);hero.pos=16*33+16;hero.viewDistance=2;MagicalSight sight=Buff.affect(hero,MagicalSight.class,8);boolean[] view=new boolean[Dungeon.level.length()];int secret=hero.pos+3;com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(secret,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_DOOR);Dungeon.level.updateFieldOfView(hero,view);assertTrue(view[hero.pos+MagicalSight.DISTANCE]);assertFalse(view[hero.pos+MagicalSight.DISTANCE+1]);assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_DOOR,Dungeon.level.map[secret]);assertTrue(sight.desc().contains(MagicalSight.DISTANCE+" tiles"));
 }
 @Test public void invisibilityDescriptionQueriesPreserveIndependentSources(){
  Invisibility first=Buff.append(hero,Invisibility.class,4),second=Buff.append(hero,Invisibility.class,8);assertEquals(2,hero.invisible);for(int i=0;i<30;i++)assertTrue(MechanicsDetails.buff(first,first.desc()).contains("last known position"));assertEquals(2,hero.invisible);first.detach();assertEquals(1,hero.invisible);assertSame(second,hero.buff(Invisibility.class));
 }
 @Test public void invulnerabilityBlocksActualDamageWithoutPretendingAllStatusImmunity()throws Exception{
  PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();Invulnerability protect=Buff.affect(target,Invulnerability.class,3);Barrier shield=Buff.affect(target,Barrier.class);shield.setShield(20);target.damage(15,new Object());assertEquals(100,target.HP);assertEquals(20,shield.shielding());assertFalse(target.isImmune(Poison.class));assertNotNull(Buff.affect(target,Roots.class));assertTrue(target.rooted);assertTrue(MechanicsDetails.buff(protect,protect.desc()).contains("does not prevent every harmful status"));
 }

 public static class MindVisionObject extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat {public MindVisionObject(){properties.add(com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.OBJECT);}}
 @Test public void mindVisionDescriptionMatchesRealCreatureSensingExclusions()throws Exception{
  Dungeon.level.blobs=new java.util.HashMap<>();Dungeon.level.mobs=new java.util.HashSet<>();Dungeon.level.heaps=new com.watabou.utils.SparseArray<>();Dungeon.level.traps=new com.watabou.utils.SparseArray<>();Dungeon.level.plants=new com.watabou.utils.SparseArray<>();Dungeon.level.customTiles=new java.util.ArrayList<>();Dungeon.level.customWalls=new java.util.ArrayList<>();Dungeon.level.setSize(33,33);Dungeon.level.buildFlagMaps();Dungeon.level.discoverable=new boolean[Dungeon.level.length()];java.util.Arrays.fill(Dungeon.level.discoverable,true);hero.pos=16*33+16;hero.viewDistance=1;MindVision sense=Buff.affect(hero,MindVision.class,8);
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat ordinary=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();ordinary.pos=hero.pos+10;Dungeon.level.mobs.add(ordinary);
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic mimic=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic();mimic.pos=hero.pos-10;mimic.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.NEUTRAL;java.lang.reflect.Field stealth=mimic.getClass().getDeclaredField("stealthy");stealth.setAccessible(true);stealth.setBoolean(mimic,true);Dungeon.level.mobs.add(mimic);
  MindVisionObject object=new MindVisionObject();object.pos=hero.pos+10*33;Dungeon.level.mobs.add(object);boolean[] view=new boolean[Dungeon.level.length()];Dungeon.level.updateFieldOfView(hero,view);assertTrue(view[ordinary.pos]);assertFalse(view[mimic.pos]);assertFalse(view[object.pos]);assertTrue(MechanicsDetails.buff(sense,sense.desc()).contains("stealthy mimics"));
 }

 @Test public void spellDescriptionsReadCurrentCostWithoutCastingOrConsumingMealBonus()throws Exception{
  hero.sprite=quietSprite();hero.talents.get(1).put(Talent.SATIATED_SPELLS,1);Talent.SatiatedSpellsTracker ready=Buff.affect(hero,Talent.SatiatedSpellsTracker.class);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome();tome.directCharge(3);int before=tome.currentCharges();float time=hero.cooldown();
  Random.pushGenerator(512);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(512);try{for(int i=0;i<100;i++){String text=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE,hero);assertTrue(text.contains("grants _3 shield points_"));assertTrue(text.contains("Tome charge cost: _2_"));}assertEquals(expected,Random.Int());}finally{Random.popGenerator();}assertEquals(before,tome.currentCharges());assertSame(ready,hero.buff(Talent.SatiatedSpellsTracker.class));assertEquals(time,hero.cooldown(),0);
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE.onSpellCast(tome,hero);assertEquals(before-2,tome.currentCharges());assertNull(hero.buff(Talent.SatiatedSpellsTracker.class));assertEquals(3,hero.buff(Barrier.class).shielding());
 }
 public static class IlluminationAttacker extends PlainStatusTarget {@Override public int damageRoll(){return 10;}}
 @Test public void guidingLightPriestDescriptionMatchesAnotherCreaturesActualIlluminationHit()throws Exception{
  hero.heroClass=HeroClass.CLERIC;hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.PRIEST;
  IlluminationAttacker attacker=new IlluminationAttacker();attacker.pos=hero.pos;PlainStatusTarget target=new PlainStatusTarget();target.pos=hero.pos+1;
  for(int level:new int[]{1,10,30}){
   hero.lvl=level;target.HP=target.HT=1000;Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.Illuminated.class);
   assertTrue(attacker.attack(target,1,0,com.shatteredpixel.shatteredpixeldungeon.actors.Char.INFINITE_ACCURACY));assertEquals(1000-10-(level+5),target.HP);assertNull(target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.Illuminated.class));
   String text=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.INSTANCE,hero);assertTrue(text,text.contains("another creature consumes Illumination with an attack"));assertTrue(text,text.contains("_"+(level+5)+" base magic damage_"));
   assertTrue(attacker.attack(target,1,0,com.shatteredpixel.shatteredpixeldungeon.actors.Char.INFINITE_ACCURACY));assertEquals(1000-20-(level+5),target.HP);
  }
 }
 @Test public void paladinSpellExtensionDescriptionMatchesActualCapWithoutShorteningLongerEffects(){
  hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.PALADIN;
  for(float initial:new float[]{10,80,95,120}){
   Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.HolyWepBuff.class);Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff.class);
   com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.HolyWepBuff weapon=Buff.append(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.HolyWepBuff.class,initial);
   com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff ward=Buff.append(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff.class,initial);
   assertEquals(initial,weapon.cooldown(),0);assertEquals(initial,ward.cooldown(),0);weapon.extend(20);ward.extend(20);float expected=initial+20<=100?initial+20:Math.max(initial,100);assertEquals(expected,weapon.cooldown(),0);assertEquals(expected,ward.cooldown(),0);
   String text=SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.INSTANCE,hero);assertTrue(text,text.contains("10 turns per Tome charge spent"));assertTrue(text,text.contains("Longer existing effects are kept"));
  }
 }
 @Test public void priestGuidingLightCostDescriptionTracksRealFreeCastCooldown(){
  hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.PRIEST;com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight spell=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.INSTANCE;assertEquals(0,spell.chargeUse(hero),0);assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.summary(spell,hero).contains("Tome charge cost: 0"));Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.GuidingLightPriestCooldown.class,50);assertEquals(1,spell.chargeUse(hero),0);assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.summary(spell,hero).contains("Tome charge cost: 1"));
 }
 public static class UndeadSpellTarget extends PlainStatusTarget {public UndeadSpellTarget(){properties.add(com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.UNDEAD);}}
 @Test public void smiteExplanationMatchesActualUndeadMaximumAcrossHeroLevels(){
  UndeadSpellTarget undead=new UndeadSpellTarget();for(int level:new int[]{1,5,20,30}){hero.lvl=level;int actual=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite.bonusDmg(hero,undead);String text=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite.INSTANCE,hero);assertEquals(10+level,actual);assertTrue(text.contains("_"+(5+level/2)+"–"+actual+" base magic damage_"));}
 }
 @Test public void judgementExplanationRefreshesCounterWithoutSpendingItOrGrantingShield(){
  hero.talents.get(2).put(Talent.JUDGEMENT,2);com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff ascend=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.class);for(int count:new int[]{0,3,7}){ascend.spellCasts=count;int minimum=15+Math.round(15*count/3f);for(int i=0;i<30;i++)assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Judgement.INSTANCE,hero).contains("_"+minimum+"–"+(2*minimum)+" base magic damage_"));assertEquals(count,ascend.spellCasts);assertEquals(0,ascend.shielding());}
 }
 @Test public void auraDamageExplanationMatchesActualAllyDamageStage(){
  Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection.AuraBuff.class);for(int rank=1;rank<=4;rank++){hero.talents.get(2).put(Talent.AURA_OF_PROTECTION,rank);PlainStatusTarget ally=new PlainStatusTarget();ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;ally.pos=hero.pos;ally.damage(20,new Object());assertEquals(100-Math.round(20*(.9f-.1f*rank)),ally.HP);assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection.INSTANCE,hero).contains("_"+Math.round(100*(1-(.9f-.1f*rank)))+"%_"));}
 }
 @Test public void holySpellPreviewDoesNotRevealUnknownArcanaLevel(){
  RingOfArcana ring=ring(0);ring.levelKnown=false;String ward=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE,hero),weapon=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE,hero);ring.level(9);assertEquals(ward,com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE,hero));assertEquals(weapon,com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE,hero));
 }

 @Test public void spellDetailsDistinguishMagicImmunityFromUnknownRingKnowledge(){
  ring(2);hero.belongings.armor=armor;hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();Buff.affect(hero,MagicImmune.class);
  for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell spell:new com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell[]{com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection.INSTANCE}){
   String text=SpellExplanation.details(spell,hero);assertTrue(text,text.contains("Magic immunity currently"));assertFalse(text,text.contains("Identify "));
  }
 }
 @Test public void conciseHolyDescriptionsHideUnknownRingsAndDoNotInventUnarmoredProtection(){
  RingOfArcana arcana=ring(0);arcana.levelKnown=false;String weapon=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE.desc(),ward=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE.desc();arcana.level(9);assertEquals(weapon,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE.desc());assertEquals(ward,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE.desc());arcana.levelKnown=true;
  hero.belongings.armor=null;String unarmored=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE.desc();assertTrue(unarmored,unarmored.contains("varies with the attacker"));hero.belongings.armor=armor;Buff.affect(hero,MagicImmune.class);assertTrue(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE.desc().contains("currently disables"));assertTrue(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE.desc().contains("disables this bonus on weapons"));
 }
 @Test public void activeHolyBuffExplanationsMatchActualArcanaScaledWeaponAndArmorProcs(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=sword.cursedKnown=true;hero.belongings.weapon=sword;
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.HolyWepBuff weaponBuff=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.HolyWepBuff.class,50);com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff armorBuff=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff.class,50);RingOfArcana ring=ring(0);
  for(HeroSubClass subclass:new HeroSubClass[]{HeroSubClass.NONE,HeroSubClass.PALADIN})for(int level=0;level<=3;level++){hero.subClass=subclass;ring.level(level);StatusDamageObserver enemy=new StatusDamageObserver();sword.proc(hero,enemy,10);assertTrue(MechanicsDetails.buff(weaponBuff,weaponBuff.desc()).contains("Current bonus: _"+enemy.received+" base magic damage_"));assertTrue(weaponBuff.desc(),weaponBuff.desc().contains("_"+enemy.received+" extra base magic damage_"));assertTrue(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE.desc().contains("_"+enemy.received+" extra base magic damage_"));int block=10-armor.proc(enemy,hero,10);assertTrue(MechanicsDetails.buff(armorBuff,armorBuff.desc()).contains("Current bonus: _"+block+" damage blocked_"));assertTrue(armorBuff.desc(),armorBuff.desc().contains("_"+block+" extra attack damage_"));assertTrue(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE.desc().contains("_"+block+" extra attack damage_"));assertTrue(weaponBuff.desc().contains("Turns remaining:"));assertFalse(weaponBuff.desc().contains("casts instantly"));assertEquals(subclass==HeroSubClass.PALADIN,weaponBuff.desc().contains("toward 100 turns"));}
 }

 public static class SelectedEnemyProbe extends com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator {public com.shatteredpixel.shatteredpixeldungeon.actors.Char selected;@Override public com.shatteredpixel.shatteredpixeldungeon.actors.Char target(){return selected;}}
 @Test public void shieldOfLightDetailsMatchActualInstantCastHeroAndLinkedAllyDisplayedTimers()throws Exception{
  descriptionSightArena();hero.sprite=quietSprite();hero.heroClass=HeroClass.CLERIC;hero.talents.get(0).put(Talent.SHIELD_OF_LIGHT,1);
  PlainStatusTarget enemy=new PlainStatusTarget(),ally=new PlainStatusTarget();enemy.pos=hero.pos+1;enemy.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;ally.pos=hero.pos-1;ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;ally.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(enemy);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(ally);Dungeon.level.heroFOV[enemy.pos]=true;
  Buff.affect(ally,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff.class,50);Buff.affect(ally,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell.LifeLinkSpellBuff.class,50);
  java.lang.reflect.Field field=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");field.setAccessible(true);Object unsafe=field.get(null);SelectedEnemyProbe probe=(SelectedEnemyProbe)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,SelectedEnemyProbe.class);
  com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator previous=com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator.instance;com.shatteredpixel.shatteredpixeldungeon.actors.Char oldQuick=com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.lastTarget,oldInventory=com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane.lastTarget;
  java.lang.reflect.Field currentUser=com.shatteredpixel.shatteredpixeldungeon.items.Item.class.getDeclaredField("curUser");currentUser.setAccessible(true);Object oldCurrentUser=currentUser.get(null);
  try{com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator.instance=probe;com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome();tome.directCharge(3);int charge=tome.currentCharges();float time=hero.cooldown();java.lang.reflect.Method cast=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight.class.getDeclaredMethod("onTargetSelected",com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome.class,Hero.class,Integer.class);cast.setAccessible(true);cast.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight.INSTANCE,tome,hero,enemy.pos);
   assertEquals(5,hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight.ShieldOfLightTracker.class).visualcooldown(),0);assertEquals(4,ally.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight.ShieldOfLightTracker.class).visualcooldown(),0);assertEquals(time,hero.cooldown(),0);assertEquals(charge-1,tome.currentCharges());String before=savedDescriptionHero();String text=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight.INSTANCE,hero);assertTrue(text,text.contains("Lasts 5 turns for you, or 4 turns for the linked ally"));assertFalse(text.contains("status update"));assertEquals(before,savedDescriptionHero());
  }finally{com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator.instance=previous;com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.lastTarget=oldQuick;com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane.lastTarget=oldInventory;currentUser.set(null,oldCurrentUser);for(Buff effect:ally.buffs())effect.detach();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(ally);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(enemy);}
 }
 @Test public void alternateShieldOfLightDescriptionMatchesActualSelectedEnemyDefense()throws Exception{
  java.lang.reflect.Field field=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");field.setAccessible(true);Object unsafe=field.get(null);SelectedEnemyProbe probe=(SelectedEnemyProbe)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,SelectedEnemyProbe.class);com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator previous=com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator.instance;
  try{com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator.instance=probe;PlainStatusTarget enemy=new PlainStatusTarget();probe.selected=enemy;
   for(int rank=1;rank<=2;rank++){hero.talents.get(0).put(Talent.SHIELD_OF_LIGHT,rank);assertTrue(MechanicsDetails.talent(Talent.SHIELD_OF_LIGHT,rank,"").contains(ObfuscationExplanation.number(100f*(1+rank)/6f)+"% for that matching attack"));
    for(int seed=0;seed<50;seed++){Random.pushGenerator(seed);boolean blocks=Random.Int(6)<1+rank;Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(blocks?9:10,hero.defenseProc(enemy,10));}finally{Random.popGenerator();}}
    assertEquals(10,hero.defenseProc(new PlainStatusTarget(),10));
   }
  }finally{com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator.instance=previous;}
 }
 public static class ContestScoreTarget extends PlainStatusTarget {@Override public int attackSkill(com.shatteredpixel.shatteredpixeldungeon.actors.Char other){return 20;}@Override public int defenseSkill(com.shatteredpixel.shatteredpixeldungeon.actors.Char other){return 20;}}
 @Test public void alternateBlessDescriptionMatchesActualAlliedAccuracyAndEvasionRolls(){
  ContestScoreTarget ally=new ContestScoreTarget(),enemy=new ContestScoreTarget();ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;enemy.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;
  for(int rank=1;rank<=2;rank++){hero.talents.get(1).put(Talent.BLESS,rank);float factor=1.01f+.02f*rank;assertTrue(MechanicsDetails.talent(Talent.BLESS,rank,"").contains("= "+ObfuscationExplanation.number(factor)));
   for(int seed=0;seed<50;seed++){Random.pushGenerator(seed);boolean hit=Random.Float(20)*factor>=Random.Float(20);Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(hit,com.shatteredpixel.shatteredpixeldungeon.actors.Char.hit(ally,enemy,1,false));}finally{Random.popGenerator();}
    Random.pushGenerator(seed);boolean enemyHit=Random.Float(20)>=Random.Float(20)*factor;Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(enemyHit,com.shatteredpixel.shatteredpixeldungeon.actors.Char.hit(enemy,ally,1,false));}finally{Random.popGenerator();}}
  }
 }
 @Test public void talentDetailsRespectExplicitAlternatePreviewEvenOnACleric(){
  hero.heroClass=HeroClass.CLERIC;String regular=MechanicsDetails.talent(Talent.SHIELD_OF_LIGHT,1,"",false),alternate=MechanicsDetails.talent(Talent.SHIELD_OF_LIGHT,1,"",true);assertTrue(regular.contains("block roll from 2 to 4"));assertTrue(alternate.contains("33.333%"));assertFalse(alternate.contains("block roll from 2 to 4"));assertTrue(MechanicsDetails.talent(Talent.BLESS,2,"",true).contains("1.05"));assertTrue(MechanicsDetails.talent(Talent.BLESS,2,"",false).contains("15 Barrier points"));
 }
 @Test public void heartyMealExplanationMatchesActualThresholdAndHealing()throws Exception{
  hero.sprite=quietSprite();
  for(int rank=1;rank<=2;rank++){hero.talents.get(0).put(Talent.HEARTY_MEAL,rank);hero.HT=100;hero.HP=33;Talent.onFoodEaten(hero,1,null);assertEquals(33+2+2*rank,hero.HP);assertTrue(MechanicsDetails.talent(Talent.HEARTY_MEAL,rank,"").contains((2+2*rank)+" HP of healing"));hero.HP=34;Talent.onFoodEaten(hero,1,null);assertEquals(34,hero.HP);}
 }
 @Test public void ironStomachExplanationMatchesRealDamageProtection(){
  Buff.affect(hero,Talent.WarriorFoodImmunity.class);hero.damageInterrupt=false;hero.HT=hero.HP=100;
  for(int rank=1;rank<=2;rank++){hero.talents.get(1).put(Talent.IRON_STOMACH,rank);hero.HP=100;hero.damage(20,new Object());assertEquals(rank==1?95:100,hero.HP);assertTrue(MechanicsDetails.talent(Talent.IRON_STOMACH,rank,"").contains(rank==1?"divided by four":"set to zero"));}
 }
 @Test public void holdFastDescriptionMatchesArmorRollAndActualDecayWithoutQuerySideEffects(){
  HoldFast hold=Buff.affect(hero,HoldFast.class);hold.pos=hero.pos;
  for(int rank=1;rank<=3;rank++){hero.talents.get(2).put(Talent.HOLD_FAST,rank);String text=MechanicsDetails.talent(Talent.HOLD_FAST,rank,"");assertTrue(text.contains(rank+"–"+(2*rank)+" damage per hit"));assertTrue(text.contains(rank==1?"run at half their normal rate":rank==2?"run at a quarter of their normal rate":"are paused"));
   for(int seed=0;seed<30;seed++){Random.pushGenerator(seed);int expected=Random.NormalIntRange(rank,2*rank);Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(expected,hold.armorBonus());}finally{Random.popGenerator();}}
  }
  hero.heroClass=HeroClass.ROGUE;hero.belongings.armor=null;assertEquals(0,hold.armorBonus());hold.pos=hero.pos+1;MechanicsDetails.talent(Talent.HOLD_FAST,3,"");assertSame(hold,hero.buff(HoldFast.class));
 }
 @Test public void intuitionDescriptionsMatchRealIdentificationFactorsAndStacking(){
  Talent[] types={Talent.VETERANS_INTUITION,Talent.ADVENTURERS_INTUITION,Talent.SCHOLARS_INTUITION,Talent.SURVIVALISTS_INTUITION,Talent.THIEFS_INTUITION};
  com.shatteredpixel.shatteredpixeldungeon.items.Item[] items={new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword(),new MailArmor(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife(),new RingOfAccuracy()};
  for(Talent talent:types)for(int rank=1;rank<=2;rank++){for(Talent other:types)hero.talents.get(0).put(other,0);hero.talents.get(0).put(talent,rank);String text=MechanicsDetails.talent(talent,rank,"");assertTrue(text.contains("not damage"));for(int j=0;j<items.length;j++){float expected=1;
    if(talent==Talent.VETERANS_INTUITION)expected=j==0?1+.75f*rank:j==1?1+rank:1;
    if(talent==Talent.ADVENTURERS_INTUITION)expected=j==0?1+1.5f*rank:j==1?1+.75f*rank:1;
    if(talent==Talent.SCHOLARS_INTUITION&&j==2)expected=1+2*rank;
    if(talent==Talent.SURVIVALISTS_INTUITION&&j==3)expected=1+2*rank;
    if(talent==Talent.THIEFS_INTUITION&&j==4)expected=1+rank;
    assertEquals(expected,Talent.itemIDSpeedFactor(hero,items[j]),0);
   }
  }
  hero.talents.get(0).put(Talent.VETERANS_INTUITION,2);hero.talents.get(0).put(Talent.ADVENTURERS_INTUITION,2);assertEquals(10f,Talent.itemIDSpeedFactor(hero,items[0]),0);assertEquals(7.5f,Talent.itemIDSpeedFactor(hero,items[1]),0);
 }
 @Test public void mealEmpowerDescriptionsMatchActualBuffOffersAndReplacementRules(){
  for(int rank=1;rank<=2;rank++){hero.talents.get(0).put(Talent.EMPOWERING_MEAL,rank);hero.talents.get(0).put(Talent.STRENGTHENING_MEAL,rank);Talent.onFoodEaten(hero,1,null);WandEmpower wand=hero.buff(WandEmpower.class);PhysicalEmpower physical=hero.buff(PhysicalEmpower.class);assertEquals(1+rank,wand.dmgBoost);assertEquals(3,wand.left);assertEquals(3,physical.dmgBoost);assertEquals(1+rank,physical.left);assertTrue(MechanicsDetails.talent(Talent.EMPOWERING_MEAL,rank,"").contains("_"+(1+rank)+" damage_"));assertTrue(MechanicsDetails.talent(Talent.STRENGTHENING_MEAL,rank,"").contains("next "+(1+rank)));assertTrue(MechanicsDetails.buff(physical,physical.desc()).contains("Adds 3 damage"));}
  PhysicalEmpower physical=hero.buff(PhysicalEmpower.class);physical.set(1,2);assertEquals(3,physical.dmgBoost);assertEquals(3,physical.left);physical.set(5,2);assertEquals(5,physical.dmgBoost);assertEquals(2,physical.left);
  WandEmpower wand=hero.buff(WandEmpower.class);wand.set(1,2);assertEquals(1,wand.dmgBoost);assertEquals(3,wand.left);
 }
 @Test public void physicalEmpowerInspectionPreservesSavedStateAndHiddenWeaponLevel(){
  PhysicalEmpower physical=Buff.affect(hero,PhysicalEmpower.class);physical.set(4,7);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();physical.storeInBundle(before);String first=MechanicsDetails.buff(physical,physical.desc());assertTrue(first.contains("next 7 hero physical damage rolls"));armor.levelKnown=false;armor.level(9);assertEquals(first,MechanicsDetails.buff(physical,physical.desc()));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();physical.storeInBundle(after);assertEquals(before.toString(),after.toString());
 }
 @Test public void focusedMealAlternateDamageUsesLiveHeroLevelAndActualOffer(){
  for(int rank=1;rank<=2;rank++)for(int level:new int[]{1,7,20}){PhysicalEmpower old=hero.buff(PhysicalEmpower.class);if(old!=null)old.detach();hero.lvl=level;hero.talents.get(0).put(Talent.FOCUSED_MEAL,rank);Talent.onFoodEaten(hero,1,null);int amount=Math.round(level/(4f-rank));PhysicalEmpower buff=hero.buff(PhysicalEmpower.class);assertEquals(amount,buff.dmgBoost);assertEquals(amount>0?1:0,buff.left);assertTrue(MechanicsDetails.talent(Talent.FOCUSED_MEAL,rank,"").contains("+"+amount+" damage"));}
  hero.heroClass=HeroClass.DUELIST;assertTrue(MechanicsDetails.talent(Talent.FOCUSED_MEAL,1,"",false).contains("0.667 weapon-ability charges"));assertTrue(MechanicsDetails.talent(Talent.FOCUSED_MEAL,1,"",true).contains("Alternate next-hit damage"));
 }
 @Test public void enlighteningMealTomeOfferMatchesActualFractionalChargeAndCap()throws Exception{
  hero.heroClass=HeroClass.CLERIC;java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge"),partial=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("partialCharge");charge.setAccessible(true);partial.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome();hero.belongings.artifact=tome;
  for(int rank=1;rank<=2;rank++){charge.setInt(tome,0);partial.setFloat(tome,0);hero.talents.get(0).put(Talent.ENLIGHTENING_MEAL,rank);Talent.onFoodEaten(hero,1,null);assertEquals((rank+1)/3f,charge.getInt(tome)+partial.getFloat(tome),.00001f);assertTrue(MechanicsDetails.talent(Talent.ENLIGHTENING_MEAL,rank,"").contains(rank==2?"_1 Tome charge_":"_0.667 Tome charges_"));assertTrue(MechanicsDetails.talent(Talent.ENLIGHTENING_MEAL,rank,"",true).contains("_"+(1+rank)+" turns_"));}
  tome.directCharge(100);int full=charge.getInt(tome);Talent.onFoodEaten(hero,1,null);assertEquals(full,charge.getInt(tome));assertEquals(0,partial.getFloat(tome),0);
 }
 @Test public void endureExplanationMatchesActualProtectionAndPerHitStorageTruncation(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure();
  for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.SHRUG_IT_OFF,rank);com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker tracker=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker();tracker.target=hero;float factor=.5f*(float)Math.pow(.8f,rank);assertEquals(20*factor,tracker.adjustDamageTaken(20),.00001f);assertEquals(10,tracker.damageBonus);tracker.adjustDamageTaken(3);assertEquals(11,tracker.damageBonus);assertTrue(AbilityExplanation.effects(ability,hero).contains("reduced by _"+ObfuscationExplanation.number(100f*(1f-factor))+"%_"));assertTrue(MechanicsDetails.buff(tracker,tracker.desc()).contains("Stored retaliation: 11 damage points"));}
 }
 public static class EndureDescriptionSprite extends QuietMovementSprite {
  @Override public com.watabou.noosa.particles.Emitter centerEmitter(){return new com.watabou.noosa.particles.Emitter();}
 }
 @Test public void endureBonusExplanationPreservesSequentialRoundingAndUnseenEnemyPrivacy()throws Exception{
  descriptionSightArena();java.util.Arrays.fill(Dungeon.level.heroFOV,false);
  java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);
  hero.sprite=(EndureDescriptionSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,EndureDescriptionSprite.class);
  hero.talents.get(3).put(Talent.SUSTAINED_RETRIBUTION,1);hero.talents.get(3).put(Talent.EVEN_THE_ODDS,3);
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker tracker=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker.class);
  tracker.adjustDamageTaken(10);assertEquals(5,tracker.damageBonus);String before=MechanicsDetails.buff(tracker,tracker.desc());
  java.util.ArrayList<PlainStatusTarget> neighbors=new java.util.ArrayList<>();
  try{
   for(int offset:new int[]{1,-1,33,3,-33}){PlainStatusTarget other=new PlainStatusTarget();other.pos=hero.pos+offset;other.alignment=offset==-33?com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY:com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;neighbors.add(other);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(other);}
   assertEquals("Inspection must not count unseen nearby enemies",before,MechanicsDetails.buff(tracker,tracker.desc()));
   tracker.endEnduring();
   // Stored5 becomes5 after the15% bonus, then7 after three nearby enemies'45%, then3 per hit.
   // Combining the factors before rounding would incorrectly give4 per hit.
   assertEquals(3,tracker.damageBonus);assertEquals(2,tracker.hitsLeft);assertFalse(tracker.enduring);
   assertTrue(MechanicsDetails.buff(tracker,tracker.desc()).contains("Extra damage per hit: _3_"));
   assertEquals(13,tracker.damageFactor(10),0);assertEquals(1,tracker.hitsLeft);assertEquals(13,tracker.damageFactor(10),0);assertNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker.class));
  }finally{for(PlainStatusTarget other:neighbors)com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(other);}
 }
 @Test public void endureInspectorPreservesReadyDamageRollsAndSavedState(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker tracker=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker.class);tracker.enduring=false;tracker.damageBonus=7;tracker.hitsLeft=2;com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();tracker.storeInBundle(before);for(int i=0;i<30;i++){String text=MechanicsDetails.buff(tracker,tracker.desc());assertTrue(text.contains("Extra damage per hit: _7_"));assertTrue(text.contains("Hits remaining: _2_"));}com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();tracker.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(17,tracker.damageFactor(10),0);assertEquals(1,tracker.hitsLeft);assertEquals(17,tracker.damageFactor(10),0);assertNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker.class));
 }
 @Test public void readyEndureUsesStoredHitsAfterTalentChangesAndHandlesOneHit(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker tracker=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker.class);
  tracker.enduring=false;tracker.damageBonus=7;tracker.hitsLeft=2;
  String ready=MechanicsDetails.buff(tracker,tracker.desc());
  for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.SUSTAINED_RETRIBUTION,rank);assertEquals(ready,MechanicsDetails.buff(tracker,tracker.desc()));}
  assertEquals(17,tracker.damageFactor(10),0);assertEquals(1,tracker.hitsLeft);
  assertTrue(tracker.desc().contains("Hits remaining: _1_"));
  String single=MechanicsDetails.buff(tracker,tracker.desc());assertTrue(single.contains("Hits remaining: _1_"));assertFalse(single.contains("1 hits"));assertFalse(single.contains("Bonus calculation"));
  hero.talents.get(3).put(Talent.SUSTAINED_RETRIBUTION,0);
  assertTrue(AbilityExplanation.effects(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure(),hero).contains("next _1 hit_"));
  assertEquals(17,tracker.damageFactor(10),0);
 }
 @Test public void shockwavePreviewUsesKnownStrengthAndTalentUnitsWithoutRevealingUnknownRings(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave();hero.STR=14;hero.talents.get(3).put(Talent.SHOCK_FORCE,2);hero.talents.get(3).put(Talent.STRIKING_WAVE,4);hero.talents.get(3).put(Talent.EXPANDING_WAVE,3);String text=AbilityExplanation.effects(ability,hero);assertTrue(text.contains("_8 tiles_ in a _105° cone_"));assertTrue(text.contains("Base roll: 9–18"));assertTrue(text.contains("_13–25 damage_"));assertTrue(text.contains("_100% chance_"));assertTrue(text.contains("_50% chance_"));RingOfMight ring=new RingOfMight();ring.levelKnown=ring.cursedKnown=true;ring.anonymize();hero.belongings.ring=ring;ring.activate(hero);ring.level(2);assertTrue(AbilityExplanation.effects(ability,hero).contains("strength "+hero.STR()));ring.levelKnown=false;String hidden=AbilityExplanation.effects(ability,hero);ring.level(9);assertEquals(hidden,AbilityExplanation.effects(ability,hero));assertTrue(hidden.contains("Identify equipped rings"));
 }
 @Test public void leapExplanationNeverRollsOrExposesHiddenArmor(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap();hero.talents.get(3).put(Talent.BODY_SLAM,2);hero.talents.get(3).put(Talent.IMPACT_WAVE,3);armor.levelKnown=false;String text=AbilityExplanation.effects(ability,hero);assertTrue(text.contains("_2–8 base damage_"));assertTrue(text.contains("_75% chance_"));armor.level(8);assertEquals(text,AbilityExplanation.effects(ability,hero));Random.pushGenerator(1834);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(1834);try{for(int i=0;i<50;i++)AbilityExplanation.details(ability,hero);assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
 }
 @Test public void shadowCloneDescriptionMatchesActualCreationHealthAndScores(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone();
  for(int rank=0;rank<=4;rank++)for(int level:new int[]{1,12,25}){hero.lvl=level;hero.talents.get(3).put(Talent.PERFECT_COPY,rank);com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone.ShadowAlly clone=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone.ShadowAlly(level);String text=AbilityExplanation.effects(ability,hero);assertTrue(text.contains("_"+clone.HT+" HP_"));assertEquals(clone.HT,clone.HP);assertTrue(text.contains("_"+clone.attackSkill(hero)+" accuracy_"));}
 }
 @Test public void smokeBombDescriptionMatchesActualLogHealthAndArmorRoll(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb();
  for(int rank=1;rank<=4;rank++){hero.talents.get(3).put(Talent.BODY_REPLACEMENT,rank);com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb.NinjaLog log=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb.NinjaLog();assertTrue(AbilityExplanation.effects(ability,hero).contains("log with _"+log.HT+" HP_"));for(int seed=0;seed<20;seed++){Random.pushGenerator(seed);int expected=Random.NormalIntRange(0,Barkskin.currentLevel(log))+Random.NormalIntRange(rank,3*rank);Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(expected,log.drRoll());}finally{Random.popGenerator();}}}
 }
 @Test public void deathMarkDescriptionMatchesActualDamageStageWithoutReadingEnemyStoredHp(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark();hero.talents.get(3).put(Talent.DEATHLY_DURABILITY,3);PlainStatusTarget target=new PlainStatusTarget();target.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark.DeathMarkTracker.class);target.damage(20,new Object());assertEquals(75,target.HP);assertTrue(target.deathMarked);String text=AbilityExplanation.effects(ability,hero);assertTrue(text.contains("_25% more incoming damage_"));assertTrue(text.contains("37.5%"));target.HP=62;assertEquals(text,AbilityExplanation.effects(ability,hero));
 }
 @Test public void rogueAbilityInspectionDoesNotConsumeHeroAttackBenefitsOrHiddenEquipment(){
  Talent.LiquidAgilACCTracker agility=Buff.affect(hero,Talent.LiquidAgilACCTracker.class);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();hero.belongings.weapon=sword;sword.levelKnown=false;armor.levelKnown=false;com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone();String text=AbilityExplanation.effects(ability,hero);sword.level(12);armor.level(9);assertEquals(text,AbilityExplanation.effects(ability,hero));Random.pushGenerator(728);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(728);try{for(int i=0;i<50;i++)AbilityExplanation.effects(ability,hero);assertEquals(expected,Random.Int());}finally{Random.popGenerator();}assertSame(agility,hero.buff(Talent.LiquidAgilACCTracker.class));
 }
 @Test public void naturesPowerDescriptionMatchesActualMovementAndBowRates()throws Exception{
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow();float movement=hero.speed(),delay=bow.delayFactor(hero);com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower.naturesPowerTracker power=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower.naturesPowerTracker.class);
  for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.GROWING_POWER,rank);assertEquals(movement*(2+.25f*rank),hero.speed(),.00001f);assertEquals(delay/(1+(8+rank)/24f),bow.delayFactor(hero),.00001f);String text=MechanicsDetails.buff(power,power.desc());assertTrue(text.contains("_"+ObfuscationExplanation.number(2+.25f*rank)+" × your usual speed_"));assertTrue(text.contains("= "+ObfuscationExplanation.number((8+rank)/24f)));}
 }
 @Test public void naturesPowerExtensionDescriptionMatchesRealLimitedExtensionAndInspection(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower.naturesPowerTracker power=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower.naturesPowerTracker.class);float initial=power.cooldown();power.extend(0);assertEquals(2,power.extensionsLeft);power.extend(3);assertEquals(initial+3,power.cooldown(),0);assertEquals(1,power.extensionsLeft);for(int i=0;i<20;i++)assertTrue(MechanicsDetails.buff(power,power.desc()).contains("opportunities: 1"));assertEquals(initial+3,power.cooldown(),0);power.extend(3);power.extend(3);assertEquals(initial+6,power.cooldown(),0);assertEquals(0,power.extensionsLeft);
 }
 @Test public void hawkDescriptionMatchesActualCreationStatsAndDoesNotSpendGuaranteedDodges(){
  hero.talents.get(3).put(Talent.EAGLE_EYE,4);hero.talents.get(3).put(Talent.SWIFT_SPIRIT,2);com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk.HawkAlly hawk=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk.HawkAlly();String text=AbilityExplanation.effects(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk(),hero);assertEquals(10,hawk.HT);assertEquals(60,hawk.attackSkill(hero));assertEquals(8,hawk.viewDistance);assertTrue(text.contains("_8 tiles_"));assertTrue(text.contains("_10 tiles_"));assertTrue(text.contains("_3 × normal walking speed_"));for(int i=0;i<50;i++)AbilityExplanation.effects(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk(),hero);for(int i=0;i<4;i++)assertEquals(com.shatteredpixel.shatteredpixeldungeon.actors.Char.INFINITE_EVASION,hawk.defenseSkill(new PlainStatusTarget()));assertNotEquals(com.shatteredpixel.shatteredpixeldungeon.actors.Char.INFINITE_EVASION,hawk.defenseSkill(new PlainStatusTarget()));
 }
 @Test public void wildMagicDescriptionMatchesActualFractionalPaymentAndTimeCheck()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic();java.lang.reflect.Method after=ability.getClass().getDeclaredMethod("afterZap",com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.class,java.util.ArrayList.class,Hero.class,int.class);after.setAccessible(true);
  for(int rank=0;rank<=4;rank++)for(int seed=0;seed<8;seed++){hero.talents.get(3).put(Talent.CONSERVED_MAGIC,rank);com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.curCharges=2;wand.partialCharge=.2f;float start=hero.cooldown(),cost=.5f*(float)Math.pow(.67f,rank);Random.pushGenerator(seed);boolean spend=Random.Int(4)>=rank;Random.popGenerator();Random.pushGenerator(seed);try{after.invoke(ability,wand,new java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand>(),hero,hero.pos);}finally{Random.popGenerator();}assertEquals(2.2f-cost,wand.curCharges+wand.partialCharge,.00001f);assertEquals(start+(spend?1:0),hero.cooldown(),.00001f);assertTrue(AbilityExplanation.effects(ability,hero).contains("_"+ObfuscationExplanation.number(cost)+" wand charges_"));}
 }
 @Test public void warpBeaconDescriptionSeparatesPlacementFromReturnWithoutReadingOccupants(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon();hero.talents.get(3).put(Talent.REMOTE_BEACON,3);hero.talents.get(3).put(Talent.LONGRANGE_WARP,2);assertTrue(AbilityExplanation.summary(ability,hero).contains("spends no armor charge"));String text=AbilityExplanation.effects(ability,hero);assertTrue(text.contains("_12 tiles_"));assertTrue(text.contains("_"+ObfuscationExplanation.number(100*(1.833f-.333f*2))+"%_"));float time=hero.cooldown();assertEquals(text,AbilityExplanation.effects(ability,hero));assertEquals(time,hero.cooldown(),0);
 }
 @Test public void elementalBlastDescriptionsUseActualTypeFactorsAndDoNotDependOnHiddenLevels()throws Exception{
  java.lang.reflect.Field factors=com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast.class.getDeclaredField("damageFactors");factors.setAccessible(true);java.util.Map<Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand>,Float> actual=(java.util.Map<Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand>,Float>)factors.get(null);assertEquals(13,actual.size());
  for(java.util.Map.Entry<Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand>,Float> entry:actual.entrySet()){com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=entry.getKey().getConstructor().newInstance();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=describedStaff(wand,0);hero.belongings.weapon=staff;staff.levelKnown=false;wand.levelKnown=false;
   for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.ELEMENTAL_POWER,rank);float power=1+.25f*rank;String text=BlastExplanation.details(hero);assertEquals(entry.getValue(),com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast.blastDamageFactor(entry.getKey()),0);assertTrue(text.contains("Current initial range: "+Math.round(15*power*entry.getValue())+"–"+Math.round(25*power*entry.getValue())));staff.level(10);wand.level(12);assertEquals(text,BlastExplanation.details(hero));}
  }
 }
 @Test public void ordinaryElementalBlastDescriptionUsesLiveTalentsAcrossAllThirteenStaffTypes()throws Exception{
  com.watabou.noosa.Game oldGame=com.watabou.noosa.Game.instance;java.lang.reflect.Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);Object game=unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.watabou.noosa.Game.class);java.lang.reflect.Field scene=com.watabou.noosa.Game.class.getDeclaredField("scene");scene.setAccessible(true);scene.set(game,unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class));com.watabou.noosa.Game.instance=(com.watabou.noosa.Game)game;
  try{for(Class<?> type:com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.WAND.classes){com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)type.getDeclaredConstructor().newInstance();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=describedStaff(wand,0);hero.belongings.weapon=staff;staff.levelKnown=wand.levelKnown=false;
   for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.ELEMENTAL_POWER,rank);hero.talents.get(3).put(Talent.BLAST_RADIUS,rank);float power=1+.25f*rank,factor=com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast.blastDamageFactor(wand.getClass());String text=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast().desc();assertTrue(text,text.contains("_"+(4+rank)+" tiles_"));if(factor>0)assertTrue(text,text.contains("_"+Math.round(15*power*factor)+"–"+Math.round(25*power*factor)+"_"));assertFalse(text.contains("game-time"));assertFalse(text.contains("scheduled units"));staff.level(10);wand.level(12);assertEquals(text,new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast().desc());if(wand instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning)assertTrue(text.contains("_"+ObfuscationExplanation.number(power*Paralysis.DURATION/2)+" turns_"));}
  }}finally{com.watabou.noosa.Game.instance=oldGame;}
 }
 @Test public void elementalBlastWardRepairExplanationMatchesActualSentryCapsAndWardZapRestoration()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding.Ward ward=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding.Ward();float power=1.5f;for(int tier=1;tier<=6;tier++){ward.tier=tier;ward.HT=100;ward.HP=5;ward.totalZaps=4;Random.pushGenerator(667);int expected=tier==1?0:tier==2?Math.round(power):tier==3?Math.round(Random.IntRange(1,2)*power):Math.round((tier==4?9:tier==5?12:16)*power);Random.popGenerator();Random.pushGenerator(667);try{ward.wandHeal(0,power);}finally{Random.popGenerator();}if(tier<=3)assertEquals(Math.max(0,4-expected),ward.totalZaps);else assertEquals(5+expected,ward.HP);}
  hero.belongings.weapon=describedStaff(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding(),0);assertTrue(BlastExplanation.details(hero).contains("available zaps"));assertTrue(BlastExplanation.details(hero).contains("9, 12 or 16"));
 }
 @Test public void ascendedFormExplanationMatchesResetShieldAndPreservesExistingCounters()throws Exception{
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff form=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.class);form.spellCasts=4;form.flashCasts=2;form.setShield(45);form.left=2;form.reset();assertEquals(45,form.shielding());assertEquals(10,form.left);assertEquals(4,form.spellCasts);assertEquals(2,form.flashCasts);String text=MechanicsDetails.buff(form,form.desc());assertTrue(text.contains("Form shield: 45 points"));assertTrue(text.contains("Counted spell casts: 4"));float time=hero.cooldown();form.act();assertEquals(9,form.left);assertEquals(time,hero.cooldown(),0);
 }
 @Test public void powerOfManyDescriptionMatchesActualIncomingDamageFactors(){
  for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.LIFE_LINK,rank);PlainStatusTarget ally=new PlainStatusTarget();ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;Buff.affect(ally,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff.class);ally.damage(20,new Object());assertEquals(85,ally.HP);Buff.affect(ally,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell.LifeLinkSpellBuff.class);ally.HP=100;ally.damage(20,new Object());assertEquals(100-Math.round(20*(.70f-.05f*rank)),ally.HP);assertTrue(AbilityExplanation.effects(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany(),hero).contains("reduced by _"+ObfuscationExplanation.number(100f*(1f-(.70f-.05f*rank)))+"%_ instead"));}
 }
 @Test public void powerOfManyCreationDescriptionMatchesRealLightAllyStats(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany();for(int level:new int[]{1,12,25}){hero.lvl=level;com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.LightAlly ally=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.LightAlly(level);assertEquals(80,ally.HT);String text=AbilityExplanation.effects(ability,hero);assertTrue(text.contains("Base accuracy: "+ally.attackSkill(hero)));}
 }
 @Test public void linkedPowerExplanationDoesNotReportAFalseExpiry(){
  for(boolean lifeLink:new boolean[]{false,true}){
   descriptionSightArena();PlainStatusTarget target=new PlainStatusTarget();target.pos=hero.pos;
   com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff power=Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff.class,0);
   Buff link=lifeLink?Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell.LifeLinkSpellBuff.class,5):Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay.BeamingRayBoost.class,5);
   float timer=power.cooldown();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();power.storeInBundle(before);String text=MechanicsDetails.buff(power,power.desc());
   assertTrue(text.contains("keeps this effect active"));assertFalse(text.contains("_Duration remaining_"));assertTrue(power.desc().contains("normal timer"));
   com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();power.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(timer,power.cooldown(),0);
   assertTrue(power.desc().contains((lifeLink?"30":"25")+"% less incoming damage"));target.damage(20,new Object());assertEquals(lifeLink?86:85,target.HP);
   power.act();assertSame(power,target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff.class));assertEquals(timer+1,power.cooldown(),0);
   link.detach();assertTrue(MechanicsDetails.buff(power,power.desc()).contains("_Duration remaining_"));power.act();assertNull(target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff.class));
  }
 }
 public static class PowerDescriptionAttacker extends PlainStatusTarget {
  @Override public int damageRoll(){return 20;}
  @Override public int attackSkill(com.shatteredpixel.shatteredpixeldungeon.actors.Char target){return 1000;}
 }
 @Test public void powerDescriptionMatchesRealMarkedAttacksAndProtectionAcrossRanks(){
  for(int rank=0;rank<=4;rank++)for(boolean marked:new boolean[]{false,true}){
   descriptionSightArena();java.util.Arrays.fill(Dungeon.level.heroFOV,false);hero.talents.get(3).put(Talent.BEAMING_RAY,rank);hero.talents.get(3).put(Talent.LIFE_LINK,rank);
   PowerDescriptionAttacker attacker=new PowerDescriptionAttacker();attacker.pos=hero.pos;attacker.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;
   PlainStatusTarget victim=new PlainStatusTarget(),other=new PlainStatusTarget();victim.pos=hero.pos+1;victim.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;
   com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff power=Buff.affect(attacker,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff.class,5);
   com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay.BeamingRayBoost ray=Buff.affect(attacker,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay.BeamingRayBoost.class,5);
   String description=power.desc();ray.object=other.id();assertEquals(description,power.desc());ray.object=marked?victim.id():other.id();assertEquals(description,power.desc());assertTrue(description.contains("_"+(30+5*rank)+"%_ instead"));
   assertTrue(attacker.attack(victim));assertEquals(100-Math.round(20*(marked?(1.3f+.05f*rank):1.25f)),victim.HP);
   if(marked)Buff.affect(attacker,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell.LifeLinkSpellBuff.class,5);
   assertTrue(power.desc().contains((marked?(30+5*rank):25)+"% less incoming damage"));attacker.damage(20,new Object());assertEquals(100-Math.round(20*(marked?(.70f-.05f*rank):.75f)),attacker.HP);
  }
 }
 @Test public void challengeInspectorReadsOnlyOwnTrackedDamageAndDoesNotAdvanceDuel(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge.DuelParticipant duel=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge.DuelParticipant.class);duel.addDamage(17);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();duel.storeInBundle(before);for(int i=0;i<50;i++){String text=MechanicsDetails.buff(duel,duel.desc());assertTrue(text.contains("HP/shield damage suffered: 17 points"));assertEquals(10,duel.remainingDuelUpdates());}com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();duel.storeInBundle(after);assertEquals(before.toString(),after.toString());PlainStatusTarget enemy=new PlainStatusTarget();com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge.DuelParticipant other=Buff.affect(enemy,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge.DuelParticipant.class);other.addDamage(123);assertFalse(MechanicsDetails.buff(other,other.desc()).contains("123"));
 }
 @Test public void feintExplanationMatchesActualDefenseTriggerDurations()throws Exception{
  hero.sprite=quietSprite();for(int rank=1;rank<=4;rank++){Buff.detach(hero,Haste.class);hero.talents.get(3).put(Talent.FEIGNED_RETREAT,rank);hero.talents.get(3).put(Talent.EXPOSE_WEAKNESS,rank);hero.talents.get(3).put(Talent.COUNTER_ABILITY,rank);com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint.AfterImage image=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint.AfterImage();PlainStatusTarget enemy=new PlainStatusTarget();enemy.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;String text=AbilityExplanation.effects(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint(),hero);assertNull(hero.buff(Haste.class));assertEquals(0,image.defenseSkill(enemy));assertEquals(2f*rank,hero.buff(Haste.class).cooldown(),0);assertEquals(2f*rank,enemy.buff(Vulnerable.class).cooldown(),0);assertEquals(2f*rank,enemy.buff(Weakness.class).cooldown(),0);assertTrue(text.contains("_"+(2*rank)+" turns of Haste_"));assertTrue(text.contains("_"+ObfuscationExplanation.number(.375f*rank)+" weapon-ability charges_"));}
 }
 @Test public void feintCounterRefundExplanationMatchesActualWeaponAbilityCompletion()throws Exception{
  java.lang.reflect.Method used=com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.class.getDeclaredMethod("afterAbilityUsed",Hero.class);used.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.Charger charger=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.Charger.class);
  for(int rank=1;rank<=4;rank++){charger.charges=0;charger.partialCharge=0;hero.talents.get(3).put(Talent.COUNTER_ABILITY,rank);Buff.affect(hero,Talent.CounterAbilityTacker.class);used.invoke(weapon,hero);assertEquals(.375f*rank,charger.charges+charger.partialCharge,0);assertNull(hero.buff(Talent.CounterAbilityTacker.class));}
 }
 @Test public void elementalStrikeUnknownEquipmentNeverDisclosesHiddenCurseOrEnchantment(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();hero.belongings.weapon=weapon;weapon.levelKnown=weapon.cursedKnown=false;String hidden=StrikeExplanation.details(hero);weapon.enchantment=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing();weapon.level(8);assertEquals(hidden,StrikeExplanation.details(hero));weapon.enchantment=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Sacrificial();weapon.cursed=true;assertEquals(hidden,StrikeExplanation.details(hero));weapon.cursedKnown=true;assertTrue(StrikeExplanation.details(hero).contains("Makes you and targets bleed"));
 }
 @Test public void elementalStrikeExplainsEveryActualBuiltInEffectWithoutInspectingHiddenLevels()throws Exception{
  java.lang.reflect.Field map=com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike.class.getDeclaredField("effectTypes");map.setAccessible(true);java.util.Map<Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment>,Integer> types=(java.util.Map<Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment>,Integer>)map.get(null);assertEquals(28,types.size());com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();hero.belongings.weapon=weapon;weapon.cursedKnown=true;weapon.levelKnown=false;
  for(Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment> type:types.keySet()){weapon.level(0);weapon.enchantment=type==null?null:type.getConstructor().newInstance();String text=StrikeExplanation.details(hero),plain=StrikeExplanation.playerDescription(hero);assertTrue(text.contains("Current known cone effect"));assertFalse(text.contains("has not been calculated"));assertFalse(plain.contains("!!!NO TEXT FOUND!!!"));weapon.level(12);assertEquals(text,StrikeExplanation.details(hero));assertEquals("Normal cone text cannot use unknown weapon upgrades",plain,StrikeExplanation.playerDescription(hero));}
 }
 @Test public void normalElementalStrikeUsesLiveTalentsAndMatchesItsActualPlainConeDamage()throws Exception{
  descriptionSightArena();hero.sprite=quietSprite();PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();target.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;target.pos=hero.pos+1;com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(target);
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike();java.lang.reflect.Field field=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");field.setAccessible(true);Object unsafe=field.get(null);com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE cone=(com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE.class);cone.cells=new java.util.HashSet<>();cone.cells.add(target.pos);
  java.lang.reflect.Method apply=ability.getClass().getDeclaredMethod("perCharEffect",com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE.class,Hero.class,com.shatteredpixel.shatteredpixeldungeon.actors.Char.class,com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.class);apply.setAccessible(true);
  try{for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.ELEMENTAL_REACH,rank);hero.talents.get(3).put(Talent.STRIKING_FORCE,rank);float power=1+.3f*rank;String text=StrikeExplanation.playerDescription(hero);assertTrue(text,text.contains("_"+(4+rank)+" tiles_"));assertTrue(text,text.contains("_"+(65+10*rank)+" degrees_"));assertTrue(text,text.contains("_"+Math.round(6*power)+"–"+Math.round(12*power)+" base damage_"));target.HP=target.HT=1000;Random.pushGenerator(17419);int expected;try{expected=Math.round(power*Hero.heroDamageIntRange(6,12));}finally{Random.popGenerator();}Random.pushGenerator(17419);try{apply.invoke(ability,cone,hero,null,null);}finally{Random.popGenerator();}assertEquals(expected,1000-target.HP);}
  }finally{com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(target);}
 }
 @Test public void elementalStrikeBarrierAndDirectedPowerDescriptionsMatchActualPreAttackHandler()throws Exception{
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike();java.lang.reflect.Field field=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");field.setAccessible(true);Object unsafe=field.get(null);com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE cone=(com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE.class);cone.cells=new java.util.HashSet<>();java.util.List<PlainStatusTarget> enemies=new java.util.ArrayList<>();
  try{for(int i=0;i<3;i++){PlainStatusTarget enemy=new PlainStatusTarget();enemy.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;enemy.pos=1000000+i;cone.cells.add(enemy.pos);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(enemy);enemies.add(enemy);}java.lang.reflect.Method pre=ability.getClass().getDeclaredMethod("preAttackEffect",com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE.class,Hero.class,com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.class);pre.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();weapon.cursedKnown=true;weapon.enchantment=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking();hero.belongings.weapon=weapon;
   for(int rank=0;rank<=4;rank++){Buff.detach(hero,Barrier.class);hero.talents.get(3).put(Talent.STRIKING_FORCE,rank);hero.talents.get(3).put(Talent.DIRECTED_POWER,rank);pre.invoke(ability,cone,hero,weapon.enchantment);float power=1+.3f*rank;assertEquals(Math.round(6*3*power),hero.buff(Barrier.class).shielding());if(rank>0)assertEquals(.3f*3*rank,hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike.DirectedPowerTracker.class).enchBoost,.00001f);assertTrue(StrikeExplanation.details(hero).contains("_"+ObfuscationExplanation.number(6*power)+" shield points per enemy_"));assertTrue(StrikeExplanation.playerDescription(hero).contains("_"+ObfuscationExplanation.number(6*power)+" base shielding per enemy_"));}
  }finally{for(PlainStatusTarget enemy:enemies)com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(enemy);}
 }
 @Test public void elementalStrikeBleedingDescriptionsMatchActualInitialRandomizationAndFirstDamage()throws Exception{
  hero.sprite=quietSprite();PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();target.sprite.visible=false;target.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;target.pos=1000000;com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(target);
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike();java.lang.reflect.Field f=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");f.setAccessible(true);Object unsafe=f.get(null);com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE cone=(com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE.class);cone.cells=new java.util.HashSet<>();cone.cells.add(target.pos);
  java.lang.reflect.Method apply=ability.getClass().getDeclaredMethod("perCharEffect",com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE.class,Hero.class,com.shatteredpixel.shatteredpixeldungeon.actors.Char.class,com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.class);apply.setAccessible(true);
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();weapon.cursedKnown=true;hero.belongings.weapon=weapon;
  try{for(int rank=0;rank<=4;rank++)for(int curse=0;curse<2;curse++)for(int seed=0;seed<20;seed++){
   hero.talents.get(3).put(Talent.STRIKING_FORCE,rank);float power=1+.3f*rank;float base=(curse==0?10:12)*power;weapon.enchantment=curse==0?new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vorpal():new com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Sacrificial();Buff.detach(target,Bleeding.class);target.HP=target.HT=1000;
   Random.pushGenerator(seed);float expected;try{expected=Random.NormalFloat(base/2,base);}finally{Random.popGenerator();}
   Random.pushGenerator(seed);try{apply.invoke(ability,cone,hero,null,weapon.enchantment);}finally{Random.popGenerator();}
   Bleeding bleed=target.buff(Bleeding.class);assertNotNull(bleed);assertEquals(expected,bleed.level(),0);bleed.act();assertEquals(Math.round(expected),1000-target.HP);
   String detail=StrikeExplanation.details(hero);assertTrue(detail,detail.contains("_"+Math.round((curse==0?5:6)*power)+"–"+Math.round(base)+""));assertFalse(detail,detail.contains("game-time units"));assertFalse(detail,detail.contains("scheduled units"));
  }}finally{Buff.detach(target,Bleeding.class);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(target);}
 }
 @Test public void elementalStrikeCrystalDescriptionMatchesRealDamageAndDurabilityRepair()throws Exception{
  descriptionSightArena();hero.sprite=quietSprite();PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();target.sprite.visible=false;target.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;target.pos=hero.pos+1;com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(target);
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike();java.lang.reflect.Field f=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");f.setAccessible(true);Object unsafe=f.get(null);com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE cone=(com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE.class);cone.cells=new java.util.HashSet<>();cone.cells.add(target.pos);
  java.lang.reflect.Method apply=ability.getClass().getDeclaredMethod("perCharEffect",com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE.class,Hero.class,com.shatteredpixel.shatteredpixeldungeon.actors.Char.class,com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.class);apply.setAccessible(true);
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.cursedKnown=true;com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal crystal=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal();sword.enchantment=crystal;hero.belongings.weapon=sword;
  try{for(int rank=0;rank<=4;rank++)for(int seed=0;seed<10;seed++){
   hero.talents.get(3).put(Talent.STRIKING_FORCE,rank);float power=1+.3f*rank;target.HP=target.HT=1000;crystal.setDurability(seed==9?98:20);
   Random.pushGenerator(seed);int expected;try{expected=Math.round(power*Hero.heroDamageIntRange(10,20));}finally{Random.popGenerator();}
   Random.pushGenerator(seed);try{apply.invoke(ability,cone,hero,null,crystal);}finally{Random.popGenerator();}
   assertEquals(expected,1000-target.HP);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();crystal.storeInBundle(state);assertEquals(Math.min(100,(seed==9?98:20)+4*power),state.getFloat("durability"),0);
   String detail=StrikeExplanation.details(hero);assertTrue(detail,detail.contains("_"+Math.round(10*power)+"–"+Math.round(20*power)+" base magic damage_"));assertTrue(detail,detail.contains("_"+ObfuscationExplanation.number(4*power)+"% of full weapon durability_"));
  }}finally{com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(target);}
 }
 public static class RatDamageProbe extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob {public RatDamageProbe(){HP=HT=100;}@Override public int damageRoll(){return 20;}@Override public int attackSkill(com.shatteredpixel.shatteredpixeldungeon.actors.Char target){return 17;}@Override public int drRoll(){return 3;}@Override public float attackDelay(){return 1.25f;}}
 @Test public void ratmogrifyExplanationMatchesActualCopiedCombatAndIntegerDamageReduction()throws Exception{
  java.lang.reflect.Field timer=com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify.TransmogRat.class.getDeclaredField("timeLeft");timer.setAccessible(true);java.lang.reflect.Method spend=com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify.TransmogRat.class.getDeclaredMethod("spend",float.class);spend.setAccessible(true);
  for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.RATSISTANCE,rank);RatDamageProbe original=new RatDamageProbe();original.HP=73;com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify.TransmogRat rat=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify.TransmogRat();rat.setup(original);assertEquals(73,rat.HP);assertEquals(100,rat.HT);assertEquals(17,rat.attackSkill(hero));assertEquals(3,rat.drRoll());assertEquals(1.25f,rat.attackDelay(),0);assertEquals((int)(20*Math.pow(.9f,rank)),rat.damageRoll());assertEquals(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify.TransmogRat.TRANSFORM_TIME,timer.getFloat(rat),0);spend.invoke(rat,2.5f);assertEquals(3.5f,timer.getFloat(rat),0);String text=AbilityExplanation.effects(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify(),hero);assertTrue(text.contains("spent 6 turns acting"));assertTrue(text.contains("about "+ObfuscationExplanation.number(100*(float)Math.pow(.9f,rank))+"% of their original damage"));}
 }
 @Test public void trinityEffectChargeFactorsPreserveActualTypeSpecificCosts(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity();hero.armorAbility=ability;Class<?>[] types={com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Grim.class,com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing.class,com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Affection.class,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast.class,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth.class,com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost.class,RingOfAccuracy.class,com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose.class,com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass.class,com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains.class,com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty.class};float[] factors={2,1,2,2,2,1,1,2,1.4f,1.4f,1};
  for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.HEROIC_ENERGY,rank);float base=ability.chargeUse(hero);for(int j=0;j<types.length;j++){assertEquals(factors[j],com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity.trinityChargeFactor(types[j]),0);assertEquals(factors[j]*base,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity.trinityChargeUsePerEffect(types[j]),0);}}
 }
 @Test public void trinityDescriptionMatchesActualFormLevelsAndNeverUpdatesStoredOrHiddenItems(){
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity();for(int rank=0;rank<=4;rank++){hero.talents.get(3).put(Talent.BODY_FORM,rank);hero.talents.get(3).put(Talent.MIND_FORM,rank);hero.talents.get(3).put(Talent.SPIRIT_FORM,rank);String text=AbilityExplanation.effects(ability,hero);assertTrue(text.contains("_"+com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm.duration()+" turns_"));assertTrue(text,text.contains("_"+ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm.duration()*1.5f)+"% durability_"));assertTrue(text.contains("_+"+com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MindForm.itemLevel()+" wand or thrown-weapon effect_"));assertTrue(text.contains("_+"+com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.ringLevel()+" ring_"));assertTrue(text.contains("_+"+com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.artifactLevel()+" artifact_"));}
  RingOfArcana ring=ring(8);ring.levelKnown=false;String text=AbilityExplanation.effects(ability,hero);ring.level(12);assertEquals(text,AbilityExplanation.effects(ability,hero));Random.pushGenerator(6618);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(6618);try{for(int i=0;i<50;i++)AbilityExplanation.effects(ability,hero);assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
 }
 @Test public void allSelectableArmorAbilitiesHaveReadOnlyPrimaryEffectExplanations()throws Exception{
  hero.belongings.weapon=describedStaff(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile(),3);int count=0;Random.pushGenerator(13133);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(13133);try{for(HeroClass type:HeroClass.values()){hero.heroClass=type;for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility ability:type.armorAbilities()){String text=AbilityExplanation.effects(ability,hero);assertFalse(ability.getClass().getSimpleName(),text.isEmpty());assertFalse(text.contains("NaN"));assertFalse(text.contains("Infinity"));count++;}}assertFalse(AbilityExplanation.effects(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify(),hero).isEmpty());count++;assertEquals(19,count);assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
 }
 @Test public void charmAndTerrorDescriptionsMatchActualRecoveryAmountsAndExceptions(){
  PlainStatusTarget target=new PlainStatusTarget();Charm charm=Buff.affect(target,Charm.class,20);float start=charm.cooldown();String description=MechanicsDetails.buff(charm,charm.desc());assertTrue(description.contains("shortens the effect by 5 turns"));charm.ignoreNextHit=true;charm.recover(hero);assertEquals(start,charm.cooldown(),0);assertFalse(charm.ignoreNextHit);charm.ignoreHeroAllies=true;PlainStatusTarget ally=new PlainStatusTarget();ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;charm.ignoreNextHit=true;charm.recover(ally);assertEquals(start,charm.cooldown(),0);assertTrue(charm.ignoreNextHit);charm.recover(hero);charm.recover(hero);assertEquals(start-5,charm.cooldown(),0);
  Terror terror=Buff.affect(new PlainStatusTarget(),Terror.class,20);float fear=terror.cooldown();terror.ignoreNextHit=true;terror.recover();assertEquals(fear,terror.cooldown(),0);terror.recover();assertEquals(fear-5,terror.cooldown(),0);assertTrue(MechanicsDetails.buff(terror,terror.desc()).contains("regardless of damage amount"));
 }
 public static class DescriptionFleeMob extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob {boolean destroyed,died;@Override public void destroy(){destroyed=true;}@Override public void die(Object source){died=true;}}
 public static class DescriptionFleeSprite extends QuietMovementSprite {boolean erased;@Override public void killAndErase(){erased=true;}}
 @Test public void dreadFleeNeedsBothDistanceAndLostSightAndUsesReducedExperienceInput()throws Exception{
  descriptionSightArena();java.lang.reflect.Field f=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");f.setAccessible(true);Object unsafe=f.get(null);
  for(int distance:new int[]{Dread.FLEE_DISTANCE-1,Dread.FLEE_DISTANCE})for(boolean visible:new boolean[]{false,true}){
   DescriptionFleeMob mob=new DescriptionFleeMob();mob.EXP=5;mob.pos=hero.pos+distance;mob.HP=mob.HT=100;mob.sprite=(DescriptionFleeSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,DescriptionFleeSprite.class);Dungeon.level.mobs.add(mob);Dungeon.level.heroFOV[mob.pos]=visible;float speed=mob.speed();Dread dread=Buff.affect(mob,Dread.class);dread.object=hero.id();assertEquals(speed*2,mob.speed(),0);assertTrue(dread.desc().contains("double speed"));
   try{dread.act();boolean leaves=!visible&&distance>=Dread.FLEE_DISTANCE;assertEquals(leaves,mob.destroyed);assertEquals(leaves,((DescriptionFleeSprite)mob.sprite).erased);assertEquals(!leaves,Dungeon.level.mobs.contains(mob));assertFalse("flee bypasses ordinary death/loot branch",mob.died);assertEquals(leaves?2:5,mob.EXP);if(!leaves){assertEquals(19,dread.remainingFearUpdates());dread.recover();assertEquals(14,dread.remainingFearUpdates());for(int i=0;i<14;i++)dread.act();assertNull(mob.buff(Dread.class));}}
   finally{Dungeon.level.mobs.remove(mob);Buff.detach(mob,Dread.class);}
  }
 }
 @Test public void dreadExplanationMatchesOwnCounterRecoveryAndSerializedState(){
  Dread dread=Buff.affect(new PlainStatusTarget(),Dread.class);int before=dread.remainingFearUpdates();dread.recover();assertEquals(before-5,dread.remainingFearUpdates());String text=MechanicsDetails.buff(dread,dread.desc());assertTrue(text.contains("at least "+Dread.FLEE_DISTANCE+" tiles"));assertTrue(text.contains("Remaining fear updates: "+(before-5)));com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();dread.storeInBundle(saved);for(int i=0;i<20;i++)MechanicsDetails.buff(dread,dread.desc());com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();dread.storeInBundle(after);assertEquals(saved.toString(),after.toString());assertTrue(dread.desc().contains("at least "+Dread.FLEE_DISTANCE+" tiles"));
 }
 @Test public void vertigoDescriptionMatchesSingleBlockedDirectionChoiceWithoutRerolling()throws Exception{
  PlainStatusTarget target=new PlainStatusTarget();target.pos=hero.pos;target.sprite=quietSprite();Buff.affect(target,Vertigo.class,10);java.util.Arrays.fill(Dungeon.level.passable,false);java.util.Arrays.fill(Dungeon.level.avoid,false);String text=MechanicsDetails.buff(target.buff(Vertigo.class),"");assertTrue(text.contains("eight neighboring directions"));assertTrue(text.contains("does not keep rerolling"));
  for(int seed=0;seed<20;seed++){Random.pushGenerator(seed);Random.Int(8);int next=Random.Int();Random.popGenerator();Random.pushGenerator(seed);try{target.move(target.pos+1,true);assertEquals(hero.pos,target.pos);assertEquals(next,Random.Int());}finally{Random.popGenerator();}}
 }
 @Test public void lightExplanationMatchesActualFloorMinimumAndPreservesLargerBaseSight(){
  Dungeon.level.blobs=new java.util.HashMap<>();Dungeon.level.mobs=new java.util.HashSet<>();Dungeon.level.heaps=new com.watabou.utils.SparseArray<>();Dungeon.level.traps=new com.watabou.utils.SparseArray<>();Dungeon.level.plants=new com.watabou.utils.SparseArray<>();Dungeon.level.customTiles=new java.util.ArrayList<>();Dungeon.level.customWalls=new java.util.ArrayList<>();Dungeon.level.setSize(33,33);java.util.Arrays.fill(Dungeon.level.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);Dungeon.level.buildFlagMaps();Dungeon.level.discoverable=new boolean[Dungeon.level.length()];java.util.Arrays.fill(Dungeon.level.discoverable,true);hero.pos=16*33+16;
  for(int base:new int[]{2,8}){Dungeon.level.viewDistance=base;hero.viewDistance=base;Light light=Buff.affect(hero,Light.class,30);assertEquals(Math.max(base,Light.DISTANCE),hero.viewDistance);assertTrue(light.desc().contains("at least "+Light.DISTANCE+" tiles"));assertTrue(MechanicsDetails.buff(light,light.desc()).contains("keeping a larger ordinary range"));light.detach();assertEquals(base,hero.viewDistance);}
 }
 @Test public void lifeLinkDescriptionMatchesActualMultiLinkRoundingAndHungerException(){
  PlainStatusTarget first=new PlainStatusTarget(),second=new PlainStatusTarget(),third=new PlainStatusTarget();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(first);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(second);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(third);
  try{LifeLink a=Buff.append(first,LifeLink.class,20);a.object=second.id();LifeLink b=Buff.append(first,LifeLink.class,20);b.object=third.id();LifeLink reverse=Buff.append(second,LifeLink.class,20);reverse.object=first.id();String text=MechanicsDetails.buff(a,a.desc());assertTrue(text.contains("ceil(incoming damage / (active link count + 1))"));assertTrue(text.contains("combined raw shares can exceed"));com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();a.storeInBundle(before);for(int i=0;i<20;i++)assertEquals(text,MechanicsDetails.buff(a,a.desc()));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();a.storeInBundle(after);assertEquals(before.toString(),after.toString());first.damage(11,new Object());assertEquals(96,first.HP);assertEquals(96,second.HP);assertEquals(96,third.HP);first.damage(6,new Hunger());assertEquals(90,first.HP);assertEquals(96,second.HP);assertEquals(96,third.HP);
  }finally{com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(first);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(second);com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(third);}
 }
 public static class SoulMarkProbe extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob {public SoulMarkProbe(){HP=HT=100;state=HUNTING;}@Override public boolean surprisedBy(com.shatteredpixel.shatteredpixeldungeon.actors.Char enemy,boolean attacking){return false;}}
 @Test public void soulMarkHealingNumberHonorsHealingRuleWithoutChangingRestoration()throws Exception{
  java.lang.reflect.Field field=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");field.setAccessible(true);Object unsafe=field.get(null);
  CursedHealingDisplaySprite sprite=(CursedHealingDisplaySprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,CursedHealingDisplaySprite.class);hero.sprite=sprite;hero.HT=100;hero.talents.get(2).put(Talent.SOUL_EATER,3);
  for(int percent:new int[]{0,25,100,200}){
   com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,percent).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);
   hero.HP=10;hero.apdCustomHealingFraction=0;Hunger appetite=Buff.affect(hero,Hunger.class);com.watabou.utils.Bundle hungerState=new com.watabou.utils.Bundle();appetite.storeInBundle(hungerState);hungerState.put("level",100f);appetite.restoreFromBundle(hungerState);
   SoulMarkProbe target=new SoulMarkProbe();target.HP=20;Buff.affect(target,Barrier.class).setShield(5);Buff.affect(target,SoulMark.class,10);
   float time=hero.cooldown();
   for(int hit=1;hit<=2;hit++){
    int before=hero.HP;target.defenseProc(hero,99);
    assertEquals("Displayed healing at rule"+percent+" hit"+hit,Integer.toString(hero.HP-before),sprite.healing);
    assertEquals(10+(int)Math.floor(hit*10*percent/100f),hero.HP);
    assertEquals(100-25*hit,appetite.hungerPoints(),0);assertEquals(20,target.HP);assertEquals(5,target.shielding());assertEquals(time,hero.cooldown(),0);
   }
  }
 }
 @Test public void soulMarkNormalDescriptionFormatsPercentAndLiveDuration(){
  PlainStatusTarget target=new PlainStatusTarget();SoulMark mark=Buff.affect(target,SoulMark.class,10);
  String normal=mark.desc();assertTrue(normal,normal.contains("40%"));assertFalse(normal,normal.contains("%s"));assertFalse(normal,normal.contains("%%"));assertTrue(normal,normal.contains("Turns remaining: 11"));
  SoulMark longer=Buff.affect(new PlainStatusTarget(),SoulMark.class,13);assertTrue(longer.desc(),longer.desc().contains("Turns remaining: 14"));
 }
 @Test public void soulMarkDescriptionMatchesActualRestorationAndNonHeroScaling()throws Exception{
  hero.sprite=quietSprite();hero.HT=100;hero.talents.get(2).put(Talent.SOUL_EATER,3);Hunger hunger=Buff.affect(hero,Hunger.class);hunger.affectHunger(-100);
  for(int rank=0;rank<=3;rank++){hero.talents.get(2).put(Talent.SOUL_SIPHON,rank);hero.HP=20;SoulMarkProbe target=new SoulMarkProbe();target.HP=20;Buff.affect(target,Barrier.class).setShield(5);SoulMark mark=Buff.affect(target,SoulMark.class,10);target.defenseProc(hero,99);assertEquals(30,hero.HP);hero.HP=20;target.defenseProc(new PlainStatusTarget(),99);int restoration=Math.round(25*.4f*rank/3f);assertEquals(20+(int)Math.ceil(restoration*.4f),hero.HP);String text=MechanicsDetails.buff(mark,mark.desc());assertTrue(text.contains("count at _"+ObfuscationExplanation.number(100f*.4f*rank/3f)+"%_"));assertTrue(text.contains("restore _40%_ of the counted damage as HP, rounded up"));}
 }
 @Test public void warlockMarkChanceExplanationMatchesActualSeededWandProcs()throws Exception{
  hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.WARLOCK;java.lang.reflect.Method proc=com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.class.getDeclaredMethod("wandProc",com.shatteredpixel.shatteredpixeldungeon.actors.Char.class,int.class,int.class);proc.setAccessible(true);
  for(int level:new int[]{0,3,8})for(int charges:new int[]{1,3})for(int seed=0;seed<30;seed++){PlainStatusTarget target=new PlainStatusTarget();double threshold=com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.soulMarkThreshold(level,charges);Random.pushGenerator(seed);boolean expected=Random.Float()>threshold;Random.popGenerator();Random.pushGenerator(seed);try{proc.invoke(null,target,level,charges);}finally{Random.popGenerator();}assertEquals(expected,target.buff(SoulMark.class)!=null);if(expected)assertEquals(SoulMark.DURATION+level,target.buff(SoulMark.class).cooldown(),0);}
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.charge(hero);String text=WandExplanation.details(wand);assertTrue(text.contains("Warlock Soul Mark"));assertTrue(text.contains("15% per qualifying check"));
 }
 public static class PreparedDamageSource extends PlainStatusTarget {int rolls;@Override public int damageRoll(){int[] values={5,11,7};return values[Math.min(rolls++,2)];}}
 public static class BossThresholdTarget extends PlainStatusTarget {public BossThresholdTarget(){properties.add(com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.BOSS);}}
 @Test public void preparationDescriptionMatchesActualBestOfRollsAndTierThresholds(){
  int[] updates={0,3,5,9},expectedRolls={1,1,2,3},expectedDamage={6,6,15,17};for(int i=0;i<updates.length;i++){Preparation prep=new Preparation();prep.target=hero;com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();state.put("turnsInvis",updates[i]);prep.restoreFromBundle(state);Preparation.AttackLevel level=prep.currentAttackLevel();PreparedDamageSource attacker=new PreparedDamageSource();assertEquals(expectedDamage[i],level.damageRoll(attacker));assertEquals(expectedRolls[i],attacker.rolls);String text=PreparedShotExplanation.preparation(prep);assertTrue(text.contains("damage "+expectedRolls[i]+" time(s)"));assertEquals(updates[i],prep.preparedUpdates());if(expectedRolls[i]>1)assertTrue(prep.desc().contains("Roll attack damage "+expectedRolls[i]+" times"));}
 }
 @Test public void preparationExplainsStrictHealthThresholdAndDoesNotRollDuringInspection(){
  Preparation prep=new Preparation();prep.target=hero;com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();state.put("turnsInvis",9);prep.restoreFromBundle(state);PlainStatusTarget normal=new PlainStatusTarget();normal.HT=100;normal.HP=50;assertFalse(prep.currentAttackLevel().canKO(normal));normal.HP=49;assertTrue(prep.currentAttackLevel().canKO(normal));BossThresholdTarget boss=new BossThresholdTarget();boss.HT=100;boss.HP=10;assertFalse(prep.currentAttackLevel().canKO(boss));boss.HP=9;assertTrue(prep.currentAttackLevel().canKO(boss));String text=PreparedShotExplanation.preparation(prep);assertTrue(text.contains("strictly below 50%"));assertTrue(text.contains("threshold: 10%"));Random.pushGenerator(717);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(717);try{for(int i=0;i<50;i++)PreparedShotExplanation.preparation(prep);assertEquals(expected,Random.Int());}finally{Random.popGenerator();}com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();prep.storeInBundle(after);assertEquals(9,after.getInt("turnsInvis"));
 }
 @Test public void sniperMarkKnowledgeIsSaveCompatibleAndDoesNotExposeLegacyUnknownBonus(){
  SnipersMark mark=new SnipersMark();mark.set(0,.5f,true);String known=PreparedShotExplanation.sniper(mark);assertTrue(known.contains("contribution: 50%"));com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();mark.storeInBundle(saved);SnipersMark copy=new SnipersMark();copy.restoreFromBundle(saved);assertTrue(copy.bonusIsKnown());assertEquals(.5f,copy.percentDmgBonus,0);assertEquals(known,PreparedShotExplanation.sniper(copy));com.watabou.utils.Bundle legacy=new com.watabou.utils.Bundle();legacy.put("bonus",.5f);copy.restoreFromBundle(legacy);assertFalse(copy.bonusIsKnown());assertEquals(.5f,copy.percentDmgBonus,0);String hidden=PreparedShotExplanation.sniper(copy);copy.percentDmgBonus=1.25f;assertEquals(hidden,PreparedShotExplanation.sniper(copy));assertFalse(hidden.contains("Known recorded contribution"));
 }
 @Test public void sniperMarkSourceKnowledgeUsesActualHitHandlerAndContributingRingKnowledge()throws Exception{
  hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.SNIPER;hero.talents.get(2).put(Talent.SHARED_UPGRADES,2);
  for(int mode=0;mode<3;mode++){com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife();weapon.level(2);weapon.levelKnown=mode!=0;weapon.cursedKnown=true;hero.belongings.weapon=weapon;if(mode==2){RingOfArcana unknown=ring(1);unknown.levelKnown=false;}
   java.util.Set<com.shatteredpixel.shatteredpixeldungeon.actors.Actor> before=new java.util.HashSet<>(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all());hero.attackProc(new PlainStatusTarget(),5);java.util.List<com.shatteredpixel.shatteredpixeldungeon.actors.Actor> queued=new java.util.ArrayList<>();for(com.shatteredpixel.shatteredpixeldungeon.actors.Actor actor:com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all())if(!before.contains(actor)&&actor.getClass().getName().startsWith(Hero.class.getName()+"$"))queued.add(actor);assertEquals(1,queued.size());java.lang.reflect.Method act=queued.get(0).getClass().getDeclaredMethod("act");act.setAccessible(true);act.invoke(queued.get(0));SnipersMark mark=hero.buff(SnipersMark.class);assertNotNull(mark);assertEquals(mode==1,mark.bonusIsKnown());}
 }
 public static class BorrowedEnchantmentProbe extends com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment {
  int calls;
  @Override public int proc(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon weapon,com.shatteredpixel.shatteredpixeldungeon.actors.Char attacker,com.shatteredpixel.shatteredpixeldungeon.actors.Char defender,int damage){calls++;return damage;}
  @Override public com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite.Glowing glowing(){return null;}
 }
 @Test public void sharedEnchantmentBorrowChancePreservesOwnProcAndMagicImmunity(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow();BorrowedEnchantmentProbe borrowed=new BorrowedEnchantmentProbe();bow.enchantment=borrowed;assertTrue(bow.collect(hero.belongings.backpack));
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife knife=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife();BorrowedEnchantmentProbe own=new BorrowedEnchantmentProbe();knife.enchantment=own;knife.levelKnown=knife.cursedKnown=true;
  for(int rank=0;rank<=3;rank++)for(int seed=0;seed<30;seed++){
   hero.talents.get(2).put(Talent.SHARED_ENCHANTMENT,rank);Random.pushGenerator(seed);boolean eligible=Random.Int(3)<rank;Random.popGenerator();borrowed.calls=own.calls=0;
   Random.pushGenerator(seed);try{knife.proc(hero,new PlainStatusTarget(),5);}finally{Random.popGenerator();}
   assertEquals(eligible?1:0,borrowed.calls);assertEquals(1,own.calls);
  }
  Buff.affect(hero,MagicImmune.class);borrowed.calls=own.calls=0;knife.proc(hero,new PlainStatusTarget(),5);assertEquals(0,borrowed.calls);assertEquals(0,own.calls);
 }
 @Test public void sharedUpgradeDescriptionMatchesActualMarkAtEveryTalentCap()throws Exception{
  hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.SNIPER;
  String normal=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(Talent.class,"shared_upgrades.desc");assertTrue(normal,normal.contains("about 17%"));assertFalse(normal,normal.contains("by 16%"));
  for(int rank=1;rank<=3;rank++)for(int level=0;level<=9;level++){
   Buff.detach(hero,SnipersMark.class);hero.talents.get(2).put(Talent.SHARED_UPGRADES,rank);
   com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife knife=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife();knife.level(level);knife.levelKnown=knife.cursedKnown=true;hero.belongings.weapon=knife;
   java.util.Set<com.shatteredpixel.shatteredpixeldungeon.actors.Actor> before=new java.util.HashSet<>(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all());hero.attackProc(new PlainStatusTarget(),5);
   int callbacks=0;for(com.shatteredpixel.shatteredpixeldungeon.actors.Actor actor:new java.util.ArrayList<>(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all()))if(!before.contains(actor)&&actor.getClass().getName().startsWith(Hero.class.getName()+"$")){java.lang.reflect.Method act=actor.getClass().getDeclaredMethod("act");act.setAccessible(true);act.invoke(actor);callbacks++;}
   assertEquals(1,callbacks);SnipersMark mark=hero.buff(SnipersMark.class);assertNotNull(mark);int counted=Math.min(2*rank,level);assertEquals(counted/6f,mark.percentDmgBonus,0);assertEquals(SnipersMark.DURATION+counted,mark.cooldown(),0);
   String details=TalentExplanation.details(Talent.SHARED_UPGRADES,rank,false);assertTrue(details,details.contains("up to _"+(2*rank)+" upgrades_"));assertTrue(details,details.contains("count ÷ 6"));assertTrue(details,details.contains("about 16.67% damage"));assertFalse(details,details.contains("⅔"));
  }
 }
 @Test public void sniperShotNormalDelayPreviewMatchesActualBowWithoutConsumingReadyBenefits(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow();SnipersMark mark=new SnipersMark();mark.set(0,0,true);RingOfFuror furor=new RingOfFuror();furor.anonymize();furor.levelKnown=furor.cursedKnown=true;hero.belongings.ring=furor;furor.activate(hero);for(int level=0;level<=3;level++){furor.level(level);bow.sniperSpecial=true;bow.augment=com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.SPEED;String text=PreparedShotExplanation.sniper(mark);assertTrue(text.contains("volley delay: "+ObfuscationExplanation.number(bow.delayFactor(hero))));bow.augment=com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.DAMAGE;assertTrue(text.contains("damage-augmented delay: "+ObfuscationExplanation.number(bow.delayFactor(hero))));assertTrue(bow.sniperSpecial);}
 }
 @Test public void greaterHasteInspectionPreservesBudgetAndBothConsumptionPaths(){
  GreaterHaste haste=new GreaterHaste();haste.set(6);String text=BuffExplanation.details(haste);assertTrue(text.contains("Remaining haste units: 6"));assertTrue(text.contains("scheduled update"));for(int i=0;i<20;i++)BuffExplanation.details(haste);assertEquals(6,haste.remainingHasteUnits());haste.spendMove();assertEquals(5,haste.remainingHasteUnits());haste.act();assertEquals(4,haste.remainingHasteUnits());com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();haste.storeInBundle(saved);GreaterHaste copy=new GreaterHaste();copy.restoreFromBundle(saved);assertEquals(4,copy.remainingHasteUnits());assertEquals(BuffExplanation.details(haste),BuffExplanation.details(copy));
 }
 @Test public void wellFedDescriptionTracksActualResetAndScheduledHealingWithoutSpendingCredit()throws Exception{
  hero.sprite=quietSprite();WellFed fed=Buff.affect(hero,WellFed.class);fed.reset();assertEquals((int)Hunger.STARVING,fed.remainingNutritionUpdates());com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();fed.storeInBundle(saved);for(int i=0;i<20;i++)BuffExplanation.details(fed);com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();fed.storeInBundle(after);assertEquals(saved.toString(),after.toString());hero.HP=10;for(int i=0;i<18;i++)fed.act();assertEquals(11,hero.HP);assertEquals((int)Hunger.STARVING-18,fed.remainingNutritionUpdates());WellFed copy=new WellFed();copy.restoreFromBundle(after);assertEquals((int)Hunger.STARVING,copy.remainingNutritionUpdates());Dungeon.challenges=Challenges.NO_FOOD;copy.reset();assertEquals((int)Hunger.STARVING/3,copy.remainingNutritionUpdates());assertTrue(BuffExplanation.details(copy).contains("multiple of 18"));
 }
 @Test public void accuracyStatusExplanationsMatchAllActualStackedRollCombinations(){
  for(int mask=0;mask<8;mask++){ContestScoreTarget attacker=new ContestScoreTarget(),defender=new ContestScoreTarget();java.util.List<Buff> effects=new java.util.ArrayList<>();if((mask&1)!=0){effects.add(Buff.affect(attacker,Bless.class));}if((mask&2)!=0){effects.add(Buff.affect(attacker,Hex.class));}if((mask&4)!=0){effects.add(Buff.affect(attacker,Daze.class));}for(Buff effect:effects){float factor=effect instanceof Bless?Bless.ROLL_FACTOR:effect instanceof Hex?Hex.ROLL_FACTOR:Daze.ROLL_FACTOR;assertTrue(effect.desc(),effect.desc().contains("_"+com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",100*Math.abs(factor-1))+"%_"));String text=BuffExplanation.details(effect);assertTrue(text.contains("not a fixed change"));assertTrue(text.contains("Bless and Hex together give ×1"));}for(int seed=0;seed<100;seed++){Random.pushGenerator(seed);float roll=Random.Float(20);if((mask&1)!=0)roll*=Bless.ROLL_FACTOR;if((mask&2)!=0)roll*=Hex.ROLL_FACTOR;if((mask&4)!=0)roll*=Daze.ROLL_FACTOR;boolean expected=roll>=Random.Float(20);Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(expected,com.shatteredpixel.shatteredpixeldungeon.actors.Char.hit(attacker,defender,1,false));}finally{Random.popGenerator();}Random.pushGenerator(seed);float attackRoll=Random.Float(20),defenseRoll=Random.Float(20);if((mask&1)!=0)defenseRoll*=Bless.ROLL_FACTOR;if((mask&2)!=0)defenseRoll*=Hex.ROLL_FACTOR;if((mask&4)!=0)defenseRoll*=Daze.ROLL_FACTOR;boolean reverse=attackRoll>=defenseRoll;Random.popGenerator();Random.pushGenerator(seed);try{assertEquals(reverse,com.shatteredpixel.shatteredpixeldungeon.actors.Char.hit(defender,attacker,1,false));}finally{Random.popGenerator();}}}
 }
 @Test public void lethalHasteDescriptionMatchesActualEnemyKillOfferAndReplacement(){
  ContestScoreTarget enemy=new ContestScoreTarget();enemy.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;for(int rank=1;rank<=2;rank++){hero.talents.get(2).put(Talent.LETHAL_HASTE,rank);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.onAbilityKill(hero,enemy);GreaterHaste haste=hero.buff(GreaterHaste.class);assertNotNull(haste);assertEquals(2+2*rank,haste.remainingHasteUnits());haste.set(50);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.onAbilityKill(hero,enemy);assertEquals(2+2*rank,haste.remainingHasteUnits());assertTrue(TalentExplanation.details(Talent.LETHAL_HASTE,rank).contains("= "+haste.remainingHasteUnits()+" units"));}
 }
 @Test public void levitationAndRootsDescriptionsMatchActualAttachmentRules()throws Exception{
  com.watabou.noosa.Game previousGame=com.watabou.noosa.Game.instance;java.lang.reflect.Field field=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");field.setAccessible(true);Object unsafe=field.get(null);com.watabou.noosa.Game.instance=(com.watabou.noosa.Game)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,com.watabou.noosa.Game.class);try{PlainStatusTarget target=new PlainStatusTarget();target.pos=hero.pos;target.sprite=quietSprite();Roots roots=Buff.affect(target,Roots.class);assertTrue(target.rooted);assertTrue(BuffExplanation.details(roots).contains("Teleportation follows its own rules"));Levitation flight=Buff.affect(target,Levitation.class);assertTrue(target.flying);assertFalse(target.rooted);assertNull(target.buff(Roots.class));assertFalse(new Roots().attachTo(target));assertTrue(BuffExplanation.details(flight).contains("Webs and sacrificial fire"));flight.detach();assertFalse(target.flying);assertTrue(new Roots().attachTo(target));}finally{com.watabou.noosa.Game.instance=previousGame;}
 }
 @Test public void freezingDescriptionMatchesRealShieldedHitThawAndWaterChill()throws Exception{
  PlainStatusTarget target=new PlainStatusTarget();target.pos=hero.pos;target.sprite=quietSprite();Dungeon.level.water[target.pos]=true;Frost frost=Buff.affect(target,Frost.class);assertEquals(1,target.paralysed);assertTrue(BuffExplanation.details(frost).contains(ObfuscationExplanation.number(Chill.DURATION/2f)+" turns of Chill"));Buff.affect(target,Barrier.class).setShield(20);int hp=target.HP;target.damage(5,this);assertNull(target.buff(Frost.class));assertEquals(0,target.paralysed);assertEquals(hp,target.HP);assertNotNull(target.buff(Chill.class));
 }
 @Test public void magicImmunityDescriptionDoesNotPromiseProtectionFromUnlistedDamage()throws Exception{
  PlainStatusTarget target=new PlainStatusTarget();target.pos=hero.pos;target.sprite=quietSprite();Buff.affect(target,Hex.class);Buff.affect(target,Weakness.class);Buff.affect(target,Daze.class);MagicImmune magic=Buff.affect(target,MagicImmune.class);assertNull(target.buff(Hex.class));assertNull(target.buff(Weakness.class));assertNotNull(target.buff(Daze.class));for(Class source:com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic.RESISTS)assertTrue(target.isImmune(source));assertFalse(target.isImmune(Poison.class));int hp=target.HP;target.damage(7,this);assertEquals(hp-7,target.HP);assertTrue(BuffExplanation.details(magic).contains("Physical attacks and other unblocked effects can still hurt you"));assertTrue(magic.desc().contains("Disables your rings"));assertTrue(magic.desc().contains("Armor abilities still work"));
 }
 @Test public void toxicImbueDescriptionMatchesGasConservationAndLingeringImmunity(){
  for(float duration:new float[]{.5f,2,2.5f}){
   Dungeon.level.blobs=new java.util.HashMap<>();PlainStatusTarget target=new PlainStatusTarget();target.pos=hero.pos;ToxicImbue toxic=Buff.affect(target,ToxicImbue.class);toxic.set(duration);
   for(int i=0;i<3;i++)Dungeon.level.solid[target.pos+com.watabou.utils.PathFinder.NEIGHBOURS8[i]]=true;
   try{
    int releases=(int)Math.ceil(duration);String first=BuffExplanation.details(toxic);assertTrue(first.contains((9*ToxicImbue.GAS_PER_CELL)+" gas-volume units"));assertTrue(first.contains("movement is not required"));assertTrue(first.contains("Gas releases left: "+releases));assertEquals(duration,toxic.emissionUpdatesLeft(),0);
    for(int i=0;i<releases;i++){toxic.act();com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob gas=Dungeon.level.blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class);assertEquals(54*(i+1),gas.volume);assertEquals(24*(i+1),gas.cur[target.pos]);assertTrue(BuffExplanation.details(toxic).contains("Gas releases left: "+(releases-i-1)));}
    assertTrue(target.isImmune(Poison.class));int volume=Dungeon.level.blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class).volume;
    for(int i=0;i<4;i++)toxic.act();assertEquals(volume,Dungeon.level.blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class).volume);assertNotNull(target.buff(ToxicImbue.class));toxic.act();assertNull(target.buff(ToxicImbue.class));assertFalse(target.isImmune(Poison.class));
   }finally{for(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob gas:Dungeon.level.blobs.values())com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(gas);}
  }
 }
 @Test public void lockedFloorDescriptionMatchesFractionalGraceCapAndSavedState(){
  Dungeon.level.locked=true;LockedFloor lock=Buff.affect(hero,LockedFloor.class);assertEquals(50,lock.remainingGraceTime(),0);lock.addTime(20);assertEquals(50,lock.remainingGraceTime(),0);lock.removeTime(49.5f);assertFalse(lock.regenOn());lock.addTime(.5f);assertTrue(lock.regenOn());lock.act();assertFalse(lock.regenOn());assertEquals(0,lock.remainingGraceTime(),0);lock.removeTime(2);assertEquals(-2,lock.remainingGraceTime(),0);String text=BuffExplanation.details(lock);assertTrue(text.contains("grace: -2"));com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();lock.storeInBundle(saved);LockedFloor copy=new LockedFloor();copy.restoreFromBundle(saved);assertEquals(text,BuffExplanation.details(copy));Dungeon.challenges=Challenges.STRONGER_BOSSES;assertEquals(20,new LockedFloor().remainingGraceTime(),0);
 }
 @Test public void environmentalImmunityDescriptionPreservesListedAndUnlistedSourceDistinction()throws Exception{
  PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();BlobImmunity immunity=Buff.affect(target,BlobImmunity.class);for(Class source:immunity.immunities())assertTrue(target.isImmune(source));assertFalse(target.isImmune(Poison.class));int hp=target.HP;target.damage(8,this);assertEquals(hp-8,target.HP);assertFalse(target.isImmune(Burning.class));assertTrue(BuffExplanation.details(immunity).contains("Burning and Poison are separate effects"));
 }
 @Test public void challengeDescriptionMatchesRealHungerPauseExpiryAndSavedArena()throws Exception{
  descriptionSightArena();hero.sprite=null;boolean intro=com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro();com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro(false);
  try{
   com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge scroll=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge();scroll.anonymize();assertTrue(scroll.desc(),scroll.desc().contains("_100 turns_"));
   com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena arena=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena();arena.setup(hero.pos);assertTrue(arena.attachTo(hero));assertEquals("100",arena.iconTextDisplay());assertTrue(arena.desc().contains("Leaving ends"));
   Hunger hunger=Buff.affect(hero,Hunger.class);float food=hunger.hungerPoints();hunger.act();assertEquals(food,hunger.hungerPoints(),0);
   arena.act();assertEquals("99",arena.iconTextDisplay());com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();arena.storeInBundle(before);String text=arena.desc();for(int i=0;i<10;i++)assertEquals(text,arena.desc());com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();arena.storeInBundle(after);assertEquals(before.toString(),after.toString());
   arena.detach();com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena copy=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena();copy.restoreFromBundle(before);copy.attachTo(hero);assertEquals(text,copy.desc());hero.pos=0;copy.act();assertNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena.class));hunger.act();assertTrue(hunger.hungerPoints()>food);
   hero.pos=16*33+16;com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena expiry=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena();expiry.setup(hero.pos);assertTrue(expiry.attachTo(hero));for(int i=0;i<99;i++)expiry.act();assertNotNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena.class));expiry.act();assertNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena.class));
  }finally{com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro(intro);}
 }
 @Test public void challengeDamageDescriptionMatchesActualAttackBeforeArmor()throws Exception{
  descriptionSightArena();java.util.Arrays.fill(Dungeon.level.heroFOV,false);PlainStatusTarget target=new PlainStatusTarget();target.pos=hero.pos+1;target.sprite=null;RatDamageProbe attacker=new RatDamageProbe();attacker.pos=hero.pos-1;attacker.sprite=null;
  float multiplier=com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.DifficultyHooks.attackDamage(attacker,20);int baseline=Math.round(multiplier);assertTrue(attacker.attack(target,1,0,Float.POSITIVE_INFINITY));assertEquals(100-baseline,target.HP);target.HP=100;
  Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena.class);assertTrue(attacker.attack(target,1,0,Float.POSITIVE_INFINITY));assertEquals(100-Math.round(multiplier*.67f),target.HP);
 }
 @Test public void foresightDescriptionMatchesActualPassiveMappingRadiusWithoutSightOrTimeCost(){
  Dungeon.level.setSize(33,33);hero.pos=16*33+16;hero.fieldOfView=new boolean[Dungeon.level.length()];Dungeon.level.traps=new com.watabou.utils.SparseArray<>();float time=hero.cooldown();Foresight sight=Buff.affect(hero,Foresight.class);assertTrue(Dungeon.level.mapped[hero.pos+Foresight.DISTANCE]);assertFalse(Dungeon.level.mapped[hero.pos+Foresight.DISTANCE+1]);assertEquals(time,hero.cooldown(),0);assertTrue(sight.desc(),sight.desc().contains("_"+Foresight.DISTANCE+" tiles_"));String text=BuffExplanation.details(sight);assertTrue(text.contains("within "+Foresight.DISTANCE+" tiles"));java.util.Arrays.fill(Dungeon.level.mapped,false);for(int i=0;i<10;i++)BuffExplanation.details(sight);assertFalse(Dungeon.level.mapped[hero.pos+1]);assertFalse(hero.search(false));assertTrue(Dungeon.level.mapped[hero.pos+Foresight.DISTANCE]);assertEquals(time,hero.cooldown(),0);
 }
 @Test public void prismaticGuardDescriptionMatchesFractionalReserveAndLockedRecovery(){
  PrismaticGuard guard=Buff.affect(hero,PrismaticGuard.class);guard.set(1);assertEquals(10+(int)Math.floor(hero.lvl*2.5f),guard.maxHP());for(int i=0;i<10;i++)guard.act();assertEquals(2,guard.storedHealth(),.00001f);LockedFloor lock=Buff.affect(hero,LockedFloor.class);lock.removeTime(60);float before=guard.storedHealth();guard.act();assertEquals(before,guard.storedHealth(),0);com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();guard.storeInBundle(saved);String first=BuffExplanation.details(guard);for(int i=0;i<10;i++)assertEquals(first,BuffExplanation.details(guard));com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();guard.storeInBundle(after);assertEquals(saved.toString(),after.toString());PrismaticGuard copy=new PrismaticGuard();copy.restoreFromBundle(saved);copy.target=hero;assertEquals(first,BuffExplanation.details(copy));int priorLevel=hero.lvl;try{for(int level:new int[]{1,2,19,30}){hero.lvl=level;int maximum=10+(int)Math.floor(level*2.5f);assertEquals(maximum,guard.maxHP());assertTrue(BuffExplanation.details(guard).contains(" / "+maximum+" HP"));assertTrue(guard.desc().contains("within 4 tiles"));}}finally{hero.lvl=priorLevel;}
 }
 @Test public void layOnHandsDescriptionMatchesActualSelfBarrierCapAndDoesNotHealSelf()throws Exception{
  hero.sprite=quietSprite();java.lang.reflect.Method apply=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands.class.getDeclaredMethod("affectChar",Hero.class,com.shatteredpixel.shatteredpixeldungeon.actors.Char.class);apply.setAccessible(true);for(int rank=1;rank<=3;rank++){hero.talents.get(2).put(Talent.LAY_ON_HANDS,rank);int amount=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands.restorationAmount(hero);Barrier barrier=Buff.affect(hero,Barrier.class);barrier.setShield(3*amount-2);hero.HP=10;apply.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands.INSTANCE,hero,hero);assertEquals(10,hero.HP);assertEquals(3*amount,barrier.shielding());barrier.setShield(3*amount+5);apply.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands.INSTANCE,hero,hero);assertEquals(3*amount+5,barrier.shielding());String text=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands.INSTANCE,hero);assertTrue(text.contains("adds up to _"+amount+" shield points_"));assertTrue(text.contains("stack up to _"+(3*amount)+" shield points_"));}
 }
 @Test public void layOnHandsDescriptionMatchesActualOtherTargetHealingAndUnusedBarrier()throws Exception{
  hero.talents.get(2).put(Talent.LAY_ON_HANDS,2);java.lang.reflect.Method apply=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands.class.getDeclaredMethod("affectChar",Hero.class,com.shatteredpixel.shatteredpixeldungeon.actors.Char.class);apply.setAccessible(true);PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();target.HP=95;Barrier barrier=Buff.affect(target,Barrier.class);barrier.setShield(55);apply.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands.INSTANCE,hero,target);assertEquals(100,target.HP);assertEquals(60,barrier.shielding());target.HP=70;apply.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands.INSTANCE,hero,target);assertEquals(90,target.HP);assertEquals(60,barrier.shielding());int hp=target.HP,shield=barrier.shielding();for(int i=0;i<20;i++)SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands.INSTANCE,hero);assertEquals(hp,target.HP);assertEquals(shield,barrier.shielding());
 }
 public static class NegativeAllyFixture extends AllyBuff {{type=buffType.NEGATIVE;}}
 @Test public void cleanseDescriptionMatchesActualStatusExceptionsAndStrongerBarrier(){
  hero.talents.get(2).put(Talent.CLEANSE,1);PlainStatusTarget target=new PlainStatusTarget();Buff.affect(target,Hex.class);Buff.affect(target,Weakness.class);LostInventory lost=Buff.affect(target,LostInventory.class);NegativeAllyFixture conversion=Buff.affect(target,NegativeAllyFixture.class);Barrier shield=Buff.affect(target,Barrier.class);shield.setShield(50);com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse.applyEffects(hero,target);assertNull(target.buff(Hex.class));assertNull(target.buff(Weakness.class));assertSame(lost,target.buff(LostInventory.class));assertSame(conversion,target.buff(NegativeAllyFixture.class));assertEquals(50,shield.shielding());assertNull(target.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.Cleanse.class));Buff.affect(target,Daze.class);assertNotNull(target.buff(Daze.class));assertTrue(SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse.INSTANCE,hero).contains("recover a lost backpack"));
 }
 @Test public void cleanseDescriptionMatchesTimedStatusRejectionAndLeavesDamageActive()throws Exception{
  for(int rank=2;rank<=3;rank++){hero.talents.get(2).put(Talent.CLEANSE,rank);PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse.applyEffects(hero,target);assertEquals(10*rank,target.buff(Barrier.class).shielding());assertNotNull(target.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.Cleanse.class));Buff.affect(target,Daze.class);assertNull(target.buff(Daze.class));Buff.affect(target,LostInventory.class);assertNotNull(target.buff(LostInventory.class));int shield=target.buff(Barrier.class).shielding();target.damage(7,this);assertEquals(shield-7,target.buff(Barrier.class).shielding());String text=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse.INSTANCE,hero);assertTrue(text.contains("for _"+(2*(rank-1)+1)+" turns_"));assertTrue(text.contains("_"+(10*rank)+" shield points_"));}
 }
 @Test public void radianceDescriptionMatchesActualFirstMarkRepeatDamageAndVisibleNonAllyFilter()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat target=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();target.sprite=quietSprite();target.pos=hero.pos+1;target.HP=target.HT=100;target.state=target.SLEEPING;Dungeon.level.heroFOV[target.pos]=false;com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Radiance.affectVisibleMonster(hero,target);assertNull(target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.Illuminated.class));Dungeon.level.heroFOV[target.pos]=true;target.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Radiance.affectVisibleMonster(hero,target);assertEquals(100,target.HP);assertNull(target.buff(Paralysis.class));target.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.NEUTRAL;com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Radiance.affectVisibleMonster(hero,target);assertEquals(100,target.HP);assertNotNull(target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.Illuminated.class));assertNotNull(target.buff(Paralysis.class));com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Radiance.affectVisibleMonster(hero,target);assertEquals(100-(hero.lvl+5),target.HP);String text=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Radiance.INSTANCE,hero);assertTrue(text.contains("_"+(hero.lvl+5)+" base magic damage_"));assertTrue(text.contains("first cast"));
 }
 @Test public void flashDescriptionTracksActualCostCounterAndInvalidTargetDoesNotSpendTime()throws Exception{
  hero.sprite=quietSprite();Dungeon.level.setSize(15,15);hero.pos=7*15+7;com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff form=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.class);java.lang.reflect.Method select=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash.class.getDeclaredMethod("onTargetSelected",com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome.class,Hero.class,Integer.class);select.setAccessible(true);hero.talents.get(3).put(Talent.FLASH,1);int target=hero.pos+4;Dungeon.level.mapped[target]=true;float time=hero.cooldown();for(int count=0;count<5;count++){form.flashCasts=count;assertEquals(2+count,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash.INSTANCE.chargeUse(hero),0);select.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash.INSTANCE,null,hero,target);assertEquals(count,form.flashCasts);assertEquals(time,hero.cooldown(),0);String text=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash.INSTANCE,hero);assertTrue(text.contains("charge cost: _"+(2+count)+"_"));}for(int rank=1;rank<=3;rank++){hero.talents.get(3).put(Talent.FLASH,rank);assertEquals(2+rank,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash.maximumDistance(hero));assertTrue(SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash.INSTANCE,hero).contains("up to _"+(2+rank)+" tiles_"));}
 }
 @Test public void divineShieldDescriptionMatchesSourceAmountAndFormDependentProtection()throws Exception{
  hero.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff form=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.class);PlainStatusTarget ally=new PlainStatusTarget();ally.sprite=quietSprite();com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineIntervention.DivineShield shield=Buff.affect(ally,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineIntervention.DivineShield.class);for(int rank=1;rank<=3;rank++){hero.talents.get(3).put(Talent.DIVINE_INTERVENTION,rank);int amount=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineIntervention.shieldOffer(hero);shield.setShield(amount);assertEquals(100+50*rank,shield.shielding());assertTrue(SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineIntervention.INSTANCE,hero).contains("_"+amount+" shield points_"));}form.detach();assertEquals(0,shield.shielding());shield.act();assertNull(ally.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineIntervention.DivineShield.class));
 }
 @Test public void holyIntuitionExplanationMatchesActualItemFilterWithoutHiddenLevelInference()throws Exception{
  java.lang.reflect.Method usable=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition.class.getDeclaredMethod("usableOnItem",com.shatteredpixel.shatteredpixeldungeon.items.Item.class);usable.setAccessible(true);for(com.shatteredpixel.shatteredpixeldungeon.items.Item item:new com.shatteredpixel.shatteredpixeldungeon.items.Item[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword(),new MailArmor(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile()}){item.levelKnown=false;item.cursedKnown=false;for(int level:new int[]{0,7}){item.level(level);item.cursed=level==7;assertEquals(true,usable.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition.INSTANCE,item));}item.cursedKnown=true;assertEquals(false,usable.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition.INSTANCE,item));}assertEquals(false,usable.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition.INSTANCE,new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing()));for(int rank=1;rank<=2;rank++){hero.talents.get(0).put(Talent.HOLY_INTUITION,rank);assertEquals(4-rank,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition.INSTANCE.chargeUse(hero),0);assertTrue(SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition.INSTANCE,hero).contains("Tome charge cost: _"+(4-rank)+"_"));}
 }
 @Test public void recallExplanationMatchesRealUseRecordingCostsAndSavedRecord(){
  hero.heroClass=HeroClass.CLERIC;Class[] classes={com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify.class,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation.class,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDivination.class,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis.class,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment.class,com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink.class,com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAugmentation.class,com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment.class};int[] costs={3,6,4,8,8,2,4,4};for(int rank=1;rank<=2;rank++){Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.UsedItemTracker.class);hero.talents.get(1).put(Talent.RECALL_INSCRIPTION,rank);for(int i=0;i<classes.length;i++){if(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.class.isAssignableFrom(classes[i]))Talent.onScrollUsed(hero,hero.pos,1,classes[i]);else Talent.onRunestoneUsed(hero,hero.pos,classes[i]);com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.UsedItemTracker tracker=hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.UsedItemTracker.class);assertSame(classes[i],tracker.item);assertEquals(rank==2?300:10,tracker.cooldown(),.001f);assertEquals(costs[i],com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.INSTANCE.chargeUse(hero),0);assertTrue(SpellExplanation.details(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.INSTANCE,hero).contains("Tome charge cost: _"+costs[i]+"_"));com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();tracker.storeInBundle(saved);com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.UsedItemTracker copy=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.UsedItemTracker();copy.restoreFromBundle(saved);assertSame(classes[i],copy.item);Talent.onScrollUsed(hero,hero.pos,1,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade.class);assertSame(classes[i],tracker.item);}}
 }
 @Test public void spiritFormDescriptionDoesNotRewriteCachedRingLevel(){
  hero.talents.get(3).put(Talent.SPIRIT_FORM,1);com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff form=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff.class);RingOfArcana ring=new RingOfArcana();ring.level(7);ring.anonymize();form.setEffect(ring);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();form.storeInBundle(before);form.desc();assertEquals(7,ring.level());com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();form.storeInBundle(after);assertEquals(before.toString(),after.toString());assertSame(ring,form.ring());assertEquals(1,ring.level());
 }
 @Test public void spiritFormDescriptionDoesNotUpgradeCachedArtifact(){
  hero.talents.get(3).put(Talent.SPIRIT_FORM,3);com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff form=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff.class);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty();horn.levelKnown=true;form.setEffect(horn);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();form.storeInBundle(before);form.desc();assertEquals(0,horn.level());com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();form.storeInBundle(after);assertEquals(before.toString(),after.toString());assertSame(horn,form.artifact());assertEquals(8,horn.level());
 }
 @Test public void formConfigurationDescriptionsMatchActualRankFormulasAndPreserveStoredChoices()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity trinity=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity();hero.armorAbility=trinity;com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.level(9);java.lang.reflect.Field mindField=trinity.getClass().getDeclaredField("mindForm");mindField.setAccessible(true);mindField.set(trinity,wand);com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty();java.lang.reflect.Field spiritField=trinity.getClass().getDeclaredField("spiritForm");spiritField.setAccessible(true);spiritField.set(trinity,horn);for(int rank=1;rank<=4;rank++){hero.talents.get(3).put(Talent.BODY_FORM,rank);hero.talents.get(3).put(Talent.MIND_FORM,rank);hero.talents.get(3).put(Talent.SPIRIT_FORM,rank);String body=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm.INSTANCE,hero),mind=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MindForm.INSTANCE,hero),spirit=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.INSTANCE,hero);assertTrue(body.contains("_"+com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm.duration()+" turns_"));assertTrue(mind.contains("_+"+com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MindForm.itemLevel()+" version_"));assertTrue(spirit.contains("_+"+com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.ringLevel()+" ring_"));assertTrue(spirit.contains("_+"+com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.artifactLevel()+" artifact_"));assertSame(wand,mindField.get(trinity));assertSame(horn,spiritField.get(trinity));assertEquals(9,wand.level());assertEquals(0,horn.level());}
 }
 @Test public void divineSenseDescriptionsMatchActualClassRangesAndArtifactTrigger(){
  descriptionSightArena();hero.fieldOfView=new boolean[Dungeon.level.length()];Buff.affect(hero,Blindness.class,50);
  for(HeroClass heroClass:new HeroClass[]{HeroClass.WARRIOR,HeroClass.CLERIC})for(int rank=1;rank<=2;rank++){
   hero.heroClass=heroClass;hero.talents.get(1).put(Talent.DIVINE_SENSE,rank);Buff.detach(hero,DivineSense.DivineSenseTracker.class);Dungeon.level.mobs.clear();
   int range=heroClass==HeroClass.CLERIC?4+4*rank:1+2*rank;
   if(heroClass==HeroClass.CLERIC)Buff.prolong(hero,DivineSense.DivineSenseTracker.class,DivineSense.DivineSenseTracker.DURATION);else{float time=hero.cooldown();Talent.onArtifactUsed(hero);assertEquals(time+1,hero.buff(DivineSense.DivineSenseTracker.class).cooldown(),0);}
   com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat near=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat(),far=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();near.pos=hero.pos+range;far.pos=hero.pos+range+2;Dungeon.level.mobs.add(near);Dungeon.level.mobs.add(far);
   Dungeon.level.updateFieldOfView(hero,hero.fieldOfView);assertTrue(hero.mindVisionEnemies.contains(near));assertFalse(hero.mindVisionEnemies.contains(far));
   String text=SpellExplanation.effects(DivineSense.INSTANCE,hero);assertTrue(text,text.contains("_"+range+" tiles_"));if(heroClass==HeroClass.CLERIC)assertTrue(text,text.contains("_50 turns_"));else assertTrue(text,text.contains("after using a wand or artifact"));
  }
 }
 @Test public void lifeLinkDisplayedDurationMatchesActualOfferedTimerAtEveryRank(){
  int[] expected={10,13,17,20};for(int rank=1;rank<=4;rank++){hero.talents.get(3).put(Talent.LIFE_LINK,rank);int duration=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell.linkDuration(hero);assertEquals(expected[rank-1],duration);PlainStatusTarget ally=new PlainStatusTarget();LifeLink link=Buff.prolong(ally,LifeLink.class,duration);assertEquals(duration,link.cooldown(),.001f);String plain=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell.INSTANCE.desc(),details=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell.INSTANCE,hero);assertTrue(plain,plain.contains("for "+duration+" turns"));assertTrue(details.contains("_"+duration+" turns_"));}
 }
 @Test public void beamingRayDescriptionMatchesActualRangeAndDoesNotPromiseNearestFallback(){
  for(int rank=1;rank<=4;rank++){hero.talents.get(3).put(Talent.BEAMING_RAY,rank);assertEquals(4*rank,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay.movementRange(hero,false));assertEquals(2*rank,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay.movementRange(hero,true));String details=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay.INSTANCE,hero);assertTrue(details.contains("_"+(4*rank)+" tiles_"));assertTrue(details.contains("_"+(30+5*rank)+"% more attack damage_"));assertFalse(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay.INSTANCE.desc().contains("closest enemy"));}
 }
 @Test public void stasisDescriptionAndSavePreserveStoredAllyAndZeroChargeRelease()throws Exception{
  hero.talents.get(3).put(Talent.STASIS,2);assertEquals(2,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis.INSTANCE.chargeUse(hero),0);com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis.StasisBuff stored=Buff.prolong(hero,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis.StasisBuff.class,90);com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat ally=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();ally.HP=3;java.lang.reflect.Field field=stored.getClass().getDeclaredField("stasisAlly");field.setAccessible(true);field.set(stored,ally);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();stored.storeInBundle(before);String text=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis.INSTANCE,hero);assertTrue(text.contains("_90 turns_"));assertEquals(0,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis.INSTANCE.chargeUse(hero),0);for(int i=0;i<10;i++)assertEquals(text,SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis.INSTANCE,hero));assertSame(ally,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis.getStasisAlly());assertEquals(3,ally.HP);com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();stored.storeInBundle(after);assertEquals(before.toString(),after.toString());com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis.StasisBuff copy=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis.StasisBuff();copy.restoreFromBundle(before);assertEquals(3,((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat)field.get(copy)).HP);
 }
 @Test public void hallowedGroundExplanationPreservesActualInitialNegativeShieldAndOngoingRules()throws Exception{
  hero.sprite=quietSprite();hero.talents.get(2).put(Talent.HALLOWED_GROUND,2);java.lang.reflect.Method initial=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround.class.getDeclaredMethod("affectChar",com.shatteredpixel.shatteredpixeldungeon.actors.Char.class);initial.setAccessible(true);Barrier shield=Buff.affect(hero,Barrier.class);shield.setShield(40);initial.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround.INSTANCE,hero);assertEquals(30,shield.shielding());PlainStatusTarget ally=new PlainStatusTarget();ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;ally.sprite=quietSprite();ally.HP=95;Barrier other=Buff.affect(ally,Barrier.class);other.setShield(40);initial.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround.INSTANCE,ally);assertEquals(100,ally.HP);assertEquals(50,other.shielding());com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround.HallowedTerrain terrain=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround.HallowedTerrain();java.lang.reflect.Method pulse=terrain.getClass().getDeclaredMethod("affectChar",com.shatteredpixel.shatteredpixeldungeon.actors.Char.class);pulse.setAccessible(true);pulse.invoke(terrain,hero);assertEquals(31,shield.shielding());String text=SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround.INSTANCE,hero);assertTrue(text.contains("can reduce Barrier"));assertTrue(text.contains(ObfuscationExplanation.number(100f/30)+"% chance per turn"));
 }
 @Test public void wallOfLightExplanationMatchesActualSolidFlagsCostAndFreeEarlyClear(){
  Dungeon.level.blobs=new java.util.HashMap<>();Dungeon.level.traps=new com.watabou.utils.SparseArray<>();Dungeon.level.plants=new com.watabou.utils.SparseArray<>();int cell=hero.pos+1;com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(cell,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);boolean los=Dungeon.level.losBlocking[cell];assertEquals(3,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.INSTANCE.chargeUse(hero),0);com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.LightWall wall=com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.seed(cell,20,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.LightWall.class);assertTrue(Dungeon.level.solid[cell]);assertFalse(Dungeon.level.passable[cell]);assertEquals(los,Dungeon.level.losBlocking[cell]);assertEquals(0,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.INSTANCE.chargeUse(hero),0);float time=hero.cooldown();com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.INSTANCE.onCast(null,hero);assertEquals(0,wall.volume);assertFalse(Dungeon.level.solid[cell]);assertEquals(time,hero.cooldown(),0);assertEquals(3,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.INSTANCE.chargeUse(hero),0);assertTrue(SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.INSTANCE,hero).contains("does not trigger spell-cast bonuses"));
 }
 @Test public void mnemonicPrayerExplanationMatchesActualTimedBudgetAndBleedingUnitsOnce()throws Exception{
  hero.talents.get(2).put(Talent.MNEMONIC_PRAYER,2);java.lang.reflect.Method apply=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer.class.getDeclaredMethod("affectChar",com.shatteredpixel.shatteredpixeldungeon.actors.Char.class,float.class,Hero.class);apply.setAccessible(true);PlainStatusTarget ally=new PlainStatusTarget();ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;ally.sprite=quietSprite();Haste haste=Buff.append(ally,Haste.class,10);GreaterHaste budget=Buff.affect(ally,GreaterHaste.class);budget.set(4);Barrier shield=Buff.affect(ally,Barrier.class);shield.setShield(20);float shieldTime=shield.cooldown();apply.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer.INSTANCE,ally,4f,hero);assertEquals(14,haste.cooldown(),.001f);assertEquals(8,budget.remainingHasteUnits());assertEquals(20,shield.shielding());assertEquals(shieldTime+4,shield.cooldown(),.001f);apply.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer.INSTANCE,ally,4f,hero);assertEquals(14,haste.cooldown(),.001f);assertEquals(8,budget.remainingHasteUnits());PlainStatusTarget enemy=new PlainStatusTarget();enemy.sprite=quietSprite();enemy.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;Bleeding bleed=Buff.affect(enemy,Bleeding.class);Random.pushGenerator(341);try{bleed.set(5);}finally{Random.popGenerator();}float originalBleed=bleed.level();apply.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer.INSTANCE,enemy,4f,hero);assertEquals(originalBleed+4,bleed.level(),0);apply.invoke(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer.INSTANCE,enemy,4f,hero);assertEquals(originalBleed+4,bleed.level(),0);assertTrue(SpellExplanation.effects(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer.INSTANCE,hero).contains("Bleeding gains _4 base damage_ on its next tick"));
 }
 @Test public void everySelectableClericSpellHasASpecificPrimaryExplanation(){
  Dungeon.level.blobs=new java.util.HashMap<>();java.util.List<com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell> spells=com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell.getAllSpells();assertEquals(27,spells.size());Random.pushGenerator(78531);float expected=Random.Float();Random.popGenerator();Random.pushGenerator(78531);try{for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell spell:spells)assertFalse(spell.getClass().getSimpleName(),SpellExplanation.effects(spell,hero).isEmpty());assertEquals(expected,Random.Float(),0);}finally{Random.popGenerator();}
 }
 @Test public void ordinaryItemWindowsDoNotGetPanelsFromGenericWordsOrSimpleResources(){
  com.shatteredpixel.shatteredpixeldungeon.items.Item[] ordinary={new com.shatteredpixel.shatteredpixeldungeon.items.food.Food(),new com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation(),new com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty(),new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed(),new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing(),new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword(),new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor()};
  for(com.shatteredpixel.shatteredpixeldungeon.items.Item item:ordinary){item.levelKnown=item.cursedKnown=true;String before=item.info();com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();item.storeInBundle(state);assertEquals(item.getClass().getSimpleName(),"",MechanicsDetails.itemCalculations(item));assertEquals(before,item.info());com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();item.storeInBundle(after);assertEquals(state.toString(),after.toString());}
  assertEquals("",MechanicsDetails.itemCalculations(null));
 }
 @Test public void arcanaAndAffectedGlyphEnchantmentsRetainTheirSpecificMathWithoutGenericItemSections(){
  RingOfArcana ring=new RingOfArcana();ring.anonymize();ring.level(3);ring.levelKnown=ring.cursedKnown=true;
  assertEquals("",MechanicsDetails.itemCalculations(ring));assertTrue(ring.statsInfo().contains("90.61"));assertFalse(RingExplanation.details(ring).contains("1.175^"));
  MailArmor armor=new MailArmor();armor.level(3);armor.levelKnown=armor.cursedKnown=true;armor.glyph=new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Obfuscation();
  // Armor glyphs now explain gameplay in the ordinary description, without an extra math panel.
  String glyph=MechanicsDetails.itemCalculations(armor);assertEquals("",glyph);assertTrue(armor.info().contains("sleeping enemy 3 tiles away"));
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.levelKnown=sword.cursedKnown=true;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing());String enchantment=MechanicsDetails.itemCalculations(sword);assertTrue(enchantment,enchantment.contains("Blazing calculations"));assertFalse(enchantment.contains("_Damage numbers_"));
  ring.levelKnown=false;armor.levelKnown=false;sword.levelKnown=false;assertEquals("",MechanicsDetails.itemCalculations(ring));assertEquals("",MechanicsDetails.itemCalculations(armor));assertEquals("",MechanicsDetails.itemCalculations(sword));
 }
 @Test public void genericGlossaryAndDurationDoNotCreateBuffPanels(){
  assertEquals("",MechanicsDetails.buff(new Buff(),"damage chance speed turns strength shielding"));
  assertEquals("",MechanicsDetails.buff(new FlavourBuff(),"lasts 10 turns"));
 }
 @Test public void genuinelyNonlinearForceTenacityAndChaliceExplanationsRemainAvailable(){
  for(com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring ring:new com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring[]{new RingOfForce(),new RingOfTenacity()}){ring.anonymize();ring.levelKnown=ring.cursedKnown=true;String text=MechanicsDetails.itemCalculations(ring);assertFalse(text.isEmpty());assertFalse(text.contains("_Time and speed_"));}
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood chalice=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood();chalice.levelKnown=chalice.cursedKnown=true;String text=MechanicsDetails.itemCalculations(chalice);assertTrue(text,text.contains("level²"));assertTrue(text.contains("healing"));
 }
 @Test public void complexArtifactTimeBudgetResourceLimitsAndConeGeometryRemainAvailable(){
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains chains=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains();chains.levelKnown=chains.cursedKnown=true;assertTrue(MechanicsDetails.itemCalculations(chains).contains("twice the natural recharge limit"));
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass hourglass=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass();hourglass.levelKnown=hourglass.cursedKnown=true;assertTrue(MechanicsDetails.itemCalculations(hourglass).contains("Each charge pays for 2 turns"));
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight talisman=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight();talisman.levelKnown=talisman.cursedKnown=true;assertTrue(MechanicsDetails.itemCalculations(talisman).contains("0.92^d"));
 }
 @Test public void ownerRetainedArtifactAndTrinketPanelsStaySpecificPendingReview(){
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty();horn.levelKnown=horn.cursedKnown=true;assertEquals(ArtifactExplanation.details(horn),MechanicsDetails.itemCalculations(horn));
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals crystal=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals();crystal.levelKnown=crystal.cursedKnown=true;assertEquals(TrinketExplanation.details(crystal),MechanicsDetails.itemCalculations(crystal));
  com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand ward=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding();ward.levelKnown=ward.cursedKnown=ward.curChargeKnown=true;assertEquals(WandExplanation.details(ward),MechanicsDetails.itemCalculations(ward));ward.curChargeKnown=false;assertEquals("",MechanicsDetails.itemCalculations(ward));
 }
 @Test public void nonlinearBlastScrollCalculationsRemainWhileRoutineScrollsDoNot(){
  com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll retribution=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution();retribution.anonymize();assertTrue(MechanicsDetails.itemCalculations(retribution).contains("4.45"));
  com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll blast=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast();blast.anonymize();assertTrue(MechanicsDetails.itemCalculations(blast).contains("0.9^N"));
  com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll recharge=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging();recharge.anonymize();assertEquals("",MechanicsDetails.itemCalculations(recharge));
 }
 @Test public void luckyShowsActualArcanaIncreaseInOrdinaryWeaponDescription(){
  RingOfArcana arcana=ring(3);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword lucky=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();lucky.levelKnown=lucky.cursedKnown=true;lucky.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky());String before=arcana.info();String description=lucky.info();assertTrue(description,description.contains("_19.06%_"));assertEquals(1,description.split("19.06%",-1).length-1);assertTrue(before,before.contains("90.61%"));assertEquals("",MechanicsDetails.itemCalculations(arcana));assertFalse(before.contains("1.175^"));
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade.RunicSlashTracker slash=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade.RunicSlashTracker.class);slash.boost=6;String withOtherBonus=lucky.info();assertTrue(withOtherBonus,withOtherBonus.contains("_79.06%_"));assertEquals(1,withOtherBonus.split("79.06%",-1).length-1);
 }
 @Test public void luckyComparisonMatchesARealProcAtARollBetweenBaseAndBoostedChance(){
  RingOfArcana arcana=ring(3);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword lucky=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();lucky.levelKnown=lucky.cursedKnown=true;com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky enchant=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky();lucky.enchant(enchant);
  long seed=0;for(;seed<10000;seed++){com.watabou.utils.Random.pushGenerator(seed);float roll=com.watabou.utils.Random.Float();com.watabou.utils.Random.popGenerator();if(roll>.1f&&roll<.19f)break;}assertTrue(seed<10000);PlainStatusTarget target=new PlainStatusTarget();com.watabou.utils.Random.pushGenerator(seed);try{enchant.proc(lucky,hero,target,1);}finally{com.watabou.utils.Random.popGenerator();}assertNotNull(target.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky.LuckProc.class));Buff.detach(target,com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky.LuckProc.class);
  for(com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana.Arcana effect:hero.buffs(com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana.Arcana.class))effect.detach();hero.belongings.ring=null;com.watabou.utils.Random.pushGenerator(seed);try{enchant.proc(lucky,hero,target,1);}finally{com.watabou.utils.Random.popGenerator();}assertNull(target.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky.LuckProc.class));
 }
 @Test public void luckyArcanaDifferenceRefreshesAfterUpgradesAndRespectsCapAndUnknownRings(){
  RingOfArcana arcana=ring(3);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword lucky=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();lucky.levelKnown=lucky.cursedKnown=true;lucky.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky());String first=lucky.info();lucky.upgrade();arcana.upgrade();String upgraded=lucky.info();assertNotEquals(first,upgraded);float base=5f/41f,power=(float)Math.pow(1.175f,5);assertTrue(upgraded,upgraded.contains("_"+com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",base*power*100)+"%_"));
  arcana.level(100);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade.RunicSlashTracker slash=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade.RunicSlashTracker.class);slash.boost=30;String capped=lucky.info();assertTrue(capped,capped.contains("_100%_"));assertFalse(capped.contains("without Arcana"));arcana.levelKnown=false;assertFalse(lucky.info().contains("without Arcana"));
 }

 @Test public void everyRingInspectionWithholdsUnknownCompanionLevels()throws Exception{
  for(Class type:com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.RING.classes){
   Ring known=(Ring)type.getDeclaredConstructor().newInstance(),unknown=(Ring)type.getDeclaredConstructor().newInstance();
   known.anonymize();known.levelKnown=known.cursedKnown=true;unknown.anonymize();unknown.levelKnown=unknown.cursedKnown=false;
   hero.belongings.ring=known;hero.belongings.misc=unknown;
   String before=known.info();unknown.level(5);assertEquals(type.getSimpleName(),before,known.info());
   unknown.levelKnown=unknown.cursedKnown=true;assertNotEquals("Identified combined values remain available for "+type.getSimpleName(),before,known.info());
  }
 }
 @Test public void forceInspectionAndUpgradePreviewDoNotExposeUnknownStrengthRing(){
  RingOfForce force=new RingOfForce();force.anonymize();force.levelKnown=force.cursedKnown=true;
  RingOfMight might=new RingOfMight();might.anonymize();might.levelKnown=might.cursedKnown=false;
  hero.belongings.ring=force;hero.belongings.misc=might;might.activate(hero);hero.heroClass=HeroClass.DUELIST;
  String first=force.info(),detail=RingExplanation.details(force),upgrade=force.upgradeStat1(3),ability=force.upgradeStat3(3);int strength=hero.STR();
  might.level(5);assertNotEquals(strength,hero.STR());assertEquals(first,force.info());assertEquals(detail,RingExplanation.details(force));assertEquals(upgrade,force.upgradeStat1(3));assertEquals(ability,force.upgradeStat3(3));
 }
 @Test public void arcanaUpgradePreviewCannotRevealAnUnknownCurse(){
  RingOfArcana arcana=new RingOfArcana();arcana.levelKnown=true;arcana.cursedKnown=false;
  String uncursed=arcana.upgradeStat1(3);arcana.cursed=true;assertEquals(uncursed,arcana.upgradeStat1(3));
  arcana.cursedKnown=true;assertNotEquals(uncursed,arcana.upgradeStat1(3));
 }
 @Test public void identifiedRingSummariesRemainBriefAndLabelSuppressedEffects(){
  RingOfAccuracy ring=new RingOfAccuracy();ring.anonymize();ring.levelKnown=ring.cursedKnown=true;hero.belongings.ring=ring;
  String plain=ring.info();assertTrue(plain.contains("Accuracy bonus"));assertTrue(plain.contains("not hit chance"));assertFalse(plain.contains("percentage points"));assertTrue(plain.length()<250);
  Buff.affect(hero,MagicImmune.class);assertTrue(ring.info().contains("inactive while you are magic-immune"));
 }
 @Test public void allContainerDescriptionsMatchTheirAcceptedItemsAndLiveCapacity(){
  com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag[] bags={hero.belongings.backpack,new com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier(),new com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder(),new com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch(),new com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster()};
  com.shatteredpixel.shatteredpixeldungeon.items.Item[] accepted={new com.shatteredpixel.shatteredpixeldungeon.items.food.Food(),new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing(),new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify(),new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed(),new com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb()};
  for(int i=0;i<bags.length;i++){
   assertTrue(bags[i].canHold(accepted[i]));String text=bags[i].info();assertTrue(text,text.contains(bags[i].capacity()+" item slots"));assertFalse(text,text.contains("NO TEXT"));
   if(i>0)assertFalse(bags[i].canHold(new com.shatteredpixel.shatteredpixeldungeon.items.food.Food()));
  }
  assertTrue(bags[4].info().contains("20%"));
  int initial=hero.belongings.backpack.capacity();hero.belongings.backpack.items.add(bags[1]);assertTrue(hero.belongings.backpack.info().contains((initial+1)+" item slots"));
 }
 @Test public void ordinaryArmorKeepsItsStatsAndRealPenaltyWithoutTheTutorial(){
  com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor scale=new com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor();
  scale.level(6);scale.levelKnown=scale.cursedKnown=true;hero.STR=scale.STRReq();
  String info=scale.info();
  assertTrue(info.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(Armor.class,"curr_absorb",scale.tier,scale.DRMin(),scale.DRMax(),scale.STRReq())));
  assertFalse(info.contains("Armor subtracts"));assertFalse(info.contains("points short"));assertFalse(info.contains("protection points"));
  assertFalse(info.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(Armor.class,"too_heavy")));
  hero.STR=scale.STRReq()-1;
  assertTrue(scale.info().contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(Armor.class,"too_heavy")));
 }
 @Test public void utilityItemDescriptionsUseRealValuesAndDoNotPointToMissingPanels(){
  com.shatteredpixel.shatteredpixeldungeon.items.Ankh ankh=new com.shatteredpixel.shatteredpixeldungeon.items.Ankh();ankh.bless();
  for(int maximum:new int[]{3,20,51,100}){hero.HT=maximum;assertTrue(ankh.desc().contains(com.shatteredpixel.shatteredpixeldungeon.items.Ankh.blessedReviveHP(maximum)+" HP"));}
  assertFalse(ankh.desc().contains("calculation details"));
  String torch=new com.shatteredpixel.shatteredpixeldungeon.items.Torch().desc();assertTrue(torch.contains(ObfuscationExplanation.number(Light.DURATION)+" turns"));assertTrue(UtilityItemExplanation.details(new com.shatteredpixel.shatteredpixeldungeon.items.Torch(),hero).contains(Light.DISTANCE+" tiles"));assertTrue(torch.contains("1 turn"));
  com.shatteredpixel.shatteredpixeldungeon.items.Waterskin skin=new com.shatteredpixel.shatteredpixeldungeon.items.Waterskin();skin.fill();String full=skin.info();
  assertTrue(full.contains(skin.storedDrops()+"/"+skin.capacity()+" drops"));assertFalse(full.contains("calculation details"));assertTrue(full.contains("5%"));assertFalse(full.contains("5%%"));
  assertFalse(new com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal().desc().contains("expanded calculations"));
 }
 @Test public void remainsDescriptionsFollowCurrentHealthAndLevelWithoutUsingTheItems(){
  com.shatteredpixel.shatteredpixeldungeon.items.remains.BrokenHilt hilt=new com.shatteredpixel.shatteredpixeldungeon.items.remains.BrokenHilt();
  com.shatteredpixel.shatteredpixeldungeon.items.remains.SealShard seal=new com.shatteredpixel.shatteredpixeldungeon.items.remains.SealShard();
  com.shatteredpixel.shatteredpixeldungeon.items.remains.TornPage page=new com.shatteredpixel.shatteredpixeldungeon.items.remains.TornPage();
  for(int level:new int[]{1,6,20})for(int maximum:new int[]{20,51,100}){
   hero.lvl=level;hero.HT=maximum;hero.HP=0;
   assertTrue(hilt.info().contains(Math.max(2,level/3)+" damage"));assertTrue(hilt.info().contains("physical hits"));
   assertTrue(seal.info().contains(Math.round(maximum/5f)+" shielding"));assertTrue(page.info().contains(Math.round(maximum/10f)+" HP"));
   assertTrue(page.info().contains("1 turn"));assertNull(hero.buff(PhysicalEmpower.class));assertNull(hero.buff(Barrier.class));
  }
  assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.remains.BowFragment().info().contains(com.shatteredpixel.shatteredpixeldungeon.items.remains.BowFragment.HIGH_GRASS_TILES+" tiles of high grass"));
 }
 @Test public void chargedCrossbowSeparatesDartDamageFromExtraTipUses(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow();
  for(int level:new int[]{0,3,8}){bow.level(level);bow.levelKnown=true;String text=bow.abilityInfo();assertTrue(text,text.contains("+"+(4+level)+" base damage"));assertTrue(text,text.contains((3+level)+" more uses"));}
 }
 @Test public void thrownWeaponTimingDescriptionsMatchActualStatusAndThrowAvailability()throws Exception{
  hero.STR=50;hero.sprite=quietSprite();
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken shuriken=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken();
  assertEquals(0,shuriken.castDelay(hero,hero.pos+1),0);assertTrue(shuriken.statsInfo().contains(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken.ShurikenInstantTracker.DURATION+" turns"));
  Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken.ShurikenInstantTracker.class,19);assertTrue(shuriken.castDelay(hero,hero.pos+1)>0);Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken.ShurikenInstantTracker.class);assertEquals(0,shuriken.castDelay(hero,hero.pos+1),0);
  PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();float speed=target.speed();com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Bolas bolas=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Bolas();bolas.proc(hero,target,1);assertEquals(speed/2,target.speed(),0);assertEquals(Cripple.DURATION/2,target.buff(Cripple.class).cooldown(),0);assertTrue(bolas.statsInfo().contains(ObfuscationExplanation.number(target.buff(Cripple.class).cooldown())+" turns"));
 }
 @Test public void ordinaryEquipmentDescriptionsDoNotExposeUnknownRingLevels()throws Exception{
  java.util.List<com.shatteredpixel.shatteredpixeldungeon.items.Item> equipment=new java.util.ArrayList<>();
  equipment.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword());equipment.add(new MailArmor());equipment.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow());
  for(Class<?> type:com.shatteredpixel.shatteredpixeldungeon.journal.Catalog.THROWN_WEAPONS.items())equipment.add((com.shatteredpixel.shatteredpixeldungeon.items.Item)type.getDeclaredConstructor().newInstance());
  for(String name:new String[]{"Dart","AdrenalineDart","BlindingDart","ChillingDart","CleansingDart","DisplacingDart","HealingDart","HolyDart","IncendiaryDart","ParalyticDart","PoisonDart","RotDart","ShockingDart"})equipment.add((com.shatteredpixel.shatteredpixeldungeon.items.Item)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts."+name).getDeclaredConstructor().newInstance());
  for(com.shatteredpixel.shatteredpixeldungeon.items.Item item:equipment){item.levelKnown=item.cursedKnown=true;}
  for(Ring ring:new Ring[]{new RingOfMight(),new RingOfSharpshooting()}){
   ring.anonymize();ring.levelKnown=ring.cursedKnown=false;hero.belongings.ring=ring;ring.activate(hero);
   for(com.shatteredpixel.shatteredpixeldungeon.items.Item item:equipment){ring.level(0);String zero=item.info();ring.level(9);assertEquals(item.getClass().getSimpleName()+" with unknown "+ring.getClass().getSimpleName(),zero,item.info());}
   Buff immune=Buff.affect(hero,MagicImmune.class);
   for(com.shatteredpixel.shatteredpixeldungeon.items.Item item:equipment){ring.level(0);String zero=item.info();ring.level(9);assertEquals(zero,item.info());assertFalse(item.info().contains("Identify your rings"));}
   immune.detach();for(Buff buff:hero.buffs())if(buff instanceof Ring.RingBuff)buff.detach();hero.belongings.ring=null;
  }
 }
 @Test public void tenacityDescribesProtectionAndHarmWithoutNegativeReductionJargon(){
  RingOfTenacity ring=new RingOfTenacity();ring.anonymize();ring.levelKnown=ring.cursedKnown=true;
  for(int level=0;level<=6;level++)for(boolean cursed:new boolean[]{false,true}){
   ring.level(level);ring.cursed=cursed;String text=ring.statsInfo();int bonus=ring.soloBuffedBonus();
   double reduction=100f*(1f-Math.pow(.85f,bonus));
   assertTrue(text,text.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("#.##",Math.abs(reduction))+"%"));
   assertTrue(text,text.contains(reduction<0?"more damage taken":"less damage"));assertFalse(text.contains("negative reduction"));
  }
  ring.levelKnown=ring.cursedKnown=false;String unknown=ring.statsInfo();ring.level(12);ring.cursed=!ring.cursed;assertEquals(unknown,ring.statsInfo());assertTrue(unknown.contains("Example for an uncursed +0 ring"));
 }
 @Test public void tornPagePreviewMatchesRealHealingWithoutSpendingCredit()throws Exception{
  hero.sprite=quietSprite();hero.HT=100;
  com.shatteredpixel.shatteredpixeldungeon.items.remains.TornPage page=new com.shatteredpixel.shatteredpixeldungeon.items.remains.TornPage();
  java.lang.reflect.Method effect=page.getClass().getDeclaredMethod("doEffect",Hero.class);effect.setAccessible(true);
  for(int percent=0;percent<=com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING.max;percent+=25){
   com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),settings=new com.watabou.utils.Bundle();
   com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,percent).store(settings);all.put("apd_custom_rules",settings);Dungeon.apdRestoreCustomRules(all);
   for(int hp:new int[]{1,50,99,100})for(float fraction:new float[]{0,.25f,.75f}){
    hero.HP=hp;hero.apdCustomHealingFraction=fraction;
    int preview=com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.preview(hero,10);
    for(int inspect=0;inspect<3;inspect++)assertTrue(page.info(),page.info().contains(preview+" HP"));
    assertEquals(hp,hero.HP);assertEquals(fraction,hero.apdCustomHealingFraction,0);
    effect.invoke(page,hero);assertEquals("healing setting "+percent,preview,hero.HP-hp);
   }
  }
 }
 @Test public void tokenDescriptionsDistinguishCurrentAndBothLegacyQuestGoals()throws Exception{
  Class<?> type=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.class;
  java.lang.reflect.Field old=type.getDeclaredField("oldQuest"),alternate=type.getDeclaredField("alternative");old.setAccessible(true);alternate.setAccessible(true);
  boolean previousOld=old.getBoolean(null),previousAlternate=alternate.getBoolean(null);
  try{
   com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken token=new com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken();
   old.setBoolean(null,false);assertTrue(token.desc().contains("10 tokens"));
   old.setBoolean(null,true);alternate.setBoolean(null,false);assertTrue(token.desc().contains("4 tokens"));
   alternate.setBoolean(null,true);assertTrue(token.desc().contains("5 tokens"));
  }finally{old.setBoolean(null,previousOld);alternate.setBoolean(null,previousAlternate);}
 }
 @Test public void guidePagesShowOnlyRecordedTitlesAndInspectionDoesNotCollectThem(){
  com.shatteredpixel.shatteredpixeldungeon.items.journal.DocumentPage[] pages={new com.shatteredpixel.shatteredpixeldungeon.items.journal.AlchemyPage(),new com.shatteredpixel.shatteredpixeldungeon.items.journal.GuidePage()};
  for(com.shatteredpixel.shatteredpixeldungeon.items.journal.DocumentPage page:pages){
   String generic=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(com.shatteredpixel.shatteredpixeldungeon.items.journal.DocumentPage.class,"desc");
   assertEquals(generic,page.desc());page.page("unrecorded-page");assertEquals(generic,page.desc());
   String name=page.document().pageNames().iterator().next();page.page(name);boolean found=page.document().isPageFound(name);
   assertTrue(page.desc().contains(page.document().pageTitle(name)));assertEquals(found,page.document().isPageFound(name));
   com.watabou.utils.Bundle before=new com.watabou.utils.Bundle(),after=new com.watabou.utils.Bundle();page.storeInBundle(before);page.info();page.storeInBundle(after);assertEquals(before.toString(),after.toString());
  }
 }
 @Test public void unknownAbilityExamplesUseOnlyPublicAugmentationAndTheLabelledZeroLevel()throws Exception{
  String[] names={"WornShortsword","Shortsword","Sword","Longsword","Greatsword","Cudgel","HandAxe","Mace","BattleAxe","WarHammer","Greataxe","Rapier","Katana","Spear","Glaive","Sickle","WarScythe","Whip"};
  for(String name:names)for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment augment:com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.values()){
   com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon weapon=(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee."+name).getDeclaredConstructor().newInstance();weapon.augment=augment;String expected=weapon.upgradeAbilityStat(0);String first=null;
   for(int hidden:new int[]{0,3,8}){weapon.level(hidden);weapon.levelKnown=false;String text=weapon.abilityInfo();assertTrue(name+" "+augment+" "+text,text.contains(expected));assertTrue(text.contains("Unupgraded example (+0)"));assertTrue(name+" "+augment+" ordinary stats: "+weapon.info(),weapon.info().contains("_"+augment.damageFactor(weapon.min(0))+"-"+augment.damageFactor(weapon.max(0))+" damage_"));if(first!=null)assertEquals(first,text);first=text;}
  }
 }
 @Test public void cleaveAndHeavyBlowPreviewsRoundBaseAndBonusSeparately(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon[] weapons={new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Mace(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BattleAxe(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HandAxe(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarHammer(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier()};
  for(int i=0;i<weapons.length;i++)for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment augment:com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.values())for(int level:new int[]{0,1,3,8}){
   com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon weapon=weapons[i];weapon.identify();weapon.level(level);weapon.augment=augment;
   int[] base={5,7,3,5,5,3,4,6,4,6,15,8,12,9,5};
   float[] scale={1,1,1.5f,1.5f,1.5f,1,1,1,1.5f,1.5f,2,2,2.5f,2,1.5f};
   int bonus=augment.damageFactor(base[i]+Math.round(scale[i]*level));
   String expected=(augment.damageFactor(weapon.min())+bonus)+"-"+(augment.damageFactor(weapon.max())+bonus);
   assertTrue(weapon.getClass().getSimpleName()+" "+augment+" +"+level+" expected "+expected+" in "+weapon.abilityInfo(),weapon.abilityInfo().contains(expected));
   assertEquals(expected,weapon.upgradeAbilityStat(level));
  }
 }
 @Test public void pickaxeAbilityPreviewRoundsItsSeparateDamagePartsLikeTheActualAttack(){
  com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe pick=new com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe();
  com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab target=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab();
  for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment augment:com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.values())for(int level:new int[]{0,1,3,8}){
   pick.augment=augment;pick.level(level);int extra=pick.pierceDamageBoost(target);
   String expected=(pick.augment.damageFactor(pick.min())+extra)+"-"+(pick.augment.damageFactor(pick.max())+extra);
   assertTrue(augment+" level="+level+" expected "+expected+" in "+pick.abilityInfo(),pick.abilityInfo().contains(expected));
   assertEquals(expected,pick.upgradeAbilityStat(level));
  }
 }
 @Test public void forceStanceUpgradePreviewUsesTheSameRingBonusAsActualDamage(){
  hero.heroClass=HeroClass.DUELIST;RingOfForce force=new RingOfForce();force.anonymize();force.levelKnown=force.cursedKnown=true;hero.belongings.ring=force;force.activate(hero);
  RingOfForce.BrawlersStance stance=Buff.affect(hero,RingOfForce.BrawlersStance.class);stance.active=true;
  for(boolean cursed:new boolean[]{false,true})for(int strength:new int[]{10,18,22})for(int level:new int[]{0,1,3,8}){
   hero.STR=strength;force.cursed=cursed;force.level(level);int bonus=force.soloBuffedBonus();float tier=RingOfForce.unarmedTier(hero.STR());
   int added=Math.round(3+tier+bonus*(4+2*tier)/8f),minimum=RingOfForce.unarmedMinimum(hero.STR(),bonus)+added,maximum=RingOfForce.unarmedMaximum(hero.STR(),bonus)+added;
   assertEquals("Strength="+strength+" level="+level+" cursed="+cursed,minimum+"-"+maximum,force.upgradeStat3(level));
   Random.pushGenerator(771);try{for(int roll=0;roll<30;roll++){int actual=RingOfForce.damageRoll(hero);assertTrue(actual>=minimum&&actual<=maximum);}}finally{Random.popGenerator();}
  }
 }
 @Test public void elasticInspectionIsOneUsefulChanceAndDistanceWithoutAMathPanel(){
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip whip=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip();whip.level(3);whip.levelKnown=whip.cursedKnown=true;whip.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic());RingOfArcana arcana=ring(0);
  String text=whip.info();assertTrue(text,text.contains("58.75%"));assertTrue(text,text.contains("2 tiles"));assertEquals(text,1,text.split("58.75%",-1).length-1);assertFalse(text.contains("Arcana changes"));assertFalse(text.contains("activation chance"));assertFalse(text.contains("Accuracy: _100"));assertEquals("",MechanicsDetails.itemCalculations(whip));
  for(int a:new int[]{0,3,12})for(int l:new int[]{0,3,9}){arcana.level(a);whip.level(l);float chance=(l+1f)/(l+5f)*EnchantmentExplanation.knownPower(hero);String shown=whip.info();assertTrue(shown,shown.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",100*Math.min(1,chance))+"%"));assertTrue(shown,shown.contains(Math.round(2*Math.max(1,chance))+" tiles"));}
  whip.levelKnown=false;String unknown=whip.info();whip.level(22);assertEquals(unknown,whip.info());
 }
 @Test public void viscosityExplainsLaterDamageAndMatchesActualDeferralWithoutCoefficients(){
  com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor leather=new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor();leather.level(2);leather.levelKnown=leather.cursedKnown=true;leather.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity());RingOfArcana arcana=ring(0);
  String text=leather.info();assertTrue(text,text.contains("44.06%"));assertTrue(text,text.contains("10%"));assertTrue(text,text.contains("1 turn"));assertFalse(text.contains("defer power"));assertFalse(text.contains("qualifying"));assertFalse(text.contains("Arcana changes"));assertEquals("",MechanicsDetails.itemCalculations(leather));
  hero.sprite=new com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite(){@Override public void showStatus(int color,String message,Object...args){}};
  for(int a:new int[]{0,3,12}){
   arcana.level(a);float q=3f/8f*com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph.genericProcChanceMultiplier(hero);
   leather.glyph.proc(leather,new PlainStatusTarget(),hero,100);
   com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity.ViscosityTracker tracker=hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity.ViscosityTracker.class);
   int delayed=q>1?Math.round(100/q):(int)Math.ceil(100*q);assertEquals(q>1?0:100-delayed,tracker.deferDamage(100));
   assertEquals(delayed,hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity.DeferedDamage.class).totalIncomingDMG());
   hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity.DeferedDamage.class).detach();tracker.detach();
  }
 }
 @Test public void bloodVialDescriptionTracksMaximumHealthRatherThanHeroLevel(){
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood vial=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood();vial.identify();
  for(int level=0;level<=3;level++)for(int maximum:new int[]{20,85,140}){
   vial.level(level);hero.HT=maximum;hero.lvl=1;String first=vial.statsDesc();
   int cap=com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood.maxHealPerTurn(vial.buffedLvl());
   assertTrue(first,first.contains("_"+cap+" HP per turn_"));assertTrue(first.contains("maximum health"));assertFalse(first.contains("hero level"));
   hero.lvl=30;assertEquals(first,vial.statsDesc());
  }
 }
 @Test public void bloodVialShowsActualRuleAdjustedLimitWithoutInventingACatalogHero()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood vial=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood();vial.identify();assertTrue(vial.collect());hero.sprite=quietSprite();
  for(int level=0;level<=3;level++)for(int maximum:new int[]{20,85,140})for(int percent=0;percent<=200;percent+=25){
   com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),settings=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,percent).store(settings);all.put("apd_custom_rules",settings);Dungeon.apdRestoreCustomRules(all);
   vial.level(level);hero.HT=maximum;hero.HP=1;hero.apdCustomHealingFraction=.999f;int cap=(int)Math.ceil((double)com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood.maxHealPerTurn(level)*percent/100.0);
   if(percent==0){assertTrue(vial.statsDesc(),vial.statsDesc().contains("rules disable the healing"));assertFalse(vial.statsDesc().contains("more health"));}else assertTrue(vial.statsDesc(),vial.statsDesc().contains("_"+cap+" HP per turn_"));String detail=TrinketExplanation.details(vial);assertEquals(percent==0,detail.contains("Your current rules prevent health restoration"));assertTrue(detail,detail.contains("base limit is _"+com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood.maxHealPerTurn(level)+" HP per turn_"));assertEquals(.999f,hero.apdCustomHealingFraction,0);assertEquals(1,hero.HP);
   Healing actual=new Healing();actual.target=hero;actual.setHeal(10000,1,0,true);actual.act();assertEquals(cap,hero.HP-1);
  }
  Dungeon.hero=null;try{String catalog=vial.statsDesc();assertTrue(catalog.contains("maximum health"));assertFalse(catalog.contains("HP per turn"));vial.levelKnown=false;String unknown=vial.statsDesc();vial.level(0);assertEquals(unknown,vial.statsDesc());}finally{Dungeon.hero=hero;}
 }
 @Test public void shardDescriptionExplainsManualIdentificationAndScrollReadiness(){
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion shard=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion();shard.identify();
  for(int level=0;level<=3;level++){shard.level(level);String text=shard.statsDesc();assertTrue(text,text.contains("counting up to _"+(level+1)+"_"));assertTrue(text.contains("use the shard"));assertTrue(text.contains("Identify scroll makes selected gear ready"));assertFalse(text.contains("item(s)"));}
  hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();hero.belongings.weapon.levelKnown=false;armor.levelKnown=false;hero.belongings.ring=new RingOfMight();hero.belongings.ring.levelKnown=false;
  Class<com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.WandUseTracker> wand=com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.WandUseTracker.class;Class<com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.ThrownUseTracker> thrown=com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.ThrownUseTracker.class;
  assertEquals(1.6f,com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.lootChanceMultiplier(3),.00001f);Buff.affect(hero,wand);assertEquals(1.8f,com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.lootChanceMultiplier(3),.00001f);Buff.affect(hero,wand);assertEquals(1.8f,com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.lootChanceMultiplier(3),.00001f);Buff.affect(hero,thrown);
  for(int level=0;level<=3;level++)assertEquals(1+.2f*(level+1),com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion.lootChanceMultiplier(level),.00001f);
 }

 @Test public void petrifiedSeedOmitsZeroBonusAndHidesUnknownUpgrades(){
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.PetrifiedSeed seed=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.PetrifiedSeed();seed.identify();seed.level(0);
  assertTrue(seed.statsDesc().contains("25%"));assertFalse(seed.statsDesc().contains("0%"));assertFalse(seed.statsDesc().contains("more often"));
  for(int level=1;level<=3;level++){seed.level(level);String shown=seed.statsDesc();assertTrue(shown,shown.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("#.##",100*com.shatteredpixel.shatteredpixeldungeon.items.trinkets.PetrifiedSeed.stoneInsteadOfSeedChance(level))+"%"));assertTrue(shown.contains("more often"));}
  seed.levelKnown=false;String unknown=seed.statsDesc();seed.level(0);assertEquals(unknown,seed.statsDesc());
 }
 @Test public void carriedCloakDoesNotDescribeAnInfiniteRechargeTime()throws Exception{
  com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows cloak=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows();cloak.identify();
  java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge");charge.setAccessible(true);charge.setInt(cloak,0);
  hero.belongings.artifact=null;String text=ArtifactEconomyExplanation.summary(cloak,hero);assertTrue(text,text.contains("Equip"));assertFalse(text.contains("∞"));assertFalse(text.contains("Infinity"));
 }

 @Test public void frostWandDescriptionMatchesItsActualFrozenTargetNoEffect()throws Exception{
  descriptionSightArena();PlainStatusTarget target=new PlainStatusTarget();target.sprite=quietSprite();target.pos=hero.pos+1;
  while(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(target.pos)!=null)target.pos++;
  com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(target);
  try{
   Frost frozen=Buff.affect(target,Frost.class);int hp=target.HP;
   com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost();wand.identify();
   assertTrue(wand.statsDesc().contains("cannot harm frozen targets"));
   wand.onZap(new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos,target.pos,com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE));
   assertEquals(hp,target.HP);assertSame(frozen,target.buff(Frost.class));
  }finally{for(Buff buff:target.buffs())buff.detach();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(target);}
 }
 @Test public void transfusionReadyFreeHealingDisplaysZeroCostWithoutReducingHealingOrConsumingIt(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion();wand.identify();
  for(int level:new int[]{0,3,8})for(int maximum:new int[]{20,85,140}){
   hero.HT=maximum;wand.level(level);int healing=Math.round(maximum*.05f)+3*wand.previewEffectiveLevels()[0];
   com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();wand.storeInBundle(state);state.put("freecharge",true);wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion();wand.restoreFromBundle(state);
   String text=wand.statsDesc();assertTrue(text,text.contains("costs _0 HP_"));assertTrue(text,text.contains("restore _"+healing+" HP_"));
   com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();wand.storeInBundle(after);assertTrue(after.getBoolean("freecharge"));
   state.put("freecharge",false);wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion();wand.restoreFromBundle(state);assertTrue(wand.statsDesc().contains("costs _"+Math.round(maximum*.05f)+" HP_"));
  }
 }

 @Test public void timedPotionNormalDescriptionsUseActualDurationsAndKeepUnknownTypesHidden()throws Exception{
  Class<?>[] types={com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste.class,com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility.class,com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision.class,com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation.class,com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity.class,com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina.class,com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight.class,com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.class};
  float[] turns={Haste.DURATION,Invisibility.DURATION,MindVision.DURATION,Levitation.DURATION,BlobImmunity.DURATION,Stamina.DURATION,MagicalSight.DURATION,com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.Cleanse.DURATION};
  for(int i=0;i<types.length;i++){
   com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion potion=(com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion)types[i].getDeclaredConstructor().newInstance();potion.anonymize();
   assertTrue(potion.desc(),potion.desc().contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",turns[i])+" turns"));
  }
  com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion hidden=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste(){@Override public boolean isKnown(){return false;}};
  String unknown=hidden.desc();assertFalse(unknown,unknown.contains("turns"));assertFalse(unknown.contains("movement"));
 }

 @Test public void healingPotionDescriptionWarnsAboutActualPharmacophobiaWithoutRevealingUnknownType(){
  com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing healing=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing();healing.anonymize();
  assertTrue(healing.desc().contains("restores health"));Dungeon.challenges=Challenges.NO_HEALING;
  assertTrue(healing.desc().contains("poisons you instead of healing"));assertTrue(healing.desc().contains("cures existing ailments"));
  com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion hidden=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing(){@Override public boolean isKnown(){return false;}};
  assertFalse(hidden.desc().contains("Pharmacophobia"));assertFalse(hidden.desc().contains("poisons you"));
 }

 @Test public void earthenArmorNormalDescriptionMatchesItsActualBarkskinAndPreservesStrongerProtection(){
  com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfEarthenArmor potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfEarthenArmor();potion.anonymize();
  for(int level:new int[]{1,12,30}){
   for(Barkskin old:hero.buffs(Barkskin.class))old.detach();hero.lvl=level;potion.apply(hero);
   assertTrue(potion.desc(),potion.desc().contains("_0-"+Barkskin.currentLevel(hero)+" extra damage_"));
  }
  hero.lvl=1;Barkskin.conditionallyAppend(hero,99,50);potion.apply(hero);assertEquals(99,Barkskin.currentLevel(hero));
 }
 @Test public void shieldingNormalDescriptionUsesExistingChallengeBehaviorAndNoStaleHeroValues(){
  com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding();potion.anonymize();
  Dungeon.challenges=Challenges.NO_HEALING;assertTrue(potion.desc().contains("poisons you instead of granting shielding"));
  Dungeon.hero=null;assertFalse(potion.desc().contains("Pharmacophobia"));assertFalse(potion.desc().contains("_22"));
  Dungeon.hero=hero;Dungeon.challenges=0;
 }

 @Test public void elixirDescriptionsUseLiveDurationsAndDistinguishChallengeAndNoHeroCases(){
  assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood().desc().contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",FireImbue.DURATION)+" turns"));
  String cold=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch().desc();assertTrue(cold.contains("chilling and freezing"));assertTrue(cold.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",FrostImbue.DURATION)+" turns"));
  Dungeon.challenges=Challenges.NO_HEALING;
  assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation().desc().contains("poisons you"));
  String honey=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing().desc();assertTrue(honey.contains("poisons you"));assertTrue(honey.contains("heal other creatures"));
  Dungeon.hero=null;
  assertFalse(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight().desc().contains("maximum HP_"));
  assertFalse(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation().desc().contains("Pharmacophobia"));
  Dungeon.hero=hero;
 }

 @Test public void allRegularAndExoticPotionDescriptionsKeepUnknownIdentityAndChallengeEffectsHidden()throws Exception{
  Object previous=com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.apdHandler();
  Random.pushGenerator(1991);try{com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.initColors();}finally{Random.popGenerator();}
  try{
   java.util.List<Class<?>> types=new java.util.ArrayList<>();java.util.Collections.addAll(types,com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.POTION.classes);types.addAll(com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion.exoToReg.keySet());
   for(Class<?> type:types){
    com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion potion=(com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion)type.getDeclaredConstructor().newInstance();
    assertFalse(type.getSimpleName(),potion.isKnown());
    Class<?> family=potion instanceof com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion?com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion.class:com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.class;
    String expected=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(family,"unknown_desc");
    assertEquals(type.getSimpleName(),expected,potion.desc());Dungeon.challenges=Challenges.NO_HEALING;assertEquals(type.getSimpleName(),expected,potion.desc());Dungeon.challenges=0;
   }
   assertEquals(24,types.size());
  }finally{com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.apdHandler(previous);}
 }

 @Test public void passageDescriptionUsesTheExistingRegionBoundariesWithoutLeakingUnknownIdentity(){
  int depth=Dungeon.depth;Object previousHandler=com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.apdHandler();Random.pushGenerator(9109);try{com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.initLabels();}finally{Random.popGenerator();}
  try{com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage known=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage();known.anonymize();com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage hidden=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage();assertFalse(hidden.isKnown());
   int[] expected={1,1,1,1,1,1,6,6,6,6,6,11,11,11,11,11,16,16,16,16,16,21,21,21,21,21};
   for(int floor=1;floor<=26;floor++){Dungeon.depth=floor;String state=savedDescriptionHero();assertEquals(expected[floor-1],com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage.destinationDepth(floor));assertTrue(known.desc(),known.desc().contains("Floor "+expected[floor-1]));assertFalse(hidden.desc().contains("Floor"));assertEquals(state,savedDescriptionHero());}
   Dungeon.hero=null;assertFalse(known.desc().contains("Floor"));
  }finally{Dungeon.depth=depth;Dungeon.hero=hero;com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.apdHandler(previousHandler);}
 }
 @Test public void lullabyZeroHealingDescriptionMatchesActualHeroAndAllySleep()throws Exception{
  hero.sprite=quietSprite();hero.HP=10;hero.HT=20;com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby scroll=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby();scroll.anonymize();assertFalse(scroll.desc().contains("prevent restorative sleep"));
  com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);
  String before=savedDescriptionHero();float fraction=hero.apdCustomHealingFraction;assertTrue(scroll.desc(),scroll.desc().contains("prevent restorative sleep for you"));assertTrue(scroll.desc().contains("allies heal"));assertEquals(before,savedDescriptionHero());assertEquals(fraction,hero.apdCustomHealingFraction,0);
  Drowsy drowsy=Buff.affect(hero,Drowsy.class,Drowsy.DURATION);drowsy.act();assertNull(hero.buff(MagicalSleep.class));assertEquals(10,hero.HP);
  PlainStatusTarget ally=new PlainStatusTarget();ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;ally.HP=5;ally.HT=20;ally.sprite=quietSprite();Buff.affect(ally,Drowsy.class,Drowsy.DURATION).act();MagicalSleep sleeping=ally.buff(MagicalSleep.class);assertNotNull(sleeping);sleeping.act();assertEquals(6,ally.HP);sleeping.detach();
  com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby hidden=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby(){@Override public boolean isKnown(){return false;}};assertFalse(hidden.desc().contains("restorative"));Dungeon.hero=null;try{assertFalse(scroll.desc().contains("Your current rules"));}finally{Dungeon.hero=hero;}
 }
 @Test public void identifyScrollNormalTextMatchesTheRealShardReadinessPath(){
  com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify scroll=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify();scroll.anonymize();assertFalse(scroll.desc().contains("ready"));
  com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion shard=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion();hero.belongings.backpack.items.add(shard);
  String text=scroll.desc();assertTrue(text,text.contains("equipment becomes ready"));
  com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();assertFalse(weapon.isIdentified());
  com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify.IDItem(weapon);assertTrue(weapon.readyToIdentify());assertFalse(weapon.isIdentified());
  shard.level(3);assertEquals(text,scroll.desc());
  com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll hidden=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify(){@Override public boolean isKnown(){return false;}};assertFalse(hidden.desc().contains("Shard"));assertFalse(hidden.desc().contains("ready"));
  hero.belongings.backpack.items.remove(shard);assertFalse(scroll.desc().contains("ready"));
  com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify.IDItem(weapon);assertTrue(weapon.isIdentified());
 }
 @Test public void allRegularAndExoticScrollDescriptionsKeepUnknownIdentityHidden()throws Exception{
  Object previous=com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.apdHandler();
  Random.pushGenerator(1992);try{com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.initLabels();}finally{Random.popGenerator();}
  try{
   java.util.List<Class<?>> types=new java.util.ArrayList<>();java.util.Collections.addAll(types,com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.SCROLL.classes);types.addAll(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll.exoToReg.keySet());
   for(Class<?> type:types){
    com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll scroll=(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll)type.getDeclaredConstructor().newInstance();
    if(type==com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfGathering.class||type==com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfForwarding.class){assertTrue("Approved fixed-name prototypes remain always known",scroll.isKnown());assertFalse(scroll.desc().contains("Who knows"));continue;}
    assertFalse(type.getSimpleName(),scroll.isKnown());
    Class<?> family=scroll instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll?com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll.class:com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.class;
    assertEquals(type.getSimpleName(),com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(family,"unknown_desc"),scroll.desc());
   }
  }finally{com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.apdHandler(previous);}
 }

 @Test public void disabledHealingDescriptionsMatchRealZeroHealingWithoutDisablingShielding()throws Exception{
  com.watabou.utils.Bundle state=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);state.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(state);
  hero.sprite=quietSprite();hero.HP=10;hero.HT=100;
  com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing();potion.anonymize();assertTrue(potion.desc().contains("disable healing"));potion.apply(hero);hero.buff(Healing.class).act();assertEquals(10,hero.HP);
  com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation aqua=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation();assertTrue(aqua.desc().contains("prevent this elixir"));aqua.apply(hero);Dungeon.level.water[hero.pos]=true;com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing stored=hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing.class);int reserve=stored.remainingHealingPool();assertTrue(stored.desc().contains("rules prevent healing"));assertTrue(stored.desc().contains("reserve is still spent"));stored.act();assertEquals(reserve-2,stored.remainingHealingPool());assertEquals(10,hero.HP);
  assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing().desc().contains("disable your healing"));com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing honey=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing();Hunger hunger=Buff.affect(hero,Hunger.class);java.lang.reflect.Field appetite=Hunger.class.getDeclaredField("level");appetite.setAccessible(true);appetite.setFloat(hunger,300);Buff.affect(hero,Weakness.class,3);Buff.affect(hero,Cripple.class,3);honey.apply(hero);assertNull(hero.buff(Weakness.class));assertNull(hero.buff(Cripple.class));assertEquals(150,hunger.hungerPoints(),0);assertEquals(10,hero.HP);
  com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit fruit=new com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit();fruit.imbuePotion(new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing());assertTrue(BlandfruitExplanation.summary(fruit).contains("disable healing"));
  com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding shield=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding();shield.anonymize();shield.apply(hero);assertEquals(70,hero.buff(Barrier.class).shielding());assertTrue(shield.desc().contains("70 shielding"));assertFalse(shield.desc().contains("disable"));
 }

 @Test public void bulkNormalDescriptionMatchesActualDoorwaySpeedAndHidesUnknownLevels()throws Exception{
  hero.sprite=quietSprite();armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Bulk());armor.levelKnown=armor.cursedKnown=true;hero.STR=50;Dungeon.level.map[hero.pos]=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.DOOR;RingOfArcana arcana=ring(0);
  for(int level:new int[]{0,3,12}){
   arcana.level(level);float speed=hero.speed();com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph curse=armor.glyph;armor.glyph=null;float normal=hero.speed();armor.glyph=curse;
   String text=armor.info();assertTrue(text,text.contains(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",100*speed/normal)+"%"));assertTrue(text.contains("normal speed in doorways"));
  }
  armor.level(-1);assertEquals(1,hero.speed(),.00001f);assertTrue(armor.info().contains("currently has no effect"));
  armor.levelKnown=false;String unknown=armor.info();armor.level(9);assertEquals(unknown,armor.info());
 }
 @Test public void metabolismNormalDescriptionMatchesFoodSpentWithoutHealingUnderZeroRule()throws Exception{
  com.watabou.utils.Bundle state=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);state.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(state);
  armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Metabolism());armor.cursedKnown=true;hero.sprite=quietSprite();hero.HP=10;hero.HT=20;Hunger hunger=Buff.affect(hero,Hunger.class);
  assertTrue(armor.info().contains("current rules prevent the healing"));
  int seed=0;for(;seed<10000;seed++){Random.pushGenerator(seed);float value;try{value=Random.Float();}finally{Random.popGenerator();}if(value<1/6f)break;}assertTrue(seed<10000);
  Random.pushGenerator(seed);try{assertEquals(7,armor.glyph.proc(armor,null,hero,7));}finally{Random.popGenerator();}
  assertEquals(10,hero.HP);assertEquals(40,hunger.hungerPoints(),.00001f);
 }

 @Test public void transfusionWithoutALoadedHeroDoesNotInventAHealthCost(){
  com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion();wand.identify();
  Dungeon.hero=null;String text=wand.statsDesc();assertTrue(text.contains("heal an ally"));assertFalse(text.contains("costs _1"));assertFalse(text.contains("restore _1"));Dungeon.hero=hero;
 }

}
