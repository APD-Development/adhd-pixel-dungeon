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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.*;

/** Detonation rules only: never arms a fuse, computes a hidden blast area or rolls protection. */
public final class BombExplanation {
    private BombExplanation(){}
    public static String details(Bomb bomb){
        int depth=Dungeon.scalingDepth(),low=4+depth,high=12+3*depth;
        String text="_Bomb range, timing and damage units_\nThe current bomb's range input is "+bomb.previewExplosionRange()+" tiles, bounded by its existing terrain/path rules rather than a promise to affect every tile through walls. An ordinary ignited fuse is scheduled with a two-unit delay under normal actor timing; special fuse types retain their own stages. "+(bomb.fuse==null?"This item's fuse is currently absent.":"This item currently has a fuse; inspection does not advance it.")+" The base bomb damage roll is middle-biased from 4 + scaling depth to 12 + 3 × scaling depth: currently "+low+"–"+high+" raw damage points. Attribution/difficulty scaling, armor and later defenses are separate from that raw range. Allies, the hero, terrain and ground loot retain their normal blast risks. ";
        if(bomb.explodesDestructively())text+="This type runs the existing destructive base-blast path, including its armor roll before positive damage is sent onward. ";
        else text+="This type replaces the ordinary destructive base-blast path with its special effect; the base range above is not a claim that an additional physical blast always occurs. ";
        if(bomb instanceof ArcaneBomb)text+="\n_Arcane blast_\nIts terrain-reachable range-2 effect uses that raw roll without the ordinary base blast's armor subtraction. Later resistance, immunity and shielding still apply; bypassing that armor roll is not bypassing every defense.";
        if(bomb instanceof HolyBomb)text+="\n_Holy bonus_\nSupported undead or demonic targets receive a separate additional raw roll from the same depth-dependent range, multiplied by 0.5 and rounded, through its holy-damage path. This is an independently rolled added effect, not a guarantee of exactly 50% more final HP loss against every creature.";
        if(bomb instanceof FlashBangBomb)text+="\n_Flashbang bonus_\nIts additional electrical input is a separate depth-dependent raw roll divided by 4 and rounded. A surviving affected character is offered Paralysis for "+ObfuscationExplanation.number(Paralysis.DURATION)+" game-time units under normal resistance and escape rules. This is not a guaranteed whole stun duration or exactly 25% more final damage.";
        if(bomb instanceof Firebomb)text+="\n_Fire_\nEligible affected non-pit cells are seeded with 10 fire-volume units; pits use 2. Volume is not a direct damage total or a guaranteed duration. Fire spread, terrain and Burning use their normal rules.";
        if(bomb instanceof FrostBomb)text+="\n_Freezing_\nEligible affected cells get 10 freezing-volume units and affected characters receive a 2-unit Frost offer before normal resistance. Freezing volume, Frost duration and final damage are separate; thawing and carried-item protection retain their own rules.";
        if(bomb instanceof SmokeBomb)text+="\n_Smoke_\nThe existing placement allocates 40 smoke-volume units per reachable cell and places remaining positive shares from its 1000-unit budget at the center. Volume is not 1000 turns or guaranteed evasion; visibility and terrain rules still apply.";
        if(bomb instanceof ShrapnelBomb)text+="\n_Shrapnel_\nThe attack area uses ordinary line of sight up to range 8 rather than the small normal bomb footprint. Each eligible target gets its own depth-dependent raw roll, source scaling and armor subtraction. Inspection does not reveal the actual area behind unknown terrain or count unseen targets.";
        if(bomb instanceof RegrowthBomb)text+="\n_Regrowth_\nEligible cells receive 10 regrowth-volume units. The existing plant step attempts 2 ordinary plants with weight 2 or 3 with weight 1, then one special plant if a suitable cell remains. The special choice weights are 6 Dewcatcher, 3 Seedpod and 1 Starflower. Terrain and available cells can reduce successful placements; these are conditional generation weights, not guaranteed loot drops or healing.";
        if(bomb instanceof WoollyBomb)text+="\n_Sheep_\nAfter the normal explosion, eligible unoccupied non-pit cells in its range-plus-2 path area can receive sheep. The existing lifetime input is 20 on boss floors or 200 elsewhere; sheep use their normal lifetime processing. This does not summon a fixed number through walls or protect the hero from the preceding blast.";
        if(bomb instanceof Noisemaker)text+="\n_Noisemaker fuse_\nIts initial fuse transitions into the existing noise/trigger stage instead of immediately behaving like an ordinary bomb. Once triggered it cannot be picked up through the ordinary pickup path; occupancy and its own fuse logic govern detonation. The ordinary two-unit initial delay is not the complete duration of that later stage.";
        return text+" Viewing this explanation does not pick a blast center, destroy loot, ignite a bomb or advance randomness.";
    }
}
