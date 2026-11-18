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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.*;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.messages.*;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;

public class RingItemEffectsTest {
    private Hero oldHero;
    private Hero hero;
    private ThrowingKnife knife;
    private ClothArmor armor;
    @Before public void setup(){HeadlessTestSupport.start();Messages.setup(Languages.ENGLISH);Notes.reset();oldHero=Dungeon.hero;hero=new Hero();hero.HP=10;hero.HT=20;Dungeon.hero=hero;knife=new ThrowingKnife();knife.levelKnown=true;armor=new ClothArmor();armor.levelKnown=true;}
    @After public void cleanup(){Dungeon.hero=oldHero;}
    private <T extends Ring> T wear(T r,int level){r.anonymize();r.level(level);r.levelKnown=true;r.cursedKnown=true;hero.belongings.ring=r;r.activate(hero);return r;}
    private String formatted(float v){return Messages.decimalFormat("0.##",v);}
    @Test public void sharpshootingShowsActualKnifeDamageBeforeAndAfter(){wear(new RingOfSharpshooting(),2);String s=RingItemEffects.describe(knife);assertTrue(s,s.contains("2-6 → 5-12"));assertEquals(5,knife.min());assertEquals(12,knife.max());assertTrue(s,s.contains(formatted(RingOfSharpshooting.durabilityMultiplier(hero))));}
    @Test public void rotDartShowsRingDamageButNeverAnInapplicableDurabilityBonus(){
        RingOfSharpshooting ring=wear(new RingOfSharpshooting(),0);
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart();dart.levelKnown=dart.cursedKnown=true;
        for(boolean cursed:new boolean[]{false,true})for(int level:new int[]{0,3,9}){
            ring.level(level);ring.cursed=cursed;Bundle before=new Bundle(),after=new Bundle();dart.storeInBundle(before);float wear=dart.previewDurabilityPerUse(0);String text=RingItemEffects.describe(dart);dart.storeInBundle(after);
            assertEquals(text,ring.soloBuffedBonus()!=0,text.contains("Sharpshooting damage"));assertFalse(text,text.contains("Sharpshooting durability"));assertEquals(20,wear,0);assertEquals(wear,dart.previewDurabilityPerUse(9),0);assertEquals(before.toString(),after.toString());
        }
    }
    @Test public void combinedAndEnhancedSharpshootingUseActualEffectiveBonus(){RingOfSharpshooting a=wear(new RingOfSharpshooting(),0);RingOfSharpshooting b=new RingOfSharpshooting();b.anonymize();b.levelKnown=true;b.cursedKnown=true;hero.belongings.misc=b;b.activate(hero);Buff.affect(hero,EnhancedRings.class);String s=RingItemEffects.describe(knife);int bonus=RingOfSharpshooting.levelDamageBonus(hero);assertEquals(4,bonus);assertTrue(s,s.contains("2-6 → "+knife.min()+"-"+knife.max()));assertTrue(s,s.contains(formatted(RingOfSharpshooting.durabilityMultiplier(hero))));}
    @Test public void cursedRingReductionMatchesActualDamageAndDoesNotGoNegative(){RingOfSharpshooting r=wear(new RingOfSharpshooting(),0);r.cursed=true;String s=RingItemEffects.describe(knife);assertTrue(s,s.contains("2-6 → "+knife.min()+"-"+knife.max()));assertEquals(0,knife.min());assertEquals(2,knife.max());}
    @Test public void weaponAugmentIsAppliedToBothRanges(){wear(new RingOfSharpshooting(),0);knife.augment=Weapon.Augment.DAMAGE;String s=RingItemEffects.describe(knife);assertTrue(s,s.contains(knife.augment.damageFactor(knife.min(0))+"-"+knife.augment.damageFactor(knife.max(0))+" → "+knife.augment.damageFactor(knife.min())+"-"+knife.augment.damageFactor(knife.max())));}
    @Test public void bowUsesItsOwnScalingRatherThanMissileUpgradeFormula(){wear(new RingOfSharpshooting(),2);SpiritBow bow=new SpiritBow();bow.levelKnown=true;String s=RingItemEffects.describe(bow);assertTrue(s,s.contains(bow.previewMin(0)+"-"+bow.previewMax(0)+" → "+bow.min()+"-"+bow.max()));assertFalse(s,s.contains("durability multiplier"));}
    @Test public void unknownRingOrEquipmentCannotExposeHiddenProperties(){wear(new RingOfSharpshooting(),9).levelKnown=false;assertEquals("",RingItemEffects.describe(knife));hero.belongings.ring.levelKnown=true;hero.belongings.misc=new RingOfHaste();assertEquals("",RingItemEffects.describe(knife));hero.belongings.misc=null;knife.levelKnown=false;assertEquals("",RingItemEffects.describe(knife));}
    @Test public void magicImmunityAndBackpackOnlyRingsShowNoActiveContribution(){hero.belongings.backpack.items.add(new RingOfSharpshooting());assertEquals("",RingItemEffects.describe(knife));wear(new RingOfSharpshooting(),1);Buff.affect(hero,MagicImmune.class);assertEquals("",RingItemEffects.describe(knife));}
    @Test public void accuracyAndFurorMatchGameplayMultipliersWithoutClaimingHitChance(){wear(new RingOfAccuracy(),1);String s=RingItemEffects.describe(knife);assertTrue(s,s.contains(formatted(RingOfAccuracy.accuracyMultiplier(hero))));assertTrue(s,s.contains("Accuracy"));assertTrue(s,s.contains("× normal"));hero=new Hero();Dungeon.hero=hero;wear(new RingOfFuror(),2);s=RingItemEffects.describe(knife);assertTrue(s,s.contains(formatted(RingOfFuror.attackSpeedMultiplier(hero))));}
    @Test public void forceAddsOnlyArmedMeleeDamageAndDoesNotInventMissileDamage(){wear(new RingOfForce(),1);Sword sword=new Sword();sword.levelKnown=true;String s=RingItemEffects.describe(sword);int force=RingOfForce.armedDamageBonus(hero);assertFalse(s.contains("Other buffs"));assertFalse(s.contains("Combined identified"));assertTrue(s.length()<160);assertTrue(s,s.contains(sword.min()+"-"+sword.max()+" → "+(sword.min()+force)+"-"+(sword.max()+force)));assertFalse(RingItemEffects.describe(knife).contains("Force,"));}
    @Test public void mightStrengthPreviewMatchesActualAndDoesNotChangeItemRequirement(){wear(new RingOfMight(),1);int req=knife.STRReq();String s=RingItemEffects.describe(knife);assertTrue(s,s.contains(hero.previewStrength(0)+" → "+hero.STR()));assertEquals(req,knife.STRReq());}
    @Test public void defensiveRingReadoutsUseCurrentConditionalGameplayValues(){wear(new RingOfTenacity(),2);String s=RingItemEffects.describe(armor);assertTrue(s,s.contains(formatted(RingOfTenacity.damageMultiplier(hero))));assertTrue(s,s.contains("current HP"));hero.HP=hero.HT;assertTrue(RingItemEffects.describe(armor).contains("_1× normal_"));}
    @Test public void hasteEvasionAndElementsUseAuthoritativeMappings(){wear(new RingOfHaste(),2);assertTrue(RingItemEffects.describe(armor).contains(formatted(RingOfHaste.speedMultiplier(hero))));hero=new Hero();Dungeon.hero=hero;wear(new RingOfEvasion(),1);assertTrue(RingItemEffects.describe(armor).contains(formatted(RingOfEvasion.evasionMultiplier(hero))));hero=new Hero();Dungeon.hero=hero;wear(new RingOfElements(),1);assertTrue(RingItemEffects.describe(armor).contains(formatted(RingOfElements.resist(hero,Burning.class))));}
    @Test public void energyOnlyListsArtifactsWhoseChargingActuallyUsesEnergy(){wear(new RingOfEnergy(),0);assertTrue(RingItemEffects.describe(new WandOfMagicMissile()).contains("wand recharge"));assertTrue(RingItemEffects.describe(new CloakOfShadows()).contains("artifact recharge"));assertEquals("",RingItemEffects.describe(new ChaliceOfBlood()));assertEquals("",RingItemEffects.describe(new CapeOfThorns()));}
    @Test public void wealthDoesNotPretendToChangeExistingItemStats(){wear(new RingOfWealth(),2);assertEquals("",RingItemEffects.describe(knife));assertEquals("",RingItemEffects.describe(armor));}
    @Test public void ringDescriptionsAreNotGivenTheAffectedItemBlock(){Ring r=wear(new RingOfSharpshooting(),1);assertEquals("",RingItemEffects.describe(r));assertFalse(r.info().contains("Equipped ring effects"));}
    @Test public void itemInfoActuallyIncludesBreakdown(){wear(new RingOfSharpshooting(),0);String info=knife.info();assertTrue(info,info.contains("Equipped ring effects"));assertTrue(info,info.contains("2-6 → 3-8"));}
    @Test public void openingDescriptionsDoesNotRollDamageConsumeBuffsOrModifyItem(){wear(new RingOfSharpshooting(),1);Bundle before=new Bundle();knife.storeInBundle(before);float time=hero.cooldown();int buffs=hero.buffs().size();Random.pushGenerator(777);long expected=Random.Long();Random.popGenerator();Random.pushGenerator(777);try{for(int i=0;i<5;i++)RingItemEffects.describe(knife);assertEquals(expected,Random.Long());}finally{Random.popGenerator();}Bundle after=new Bundle();knife.storeInBundle(after);assertEquals(before.toString(),after.toString());assertEquals(time,hero.cooldown(),0);assertEquals(buffs,hero.buffs().size());}
    @Test public void spiritFormProjectionDoesNotRewriteStoredEffectLevel(){RingOfSharpshooting effect=new RingOfSharpshooting();effect.anonymize();effect.level(9);effect.levelKnown=true;effect.cursedKnown=true;SpiritForm.SpiritFormBuff spirit=Buff.affect(hero,SpiritForm.SpiritFormBuff.class);spirit.setEffect(effect);Bundle before=new Bundle();effect.storeInBundle(before);RingItemEffects.describe(knife);Bundle after=new Bundle();effect.storeInBundle(after);assertEquals(before.toString(),after.toString());}
}
