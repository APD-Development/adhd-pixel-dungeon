/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.apd.humor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.watabou.utils.Random;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// The humor code is flavor only. These tests pin that: it cannot reach the game's random numbers or state, and the game reaches it only
// through the few files that are meant to.
public class HumorIsolationTest {

	@Test
	public void usingTheGrudgeDoesNotMoveTheGamesRandomNumbers(){
		int[] baseline = draw( false );
		int[] withGrudge = draw( true );
		assertEquals( Arrays.toString( baseline ), Arrays.toString( withGrudge ) );
	}

	private static int[] draw( boolean useGrudge ){
		Random.pushGenerator( 4242 );
		try {
			int[] out = new int[10];
			for (int i = 0; i < 5; i++) out[i] = Random.Int( 1000 );
			if (useGrudge){
				ClericGrudge g = new ClericGrudge( HumorTestSupport.shipped(), new Ledger( null ), new java.util.Random( 5 ), true );
				Context ctx = Context.EMPTY.with( "lastHero", "CLERIC" ).with( "lastOutcome", "DIED" ).with( "depth", 12 );
				for (int i = 0; i < 400; i++){
					Shown shown = g.popup( Moment.RUN_END_DEATH, ctx, Safety.CALM, HumorTestSupport.prefs( Frequency.RARE ), i % 2 == 0, 1_000_000_000_000L + i * 86_400_000L );
					if (shown != null) g.confirm( shown, 1_000_000_000_000L + i * 86_400_000L );
					g.inlineAlways( Moment.RUN_END_DEATH, ctx, i );
					g.counter();
				}
				g.firstClericRun( HumorTestSupport.prefs( Frequency.RARE ), false, 1 );
			}
			for (int i = 5; i < 10; i++) out[i] = Random.Int( 1000 );
			return out;
		} finally {
			Random.popGenerator();
		}
	}

	private static File humorSources(){
		return new File( System.getProperty( "apd.game.src" ), "apd/humor" );
	}

	@Test
	public void theHumorPackageDependsOnNothingButTheJavaLibrary() throws Exception {
		File[] files = humorSources().listFiles();
		assertTrue( files != null && files.length >= 8 );
		Pattern imp = Pattern.compile( "^import\\s+(static\\s+)?([\\w.]+);", Pattern.MULTILINE );
		for (File f : files){
			String src = new String( Files.readAllBytes( f.toPath() ), StandardCharsets.UTF_8 );
			Matcher m = imp.matcher( src );
			while (m.find()){
				String i = m.group( 2 );
				assertTrue( f.getName() + " imports " + i + ": the humor code may use only java.*", i.startsWith( "java." ) );
			}
			//the game's own random source, settings and state are out of reach
			String body = src.substring( src.indexOf( "package " ) );
			assertFalse( f.getName(), body.contains( "com.watabou" ) || Pattern.compile( "\\b(Dungeon|Hero)\\b" ).matcher( body ).find() );
		}
	}

	@Test
	public void onlyTheIntendedGameFilesTouchTheHumorPackage() throws Exception {
		Set<String> allowed = new HashSet<>( Arrays.asList(
				"windows/WndClericGrudge.java", "windows/GrudgeHooks.java" ) );
		File root = new File( System.getProperty( "apd.game.src" ) );
		List<String> offenders = new ArrayList<>();
		scan( root, root, allowed, offenders );
		assertTrue( "game files that use the humor package without being meant to: " + offenders, offenders.isEmpty() );
	}

	@Test
	public void onlyTheIntendedFilesCallTheGamesSideOfTheGrudge() throws Exception {
		Set<String> allowed = new HashSet<>( Arrays.asList( "Dungeon.java", "ShatteredPixelDungeon.java", "windows/APDSettingsPane.java",
				"windows/WndClericGrudge.java", "windows/GrudgeHooks.java", "scenes/HeroSelectScene.java" ) );
		File root = new File( System.getProperty( "apd.game.src" ) );
		List<String> offenders = new ArrayList<>();
		scanFor( "GrudgeHooks", root, root, allowed, offenders );
		assertTrue( "files calling GrudgeHooks without being meant to: " + offenders, offenders.isEmpty() );
	}

	@Test
	public void theGamesSideDoesNothingWhileTheSwitchIsOff() throws Exception {
		assertFalse( com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.available() );
		//both entry points return before looking at anything in the game
		com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.tick();
		com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.onRunEnded( true, null );
		String hooks = new String( Files.readAllBytes( new File( System.getProperty( "apd.game.src" ), "windows/GrudgeHooks.java" ).toPath() ), StandardCharsets.UTF_8 );
		for (String method : new String[]{ "public static void onRunEnded", "public static void tick" }){
			String body = hooks.substring( hooks.indexOf( method ) );
			body = body.substring( body.indexOf( '{' ) + 1 ).trim();
			assertTrue( method + " starts with the switch", body.startsWith( "if (!ClericGrudge.enabled()) return;" ) );
		}
	}

	@Test
	public void theGamesSideCannotSpendATurnChangeStateOrTouchTheSave() throws Exception {
		//GrudgeHooks only reads the game; none of the calls that act, change, roll dice or save appear in it
		String hooks = new String( Files.readAllBytes( new File( System.getProperty( "apd.game.src" ), "windows/GrudgeHooks.java" ).toPath() ), StandardCharsets.UTF_8 );
		hooks = hooks.substring( hooks.indexOf( "package " ) );
		for (String forbidden : new String[]{ ".spend(", ".next()", "Actor.", "Random.", ".damage(", ".heal(", ".die(", ".move(", ".act(", "saveGame", "saveAll",
				"Bundle", "GLog", "Badges.", "Rankings.", "Statistics.total = ", "Dungeon.hero.pos", "SPDSettings.put" }){
			assertFalse( "GrudgeHooks must not use " + forbidden, hooks.contains( forbidden ) );
		}
		//and the humor ledger is the only file it writes, inside its own folder
		assertTrue( hooks.contains( "apd-humor/ledger.txt" ) );
	}

	private static void scanFor( String needle, File root, File dir, Set<String> allowed, List<String> offenders ) throws Exception {
		File[] list = dir.listFiles();
		if (list == null) return;
		for (File f : list){
			if (f.isDirectory()){
				scanFor( needle, root, f, allowed, offenders );
			} else if (f.getName().endsWith( ".java" )){
				String rel = root.toURI().relativize( f.toURI() ).getPath();
				String src = new String( Files.readAllBytes( f.toPath() ), StandardCharsets.UTF_8 );
				if (src.contains( needle ) && !allowed.contains( rel )) offenders.add( rel );
			}
		}
	}

	private static void scan( File root, File dir, Set<String> allowed, List<String> offenders ) throws Exception {
		File[] list = dir.listFiles();
		if (list == null) return;
		for (File f : list){
			if (f.isDirectory()){
				scan( root, f, allowed, offenders );
			} else if (f.getName().endsWith( ".java" )){
				String rel = root.toURI().relativize( f.toURI() ).getPath();
				if (rel.startsWith( "apd/humor/" )) continue;
				String src = new String( Files.readAllBytes( f.toPath() ), StandardCharsets.UTF_8 );
				if (src.contains( "apd.humor" ) && !allowed.contains( rel )) offenders.add( rel );
			}
		}
	}

	@Test
	public void noLineIsAboutAnyOtherCharacterAndClericIsTheOnlySubject() throws Exception {
		//the code has no per-class vendetta either: no other class is named anywhere in the package
		Pattern others = Pattern.compile( "\\b(warrior|mage|rogue|huntress|duelist)\\b", Pattern.CASE_INSENSITIVE );
		for (File f : humorSources().listFiles()){
			String src = new String( Files.readAllBytes( f.toPath() ), StandardCharsets.UTF_8 );
			Matcher m = others.matcher( src );
			while (m.find()){
				//only inside the lint's own deny-list pattern
				assertTrue( f.getName() + " mentions " + m.group(), f.getName().equals( "LinePack.java" ) );
			}
		}
	}
}
