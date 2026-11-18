package com.shatteredpixel.shatteredpixeldungeon.items;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Every alchemy recipe the pot knows: well formed inputs, a real output with text, and a sane energy cost. */
public class AllRecipesTest {
	private static List<Recipe> all() throws Exception {
		List<Recipe> out = new ArrayList<>();
		for (String name : new String[]{"variableRecipes", "oneIngredientRecipes", "twoIngredientRecipes", "threeIngredientRecipes"}){
			Field f = Recipe.class.getDeclaredField(name);
			f.setAccessible(true);
			for (Recipe r : (Recipe[]) f.get(null)) out.add(r);
		}
		return out;
	}

	private static Object get(Recipe.SimpleRecipe r, String name) throws Exception {
		Field f = Recipe.SimpleRecipe.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(r);
	}

	@Test public void recipeListIncludesTheApdRecipes() throws Exception {
		boolean dust = false;
		for (Recipe r : all()) if (r instanceof RingDust.Recipe) dust = true;
		assertTrue(dust);
	}

	@Test public void simpleRecipesAreWellFormed() throws Exception {
		HeadlessTestSupport.start();
		Hero old = Dungeon.hero;
		Dungeon.hero = new Hero();
		List<String> problems = new ArrayList<>();
		try {
			for (Recipe recipe : all()){
				if (!(recipe instanceof Recipe.SimpleRecipe)) continue;
				Recipe.SimpleRecipe r = (Recipe.SimpleRecipe) recipe;
				String n = r.getClass().getName();
				Class<?>[] in = (Class<?>[]) get(r, "inputs");
				int[] q = (int[]) get(r, "inQuantity");
				int cost = (Integer) get(r, "cost");
				if (in == null || q == null || in.length != q.length || in.length == 0) { problems.add(n + " inputs"); continue; }
				Set<Class<?>> unique = new HashSet<>();
				for (int i = 0; i < in.length; i++){
					if (!unique.add(in[i])) problems.add(n + " repeats " + in[i].getSimpleName());
					if (q[i] < 1) problems.add(n + " quantity " + q[i]);
				}
				if (cost < 0) problems.add(n + " cost " + cost);
				Item out = r.sampleOutput(null);
				if (out == null) { problems.add(n + " no output"); continue; }
				if (out.quantity() < 1) problems.add(n + " output quantity");
				String name = out.name();
				if (name == null || name.isEmpty() || name.contains(Messages.NO_TEXT_FOUND)) problems.add(n + " output name");
				if (r.getIngredients().size() != in.length) problems.add(n + " getIngredients");
			}
		} finally { Dungeon.hero = old; }
		assertTrue(problems.toString(), problems.isEmpty());
	}
}
