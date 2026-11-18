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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.RingItemEffects;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;

/** Charge units and live cost; does not activate abilities or query attack/damage handlers. */
public final class AbilityExplanation {
    private AbilityExplanation() {}
    private static String n(float value) { return ObfuscationExplanation.number(value); }
    public static String summary(ArmorAbility ability, Hero hero) {
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity)
            return "Charge cost depends on the effect you choose. Each stored effect shows its own cost.";
        float cost=hero==null?ability.baseChargePoints():ability.chargeUse(hero);
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon&&hero!=null&&hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon.WarpBeaconTracker.class)==null)
            return "Placing a beacon takes 1 turn and spends no armor charge, but requires "+n(cost)+" charge available. Returning costs "+n(cost)+" charge before any Longrange Warp adjustment.";
        return (hero==null?"Base cost: ":"Current cost: ")+n(cost)+" charge points out of 100.";
    }
    public static String details(ArmorAbility ability, Hero hero) {
        boolean trinity=ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
        String result="_Armor charge_\n"+(trinity?"Each stored effect shows its own charge cost; there is no single cost for Trinity.":"Base cost: "+n(ability.baseChargePoints())+" charge points out of 100.");
        if(hero==null)return result;
        int rank=hero.pointsInTalent(Talent.HEROIC_ENERGY);
        if(rank>0)result+=" Heroic Energy reduces the cost by "+n(100*(1-ArmorAbility.heroicEnergyFactor(rank)))+"% at rank "+rank+".";
        if(!trinity)result+="\nCurrent cost: "+n(ability.chargeUse(hero))+" charge points, including active ability discounts.";
        Talent discount=null;double discountBase=.84;String discountName="";
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap){discount=Talent.DOUBLE_JUMP;discountName="Double Jump";}
        else if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge){discount=Talent.ELIMINATION_MATCH;discountName="Elimination Match";}
        else if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark){discount=Talent.DOUBLE_MARK;discountBase=.707;discountName="Double Mark";}
        if(discount!=null&&hero.hasTalent(discount))result+="\n"+discountName+": while its follow-up is ready, uses "+n(100*(float)Math.pow(discountBase,hero.pointsInTalent(discount)))+"% of the otherwise required charge ("+discountBase+" raised to talent rank). Already included in the current cost when active.";
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk||ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone)result+=" Directing an existing summon costs no armor charge; creating a replacement still spends charge.";
        Integer energy=RingItemEffects.knownBonus(hero,RingOfEnergy.class,true);
        result+="\n\n_Armor recharge_\nCharge returns only while natural recovery is allowed, up to 100 points.";
        if(energy==null)result+=" Base recharge: 0.2 points per turn. Identify your rings to preview the combined recharge rate.";
        else result+=" Current recharge: "+n(.2f*RingOfEnergy.ringChargeMultiplier(energy))+" charge points per turn, including known Energy bonuses.";
        return result+effects(ability,hero);
    }
    public static String effects(ArmorAbility ability,Hero hero){
        if(hero==null)return "";
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify){
            int resistance=hero.pointsInTalent(Talent.RATSISTANCE),diplomacy=hero.pointsInTalent(Talent.RATLOMACY),rats=hero.pointsInTalent(Talent.RATFORCEMENTS);
            String text="\n\n_Rat transformation_\nTurns a visible hostile monster into a rat until it has spent "+n(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify.TransmogRat.TRANSFORM_TIME)+" turns acting. This is the rat’s action time, not a fixed number of your actions. Casting takes one turn. Ordinary rats, bosses and minibosses cannot be transformed. The rat keeps the original’s HP, accuracy, evasion, armor and attack timing.\n\n_Your talents_\nHostile transformed rats deal about "+n(100*(float)Math.pow(.9f,resistance))+"% of their original damage (0.9 raised to Ratsistance rank), rounded down. Rounding can remove an extra damage point. This reduction ends if the rat becomes an ally.";
            text+="\nWith Ratlomacy, cast again on a hostile transformed rat to make it a lasting ally.";
            if(diplomacy>1)text+=" At your rank, it also gains "+(2*(diplomacy-1))+" turns of Adrenaline.";
            text+=rats>0?"\nTarget yourself to summon up to "+rats+" allied rats, limited by free, walkable neighboring tiles.":"\nLearn Ratforcements to summon allied rats by targeting yourself.";
            return text;
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity){
            int body=hero.pointsInTalent(Talent.BODY_FORM),mind=hero.pointsInTalent(Talent.MIND_FORM),spirit=hero.pointsInTalent(Talent.SPIRIT_FORM);
            float base=ability.chargeUse(hero);
            int duration=Math.round(13.33f+6.67f*body);
            return "\n\n_Stored effects and armor charge_\nBody, Mind and Spirit Tome spells choose your stored effects. Their Tome charges are separate from using Trinity. Standard effects cost _"+n(base)+" armor charge_. Rare enchantments/glyphs, Fireblast/Regrowth, Dried Rose, Unstable Spellbook and Skeleton Key cost _"+n(2*base)+"_ (twice normal). Chains, Talisman of Foresight and Hourglass cost _"+n(1.4f*base)+"_ (1.4 × normal). Heroic Energy is included.\n\n_Body_\nUses the chosen enchantment or glyph for _"+duration+" turns_; applying it takes _1 turn_. Cannot duplicate an effect already on your equipment. Crystal starts with _"+n(duration*1.5f)+"% durability_; thrown weapons keep using their own durability.\nDuration = round(13.33 + 6.67 × Body Form rank); Crystal durability = duration × 1.5.\n\n_Mind_\nUses a _+"+(2+mind)+" wand or thrown-weapon effect_ (2 + Mind Form rank). Its normal targeting, collisions and action time still apply. Your carried item is not upgraded.\n\n_Spirit_\nUses a _+"+spirit+" ring_ or _+"+(2+2*spirit)+" artifact_. Artifact levels use the normal 0–10 scale. Passive rings and Chalice last _"+n(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff.DURATION)+" turns_ and take _1 turn_ to apply; active artifacts use their own action time.\nRing level = Spirit Form rank; artifact level = 2 + 2 × rank.";
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike)return StrikeExplanation.details(hero);
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge){
            int gap=hero.pointsInTalent(Talent.CLOSE_THE_GAP),victory=hero.pointsInTalent(Talent.INVIGORATING_VICTORY);
            String result="\n\n_Challenge_\nStart a duel with a visible enemy or eligible neutral mimic, within _5 tiles_ after any Close the Gap movement. You cannot already be dueling. Casting spends armor charge and is _instant_.";
            if(gap>0)result+=" Close the Gap can move up to _"+(1+gap)+" legal steps_ toward the target; Roots prevents this movement.";
            result+="\nThe duel lasts up to _"+n(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge.DuelParticipant.DURATION)+" turns_. It ends early if either participant disappears, they become allies, or they move more than five tiles apart. Other creatures are normally delayed, except allies and NPCs. Challenging a boss leaves other creatures active. Hunger and other ongoing effects still apply.";
            if(victory>0)result+="\n\n_Invigorating Victory_\nWinning restores about _"+n(100*(1-(float)Math.pow(.707f,victory)))+"%_ of the HP and shielding you lost during the duel, plus _"+(5*victory)+" HP_, before healing modifiers and your missing-health limit.\nHealing = round(damage suffered × (1 − 0.707^rank)) + 5 × rank.";
            return result+"\nWinning can ready Elimination Match’s three-turn discount for your next Challenge.";
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint){
            int retreat=hero.pointsInTalent(Talent.FEIGNED_RETREAT),expose=hero.pointsInTalent(Talent.EXPOSE_WEAKNESS),counter=hero.pointsInTalent(Talent.COUNTER_ABILITY);
            String result="\n\n_Feint_\nStep onto an adjacent empty, non-solid tile, leaving a harmless image behind. Takes _1 turn_ and ends invisibility. Roots prevents the move. The image draws nearby active enemies and enemies already hunting you, then disappears just before your next action. Its swing deals no damage, and the image ignores incoming damage.\nWhen an enemy attacks the image, it loses its current target and is briefly confused.";
            if(retreat>0)result+=" Feigned Retreat grants _"+(2*retreat)+" turns of Haste_.";
            if(expose>0)result+=" Expose Weakness gives that enemy _"+(2*expose)+" turns each of Weakness and Vulnerable_, before resistance.";
            if(counter>0)result+="\n\n_Counter Ability_\nFor three turns, your next completed weapon ability can refund _"+n(.375f*counter)+" weapon-ability charges_ (0.375 × talent rank), up to its charge capacity. That ability consumes the opportunity.";
            return result;
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm){
            return "\n\n_Ascended Form_\nActivation takes _1 turn_, grants _30 shield points_ and resets the form to _"+n(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.DURATION)+" turns_. Keeps a stronger existing form shield. The form continues if its shield is used up.\nAdds _2 tiles_ to normal weapon reach. Unarmed attacks can reach through a legal path of up to _3 tiles_; walls and target restrictions still apply.\nTome casts that grant spell bonuses also add shielding equal to _10 × the spell’s charge cost_, rounded down, and build the cast count used by Judgement. Spells retain their own costs, action times and limits. Reapplying the form resets its duration but keeps its spell and Flash counters.";
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany){
            int link=hero.pointsInTalent(Talent.LIFE_LINK),beam=hero.pointsInTalent(Talent.BEAMING_RAY);
            return "\n\n_Powering an ally_\nEmpower a visible ally, or summon a light ally on a visible, empty, safe tile. Activation takes _1 turn_, grants _25 shielding_ and powers the ally for _"+n(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff.DURATION)+" turns_. Only one ally can be empowered, including one temporarily stored or withdrawn.\n\n_Attack and protection_\nPowered attacks deal _25% more damage_, and the ally takes _25% less damage_ before other effects. Against Beaming Ray's marked target, the attack bonus becomes _"+n(100f*((1.3f+.05f*beam)-1f))+"%_. While Life Link's spell benefit is active, incoming damage is reduced by _"+n(100f*(1f-(.70f-.05f*link)))+"%_ instead. Either link prevents the power from expiring while active; it does not restart the 100-turn timer.\n\n_Summoning a new light ally_\nBase stats: _80 HP_, _5–30 base damage_ and _1–5 armor protection_. Base accuracy: "+(hero.lvl+9)+"; base evasion: "+(hero.lvl+4)+". These ratings compete with the enemy's defenses and attacks. The light ally disappears when its power ends. An existing ally keeps its own stats and equipment.";
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast)return BlastExplanation.details(hero);
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic){
            int conserved=hero.pointsInTalent(Talent.CONSERVED_MAGIC),everything=hero.pointsInTalent(Talent.FIRE_EVERYTHING),power=hero.pointsInTalent(Talent.WILD_POWER);
            int low=(4+power)/2,high=(5+power)/2;
            String boost=low==high?"_+"+low+" casting levels_":"_+"+low+" or +"+high+" casting levels_, equally likely";
            return "\n\n_Wild Magic_\nAttempts up to _"+(4+everything)+" wand zaps_ in random order. Each needs enough charge and spends _"+n(.5f*(float)Math.pow(.67f,conserved))+" wand charges_ (0.5 × 0.67^Conserved Magic rank), drawing on partial charge first. An unsuccessful targeting attempt can still spend charge. Armor charge is paid once for the whole ability.\nIf too few different wands are available, charged wands can fire a second time. A third attempt also requires a _"+(25*everything)+"%_ Fire Everything check and enough remaining charge.\nNormally takes _1 turn_ for the entire sequence."+(conserved>0?" Conserved Magic gives a _"+(25*conserved)+"% chance_ to take no time.":"")+" Animation delays do not cost extra turns.\n\n_Wild Power_\nAdds "+boost+". This boost cannot raise a lower-level wand above _+"+(3+power)+"_; a wand already at or above that level keeps its level. A stronger Magic Charge can raise it further. These are temporary casting levels, not permanent upgrades. Wild Magic can operate through magic immunity; each wand keeps its own targeting and curse effects.";
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon){
            int remote=hero.pointsInTalent(Talent.REMOTE_BEACON),longrange=hero.pointsInTalent(Talent.LONGRANGE_WARP),frag=hero.pointsInTalent(Talent.TELEFRAG);
            String result="\n\n_Warp Beacon_\nPlacing a beacon takes _1 turn_ and spends no armor charge, but requires the normal charge available. The tile must be mapped or visited, reachable and valid for a beacon; pits are excluded. Placement range: _"+(4*remote)+" tiles_ (4 × Remote Beacon rank). Without that talent, place it on your own tile.\nReturning on the same floor is instant. A different floor number requires Longrange Warp and uses _"+n(100*(1.833f-.333f*longrange))+"%_ of the usual return charge (1.833 − 0.333 × rank). Normal floor-transition restrictions still apply. You can also clear the beacon without returning.";
            if(frag>0)result+="\n\n_Telefrag_\nAn occupant at the same-floor destination takes _"+(10*frag)+"–"+(15*frag)+" base damage_. You also take a base hit of _"+(5*frag)+"_, capped at your current HP plus shielding minus 1 before damage modifiers. Normal damage rules still apply. A surviving occupant needs a legal neighboring tile to move into.";
            return result;
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower){
            int growing=hero.pointsInTalent(Talent.GROWING_POWER),wrath=hero.pointsInTalent(Talent.NATURES_WRATH),momentum=hero.pointsInTalent(Talent.WILD_MOMENTUM);
            String result="\n\n_Nature’s Power_\nActivation takes _1 turn_. Lasts _"+n(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower.naturesPowerTracker.DURATION)+" turns_. Walk at _"+n(2+.25f*growing)+" × your usual speed_ (2 + 0.25 × Growing Power rank).\nThe Spirit Bow gains _"+n(100f*(8+growing)/24f)+"% of its basic firing rate_, added to other bow-speed bonuses. Augments and special shots keep their own timing. Bow-speed addition = (8 + Growing Power rank) / 24 = "+n((8+growing)/24f)+".";
            if(wrath>0)result+="\nNature’s Wrath gives ordinary bow hits a _"+n(100f*wrath/12f)+"% chance_ to apply a random Blindweed, Firebloom, Icecap, Sorrowmoss or Stormvine effect, subject to that effect’s restrictions.";
            if(momentum>0)result+=" Wild Momentum adds _"+momentum+" turns_ on a qualifying kill, up to _2 extensions_ per activation.";
            return result;
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk){
            int eagle=hero.pointsInTalent(Talent.EAGLE_EYE),swift=hero.pointsInTalent(Talent.SWIFT_SPIRIT),eyes=hero.pointsInTalent(Talent.GO_FOR_THE_EYES);
            int initialSight=Math.min(8,6+eagle),laterSight=6+eagle;
            String result="\n\n_New Spirit Hawk_\nSummoning takes _1 turn_ and needs a free, walkable neighboring tile. A new hawk has _10 HP_, _5–10 base damage_, _60 accuracy_ and _60 evasion_. These ratings affect attack and dodge rolls, not fixed hit chances.\nMoves at _"+n(2+swift/2f)+" × normal walking speed_ (2 + Swift Spirit rank / 2). Its lifetime is _100 turns of its own activity_, not a fixed number of steps. Sight: _"+initialSight+" tiles_"+(initialSight==laterSight?".":" when summoned, then _"+laterSight+" tiles_ once it starts acting.");
            if(swift>0)result+=" Swift Spirit lets a new hawk automatically dodge its first _"+(2*swift)+" attacks_ that use normal hit checks. Damage that skips those checks can still hurt it.";
            if(eyes>0)result+="\nGo for the Eyes: hits blind for _"+(eyes==1?2:5)+" turns_"+(eyes<3?".":" and cripple for _"+(eyes==3?2:5)+" turns_.")+" Target resistance and immunity still apply.";
            return result;
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades){
            int fan=hero.pointsInTalent(Talent.FAN_OF_BLADES),projecting=hero.pointsInTalent(Talent.PROJECTING_BLADES);
            String result="\n\n_Spectral Blades_\nThe main target takes _100%_ of a normal attack’s damage; extra targets take _50%_, before defenses and other attack effects. The whole volley takes your normal attack time.\nEach blade selects the first eligible non-allied creature along its path that you can currently see.";
            if(fan>0)result+=" Fan of Blades adds a _"+(30*fan)+"° cone_, hitting up to _"+(1+fan)+" targets_ in total when valid paths exist.";
            if(projecting>0)result+=" Projecting Blades lets these blades pass through up to _"+(2*projecting)+" solid tiles_ and grants _"+(25*projecting)+"% more attack accuracy_. This improves the attack roll, not the final hit chance by that many percentage points.";
            return result+" Spirit Blades can apply its learned attack effects under their usual conditions.";
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb){
            int retreat=hero.pointsInTalent(Talent.HASTY_RETREAT),body=hero.pointsInTalent(Talent.BODY_REPLACEMENT),step=hero.pointsInTalent(Talent.SHADOW_STEP);
            String result="\n\n_Smoke Bomb_\nChoose a visible, empty tile within _6 reachable steps_, or your own tile. Roots prevents moving. Normal use takes _1 turn_, blinds adjacent non-allied monsters for _"+n(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.DURATION/2f)+" turns_ before resistance, and makes hunting monsters wander. Landing can still trigger hazards.";
            if(retreat>0)result+="\nHasty Retreat grants _"+n(.67f+retreat)+" turns each of Haste and Invisibility_; casting uses part of that duration.";
            if(body>0)result+=" Body Replacement leaves a log with _"+(20*body)+" HP_ and _"+body+"–"+(3*body)+" armor protection_, replacing your previous log.";
            if(step>0)result+="\n\n_Shadow Step_\nWhile invisible, instead move instantly for _"+n(100*(float)Math.pow(.84,step))+"%_ of the normal charge (0.84^rank). This mode adds no blindness, log, Haste or Invisibility. Destination restrictions still apply.";
            return result;
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark){
            int durability=hero.pointsInTalent(Talent.DEATHLY_DURABILITY);
            String result="\n\n_Death Mark_\nInstantly mark a visible enemy for _"+n(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark.DeathMarkTracker.DURATION)+" turns_, spending armor charge. A marked enemy takes _25% more incoming damage_ before other protection and can remain alive at zero HP until the mark ends.";
            if(durability>0)result+="\nDeathly Durability grants shielding equal to _"+n(12.5f*durability)+"%_ of the target’s HP when marked, rounded, when it dies under the mark’s conditions. Keeps stronger existing shielding. Refreshing the mark keeps the greater starting HP.\nShield = round(starting HP × 0.125 × talent rank).";
            return result;
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone){
            int copy=hero.pointsInTalent(Talent.PERFECT_COPY),blade=hero.pointsInTalent(Talent.SHADOW_BLADE),cloned=hero.pointsInTalent(Talent.CLONED_ARMOR);
            String result="\n\n_New Shadow Clone_\nSummoning takes _1 turn_ and needs a free, walkable neighboring tile. At your current level, a new clone has _"+(80+Math.round(.1f*copy*(15+5*hero.lvl)))+" HP_, _10–20 base damage_, _"+(hero.lvl+9)+" accuracy_ and _"+(hero.lvl+4)+" evasion_. Accuracy and evasion are competing ratings, not fixed hit chances. Existing clones keep their creation stats.\nHP = 80 + round(0.1 × Perfect Copy rank × (15 + 5 × hero level)).";
            if(blade>0)result+="\n\n_Shadow Blade_\nAdds _"+(8*blade)+"%_ of your rolled damage after adjusting for attack time. Drop the fraction from damage ÷ attack time, then multiply by 0.08 × rank and round; only a positive bonus is added. Separately, each hit has a _"+(25*blade)+"% chance_ to borrow your equipped weapon’s hit effects, which keep their own activation rules.";
            if(cloned>0)result+="\n\n_Cloned Armor_\nAdds _"+(12*cloned)+"%_ of your rolled protection, rounded (0.12 × rank). Has a _"+(25*cloned)+"% chance_ to borrow your glyph’s strength when checked and a separate _"+(25*cloned)+"% chance_ to trigger your armor’s hit effects.";
            return result;
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure){
            int shrug=hero.pointsInTalent(Talent.SHRUG_IT_OFF),sustained=hero.pointsInTalent(Talent.SUSTAINED_RETRIBUTION),odds=hero.pointsInTalent(Talent.EVEN_THE_ODDS);
            return "\n\n_Enduring_\nSkip _3 turns_. Incoming damage is reduced by _"+n(100f*(1f-.5f*(float)Math.pow(.8f,shrug)))+"%_ before armor and other protection. Half of each incoming blow or damaging effect is stored before protection, rounded down. Shrug It Off improves protection without reducing the amount stored.\n\n_Retaliation_\nWhen you can act again, the stored damage becomes a bonus for your next _"+(1+sustained)+(sustained==0?" hit":" hits")+"_. Each hit adds the same damage bonus before defenses; misses do not spend it. The bonus fades if unused.\n\n_Bonus calculation_\nMultiply the stored amount by _"+n(1f+.15f*sustained)+"_, then by _1 + "+n(.05f*odds)+" × nearby enemies_, then divide by _"+(1+sustained)+"_. Round down after each step. Enemies must be within 2 tiles when the bonus is prepared; this description does not reveal unseen enemies.";
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave){
            int expansion=hero.pointsInTalent(Talent.EXPANDING_WAVE),force=hero.pointsInTalent(Talent.SHOCK_FORCE),striking=hero.pointsInTalent(Talent.STRIKING_WAVE);
            String range="Identify equipped rings before the current strength-based damage range is shown.";
            Integer might=RingItemEffects.knownBonus(hero,com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight.class,false);
            if(might!=null){int strength=hero.previewStrength(might);int scaled=strength-10;range="At known strength "+strength+": _"+Math.round((5+scaled)*(1+.2f*force))+"–"+Math.round((10+2*scaled)*(1+.2f*force))+" damage_ after Shock Force, before other damage modifiers and enemy armor. Base roll: "+(5+scaled)+"–"+(10+2*scaled)+".";}
            return "\n\n_Shockwave_\nTakes _1 turn_. Reaches up to _"+(5+expansion)+" tiles_ in a _"+(60+15*expansion)+"° cone_, stopping at solid terrain and the aimed distance. Can hit any creature whose allegiance differs from yours.\n"+range+"\nDamage starts between strength − 5 and 2 × strength − 10. Shock Force adds _"+(20*force)+"%_, rounded. Custom damage rules can also change the result.\nStriking Wave has a _"+Math.min(100,30*striking)+"% chance_ to apply your attack effects, which keep their own activation rules. Surviving targets have a _"+(25*force)+"% chance_ of _5 turns of Paralysis_; otherwise they receive _5 turns of Cripple_, before resistance and immunity.";
        }
        if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap){
            int slam=hero.pointsInTalent(Talent.BODY_SLAM),impact=hero.pointsInTalent(Talent.IMPACT_WAVE);
            String result="\n\n_Heroic Leap_\nLeap in a straight line toward the selected tile, stopping before solid terrain. If the landing is occupied, land on an empty tile earlier along the route. Roots prevents leaping. Takes _1 turn_, ends invisibility and triggers the landing tile’s normal effects.";
            if(slam>0)result+="\nBody Slam hits adjacent non-allied creatures for _"+slam+"–"+(4*slam)+" base damage_, plus _"+(25*slam)+"%_ of a normal protection roll, rounded, before other damage modifiers and enemy armor. The protection bonus is rolled for the attack, rather than a fixed bonus.";
            if(impact>0)result+=" Impact Wave pushes them outward up to _"+(1+impact)+" tiles_, with reduced distance against bosses and normal terrain and movement restrictions. Has a _"+(25*impact)+"% chance_ to make them Vulnerable for _5 turns_, before resistance.";
            return result+" A ready Double Jump discount is consumed by this leap; otherwise the learned talent readies a three-turn opportunity.";
        }
        return "";
    }
}
