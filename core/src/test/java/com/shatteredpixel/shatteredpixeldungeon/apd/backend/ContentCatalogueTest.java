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

package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ContentCatalogueTest {

	private static final long T0 = 1_000, T1 = 10_000_000, NOW = 5_000;
	private static final String RULES = "r1-37d60350c6b1";
	private static final Id AUTHOR = Id.of(Id.Kind.PLAYER, "pl_author");

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private static byte[] pkg( String s ){
		return s.getBytes(StandardCharsets.UTF_8);
	}

	private static ContentVersion version( String id, int major, byte[] bytes, String rules ){
		return new ContentVersion(Id.of(Id.Kind.CONTENT, id), AUTHOR, ContentVersion.Kind.MOD, major, 0, 0, Canon.hex(Canon.sha256(bytes)), rules, 100);
	}

	private static ContentCatalogue.Entry approved( TestAuthority a, ContentVersion v ){
		return ContentCatalogue.entry(v, 200, a.keyId, a.sign(ContentVersion.approvalBytes(v, 200, a.keyId)));
	}

	private static ContentCatalogue catalogue( TestAuthority a, long seq, long expires, List<ContentCatalogue.Entry> entries, String... revoked ){
		List<String> rev = Arrays.asList(revoked);
		return new ContentCatalogue(seq, 2_000, expires, a.keyId, entries, rev, a.sign(ContentCatalogue.signedBytes(seq, 2_000, expires, a.keyId, entries, rev)));
	}

	@Test
	public void aSignedCatalogueRoundTripsThroughTextAndVerifies(){
		TestAuthority a = new TestAuthority("cat-1");
		ContentVersion v = version("ct_one", 1, pkg("one"), RULES);
		ContentCatalogue c = catalogue(a, 1, T1, Collections.singletonList(approved(a, v)), "ct_old@1.0.0");
		ContentCatalogue back = ContentCatalogue.parse(c.toText());
		assertEquals(ContentCatalogue.Verdict.OK, back.verify(a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1), NOW));
		assertEquals(1, back.entries().size());
		assertEquals(c.toText(), back.toText());
	}

	@Test
	public void tamperingWithAnythingBreaksTheSignature(){
		TestAuthority a = new TestAuthority("cat-1");
		TrustRoot trust = a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1);
		ContentVersion v = version("ct_one", 1, pkg("one"), RULES);
		String text = catalogue(a, 3, T1, Collections.singletonList(approved(a, v)), "ct_bad@1.0.0").toText();
		//dropping a revocation, changing the sequence, swapping the package hash: each must fail
		assertEquals(ContentCatalogue.Verdict.BAD_SIGNATURE, ContentCatalogue.parse(text.replace("revoked\tct_bad@1.0.0\n", "")).verify(trust, NOW));
		assertEquals(ContentCatalogue.Verdict.BAD_SIGNATURE, ContentCatalogue.parse(text.replace("seq\t3", "seq\t99")).verify(trust, NOW));
		assertEquals(ContentCatalogue.Verdict.BAD_SIGNATURE, ContentCatalogue.parse(text.replace(v.packageHashHex, Canon.hex(Canon.sha256(pkg("evil"))))).verify(trust, NOW));
	}

	@Test
	public void aKeyForAnotherPurposeOrAnUnknownKeyIsRefused(){
		TestAuthority a = new TestAuthority("cat-1"), other = new TestAuthority("cat-2");
		ContentCatalogue c = catalogue(a, 1, T1, Collections.<ContentCatalogue.Entry>emptyList());
		assertEquals(ContentCatalogue.Verdict.KEY_NOT_ALLOWED, c.verify(a.trustFor(TrustRoot.Purpose.DEVELOPER_GRANT, T0, T1), NOW));
		assertEquals(ContentCatalogue.Verdict.UNKNOWN_KEY, c.verify(other.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1), NOW));
		assertEquals(ContentCatalogue.Verdict.KEY_NOT_ALLOWED, c.verify(a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1).revoke("cat-1"), NOW));
		assertEquals(ContentCatalogue.Verdict.KEY_NOT_VALID, c.verify(a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, 9_000_000, T1), NOW));
	}

	@Test
	public void anExpiredCatalogueIsExpired(){
		TestAuthority a = new TestAuthority("cat-1");
		ContentCatalogue c = catalogue(a, 1, 4_000, Collections.<ContentCatalogue.Entry>emptyList());
		assertEquals(ContentCatalogue.Verdict.EXPIRED, c.verify(a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1), NOW));
	}

	@Test
	public void malformedTextIsRejectedWholeNotHalfRead(){
		TestAuthority a = new TestAuthority("cat-1");
		String text = catalogue(a, 1, T1, Collections.singletonList(approved(a, version("ct_one", 1, pkg("one"), RULES)))).toText();
		for (String bad : new String[]{ "", "apd.catalogue 2\n", text.replace("seq\t1", "seq\tx"), text + "mystery\tx\n", text.replace("sig\t", "sigx\t"),
				text.replace("\nkey\t" + a.keyId, "") }){
			try {
				ContentCatalogue.parse(bad);
				fail("accepted: " + bad);
			} catch (IllegalArgumentException expected){
			}
		}
	}

	@Test
	public void theKeeperAcceptsOnlyNewerVerifiedCataloguesAndSurvivesRestart() throws Exception {
		TestAuthority a = new TestAuthority("cat-1");
		TrustRoot trust = a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1);
		File f = new File(tmp.getRoot(), "catalogue.txt");
		CatalogueKeeper k = new CatalogueKeeper(f, trust);
		assertNull(k.load(NOW));
		ContentVersion v = version("ct_one", 1, pkg("one"), RULES);
		ContentCatalogue c2 = catalogue(a, 2, T1, Collections.singletonList(approved(a, v)));
		assertEquals(CatalogueKeeper.Result.ACCEPTED, k.offer(c2.toText(), NOW));
		//older and equal sequences never replace it, even if validly signed
		assertEquals(CatalogueKeeper.Result.SAME_OR_OLDER, k.offer(catalogue(a, 1, T1, Collections.<ContentCatalogue.Entry>emptyList()).toText(), NOW));
		assertEquals(CatalogueKeeper.Result.SAME_OR_OLDER, k.offer(c2.toText(), NOW));
		assertEquals(CatalogueKeeper.Result.MALFORMED, k.offer("garbage", NOW));
		assertEquals(2, k.held().sequence);
		//a new keeper on the same file (app restart) holds the same catalogue
		CatalogueKeeper again = new CatalogueKeeper(f, trust);
		assertEquals(2, again.load(NOW).sequence);
		assertEquals(1, again.offered(RULES, NOW).size());
	}

	@Test
	public void aRevocationOnlyArrivesInANewerCatalogueAndAReplayCannotUndoIt(){
		TestAuthority a = new TestAuthority("cat-1");
		TrustRoot trust = a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1);
		CatalogueKeeper k = new CatalogueKeeper(new File(tmp.getRoot(), "c.txt"), trust);
		byte[] bytes = pkg("one");
		ContentVersion v = version("ct_one", 1, bytes, RULES);
		ContentCatalogue before = catalogue(a, 1, T1, Collections.singletonList(approved(a, v)));
		ContentCatalogue after = catalogue(a, 2, T1, Collections.singletonList(approved(a, v)), v.key());
		assertEquals(CatalogueKeeper.Result.ACCEPTED, k.offer(before.toText(), NOW));
		assertTrue(k.mayLoad(v, bytes, NOW));
		assertEquals(CatalogueKeeper.Result.ACCEPTED, k.offer(after.toText(), NOW));
		assertFalse(k.mayLoad(v, bytes, NOW));
		assertTrue(k.offered(RULES, NOW).isEmpty());
		//the attacker replays the old catalogue
		assertEquals(CatalogueKeeper.Result.SAME_OR_OLDER, k.offer(before.toText(), NOW));
		assertFalse(k.mayLoad(v, bytes, NOW));
	}

	@Test
	public void contentForAnotherRulesetEraTamperedBytesAndStaleCataloguesAreNotOffered(){
		TestAuthority a = new TestAuthority("cat-1");
		TrustRoot trust = a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1);
		CatalogueKeeper k = new CatalogueKeeper(new File(tmp.getRoot(), "c.txt"), trust);
		ContentVersion mine = version("ct_mine", 1, pkg("mine"), RULES), theirs = version("ct_theirs", 1, pkg("theirs"), "r9-other");
		assertEquals(CatalogueKeeper.Result.ACCEPTED, k.offer(catalogue(a, 1, 9_000, Arrays.asList(approved(a, mine), approved(a, theirs))).toText(), NOW));
		assertEquals(1, k.offered(RULES, NOW).size());
		assertEquals("ct_mine@1.0.0", k.offered(RULES, NOW).get(0).version.key());
		assertTrue(k.mayLoad(mine, pkg("mine"), NOW));
		assertFalse("a package that does not match the approved hash", k.mayLoad(mine, pkg("evil"), NOW));
		assertFalse("not listed", k.mayLoad(version("ct_none", 1, pkg("x"), RULES), pkg("x"), NOW));
		//past its expiry nothing is OFFERED, but revocations in it still count
		assertTrue(k.offered(RULES, 9_001).isEmpty());
	}

	@Test
	public void aDamagedOrNewlyDistrustedCacheFileIsIgnoredNotACrash() throws Exception {
		TestAuthority a = new TestAuthority("cat-1");
		File f = new File(tmp.getRoot(), "c.txt");
		CatalogueKeeper k = new CatalogueKeeper(f, a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1));
		k.offer(catalogue(a, 1, T1, Collections.<ContentCatalogue.Entry>emptyList()).toText(), NOW);
		//the signing key is revoked after the file was saved
		assertNull(new CatalogueKeeper(f, a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1).revoke("cat-1")).load(NOW));
		Files.write(f.toPath(), "damaged".getBytes(StandardCharsets.UTF_8));
		assertNull(new CatalogueKeeper(f, a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1)).load(NOW));
	}

	@Test
	public void aStaleKeeperCannotReplayAnOlderCatalogueOverANewerOne() throws Exception {
		TestAuthority a = new TestAuthority("cat-1");
		TrustRoot trust = a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1);
		File f = new File(tmp.getRoot(), "shared-catalogue.txt");
		CatalogueKeeper stale = new CatalogueKeeper(f, trust);
		CatalogueKeeper current = new CatalogueKeeper(f, trust);
		ContentCatalogue older = catalogue(a, 1, T1, Collections.<ContentCatalogue.Entry>emptyList());
		ContentCatalogue newer = catalogue(a, 2, T1, Collections.<ContentCatalogue.Entry>emptyList());

		assertEquals(CatalogueKeeper.Result.ACCEPTED, current.offer(newer.toText(), NOW));
		assertEquals(CatalogueKeeper.Result.SAME_OR_OLDER, stale.offer(older.toText(), NOW));
		assertEquals(2, new CatalogueKeeper(f, trust).load(NOW).sequence);
	}

	@Test
	public void separateKeepersSerializeTheirCompareAndReplace() throws Exception {
		TestAuthority a = new TestAuthority("cat-1");
		TrustRoot trust = a.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1);
		File f = new File(tmp.getRoot(), "concurrent-catalogue.txt");
		CatalogueKeeper first = new CatalogueKeeper(f, trust);
		CatalogueKeeper second = new CatalogueKeeper(f, trust);
		String one = catalogue(a, 1, T1, Collections.<ContentCatalogue.Entry>emptyList()).toText();
		String two = catalogue(a, 2, T1, Collections.<ContentCatalogue.Entry>emptyList()).toText();

		CountDownLatch firstInside = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);
		CountDownLatch secondStarted = new CountDownLatch(1);
		CountDownLatch secondInside = new CountDownLatch(1);
		AtomicReference<CatalogueKeeper.Result> firstResult = new AtomicReference<>();
		AtomicReference<CatalogueKeeper.Result> secondResult = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread firstThread = new Thread(new Runnable(){
			@Override public void run(){
				try {
					firstResult.set(first.offer(one, NOW, new CatalogueKeeper.OfferHook(){
						@Override public void insideStoreLock( File temp ) throws InterruptedException {
							firstInside.countDown();
							releaseFirst.await();
						}
					}));
				} catch (Throwable t){
					failure.compareAndSet(null, t);
				}
			}
		}, "first-catalogue-offer");
		Thread secondThread = new Thread(new Runnable(){
			@Override public void run(){
				secondStarted.countDown();
				try {
					secondResult.set(second.offer(two, NOW, new CatalogueKeeper.OfferHook(){
						@Override public void insideStoreLock( File temp ){
							secondInside.countDown();
						}
					}));
				} catch (Throwable t){
					failure.compareAndSet(null, t);
				}
			}
		}, "second-catalogue-offer");

		firstThread.start();
		assertTrue(firstInside.await(5, TimeUnit.SECONDS));
		secondThread.start();
		assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
		try {
			assertFalse("a second keeper must not enter the shared replacement section", secondInside.await(200, TimeUnit.MILLISECONDS));
		} finally {
			releaseFirst.countDown();
		}
		firstThread.join(5000);
		secondThread.join(5000);
		assertFalse(firstThread.isAlive());
		assertFalse(secondThread.isAlive());
		assertNull(failure.get());
		assertEquals(CatalogueKeeper.Result.ACCEPTED, firstResult.get());
		assertEquals(CatalogueKeeper.Result.ACCEPTED, secondResult.get());
		assertTrue(secondInside.await(1, TimeUnit.SECONDS));
		assertEquals(2, new CatalogueKeeper(f, trust).load(NOW).sequence);
		assertFalse(new File(f.getPath() + ".tmp").exists());
	}

}
