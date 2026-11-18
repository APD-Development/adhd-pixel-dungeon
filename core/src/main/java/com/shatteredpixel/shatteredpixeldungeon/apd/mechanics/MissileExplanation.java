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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.RingItemEffects;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.*;

/** Describes thrown effects without querying future targets or invoking dart wear calculations. */
public final class MissileExplanation {
    private MissileExplanation(){}
    private static String n(float value){return ObfuscationExplanation.number(value);}
    public static String details(MissileWeapon weapon,Hero hero){
        if(weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow.SpiritArrow)return "";
        String text="_Thrown-weapon durability units_\nA full durability reserve is 100 points. The ordinary expected-use input starts at "+n(weapon.baseDurabilityUses())+" × 1.5^(ordinary weapon level). Durable Projectiles, holster storage, damage/speed augmentation and Sharpshooting apply their separate factors. This uses the ordinary weapon-level path, separate from temporary effective damage-level bonuses. Crystal enchantment halves that use input and caps it at 50. For the ordinary rounded path, an unrounded input of at least 100 gives no wear; otherwise round the use input to whole uses and spend 100 / rounded uses + 0.001 durability points per qualifying use. That small correction is existing rounding protection, not extra damage. Subclasses such as tipped darts apply their additional wear rules and their own rounding stage. The uses shown above are durability estimates, not a promise that every throw hits or a random independent break chance. Shared stacks retain their normal wear and recovery rules. Inspection does not spend durability, choose a target or reset a dart's pending impact position.";
        if(weapon instanceof Bolas)text+="\n_Bolas_\nA qualifying hit offers Cripple for "+n(Cripple.DURATION/2)+" game-time units before normal resistance and reapplication rules. This slows supported movement; it is not extra damage or a guaranteed number of missed enemy attacks.";
        if(weapon instanceof Tomahawk){
            text+="\n_Tomahawk bleeding_\nA qualifying hit offers a middle-biased random bleeding strength between 3 + damage level / 2 and 6 + damage level, then applies damage augmentation. Damage level includes the normal effective weapon level and Sharpshooting damage-level bonus. Bleeding then uses its own random strength-reduction, damage-tick and defense rules; the strength is not guaranteed total health lost.";
            Integer sharpshooting=RingItemEffects.knownBonus(hero,RingOfSharpshooting.class,true);
            if(weapon.isIdentified()&&sharpshooting!=null){int level=weapon.buffedLvl()+sharpshooting;text+=" Current offered strength range before subsequent defenses: "+n(weapon.augment.damageFactor(((Tomahawk)weapon).minBleed(level)))+"–"+n(weapon.augment.damageFactor(((Tomahawk)weapon).maxBleed(level)))+" points.";}
        }
        if(weapon instanceof FishingSpear)text+="\n_Fishing spear against piranhas_\nAt its on-hit processing step, the damage input becomes the greater of its incoming damage and half the piranha's current HP, rounded down. It does not guarantee half of maximum HP as final damage, bypass later defenses or apply this minimum to every enemy type. No target HP is queried while inspecting.";
        if(weapon instanceof Shuriken)text+="\n_Shuriken's time-free throw_\nWithout the Shuriken cooldown tracker, the cast's normal time is zero and throwing starts the existing 19-unit scheduled cooldown (the displayed convention includes one additional unit). While the tracker is present, subsequent throws use normal cast timing. This is not free ammunition, guaranteed damage or a change to the visual Game speed setting.";
        if(weapon instanceof HeavyBoomerang)text+="\n_Returning boomerang_\nA surviving boomerang schedules a return after five supported updates on the recorded floor and branch. The destination is the hero's recorded throwing cell, rather than following the hero wherever they move. A return to the hero can be collected with no normal pickup delay; an occupied destination follows the existing return-hit rules and can attack another character occupying that return cell. Collision, durability, inventory space and floor changes keep their normal effects.";
        if(weapon instanceof ForceCube)text+="\n_Force cube impact area_\nA supported impact processes the center cell and its eight immediate neighbors: a 3 × 3 area. Characters there receive separate attack processing; allies and the hero can be affected. Cell presses can trigger traps, with the existing Tengu-dart-trap exception for neighboring cells. An empty pit follows the ordinary dropped-projectile branch. Area size is not a guarantee that every target takes the displayed maximum damage.";
        if(weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.TippedDart)text+="\n_Dart tips_\nTip wear follows the dart's special calculation, including Durable Tips, supported lotus preservation and Charged Shot. Used-up tips return ordinary darts under the normal rules; a status effect's strength or duration is separate from how many tipped uses remain. This inspector does not simulate a future lotus position or charged impact.";
        return text+dartEffect(weapon)+MetalExplanation.forWeapon(weapon);
    }
    private static String dartEffect(MissileWeapon weapon){
        String effect;
        switch(weapon.getClass().getSimpleName()){
            case "AdrenalineDart":effect="On an allied target, offers Adrenaline for "+n(Adrenaline.DURATION)+" scheduled units and returns zero ordinary hit damage; on an enemy, offers Cripple for "+n(Cripple.DURATION/2)+" units. Its Charged Shot branch excludes the thrower.";break;
            case "BlindingDart":effect="Offers Blindness for "+n(Blindness.DURATION)+" scheduled units; Charged Shot applies this status only across opposing alignments.";break;
            case "ChillingDart":effect="Offers Chill for 6 scheduled units on dry terrain, or "+n(Chill.DURATION)+" in water. Charged Shot applies it only across opposing alignments.";break;
            case "ParalyticDart":effect="Offers Paralysis for 5 scheduled units. Charged Shot applies it only across opposing alignments.";break;
            case "PoisonDart":effect="Offers Poison for 3 + floor(scaling floor / 2) game-time units, currently "+(3+Dungeon.scalingDepth()/2)+" units. This duration also controls Poison's separate damage-per-tick rule; it is not guaranteed total damage. Charged Shot applies it only across opposing alignments.";break;
            case "RotDart":effect="Offers Corrosion with a 10-unit duration input and strength equal to the scaling floor; bosses and minibosses instead use 5 units and floor(scaling floor / 3) strength. Current ordinary/boss strengths: "+Dungeon.scalingDepth()+" / "+(Dungeon.scalingDepth()/3)+". Tip wear is always 20 durability points per qualifying use: five full-reserve uses, independently of the ordinary dart-tip modifiers. Charged Shot excludes allied targets from this corrosion.";break;
            case "ShockingDart":effect="Offers a separate middle-biased electricity damage roll from 5 + floor(scaling floor / 4) to 10 + floor(scaling floor / 4), currently "+(5+Dungeon.scalingDepth()/4)+"–"+(10+Dungeon.scalingDepth()/4)+" base damage points. Attribution, damage modifiers and defenses still apply. Charged Shot offers it only across opposing alignments.";break;
            case "HealingDart":effect="Offers the Healing potion's supported ailment cure and a healing pool of floor(0.5 × target maximum HP + 30), using Healing's 0.25 per-update fraction input and zero flat increment. Allied hits return zero ordinary hit damage and skip normal on-hit effects. Charged Shot excludes the thrower and opposing-alignment targets from this healing. Target health and modifiers are not queried during inspection.";break;
            case "HolyDart":effect="Allied hits offer Bless for "+Math.round(Bless.DURATION)+" scheduled units and return zero ordinary hit damage. Undead or demonic enemies instead receive a separate middle-biased bonus roll from 10 + floor(scaling floor / 3) to 20 + floor(scaling floor / 3), currently "+(10+Dungeon.scalingDepth()/3)+"–"+(20+Dungeon.scalingDepth()/3)+" base damage points before later modifiers. Other enemies can be Blessed on an ordinary shot. Charged Shot excludes the thrower and does not Bless ordinary enemies.";break;
            case "CleansingDart":effect="Allied hits use the supported Cleansing effect for twice its ordinary duration and return zero ordinary hit damage. Enemy hits remove eligible positive buffs, excluding Champion effects and Charged Shot, then use the existing delayed wandering/beckoning behavior if the target survives. It can remove a buff that was keeping an enemy alive. Charged Shot excludes the thrower. Immunity, excluded buffs and target state still apply.";break;
            case "DisplacingDart":effect="Attempts a teleport to a legal empty passable cell at movement-path distance 8–10 from the attacker. It prefers visible candidates and then the one nearest the defender; if no visible candidate exists it uses the nearest eligible unseen candidate and offers 5 scheduled units of character awareness. Immovable targets, large-character space, occupancy and lack of legal cells can prevent it. Charged Shot excludes allied targets. Inspection does not build a path map or query unseen destinations.";break;
            case "IncendiaryDart":effect="Offers attributed Burning on a qualifying hit; Charged Shot burns only across opposing alignments. An eligible empty or thrower-occupied flammable impact cell instead receives 4 units of fire-cloud volume and normal tip wear. Cloud volume is not four damage points or a guaranteed four-turn burn.";break;
            default:return "";
        }
        return "\n_Dart's on-hit effect_\n"+effect+" Offered status durations use game-time units before resistance and reapplication. The effect is separate from physical hit damage, accuracy and durability; inspection applies none of them.";
    }

}
