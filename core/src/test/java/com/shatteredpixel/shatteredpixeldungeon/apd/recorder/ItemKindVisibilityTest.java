package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.RowBuilders;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.ItemStatusHandler;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

//UIMSD groups items by their TRUE class (a public "type" built from hidden truth), but only where the player already knows it. This pins
//that rule to the game's own identification behaviour: any item family whose kind is hidden until it is identified must be one UIMSD
//treats as hidden until the game says it was learned. If a new or changed family needs identification and this test fails, UIMSD would
//be showing an unidentified item's real kind: fix RowBuilders.kindIsVisible / the recording before touching this test.
//
//Nothing here makes an item (item classes start the game's graphics and text systems): classes are loaded without initialising them.
public class ItemKindVisibilityTest {

	private static final String PKG = "com.shatteredpixel.shatteredpixeldungeon.";

	private static List<Class<?>> itemClasses() throws Exception {
		File classes;
		try {
			classes = new File(Item.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e){
			throw new IllegalStateException(e);
		}
		File root = new File(classes, PKG.replace('.', '/') + "items");
		assertTrue("the compiled item classes are at " + root, root.isDirectory());
		List<Class<?>> out = new ArrayList<>();
		collect(classes, root, out);
		return out;
	}

	private static void collect( File base, File dir, List<Class<?>> out ) throws Exception {
		File[] kids = dir.listFiles();
		if (kids == null) return;
		for (File f : kids){
			if (f.isDirectory()){
				collect(base, f, out);
			} else if (f.getName().endsWith(".class")){
				String name = base.toURI().relativize(f.toURI()).getPath();
				name = name.substring(0, name.length() - ".class".length()).replace('/', '.');
				Class<?> c = Class.forName(name, false, Item.class.getClassLoader());   //loaded, not initialised
				if (Item.class.isAssignableFrom(c)) out.add(c);
			}
		}
	}

	//every class in the hierarchy (up to Item) that declares an identification handler
	private static boolean usesHandler( Class<?> c ){
		for (Class<?> k = c; k != null && k != Item.class; k = k.getSuperclass()){
			for (Field f : k.getDeclaredFields()) if (f.getType() == ItemStatusHandler.class) return true;
		}
		return false;
	}

	//a class in the hierarchy that decides for itself whether its kind is known
	private static boolean overridesIsKnown( Class<?> c ){
		for (Class<?> k = c; k != null && k != Item.class; k = k.getSuperclass()){
			try {
				Method m = k.getDeclaredMethod("isKnown");
				if (!Modifier.isAbstract(m.getModifiers())) return true;
			} catch (NoSuchMethodException ignored){
			}
		}
		return false;
	}

	@Test
	public void onlyPotionsScrollsAndRingsHideTheirKindUntilIdentified() throws Exception {
		TreeSet<String> roots = new TreeSet<>();
		for (Class<?> c : itemClasses()){
			for (Field f : c.getDeclaredFields()){
				if (f.getType() == ItemStatusHandler.class) roots.add(c.getSimpleName());
			}
		}
		assertEquals("A class that keeps an ItemStatusHandler hides its kind until identified. UIMSD must treat its category as hidden "
				+ "until ITEM_CLASS_KNOWN (RowBuilders.kindIsVisible) and the recorder must emit that event for it. Review both, then update this list.",
				"[Potion, Ring, Scroll]", roots.toString());
	}

	@Test
	public void noItemThatNeedsIdentificationIsTreatedAsHavingAVisibleKind() throws Exception {
		List<String> bad = new ArrayList<>();
		int checked = 0;
		for (Class<?> c : itemClasses()){
			if (Modifier.isAbstract(c.getModifiers()) && !usesHandler(c)) continue;
			int category = ItemFacts.categoryOfClass(c);
			boolean needsIdentification = usesHandler(c) || overridesIsKnown(c);
			if (needsIdentification && RowBuilders.kindIsVisible(category)){
				bad.add(c.getName() + " needs identification but its category " + category + " is treated as visible");
			}
			checked++;
		}
		Collections.sort(bad);
		assertTrue("UIMSD would show the true kind of these before the player knows it:\n" + String.join("\n", bad), bad.isEmpty());
		assertTrue("looked at the item classes (" + checked + ")", checked > 300);
	}

	@Test
	public void everyIdentificationFamilyIsInACategoryThatWaitsForTheKindToBeLearned() {
		for (Class<?> c : new Class<?>[]{ Potion.class, ExoticPotion.class, Scroll.class, ExoticScroll.class, Ring.class }){
			int category = ItemFacts.categoryOfClass(c);
			assertFalse(c.getSimpleName() + " (category " + category + ") must wait for ITEM_CLASS_KNOWN", RowBuilders.kindIsVisible(category));
		}
		assertEquals(ItemEvents.CAT_POTION, ItemFacts.categoryOfClass(Potion.class));
		assertEquals(ItemEvents.CAT_SCROLL, ItemFacts.categoryOfClass(Scroll.class));
		assertEquals(ItemEvents.CAT_RING, ItemFacts.categoryOfClass(Ring.class));
		//the only categories treated as hidden-until-known are exactly these three, plus "other", which is not understood well enough to trust
		TreeSet<Integer> hidden = new TreeSet<>();
		for (int cat = 0; cat <= ItemEvents.CAT_TOOL; cat++) if (!RowBuilders.kindIsVisible(cat)) hidden.add(cat);
		TreeSet<Integer> expected = new TreeSet<>();
		Collections.addAll(expected, ItemEvents.CAT_OTHER, ItemEvents.CAT_RING, ItemEvents.CAT_POTION, ItemEvents.CAT_SCROLL);
		assertEquals(expected, hidden);
	}

	@Test
	public void theRecorderReportsWhenEachIdentificationFamilyLearnsItsKind() throws Exception {
		//the public event that lets UIMSD show the real kind must be emitted by every family that hides it
		String src = System.getProperty("apd.game.src");
		for (String file : new String[]{ "items/potions/Potion.java", "items/potions/exotic/ExoticPotion.java", "items/scrolls/Scroll.java",
				"items/scrolls/exotic/ExoticScroll.java", "items/rings/Ring.java" }){
			String text = new String(Files.readAllBytes(new File(src, file).toPath()), "UTF-8");
			assertTrue(file + " must call ApdItems.onClassKnown when its kind is learned", text.contains("ApdItems.onClassKnown"));
		}
		//and no other source file keeps a handler (a new family would need the same treatment)
		List<String> handlers = new ArrayList<>();
		scan(new File(src, "items"), handlers);
		Collections.sort(handlers);
		assertEquals("[items/ItemStatusHandler.java, items/potions/Potion.java, items/rings/Ring.java, items/scrolls/Scroll.java]", handlers.toString());
	}

	private static void scan( File dir, List<String> out ) throws Exception {
		File[] kids = dir.listFiles();
		if (kids == null) return;
		for (File f : kids){
			if (f.isDirectory()) scan(f, out);
			else if (f.getName().endsWith(".java")){
				String text = new String(Files.readAllBytes(f.toPath()), "UTF-8");
				if (text.contains("ItemStatusHandler")){
					String rel = f.getPath();
					out.add(rel.substring(rel.indexOf("items" + File.separator)).replace(File.separatorChar, '/'));
				}
			}
		}
	}
}
