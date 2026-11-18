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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.*;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Equipped, identified ring contributions. No equip simulation, damage rolls or buff-consuming queries. */
public final class RingItemEffects {
    private RingItemEffects(){}

    /** Public-value numerical authority for description helpers, with the same privacy guards. */
    public static Integer knownBonus(Hero hero,Class<? extends Ring> type,boolean buffed){
        if(hero==null)return null;
        // Suppressed rings contribute a known zero, even when their properties are unknown.
        if(hero.buff(MagicImmune.class)!=null)return 0;
        for(Item worn:new Item[]{hero.belongings.ring(),hero.belongings.misc()})if(worn instanceof Ring&&!worn.isIdentified())return null;
        SpiritForm.SpiritFormBuff spirit=hero.buff(SpiritForm.SpiritFormBuff.class);
        if(spirit!=null&&!spirit.previewRingIdentified())return null;
        return bonus(hero,type,buffed);
    }
    private static int bonus(Hero hero, Class<? extends Ring> type, boolean buffed) {
        int total=0;
        for (Item item : new Item[]{hero.belongings.ring(),hero.belongings.misc()}) {
            if (type.isInstance(item)) total+=buffed?((Ring)item).soloBuffedBonus():((Ring)item).soloBonus();
        }
        SpiritForm.SpiritFormBuff spirit=hero.buff(SpiritForm.SpiritFormBuff.class);
        if (total==0 && spirit!=null) total=spirit.previewRingBonus(hero,type,buffed);
        return total;
    }
    private static int bonus(Hero hero, Class<? extends Ring> type){return bonus(hero,type,true);}
    private static String text(String key,Object...values){return Messages.get(RingItemEffects.class,key,values);}
    private static String number(float value){return Messages.decimalFormat(Math.abs(value)>=1000000?"0.##E0":"0.##",value);}
    private static void multiplier(StringBuilder out,String key,float value){out.append("\n").append(text(key,number(value)));}

    private static boolean aliveForInspection(Hero hero){
        if(hero.HP>0)return true;
        Berserk rage=hero.buff(Berserk.class);
        return rage!=null && rage.activeForInspection();
    }

    public static String describe(Item item){
        if(item instanceof Weapon)
            return com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->describeInContext(item));
        return describeInContext(item);
    }
    private static String describeInContext(Item item) {
        Hero hero=Dungeon.hero;
        if (hero==null || !aliveForInspection(hero) || hero.buff(MagicImmune.class)!=null || item instanceof Ring) return "";
        if (!(item instanceof Weapon || item instanceof Armor || item instanceof Wand || item instanceof Artifact)) return "";
        if ((item instanceof Weapon || item instanceof Armor) && !item.levelKnown) return "";
        // Even suppressing only one unknown type would reveal it through the presence/absence of a line.
        for(Item worn:new Item[]{hero.belongings.ring(),hero.belongings.misc()}){
            if(worn instanceof Ring && !worn.isIdentified())return "";
        }
        SpiritForm.SpiritFormBuff spirit=hero.buff(SpiritForm.SpiritFormBuff.class);
        if(spirit!=null && !spirit.previewRingIdentified())return "";
        StringBuilder out=new StringBuilder();
        if(item instanceof Weapon){
            Weapon weapon=(Weapon)item;
            int aim=bonus(hero,RingOfSharpshooting.class);
            if(aim!=0 && weapon instanceof MissileWeapon){
                int level=weapon.buffedLvl();
                out.append("\n").append(text("sharpshooting",weapon.augment.damageFactor(Math.max(0,weapon.min(level))),
                        weapon.augment.damageFactor(Math.max(0,weapon.max(level))),
                        weapon.augment.damageFactor(Math.max(0,weapon.min(level+aim))),
                        weapon.augment.damageFactor(Math.max(0,weapon.max(level+aim)))));
                // Rot tips override the ordinary wear calculation: always five uses.
                if(!(weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart)
                        && ((MissileWeapon)weapon).previewDurabilityPerUse()>0)
                    multiplier(out,"durability",RingOfSharpshooting.durabilityMultiplier(bonus(hero,RingOfSharpshooting.class,false)));
            } else if(aim!=0 && weapon instanceof SpiritBow){
                // Bow scaling differs from missile levels; use its own pure damage projection.
                SpiritBow bow=(SpiritBow)weapon;
                out.append("\n").append(text("sharpshooting",weapon.augment.damageFactor(bow.previewMin(0)),
                        weapon.augment.damageFactor(bow.previewMax(0)),weapon.augment.damageFactor(bow.previewMin(aim)),
                        weapon.augment.damageFactor(bow.previewMax(aim))));
            }
            if(bonus(hero,RingOfAccuracy.class)!=0)multiplier(out,"accuracy",RingOfAccuracy.accuracyMultiplier(bonus(hero,RingOfAccuracy.class)));
            if(bonus(hero,RingOfFuror.class)!=0)multiplier(out,"attack_speed",RingOfFuror.attackSpeedMultiplier(bonus(hero,RingOfFuror.class)));
            int force=bonus(hero,RingOfForce.class);
            if(force!=0 && !(weapon instanceof MissileWeapon) && !(weapon instanceof SpiritBow)){
                RingOfForce.BrawlersStance stance=hero.buff(RingOfForce.BrawlersStance.class);
                if(stance!=null && stance.active)out.append("\n").append(text("brawler"));
                else out.append("\n").append(text("force",weapon.augment.damageFactor(weapon.min()),
                        weapon.augment.damageFactor(weapon.max()),Math.max(0,weapon.augment.damageFactor(weapon.min())+force),
                        Math.max(0,weapon.augment.damageFactor(weapon.max())+force)));
            }
        }
        if(item instanceof Weapon || item instanceof Armor){
            int might=bonus(hero,RingOfMight.class,false);
            if(might!=0)out.append("\n").append(text("might",hero.previewStrength(0),hero.previewStrength(might)));
        }
        if(item instanceof Armor){
            if(bonus(hero,RingOfEvasion.class)!=0)multiplier(out,"evasion",RingOfEvasion.evasionMultiplier(bonus(hero,RingOfEvasion.class)));
            if(bonus(hero,RingOfHaste.class)!=0)multiplier(out,"movement",RingOfHaste.speedMultiplier(bonus(hero,RingOfHaste.class)));
            if(bonus(hero,RingOfElements.class)!=0)multiplier(out,"elements",RingOfElements.elementalMultiplier(bonus(hero,RingOfElements.class)));
            if(bonus(hero,RingOfTenacity.class)!=0 && hero.HT>0)multiplier(out,"tenacity",RingOfTenacity.damageMultiplier(bonus(hero,RingOfTenacity.class),(float)(hero.HT-hero.HP)/hero.HT));
        }
        int energy=bonus(hero,RingOfEnergy.class);
        if(energy!=0){
            float charge=RingOfEnergy.ringChargeMultiplier(energy);
            if(item instanceof Wand || item instanceof MagesStaff && ((MagesStaff)item).wandClass()!=null)multiplier(out,"wand_charge",charge);
            if(item instanceof ClassArmor)multiplier(out,"armor_charge",charge);
            if(energyArtifact(item))multiplier(out,"artifact_charge",charge);
        }
        return out.length()==0?"":"\n\n"+text("title")+out;
    }

    private static boolean energyArtifact(Item item){
        return item instanceof AlchemistsToolkit || item instanceof CloakOfShadows || item instanceof DriedRose
                || item instanceof EtherealChains || item instanceof HolyTome || item instanceof HornOfPlenty
                || item instanceof MasterThievesArmband || item instanceof SandalsOfNature || item instanceof SkeletonKey
                || item instanceof TalismanOfForesight || item instanceof TimekeepersHourglass || item instanceof UnstableSpellbook;
    }
}
