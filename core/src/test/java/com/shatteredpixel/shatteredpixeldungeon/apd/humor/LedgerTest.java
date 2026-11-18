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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;

public class LedgerTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void whatWasShownIsRememberedAcrossRestarts() throws Exception {
		File f = new File( tmp.getRoot(), "humor/ledger.txt" );
		Ledger a = new Ledger( f );
		a.markShown( "x.1", 1000 );
		a.markShown( "x.2", 2000 );
		a.markShown( "x.1", 3000 );
		a.setState( "lastPopupAt", "3000" );
		a.save();

		Ledger b = new Ledger( f );
		assertEquals( 2, b.get( "x.1" ).count );
		assertEquals( 1, b.get( "x.2" ).count );
		assertEquals( 3000, b.get( "x.1" ).lastAt );
		assertEquals( 3000L, b.stateLong( "lastPopupAt", -1 ) );
		assertEquals( "the running counter continues", 3, b.seq() );
		b.markShown( "x.3", 4000 );
		assertEquals( 4, b.get( "x.3" ).lastSeq );
	}

	@Test
	public void aDamagedFileReadsAsFarAsItCanAndNeverThrows() throws Exception {
		File f = new File( tmp.getRoot(), "l.txt" );
		Files.write( f.toPath(), ("#apd-humor-ledger v1\n@seq=5\ngood.1\t2\t5\t99\nthis is garbage\nbad.2\tx\ty\tz\n\u0000\u0001\ngood.2\t1\t4\t98\n").getBytes( StandardCharsets.UTF_8 ) );
		Ledger l = new Ledger( f );
		assertEquals( 2, l.get( "good.1" ).count );
		assertEquals( 1, l.get( "good.2" ).count );
		assertNull( l.get( "bad.2" ) );
		//a file that is not even text
		Files.write( f.toPath(), new byte[]{ (byte)0xff, (byte)0xfe, 0, 1, 2, 3 } );
		assertEquals( 0, new Ledger( f ).seq() );
		//a missing file and an unwritable place are both fine
		new Ledger( new File( tmp.getRoot(), "nope/none.txt" ) );
		Ledger memory = new Ledger( null );
		memory.markShown( "a.1", 1 );
		memory.save();
		assertNotNull( memory.get( "a.1" ) );
	}

	@Test
	public void distinctCountsEachIdOnceAndOnlyForIdsInThePack(){
		Ledger l = new Ledger( null );
		for (int i = 0; i < 5; i++) l.markShown( "a.1", i );
		l.markShown( "a.2", 9 );
		l.markShown( "retired.1", 10 );
		assertEquals( 2, l.distinctSeenAmong( new HashSet<>( Arrays.asList( "a.1", "a.2", "a.3" ) ) ) );
	}

	@Test
	public void savingWritesOnlyTheLedgerFileInItsOwnFolderAndLeavesNoTemporaryFile() throws Exception {
		File dir = new File( tmp.getRoot(), "apd-humor" );
		Ledger l = new Ledger( new File( dir, "ledger.txt" ) );
		for (int i = 0; i < 20; i++){
			l.markShown( "a." + i, i );
			l.save();
		}
		String[] names = dir.list();
		assertEquals( "only the ledger, no leftover temporary file", 1, names.length );
		assertEquals( "ledger.txt", names[0] );
		assertEquals( "nothing was written next to the folder", 1, tmp.getRoot().list().length );
	}

	@Test
	public void theResetForgetsEverything() throws Exception {
		File f = new File( tmp.getRoot(), "l.txt" );
		Ledger l = new Ledger( f );
		l.markShown( "a.1", 1 );
		l.save();
		l.clear();
		assertEquals( 0, new Ledger( f ).seq() );
		assertNull( new Ledger( f ).get( "a.1" ) );
	}

	@Test
	public void automaticSaveDoesNotRewriteAFileWithDamagedRows() throws Exception {
		File f = new File( tmp.getRoot(), "l.txt" );
		byte[] original = ("#apd-humor-ledger v1\n@seq=2\ngood.1\t1\t2\t99\t99\n"
				+ "damaged\trow\n").getBytes( StandardCharsets.UTF_8 );
		Files.write( f.toPath(), original );

		Ledger loaded = new Ledger( f );
		loaded.markShown( "new.1", 100 );
		loaded.save();

		assertArrayEquals( "an automatic save must preserve every original byte after a partial load",
				original, Files.readAllBytes( f.toPath() ) );
		assertNull( "the refused write must not pretend it persisted the new entry", new Ledger( f ).get( "new.1" ) );

		loaded.clear();
		assertEquals( "an explicit player reset may replace a damaged ledger", 0, new Ledger( f ).seq() );
	}

	@Test
	public void saveNeverReplacesAnExistingDirectoryAtTheLedgerPath() throws Exception {
		File f = tmp.newFolder( "ledger.txt" );
		Ledger ledger = new Ledger( f );
		ledger.markShown( "a.1", 1 );
		ledger.save();

		assertTrue( "the configured directory must remain a directory", f.isDirectory() );
	}
}
