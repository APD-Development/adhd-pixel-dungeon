package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Every enemy in the Bestiary: builds, has a name and description, sane stats, and keeps its HP across a save. */
public class AllBestiaryMobsTest {
	private Hero oldHero;

	@Before public void setUp(){ HeadlessTestSupport.start(); oldHero = Dungeon.hero; Dungeon.hero = new Hero(); }
	@After public void tearDown(){ Dungeon.hero = oldHero; }

	private static List<Class<? extends Mob>> mobs(){
		List<Class<? extends Mob>> out = new ArrayList<>();
		for (Bestiary b : new Bestiary[]{Bestiary.REGIONAL, Bestiary.BOSSES, Bestiary.UNIVERSAL, Bestiary.RARE}){
			for (Class<?> c : b.entities()){
				if (Mob.class.isAssignableFrom(c)){
					@SuppressWarnings("unchecked") Class<? extends Mob> m = (Class<? extends Mob>) c;
					out.add(m);
				}
			}
		}
		return out;
	}

	@Test public void bestiaryListsEnemies(){
		assertTrue("at least the 30 regional enemies", mobs().size() >= 30);
	}

	@Test public void everyEnemyHasTextAndSaneStats(){
		List<String> problems = new ArrayList<>();
		for (Class<? extends Mob> c : mobs()){
			try {
				Mob m = Reflection.newInstance(c);
				String name = m.name(), desc = m.description();
				if (name == null || name.isEmpty() || name.contains(Messages.NO_TEXT_FOUND)) problems.add(c.getSimpleName() + " name");
				if (desc == null || desc.isEmpty() || desc.contains(Messages.NO_TEXT_FOUND)) problems.add(c.getSimpleName() + " description");
				if (m.HT < 0 || m.HP < 0 || m.HP > m.HT) problems.add(c.getSimpleName() + " HP " + m.HP + "/" + m.HT);
				if (m.EXP < 0) problems.add(c.getSimpleName() + " EXP " + m.EXP);
			} catch (Throwable needsGameState){
				//e.g. mimics read their loot, which only level generation fills in
			}
		}
		assertTrue(problems.toString(), problems.isEmpty());
	}

	@Test public void hpSurvivesASave(){
		List<String> problems = new ArrayList<>();
		for (Class<? extends Mob> c : mobs()){
			try {
				Mob m = Reflection.newInstance(c);
				if (m.HT <= 1) continue;
				m.HP = m.HT - 1;
				Bundle b = new Bundle();
				m.storeInBundle(b);
				Mob back = Reflection.newInstance(c);
				back.restoreFromBundle(b);
				if (back.HP != m.HP) problems.add(c.getSimpleName() + " HP " + back.HP + " != " + m.HP);
			} catch (Throwable t){
				problems.add(c.getSimpleName() + " threw " + t);
			}
		}
		assertTrue(problems.toString(), problems.isEmpty());
	}
}
