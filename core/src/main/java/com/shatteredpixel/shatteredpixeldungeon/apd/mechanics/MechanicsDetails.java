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
package com.shatteredpixel.shatteredpixeldungeon.apd.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import java.util.Locale;

/** Public-value explanations; never asks for a damage roll, hidden enemy stat, or proc. */
public final class MechanicsDetails {
    private MechanicsDetails(){}
    private static float holdFastDecay(com.shatteredpixel.shatteredpixeldungeon.actors.Char target){
        if(target instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero){
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero owner=(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero)target;HoldFast hold=owner.buff(HoldFast.class);
            if(hold!=null&&owner.pos==hold.pos){int points=owner.pointsInTalent(Talent.HOLD_FAST);return points==1?.5f:points==2?.25f:points==3?0:1;}
        }
        return 1;
    }
    public static String glossary(String description){
        String text=description.toLowerCase(Locale.ROOT);StringBuilder out=new StringBuilder();
        if(text.contains("accuracy")||text.contains("evasion"))out.append("_Accuracy and evasion_\nThese are competing scores, not hit/dodge percentages. A normal attack rolls a value from zero up to the attacker's final accuracy and another from zero up to the defender's final evasion. The attack hits when its roll is at least the defense roll.\nFor positive finite scores after applicable modifiers, the usual continuous-roll estimate is: if accuracy ≥ evasion, hit chance = 1 − evasion / (2 × accuracy); otherwise hit chance = accuracy / (2 × evasion). Guaranteed hits/dodges and other special rules override this normal contest. The game uses finite-precision rolls and rounded scores. The target and current effects matter, so a weapon's accuracy percentage is not a fixed chance to hit.\n\n");
        if(text.contains("shield"))out.append("_Shielding_\nShield points are temporary damage protection, separate from maximum HP. Damage which uses shielding consumes those points before health. A shield's own description determines its duration, decay, replacement or stacking; different shield sources need not follow the same rule. This is not a percentage reduction or permanent healing.\n\n");
        if(text.contains("turn")||text.contains("speed")||text.contains("recharg"))out.append("_Time and speed_\nTurns measure game time, not seconds on your phone. An ordinary wait uses one unit of game time; actions, talents and other effects can cost different amounts. A speed or recharge-rate factor multiplies that rate, so increasing a rate shortens the time needed for the same action or charge. Movement, attack speed and recharge are separate rates. The visual Game speed setting does not change these costs.\n\n");
        if(text.contains("chance")||text.contains("trigger"))out.append("_Conditional chances_\nA chance applies only when the effect's stated trigger and other requirements are met. It is not a guarantee against immunity or an unavailable target. Repeated checks are separate opportunities; a per-hit chance is not a per-second chance. Where an effect uses power beyond a guaranteed trigger, its own rule says what becomes stronger.\n\n");
        if(text.contains("damage"))out.append("_Damage numbers_\nDamage ranges are amounts of damage, not percentages or a promise of final HP loss. Equipment, the target's defenses, resistance, shielding and the specific effect's rules can change the result. A larger minimum improves weaker rolls; a larger maximum improves the upper end. Raw weapon damage, armor absorption and damage-over-time are different quantities.\n\n");
        if(text.contains("strength"))out.append("_Strength_\nStrength is a score measured in points. An equipment requirement is a minimum, not a percentage. Being below it can impose the penalties described by that equipment; excess strength only grants benefits where the relevant mechanic explicitly uses it.\n\n");
        if(text.contains("multiplier")||text.contains("×")||text.contains("1x"))out.append("_Added amounts and multiplied amounts_\nAn added bonus increases a score by that amount. A multiplier scales the stated quantity: a factor above one increases it, below one reduces it. Do not add two percentage bonuses together unless that effect's rule says to; different effects can combine at different stages. Caps and rounding belong to the particular effect.\n\n");
        return out.toString().trim();
    }
    /** Existing specific-effect panels retained for owner review; never the generic item/glossary dispatcher. */
    public static String itemCalculations(Item item){
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana)return "";
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce
                ||item instanceof com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity)
            return item.isIdentified()?RingExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring)item):"";
        // Owner explicitly retained existing artifact/trinket-specific panels pending review.
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact)
            return item.isIdentified()?ArtifactExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact)item):"";
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket)
            return item.isIdentified()?TrinketExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket)item):"";
        // Separate ward lifecycles and cumulative regrowth depletion are genuinely multi-stage.
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding
                ||item instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth)
            return item.isIdentified()?WandExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)item):"";
        // Health-dependent blast power and crowd-dependent self-damage are not routine scroll text.
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution
                ||item instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast){
            com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll scroll=(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll)item;
            return scroll.isKnown()?ScrollExplanation.details(scroll,Dungeon.hero):"";
        }
        // Ordinary armor glyphs explain their effect directly in the item description.
        // No duplicate technical calculation panel or Show calculations button.
        if(item instanceof Armor)return "";
        if(item instanceof Weapon){
            Weapon weapon=(Weapon)item;
            if(!weapon.levelKnown||weapon.enchantment==null||weapon.enchantment.curse()||weapon.enchantment instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic)return "";
            if(weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart)
                return com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->EnchantmentExplanation.describe(weapon,Dungeon.hero,true));
            return EnchantmentExplanation.describe(weapon,Dungeon.hero,true);
        }
        return "";
    }
    public static String item(Item item,String visibleDescription){
        if(item instanceof Weapon)
            return com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->itemInContext(item,visibleDescription));
        return itemInContext(item,visibleDescription);
    }
    private static String itemInContext(Item item,String visibleDescription){
        StringBuilder result=new StringBuilder();
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.food.Food)result.append(FoodExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.food.Food)item)).append("\n\n");
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)result.append(WandExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)item)).append("\n\n");
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact)result.append(ArtifactExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact)item)).append("\n\n");
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring)result.append(RingExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring)item)).append("\n\n");
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket)result.append(TrinketExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket)item)).append("\n\n");
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion)result.append(PotionExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion)item,Dungeon.hero)).append("\n\n");
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll)result.append(ScrollExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll)item,Dungeon.hero)).append("\n\n");
        result.append(UtilityItemExplanation.details(item,Dungeon.hero)).append("\n\n");
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb)result.append(BombExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb)item)).append("\n\n");
        result.append(CraftingEffectExplanation.details(item)).append("\n\n");
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed)result.append(PlantExplanation.details(((com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed)item).plantType(),Dungeon.hero)).append("\n\n");
        if(item instanceof Armor){
            Armor armor=(Armor)item;
            if(armor instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor && Dungeon.hero!=null && Dungeon.hero.armorAbility!=null)
                result.append(AbilityExplanation.details(Dungeon.hero.armorAbility,Dungeon.hero)).append("\n\n");
            result.append(GlyphExplanation.details(armor,Dungeon.hero)).append("\n\n").append(CurseExplanation.armor(armor,Dungeon.hero,true)).append("\n\n");
            result.append(ArmorExplanation.details(armor,Dungeon.hero)).append("\n\n");
        }
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff)
            result.append(((com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff)item).mechanicsDetails()).append("\n\n");
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon)result.append(WeaponAbilityExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon)item,Dungeon.hero)).append("\n\n");
        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon)result.append(MissileExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon)item,Dungeon.hero)).append("\n\n");
        if(item instanceof Weapon)result.append(WeaponExplanation.details((Weapon)item,Dungeon.hero)).append("\n\n").append(EnchantmentExplanation.describe((Weapon)item,Dungeon.hero,true)).append("\n\n").append(CurseExplanation.weapon((Weapon)item,Dungeon.hero,true)).append("\n\n");
        String common=glossary(visibleDescription);if(!common.isEmpty())result.append(common);
        return result.toString().trim();
    }
    private static String turns(float value){String amount=ObfuscationExplanation.number(value);return amount+(amount.equals("1")?" turn":" turns");}
    public static String buff(Buff buff,String visibleDescription){
        String extra=BuffExplanation.details(buff);
        if(buff instanceof Combo){
            Combo combo=(Combo)buff;int count=combo.getComboCount();
            extra+="\n\n_Combo count and remaining window_\nCombo: "+count+" hits. Time left: "+turns(combo.remainingComboTime())+" at the normal decay rate. Hold Fast currently reduces the countdown by "+ObfuscationExplanation.number(holdFastDecay(buff.target))+" each turn.\nA normal hit leaves at least five turns to continue the combo. Cleave replaces that time after a kill. Hits needed for each finisher: Clobber "+Combo.ComboMove.CLOBBER.comboReq+", Slam "+Combo.ComboMove.SLAM.comboReq+", Parry "+Combo.ComboMove.PARRY.comboReq+", Crush "+Combo.ComboMove.CRUSH.comboReq+", Fury "+Combo.ComboMove.FURY.comboReq+". Clobber and Parry each have their own already-used restriction.\nSlam adds 20% of a protection roll per combo hit, rounded to the nearest damage point. Crush uses 25% of normal attack damage per combo hit: currently "+ObfuscationExplanation.number(.25f*count)+" times normal damage. Fury attempts up to "+count+" strikes at 60% damage each, stopping if the enemy dies, leaves reach or you become paralysed. Defenses and other attack effects still apply.";
        }

        if(buff instanceof Talent.LiquidAgilACCTracker && Dungeon.hero!=null){
            extra+="\n\n_Remaining potion accuracy uses_\n"+((Talent.LiquidAgilACCTracker)buff).uses+" qualifying attack uses remain.\n"+TalentExplanation.details(Talent.LIQUID_AGILITY,Dungeon.hero.pointsInTalent(Talent.LIQUID_AGILITY));
        }
        if(buff instanceof Talent.PreciseAssaultTracker && Dungeon.hero!=null)extra+="\n\n"+TalentExplanation.details(Talent.PRECISE_ASSAULT,Dungeon.hero.pointsInTalent(Talent.PRECISE_ASSAULT));
        if(!extra.isEmpty())extra+="\n\n";
        if(buff instanceof Haste)extra+="_Haste_\nWalk three times as fast: each step takes one third of its usual time. Attacks and recharging are unchanged. Other movement bonuses still apply.\n\n";
        if(buff instanceof Slow)extra+="_Slow_\nActions take twice as long. Slow takes priority over Chill; their penalties do not stack. Speed can cancel this slowdown.\n\n";
        if(buff instanceof Speed)extra+="_Speed_\nActions take half their usual time. This can cancel Slow or reduce Chill’s penalty. Other movement and attack bonuses still apply.\n\n";
        if(buff instanceof Chill){Chill chill=(Chill)buff;extra+="_Chill_\nCurrent action speed: "+ObfuscationExplanation.number(100*chill.speedFactor())+"% of normal. Chill slows actions by 10% for each turn remaining, up to 50%; the penalty fades as it wears off. Slow takes priority if both are present.\n\n";}
        if(buff instanceof Weakness)extra+="_Weakness_\nAttacks deal 33% less damage before armor and rounding. Your strength and equipment requirements do not change.\n\n";
        if(buff instanceof Vulnerable)extra+="_Vulnerable_\nAttacks deal 33% more of the damage left after armor, rounded down. This does not increase every other source of damage.\n\n";
        if(buff instanceof Recharging){Recharging recharge=(Recharging)buff;extra+="_Extra wand charging_\nAdds "+ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.rechargingBonusPerTurn())+" of a charge per turn to each charging wand, on top of normal recharging. A partial final turn gives proportionally less: currently "+ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.rechargingBonusPerTurn()*Math.max(0,recharge.remainder()))+" of a charge. Full wands and magic immunity prevent charging.\n\n";}
        if(buff instanceof ArtifactRecharge){ArtifactRecharge recharge=(ArtifactRecharge)buff;extra+="_Extra artifact charging_\nRecharge time left: "+turns(Math.max(0,recharge.left()))+". Boosts each equipped, uncursed artifact using that artifact’s recharge rules. A partial final turn gives proportionally less. "+(recharge.ignoreHornOfPlenty?"The Horn of Plenty is excluded by this effect.":"The Horn of Plenty is not excluded by this effect.")+"\n\n";}
        if(buff instanceof ShieldBuff && !(buff instanceof com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield) && !(buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff))extra+="_Current shield_\n"+((ShieldBuff)buff).shielding()+" shield points from this effect.\n\n";
        if(buff instanceof Poison){Poison poison=(Poison)buff;extra+="_Poison_\nNext turn: "+poison.apdNextInput()+" damage points before protection and shields. Poison lasts another "+turns(poison.apdRemaining())+".\n\n";}
        if(buff instanceof Bleeding){Bleeding bleeding=(Bleeding)buff;extra+="_Bleeding_\nNext turn: "+bleeding.apdNextRaw()+" damage points before source-specific bonuses and protection. After each turn, bleeding randomly weakens to between half and its previous strength, usually near the middle. It ends when the damage rounds to zero. A new wound normally replaces weaker bleeding rather than adding to it.\n\n";}
        if(buff instanceof Barkskin)extra+="_Current Barkskin roll_\nAdds a protection roll from 0 to "+((Barkskin)buff).level()+" damage points on an applicable physical hit. This adds damage-blocking points rather than a percentage reduction.\n\n";
        if(buff instanceof ArcaneArmor)extra+="_Current Arcane Armor roll_\nAdds a protection roll from 0 to "+((ArcaneArmor)buff).level()+" damage points against qualifying magical damage, before the remaining defenses apply.\n\n";
        if(buff instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking.BlockBuff){
            com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking.BlockBuff block=(com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking.BlockBuff)buff;
            extra+="_Blocking shield duration_\nCountdown: "+turns(block.remainingDecay())+" at normal speed. Hold Fast currently reduces it by "+ObfuscationExplanation.number(holdFastDecay(buff.target))+" each turn. The shield expires at zero or when damage uses it up. Triggering Blocking again resets the countdown to five.\n\n";
        }
        if(buff instanceof Barrier){
            Barrier barrier=(Barrier)buff;float factor=holdFastDecay(buff.target);
            float rate=Math.min(1f,barrier.shielding()/20f)*factor;
            extra+="_Barrier decay_\nBarrier gradually fades, losing one shield point when its decay reaches 100%. Progress: "+ObfuscationExplanation.number(100*barrier.decayProgress())+"%; currently adds "+ObfuscationExplanation.number(100*rate)+"% each turn, including Hold Fast. Below 20 shielding, decay slows with the remaining shield. Damage can also use it up.\n\n";
        }
        boolean linkedPower=buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff
                &&((com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff)buff).isDurationExtendedByLink();
        if(linkedPower)extra+="_Extended by a link_\nBeaming Ray or Life Link keeps this effect active beyond its normal duration. Turns on its normal timer: "+ObfuscationExplanation.number(Math.max(0,buff.visualcooldown()))+".\n\n";
        if(buff instanceof FlavourBuff && !(buff instanceof Shadows) && !linkedPower && !extra.isEmpty()){
            String duration=ObfuscationExplanation.number(Math.max(0,buff.visualcooldown()));
            extra+="_Duration remaining_\n"+duration+(duration.equals("1")?" turn.":" turns.")+" Faster or slower actions can use part of a turn or several turns.\n\n";
        }
        return extra.trim();
    }
    public static String talent(Talent talent,int points,String visibleDescription){
        return talent(talent,points,visibleDescription,false);
    }
    public static String talent(Talent talent,int points,String visibleDescription,boolean alternateDescription){
        String specific=TalentExplanation.details(talent,points,alternateDescription);
        if(!specific.isEmpty())specific+="\n\n";
        if(talent==Talent.ENHANCED_RINGS)specific=points<=0?"_Enhanced Rings_\nNo effect until learned.\n\n":"_Enhanced Rings_\nUsing an artifact temporarily adds one effective ring level for "+(3*points)+" turns. This is not a permanent upgrade and does not increase Might’s strength bonus or Sharpshooting’s durability bonus.\n\n";
        if(talent==Talent.PROTECTIVE_SHADOWS)specific=points<=0?"_Protective Shadows_\nNo effect until learned.\n\n":"_Protective Shadows_\nWhile invisible, builds up to "+(1+2*points)+" shield points. After the first point, gains one point every "+(points==1?"two turns":"turn")+" while below that limit. Keeps Barrier from fading while active; stronger shields from other sources are not reduced.\n\n";
        if(talent==Talent.SILENT_STEPS)specific=points<=0?"_Silent Steps_\nNo effect until learned.\n\n":"_Silent Steps_\nSleeping enemies do not notice your footsteps from "+(4-points)+" or more tiles away. Also applies to your shadow clone. Other creatures or alarms can still wake them; this does not hide you from enemies already awake.\n\n";
        if(!alternateDescription&&points>0){String spell=SpellExplanation.talentDetails(talent,com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero);if(!spell.isEmpty())specific+=spell+"\n\n";}
        if(specific.isEmpty())return "";
        return "_Talent rank_\nCurrent rank: "+points+" / "+talent.maxPoints()+".\n\n"+specific;
    }
}
