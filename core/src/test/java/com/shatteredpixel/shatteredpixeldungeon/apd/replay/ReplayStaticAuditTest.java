package com.shatteredpixel.shatteredpixeldungeon.apd.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

//The isolation audit: looks at every static field of the game's level/item/actor/journal code before and after a replay and lists what
//the replay changed. The list is pinned: a replay may only change state that is either put back by FloorReplay or replaced by every new
//or continued game before it is read. A new static that generation starts to change fails this test until someone classifies it.
public class ReplayStaticAuditTest {

	private static final String BASE = "com.shatteredpixel.shatteredpixeldungeon";
	private static final String[] PACKAGES = { "levels", "items", "actors", "journal", "plants", "mechanics", "messages", "utils" };

	private static List<String> classNames() throws URISyntaxException {
		File root = new File(Dungeon.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		List<String> out = new ArrayList<>();
		for (String p : PACKAGES) collect(new File(root, BASE.replace('.', '/') + "/" + p), BASE + "." + p, out);
		for (String top : new String[]{ "Dungeon", "Statistics", "Badges", "Challenges", "GamesInProgress", "Rankings", "SPDSettings", "Assets" }){
			out.add(BASE + "." + top);
		}
		return out;
	}

	private static void collect( File dir, String pkg, List<String> out ){
		File[] kids = dir.listFiles();
		if (kids == null) return;
		for (File k : kids){
			if (k.isDirectory()) collect(k, pkg + "." + k.getName(), out);
			else if (k.getName().endsWith(".class")) out.add(pkg + "." + k.getName().substring(0, k.getName().length() - 6));
		}
	}

	private static String describe( Object v ){
		if (v == null) return "null";
		if (v instanceof Number || v instanceof Boolean || v instanceof String || v instanceof Character || v instanceof Enum) return v.toString();
		if (v.getClass().isArray()){
			Class<?> c = v.getClass().getComponentType();
			if (c.isPrimitive()){
				switch (c.getName()){
					case "int": return Arrays.toString((int[]) v);
					case "long": return Arrays.toString((long[]) v);
					case "boolean": return Arrays.toString((boolean[]) v);
					case "float": return Arrays.toString((float[]) v);
					case "byte": return Arrays.toString((byte[]) v);
					default: return "array" + java.lang.reflect.Array.getLength(v);
				}
			}
			return "objarray" + java.lang.reflect.Array.getLength(v);
		}
		//Objects are compared by what they hold when that is plain (so a restored copy equals the original) and by size/class otherwise.
		if (v instanceof Collection) return "collection" + ((Collection<?>) v).size() + plainContent((Collection<?>) v);
		if (v instanceof Map) return "map" + ((Map<?, ?>) v).size() + plainContent(((Map<?, ?>) v).keySet());
		if (v instanceof Class) return ((Class<?>) v).getName();
		return v.getClass().getName();
	}

	private static String plainContent( Collection<?> c ){
		List<String> parts = new ArrayList<>();
		for (Object o : c){
			if (o instanceof Number || o instanceof Boolean || o instanceof String || o instanceof Enum || o instanceof Class) parts.add(String.valueOf(o instanceof Class ? ((Class<?>) o).getName() : o));
			else return "";
		}
		java.util.Collections.sort(parts);
		return parts.toString();
	}

	private static Map<String, String> statics() throws Exception {
		Map<String, String> out = new TreeMap<>();
		for (String name : classNames()){
			Class<?> c;
			try {
				c = Class.forName(name, false, ReplayStaticAuditTest.class.getClassLoader());
			} catch (Throwable t){
				continue;
			}
			Field[] fields;
			try {
				fields = c.getDeclaredFields();
			} catch (Throwable t){
				continue;
			}
			for (Field f : fields){
				if (!Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers()) && f.getType().isPrimitive()) continue;
				if (f.isSynthetic()) continue;
				try {
					f.setAccessible(true);
					out.put(name + "." + f.getName(), describe(f.get(null)));
				} catch (Throwable t){
					//a class that cannot be initialised here is not touched by generation either
				}
			}
		}
		return out;
	}

	@Test
	public void replayChangesOnlyClassifiedStatics() throws Exception {
		HeadlessTestSupport.start();
		//a lived-in state to start from
		LevelGenerationHarness.generate(2468L, 0, HeroClass.MAGE, LevelGenerationHarness.Context.FRESH, 6);
		Map<String, String> before = statics();
		FloorReplay.replay(13579L, 0, HeroClass.HUNTRESS, FloorReplay.LAST_FLOOR);
		Map<String, String> after = statics();

		TreeSet<String> changed = new TreeSet<>();
		for (Map.Entry<String, String> e : before.entrySet()){
			String now = after.get(e.getKey());
			if (now != null && !now.equals(e.getValue())) changed.add(e.getKey());
		}
		System.out.println("REPLAY CHANGED STATICS (" + changed.size() + "):\n" + String.join("\n", changed));
		assertTrue(before.size() > 500);
		assertEquals("statics a replay changes (pinned list)", EXPECTED_TEXT, String.join("\n", changed));
	}

	//All three are rebuilt before they are read: SecretRoom.regionSecretsThisRun by SecretRoom.initForRun / restoreRoomsFromBundle (new
	//or continued game), ArmoryRoom.prizeCats at the start of every armory paint, SpecialRoom.floorSpecials by initForFloor on every floor.
	private static final String EXPECTED_TEXT = "com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom.regionSecretsThisRun\n"
			+ "com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ArmoryRoom.prizeCats\n"
			+ "com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom.floorSpecials";
}
