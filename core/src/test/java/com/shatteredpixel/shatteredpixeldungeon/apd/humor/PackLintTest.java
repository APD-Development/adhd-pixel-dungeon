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
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PackLintTest {

	private static void rejects( String row, String fragment ){
		try {
			LinePack.parse( row, "t" );
			fail( "should have been refused: " + row );
		} catch (LinePack.PackException e){
			assertTrue( e.getMessage() + " should mention " + fragment, e.getMessage().contains( fragment ) );
		}
	}

	private static final String OK = "a.0001 | anytime | since=1.0 | - | 5 | Cleric is a fine class. |";

	@Test
	public void aWellFormedLineParses(){
		List<Line> l = LinePack.parse( OK + "\n# comment\n\n", "t" );
		assertEquals( 1, l.size() );
		assertEquals( "a.0001", l.get( 0 ).id );
		assertTrue( l.get( 0 ).counted );
	}

	@Test
	public void theLintRefusesBadLinesWithTheLineNumber(){
		rejects( OK + "\n" + OK, "t:2" );
		rejects( OK + "\n" + OK, "duplicate id" );
		rejects( "a.0001 | anytime | - | - | 5 | Cleric is bad. ", "7 fields" );
		rejects( "A.0001 | anytime | - | - | 5 | Cleric. |", "bad id" );
		rejects( "a.0001 | nowhere | - | - | 5 | Cleric. |", "unknown category" );
		rejects( "a.0001 | anytime | wrong | - | 5 | Cleric. |", "unknown flag" );
		rejects( "a.0001 | anytime | - | - | 0 | Cleric. |", "weight" );
		rejects( "a.0001 | anytime | - | - | 5 |  |", "clean wording" );
		rejects( "a.0001 | anytime | - | colour=red | 5 | Cleric. |", "unknown key" );
		rejects( "a.0001 | anytime | - | depth | 5 | Cleric. |", "cannot read condition" );
	}

	@Test
	public void cleanWordingHasNoProfanityButAdultMay(){
		rejects( "a.0001 | anytime | - | - | 5 | Cleric is a fucking class. |", "clean wording contains" );
		rejects( "a.0001 | anytime | - | - | 5 | Cleric is a SHIT class. |", "clean wording contains" );
		List<Line> l = LinePack.parse( "a.0001 | anytime | - | - | 5 | Cleric is a bad class. | Cleric is a fucking bad class.", "t" );
		assertEquals( "Cleric is a fucking bad class.", l.get( 0 ).render( Context.EMPTY, true ) );
		assertEquals( "Cleric is a bad class.", l.get( 0 ).render( Context.EMPTY, false ) );
	}

	@Test
	public void clericIsTheOnlyTargetButOtherClassesMayBeNamedAndPraised(){
		//a line must be about Cleric: a line that is only about another class is the wrong kind of vendetta
		rejects( "a.0001 | anytime | - | - | 5 | Warrior is a bad class. |", "about Cleric" );
		rejects( "a.0001 | anytime | - | - | 5 | Warrior is a bad class. | Warrior is a shit class.", "about Cleric" );
		//other classes can be compared, praised and mentioned, as long as Cleric is the one being roasted
		List<Line> ok = LinePack.parse(
				"a.0001 | anytime | - | - | 5 | Even Warrior is embarrassed to share a character-selection screen with Cleric. |\n"
				+ "a.0002 | anytime | - | - | 5 | Mage burns things and gets respect. Cleric holds a book and gets pity. | Mage burns things and gets respect. Cleric holds a book and gets a pamphlet.\n"
				+ "a.0003 | anytime | - | hero=WARRIOR | 5 | You picked Warrior. Good. Cleric would have picked a candle. |\n"
				+ "a.0004 | anytime | - | lastHero!=HUNTRESS & runVerified=1 | 5 | Huntress is great. Cleric is the problem. |\n", "t" );
		assertEquals( 4, ok.size() );
		//a class condition must still name a class code
		rejects( "a.0001 | anytime | - | hero=not a class | 5 | Cleric. |", "class code" );
	}

	@Test
	public void placeholdersAreOnlyRecordedFacts(){
		rejects( "a.0001 | anytime | - | - | 5 | Cleric {nonsense}. |", "unknown placeholder" );
		rejects( "a.0001 | anytime | joke | - | 5 | Cleric died {depth} floors ago. |", "joke" );
		rejects( "a.0001 | anytime | - | depth>=1 | 5 | Cleric died on {depth}. | Cleric died on the floor.", "same {placeholders}" );
	}

	@Test
	public void theMetaFlagAndCategoryGoTogether(){
		rejects( "a.0001 | meta | - | - | 5 | You found the counter. |", "meta" );
		rejects( "a.0001 | anytime | meta | - | 5 | Cleric. |", "meta" );
		List<Line> l = LinePack.parse( "a.0001 | meta | meta | - | 5 | You found the counter. |", "t" );
		assertFalse( l.get( 0 ).counted );
	}

	@Test
	public void theShippedPacksAreCleanAndAllLoad() throws Exception {
		List<Line> lines = HumorTestSupport.shipped();
		int files = HumorTestSupport.SHIPPED.names().size();
		//loadPacks drops a broken pack silently, so parse each one strictly here
		int parsed = 0;
		for (String name : HumorTestSupport.SHIPPED.names()) parsed += LinePack.parse( HumorTestSupport.SHIPPED.read( name ), name ).size();
		assertEquals( "every shipped pack passes its lint", parsed, lines.size() );
		assertTrue( files == 5 && lines.size() == 50 );
		for (Line l : lines){
			assertFalse( l.id, LinePack.PROFANITY.matcher( l.clean ).find() );
			assertTrue( l.id, l.clean.length() <= LinePack.MAX_CLEAN );
		}
	}

	@Test
	public void curatedIdsRemainStable() throws Exception {
		//Owner-approved curation removes retired entries without renaming or reusing retained IDs.
		Set<String> now = new HashSet<>();
		for (Line l : HumorTestSupport.shipped()) now.add( l.id );
		File list = new File( System.getProperty( "apd.humor.ids", "../../test/resources/apd/cleric_complaint_ids.txt" ) );
		assertTrue( "committed id list " + list.getAbsolutePath(), list.isFile() );
		for (String id : new String( Files.readAllBytes( list.toPath() ), StandardCharsets.UTF_8 ).split( "\r?\n" )){
			if (id.trim().isEmpty() || id.startsWith( "#" )) continue;
			assertTrue( "id " + id + " is retained in the curated collection and must still exist", now.contains( id.trim() ) );
		}
	}
}
