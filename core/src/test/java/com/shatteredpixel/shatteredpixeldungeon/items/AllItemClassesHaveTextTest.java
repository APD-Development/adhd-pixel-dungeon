package com.shatteredpixel.shatteredpixeldungeon.items;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

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

/** Scans every top-level item and creature class in the game: anything that can be built plainly must have text. */
public class AllItemClassesHaveTextTest {
	private Hero oldHero;

	@Before public void setUp(){ HeadlessTestSupport.start(); oldHero = Dungeon.hero; Dungeon.hero = new Hero(); }
	@After public void tearDown(){ Dungeon.hero = oldHero; }

	static List<Class<?>> classesOf(Class<?> base) throws Exception {
		Path root = Paths.get(System.getProperty("apd.game.src"));
		List<Class<?>> out = new ArrayList<>();
		try (Stream<Path> files = Files.walk(root)){
			for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator){
				String rel = root.relativize(p).toString().replace('\\', '/').replace('/', '.');
				String name = "com.shatteredpixel.shatteredpixeldungeon." + rel.substring(0, rel.length() - 5);
				try {
					Class<?> c = Class.forName(name, false, AllItemClassesHaveTextTest.class.getClassLoader());
					if (base.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers()) && !c.isInterface()) out.add(c);
				} catch (Throwable ignored){ }
			}
		}
		return out;
	}

	private static Object build(Class<?> c){
		try {
			java.lang.reflect.Constructor<?> k = c.getDeclaredConstructor();
			k.setAccessible(true);
			return k.newInstance();
		} catch (Throwable t){ return null; }
	}

	private static boolean bad(String s){ return s == null || s.isEmpty() || s.contains(Messages.NO_TEXT_FOUND); }

	@Test public void everyItemHasANameAndDescription() throws Exception {
		List<Class<?>> all = classesOf(Item.class);
		assertTrue(all.size() > 200);
		List<String> missing = new ArrayList<>();
		for (Class<?> c : all){
			Object o = build(c);
			if (o == null || c == Item.class || c.getSimpleName().equals("Artifact") || c.getSimpleName().equals("MeleeWeapon")) continue; //generic base classes
			try {
				Item item = (Item) o;
				if (bad(item.name())) missing.add(c.getSimpleName() + " name");
				if (bad(item.desc())) missing.add(c.getSimpleName() + " desc");
			} catch (Throwable needsContext){ }
		}
		assertTrue(missing.toString(), missing.isEmpty());
	}

	@Test public void everyCreatureHasANameAndDescription() throws Exception {
		List<Class<?>> all = classesOf(Mob.class);
		assertTrue(all.size() > 60);
		List<String> missing = new ArrayList<>();
		for (Class<?> c : all){
			Object o = build(c);
			if (o == null || c.getSimpleName().equals("DirectableAlly")) continue; //generic base class
			try {
				Mob m = (Mob) o;
				if (bad(m.name())) missing.add(c.getSimpleName() + " name");
				if (bad(m.description())) missing.add(c.getSimpleName() + " description");
			} catch (Throwable needsContext){ }
		}
		assertTrue(missing.toString(), missing.isEmpty());
	}
}
