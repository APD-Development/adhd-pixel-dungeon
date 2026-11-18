package com.shatteredpixel.shatteredpixeldungeon.apd;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.messages.*;
import org.junit.*;import static org.junit.Assert.*;
public class SpiritFormArcanaDisplayTest {
 private Hero previous,hero;private RingOfArcana physical,spiritual;private Sword sword;
 @Before public void setup(){
  HeadlessTestSupport.start();Messages.setup(Languages.ENGLISH);previous=Dungeon.hero;hero=new Hero();Dungeon.hero=hero;hero.HP=hero.HT=20;
  hero.heroClass=HeroClass.CLERIC;hero.armorAbility=new Trinity();Talent.initClassTalents(hero);Talent.initArmorTalents(hero);hero.talents.get(3).put(Talent.SPIRIT_FORM,2);
  physical=new RingOfArcana();physical.anonymize();physical.level(2);physical.cursed=true;physical.levelKnown=physical.cursedKnown=true;hero.belongings.ring=physical;physical.activate(hero);
  spiritual=new RingOfArcana();spiritual.level(10);Buff.affect(hero,SpiritForm.SpiritFormBuff.class).setEffect(spiritual);
  sword=new Sword();sword.enchant(new Blazing());sword.levelKnown=true;
 }
 @After public void restore(){Dungeon.hero=previous;}
 @Test public void zeroPhysicalBonusUsesCurrentSpiritArcanaWithoutMutatingItsCachedLevel(){
  assertEquals(0,physical.soloBuffedBonus());int before=spiritual.level();
  String description=ArcanaInfoDisplay.weapon(sword);
  assertEquals(before,spiritual.level());
  float actual=RingOfArcana.enchantPowerMultiplier(hero);
  String expected="+"+Messages.decimalFormat("0.##",100*(actual-1))+"%_ power";
  assertTrue(description,description.contains(expected));
 }
 @Test public void ordinaryPhysicalBonusKeepsCoreNonStackingRule(){
  physical.cursed=false;physical.level(0);
  String description=ArcanaInfoDisplay.weapon(sword);assertTrue(description,description.contains("+17.5%_ power"));
  assertEquals(1.175f,RingOfArcana.enchantPowerMultiplier(hero),0f);
 }
 @Test public void temporaryModifiersMatchCoreAndPreviewDoesNotConsumeRngOrResources(){
  for(int points=1;points<=4;points++)for(boolean enhanced:new boolean[]{false,true}){
   hero.talents.get(3).put(Talent.SPIRIT_FORM,points);
   if(enhanced)Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings.class);
   else Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings.class);
   spiritual.level(10);int hp=hero.HP,quantity=sword.quantity(),before=spiritual.level();float time=hero.cooldown();int expectedNext;
   com.watabou.utils.Random.pushGenerator(7737);try{expectedNext=com.watabou.utils.Random.Int();}finally{com.watabou.utils.Random.popGenerator();}
   String description;
   com.watabou.utils.Random.pushGenerator(7737);try{description=ArcanaInfoDisplay.weapon(sword);assertEquals(expectedNext,com.watabou.utils.Random.Int());}finally{com.watabou.utils.Random.popGenerator();}
   assertEquals(before,spiritual.level());assertEquals(hp,hero.HP);assertEquals(quantity,sword.quantity());assertEquals(time,hero.cooldown(),0f);
   float actual=RingOfArcana.enchantPowerMultiplier(hero);assertTrue(description,description.contains("+"+Messages.decimalFormat("0.##",100*(actual-1))+"%_ power"));
  }
  Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings.class);physical.level(1);spiritual.level(10);
  assertTrue(ArcanaInfoDisplay.weapon(sword).contains("-14.89%_ power"));assertEquals(10,spiritual.level());
 }
 @Test public void spiritFormDoesNotRevealUnknownOrUnequippedPhysicalRings(){
  physical.levelKnown=false;assertEquals("",ArcanaInfoDisplay.weapon(sword));
  hero.belongings.ring=null;assertEquals("",ArcanaInfoDisplay.weapon(sword));
 }
}
