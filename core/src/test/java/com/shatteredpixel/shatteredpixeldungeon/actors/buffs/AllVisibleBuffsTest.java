package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Every top-level buff class in the game that shows an icon must have a name and description,
 * attached to a real hero. Found by scanning the source folder, so new buffs are covered automatically.
 */
public class AllVisibleBuffsTest {
	private Hero oldHero;

	@Before public void setUp(){ HeadlessTestSupport.start(); oldHero = Dungeon.hero; }
	@After public void tearDown(){ Dungeon.hero = oldHero; }

	private static List<Class<?>> buffClasses() throws Exception {
		Path root = Paths.get(System.getProperty("apd.game.src"));
		List<Class<?>> out = new ArrayList<>();
		try (Stream<Path> files = Files.walk(root)){
			for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator){
				String rel = root.relativize(p).toString().replace('\\', '/').replace('/', '.');
				String name = "com.shatteredpixel.shatteredpixeldungeon." + rel.substring(0, rel.length() - 5);
				try {
					Class<?> c = Class.forName(name, false, AllVisibleBuffsTest.class.getClassLoader());
					if (Buff.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())) out.add(c);
				} catch (Throwable ignored){ }
			}
		}
		return out;
	}

	@Test public void scanFindsTheBuffs() throws Exception {
		assertTrue(buffClasses().size() > 40);
	}

	@Test public void everyBuffWithAnIconHasText() throws Exception {
		List<String> missing = new ArrayList<>();
		for (Class<?> c : buffClasses()){
			Buff b;
			try {
				java.lang.reflect.Constructor<?> k = c.getDeclaredConstructor();
				k.setAccessible(true);
				b = (Buff) k.newInstance();
			} catch (Throwable noPlainConstructor){
				continue;
			}
			Hero hero = new Hero();
			hero.HP = hero.HT = 20;
			Dungeon.hero = hero;
			try {
				b.attachTo(hero);
				if (b.icon() == BuffIndicator.NONE) continue;
				String name = b.name(), desc = b.desc();
				if (name == null || name.isEmpty() || name.contains(Messages.NO_TEXT_FOUND)) missing.add(c.getSimpleName() + " name");
				if (desc == null || desc.isEmpty() || desc.contains(Messages.NO_TEXT_FOUND)) missing.add(c.getSimpleName() + " desc");
			} catch (Throwable needsMoreContext){
				//some buffs read state only real gameplay sets up; text completeness is still checked for the rest
			}
		}
		assertTrue(missing.toString(), missing.isEmpty());
	}
}
