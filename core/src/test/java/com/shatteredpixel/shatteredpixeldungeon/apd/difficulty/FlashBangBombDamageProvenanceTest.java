package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.FlashBangBomb;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FlashBangBombDamageProvenanceTest {

	private SpecialBombDamageProvenanceTest fixture;

	@Before
	public void setup() throws Exception {
		fixture = new SpecialBombDamageProvenanceTest();
		fixture.setup();
		Dungeon.hero.sprite = new SpecialBombDamageProvenanceTest.Quiet();
		new Group().add(Dungeon.hero.sprite);
	}

	@After
	public void cleanup() throws Exception {
		fixture.cleanup();
	}

	private static int scaled(int raw, int percentage) {
		return percentage == 100 ? raw : Math.round(raw * percentage / 100f);
	}

	private void verify(OffensiveDamageOrigin origin, int percentage, boolean restored) {
		fixture.rules(100, 100, 100);
		SpecialBombDamageProvenanceTest.Target reference =
				new SpecialBombDamageProvenanceTest.Target(false);
		Actor.add(reference);
		int rawBase;
		int rawElectricity;
		long next;
		try {
			Random.pushGenerator(927);
			try {
				new FlashBangBomb().explode(reference.pos);
				assertEquals(2, reference.hits.size());
				rawBase = reference.hits.get(0) + 3;
				rawElectricity = reference.hits.get(1);
				next = Random.Long();
			} finally {
				Random.popGenerator();
			}
		} finally {
			Buff.detach(reference, Paralysis.class);
			Actor.remove(reference);
		}

		fixture.rules(
				origin == OffensiveDamageOrigin.PLAYER ? percentage : 275,
				origin == OffensiveDamageOrigin.ENEMY ? percentage : 325,
				origin == OffensiveDamageOrigin.BOSS ? percentage : 425);
		SpecialBombDamageProvenanceTest.Target target =
				new SpecialBombDamageProvenanceTest.Target(false);
		Actor.add(target);
		try {
			FlashBangBomb bomb = new FlashBangBomb();
			Random.pushGenerator(927);
			try {
				if (restored) {
					Bundle state = new Bundle();
					bomb.storeInBundle(state);
					state.put("apd_bomb_origin", origin.name());
					bomb.restoreFromBundle(state);
					bomb.explode(target.pos);
				} else if (origin == OffensiveDamageOrigin.UNATTRIBUTED) {
					bomb.explode(target.pos);
				} else {
					bomb.apdExplodeAttributed(target.pos, origin);
				}

				int base = scaled(rawBase, percentage) - 3;
				int bonusIndex = 0;
				if (base > 0) {
					assertEquals(base, (int) target.hits.get(0));
					assertSame(bomb, target.sources.get(0));
					bonusIndex = 1;
				}
				assertEquals(bonusIndex + 1, target.hits.size());
				assertEquals(scaled(rawElectricity, percentage),
						(int) target.hits.get(bonusIndex));
				assertTrue(target.sources.get(bonusIndex) instanceof Electricity);
				assertEquals("No added RNG draws", next, Random.Long());
			} finally {
				Random.popGenerator();
			}
		} finally {
			Buff.detach(target, Paralysis.class);
			Actor.remove(target);
		}
	}

	@Test
	public void electricityBonusUsesActualBombCreatorExactlyOnce() {
		for (OffensiveDamageOrigin origin : new OffensiveDamageOrigin[]{
				OffensiveDamageOrigin.PLAYER,
				OffensiveDamageOrigin.ENEMY,
				OffensiveDamageOrigin.BOSS}) {
			for (int percentage = 25; percentage <= 500; percentage += 25) {
				verify(origin, percentage, false);
			}
		}
	}

	@Test
	public void unownedAndNeutralBonusPreserveOriginalDamageAndRng() {
		verify(OffensiveDamageOrigin.UNATTRIBUTED, 100, false);
		verify(OffensiveDamageOrigin.PLAYER, 100, false);
	}

	@Test
	public void savedCreatorStillAppliesToElectricityBonus() {
		verify(OffensiveDamageOrigin.BOSS, 425, true);
	}
}
