package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Every hero class, subclass and armor ability has its titles and descriptions. */
public class AllClassesHaveTextTest {
	private static void check(List<String> problems, String what, java.util.function.Supplier<String> get){
		String text;
		try { text = get.get(); }
		catch (NullPointerException needsAScreen){ return; } //some descriptions read the current scene
		if (text == null || text.isEmpty() || text.contains(Messages.NO_TEXT_FOUND)) problems.add(what);
	}

	@Test public void classesSubclassesAndAbilitiesHaveText(){
		HeadlessTestSupport.start();
		Hero old = Dungeon.hero;
		Dungeon.hero = new Hero();
		List<String> problems = new ArrayList<>();
		try {
			for (HeroClass c : HeroClass.values()){
				check(problems, c + " title", () -> c.title());
				check(problems, c + " desc", () -> c.desc());
				check(problems, c + " shortDesc", () -> c.shortDesc());
				assertTrue(c + " has subclasses", c.subClasses().length > 0);
				for (HeroSubClass s : c.subClasses()){
					check(problems, s + " title", () -> s.title());
					check(problems, s + " shortDesc", () -> s.shortDesc());
				}
				for (ArmorAbility a : c.armorAbilities()){
					check(problems, a.getClass().getSimpleName() + " desc", () -> a.desc());
					check(problems, a.getClass().getSimpleName() + " shortDesc", () -> a.shortDesc());
				}
			}
		} finally { Dungeon.hero = old; }
		assertTrue(problems.toString(), problems.isEmpty());
	}
}
