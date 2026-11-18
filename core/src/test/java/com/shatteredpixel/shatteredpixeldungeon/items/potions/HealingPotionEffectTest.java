package com.shatteredpixel.shatteredpixeldungeon.items.potions;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;

import org.junit.Test;

import java.lang.reflect.Field;

/** Healing on a non-hero: queues 80% of max HP + 14 as gradual healing; cure removes the listed ailments. */
public class HealingPotionEffectTest {
	@Test public void healQueuesEightyPercentPlusFourteen() throws Exception {
		HeadlessTestSupport.start();
		Char ch = new Char(){};
		ch.HT = 50; ch.HP = 10;
		PotionOfHealing.heal(ch);
		Healing h = ch.buff(Healing.class);
		assertNotNull(h);
		Field left = Healing.class.getDeclaredField("healingLeft");
		left.setAccessible(true);
		assertEquals((int)(0.8f * 50 + 14), left.getInt(h));
	}

	@Test public void cureRemovesAilments(){
		HeadlessTestSupport.start();
		Char ch = new Char(){};
		ch.HT = ch.HP = 20;
		Buff.affect(ch, Poison.class).set(5);
		Buff.affect(ch, Cripple.class, 5);
		Buff.affect(ch, Weakness.class, 5);
		Buff.affect(ch, Bleeding.class).set(3);
		PotionOfHealing.cure(ch);
		assertNull(ch.buff(Poison.class));
		assertNull(ch.buff(Cripple.class));
		assertNull(ch.buff(Weakness.class));
		assertNull(ch.buff(Bleeding.class));
	}
}
