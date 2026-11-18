package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static org.junit.Assert.assertEquals;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;

import org.junit.Test;

//APD-0001 regression: starvation damage must consume shielding before HP
public class ShieldHungerTest {

	private static class TestChar extends Char {
		TestChar(int hp) {
			HP = HT = hp;
		}
	}

	private static class TestShield extends ShieldBuff {
		TestShield(int amount) {
			setShield(amount);
		}
	}

	@Test
	public void starvationTickConsumesShieldBeforeHp() {
		Char target = new TestChar(1);
		TestShield shield = new TestShield(5);
		shield.attachTo(target);

		//one starvation tick of 1 damage, as dealt by Hunger.act()
		int leftover = ShieldBuff.processDamage(target, 1, new Hunger());
		target.HP -= leftover;

		assertEquals(4, shield.shielding());
		assertEquals(1, target.HP);
	}

	@Test
	public void starvationOnlyHurtsHpOnceShieldIsGone() {
		Char target = new TestChar(10);
		TestShield shield = new TestShield(1);
		shield.attachTo(target);

		int leftover = ShieldBuff.processDamage(target, 3, new Hunger());

		assertEquals(0, shield.shielding());
		assertEquals(2, leftover);
	}
}
