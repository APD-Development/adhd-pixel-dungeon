package com.shatteredpixel.shatteredpixeldungeon.items.armor;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArmorStrengthPenaltyTextTest {
	@Test public void matchesTheRealFormulas(){
		assertEquals("", Armor.strengthPenalty(0));
		assertEquals("You are _1 strength_ short: _-33% evasion_, _-17% movement speed_ and _-2 armor blocking_ per hit.", Armor.strengthPenalty(1));
		assertEquals("You are _2 strength_ short: _-56% evasion_, _-31% movement speed_ and _-4 armor blocking_ per hit.", Armor.strengthPenalty(2));
		assertEquals("You are _3 strength_ short: _-70% evasion_, _-42% movement speed_ and _-6 armor blocking_ per hit.", Armor.strengthPenalty(3));
		assertEquals("", Armor.strengthPenaltyUnupgraded(0));
		assertEquals("This armor might need too much strength. If it's unupgraded, you'd be _2 strength_ short: _-56% evasion_, _-31% movement speed_ and _-4 armor blocking_ per hit.", Armor.strengthPenaltyUnupgraded(2));
	}
}
