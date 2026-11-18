/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalSpire;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FungalCore;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BossClassesTest {

	private static final Class<?>[] BOSSES = {
			Goo.class, Tengu.class, DM300.class, DwarfKing.class, YogDzewa.class, YogFist.class,
			YogFist.BurningFist.class, YogFist.SoiledFist.class, YogFist.RottingFist.class,
			YogFist.RustedFist.class, YogFist.BrightFist.class, YogFist.DarkFist.class,
			CrystalSpire.class, GnollGeomancer.class, FungalCore.class, VaultBossElemental.class
	};

	@Test
	public void everyCurrentBossClassIsRecognizedByFullAndSimpleName(){
		for (Class<?> boss : BOSSES){
			assertTrue(boss.getName(), BossClasses.isBoss(boss.getName()));
			assertTrue(boss.getSimpleName(), BossClasses.isBoss(boss.getSimpleName()));
		}
	}

	@Test
	public void bossHelpersAndLookalikesAreNotBosses(){
		for (String name : new String[]{
				"Rat", "com.x.Gnoll", "DM201", "Gooey", "Tengu$BombItem", "DwarfKing$DKGhoul",
				"YogDzewa$Larva", "YogFist$LightBeam", "CrystalSpire$SpireSpike",
				"VaultBossElemental$FireWall", "(several)", "", null
		}){
			assertFalse(String.valueOf(name), BossClasses.isBoss(name));
		}
	}
}
