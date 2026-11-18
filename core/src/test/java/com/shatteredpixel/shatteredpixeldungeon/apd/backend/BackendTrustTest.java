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

import org.junit.Test;

import java.util.*;

public class BackendTrustTest {

	private static final long T0 = 1_000, T1 = 1_000_000;
	private static final Id ME = Id.of(Id.Kind.PLAYER, "pl_me"), OTHER = Id.of(Id.Kind.PLAYER, "pl_other");

	@Test
	public void aDeveloperGrantIsAcceptedOnceForTheRightPlayer(){
		TestAuthority dev = new TestAuthority("dev-1");
		GrantLedger ledger = new GrantLedger(ME, dev.trustFor(TrustRoot.Purpose.DEVELOPER_GRANT, T0, T1));
		AchievementGrant g = dev.grant("gr_1", ME, "ac_first", 5_000);
		assertEquals(GrantLedger.Result.ACCEPTED, ledger.accept(g, 6_000));
		assertTrue(ledger.has(Id.of(Id.Kind.ACHIEVEMENT, "ac_first")));
		assertEquals("granting twice changes nothing", GrantLedger.Result.ALREADY_HAVE, ledger.accept(g, 7_000));
		assertEquals(1, ledger.all().size());
	}

	@Test
	public void forgedOrAlteredGrantsAreRefused(){
		TestAuthority dev = new TestAuthority("dev-1"), attacker = new TestAuthority("dev-1");   //same key id, different key
		TrustRoot trust = dev.trustFor(TrustRoot.Purpose.DEVELOPER_GRANT, T0, T1);
		GrantLedger ledger = new GrantLedger(ME, trust);
		assertEquals("signed with another key", GrantLedger.Result.REJECTED_SIGNATURE, ledger.accept(attacker.grant("gr_2", ME, "ac_x", 5_000), 6_000));
		AchievementGrant real = dev.grant("gr_3", ME, "ac_x", 5_000);
		AchievementGrant moved = new AchievementGrant(real.grantId, ME, Id.of(Id.Kind.ACHIEVEMENT, "ac_other"), real.issuedAtMillis, real.keyId, real.signature);
		assertEquals("a different achievement under the same signature", GrantLedger.Result.REJECTED_SIGNATURE, ledger.accept(moved, 6_000));
		assertEquals("a grant for somebody else", GrantLedger.Result.WRONG_PLAYER, ledger.accept(dev.grant("gr_4", OTHER, "ac_x", 5_000), 6_000));
		assertEquals("a key the app does not know", GrantLedger.Result.REJECTED_SIGNATURE, ledger.accept(new TestAuthority("unknown").grant("gr_5", ME, "ac_x", 5_000), 6_000));
		assertTrue(ledger.all().isEmpty());
	}

	@Test
	public void keysHavePurposesWindowsAndCanBeRevoked(){
		TestAuthority dev = new TestAuthority("k");
		byte[] msg = {1, 2, 3};
		byte[] sig = dev.sign(msg);
		TrustRoot ok = dev.trustFor(TrustRoot.Purpose.DEVELOPER_GRANT, T0, T1);
		assertEquals(TrustRoot.Check.OK, ok.check("k", TrustRoot.Purpose.DEVELOPER_GRANT, msg, sig, 5_000));
		assertEquals(TrustRoot.Check.WRONG_PURPOSE, ok.check("k", TrustRoot.Purpose.CONTENT_APPROVAL, msg, sig, 5_000));
		assertEquals(TrustRoot.Check.NOT_YET_VALID, ok.check("k", TrustRoot.Purpose.DEVELOPER_GRANT, msg, sig, 5));
		assertEquals(TrustRoot.Check.EXPIRED, ok.check("k", TrustRoot.Purpose.DEVELOPER_GRANT, msg, sig, T1 + 1));
		assertEquals(TrustRoot.Check.BAD_SIGNATURE, ok.check("k", TrustRoot.Purpose.DEVELOPER_GRANT, new byte[]{9}, sig, 5_000));
		assertEquals(TrustRoot.Check.UNKNOWN_KEY, ok.check("zz", TrustRoot.Purpose.DEVELOPER_GRANT, msg, sig, 5_000));
		ok.revoke("k");
		assertEquals(TrustRoot.Check.REVOKED, ok.check("k", TrustRoot.Purpose.DEVELOPER_GRANT, msg, sig, 5_000));
	}

	@Test
	public void keyRotationKeepsOldGrantsValidWhileTheOldKeyIsTrusted(){
		TestAuthority k1 = new TestAuthority("k1"), k2 = new TestAuthority("k2");
		TrustRoot trust = k1.trustFor(TrustRoot.Purpose.DEVELOPER_GRANT, T0, T1)
				.add("k2", TrustRoot.Purpose.DEVELOPER_GRANT, Verifier.PublicKeyVerifier.fromBase64(k2.publicKeyBase64()), T0, T1);
		GrantLedger ledger = new GrantLedger(ME, trust);
		assertEquals(GrantLedger.Result.ACCEPTED, ledger.accept(k1.grant("gr_a", ME, "ac_1", 5_000), 6_000));
		assertEquals(GrantLedger.Result.ACCEPTED, ledger.accept(k2.grant("gr_b", ME, "ac_2", 5_000), 6_000));
		trust.revoke("k1");
		assertEquals("a leaked key's NEW grants stop working", GrantLedger.Result.REJECTED_SIGNATURE, ledger.accept(k1.grant("gr_c", ME, "ac_3", 5_000), 6_000));
	}

	private static ContentVersion version( byte[] bytes ){
		return new ContentVersion(Id.of(Id.Kind.CONTENT, "ct_hero"), ME, ContentVersion.Kind.CHARACTER, 1, 0, 0, Canon.hex(Canon.sha256(bytes)), "ruleset-1", 10);
	}

	@Test
	public void contentOnlyLoadsIfApprovedAndUntampered(){
		TestAuthority reviewer = new TestAuthority("rev-1");
		TrustRoot trust = reviewer.trustFor(TrustRoot.Purpose.CONTENT_APPROVAL, T0, T1);
		byte[] pkg = "the package".getBytes();
		ContentVersion v = version(pkg);
		byte[] approval = reviewer.sign(ContentVersion.approvalBytes(v, 5_000, "rev-1"));
		assertTrue(ContentVersion.clientMayLoad(v, pkg, 5_000, "rev-1", approval, trust, 6_000));
		assertFalse("tampered download", ContentVersion.clientMayLoad(v, "the package!".getBytes(), 5_000, "rev-1", approval, trust, 6_000));
		assertFalse("forged approval", ContentVersion.clientMayLoad(v, pkg, 5_000, "rev-1", new TestAuthority("rev-1").sign(ContentVersion.approvalBytes(v, 5_000, "rev-1")), trust, 6_000));
		assertFalse("approval for another time", ContentVersion.clientMayLoad(v, pkg, 5_001, "rev-1", approval, trust, 6_000));
		assertFalse("a grant-signing key cannot approve content",
				ContentVersion.clientMayLoad(v, pkg, 5_000, "rev-1", approval, reviewer.trustFor(TrustRoot.Purpose.DEVELOPER_GRANT, T0, T1), 6_000));
	}

	@Test
	public void contentStatesFollowTheTable(){
		assertTrue(ContentVersion.canMove(ContentVersion.State.DRAFT, ContentVersion.State.SUBMITTED));
		assertTrue(ContentVersion.canMove(ContentVersion.State.APPROVED, ContentVersion.State.REVOKED));
		assertFalse("cannot skip review", ContentVersion.canMove(ContentVersion.State.DRAFT, ContentVersion.State.APPROVED));
		assertFalse("a rejected version never comes back (publish a new one)", ContentVersion.canMove(ContentVersion.State.REJECTED, ContentVersion.State.APPROVED));
		for (ContentVersion.State s : ContentVersion.State.values()) assertFalse("revoked is final", ContentVersion.canMove(ContentVersion.State.REVOKED, s));
		assertEquals("ct_hero@1.0.0", version(new byte[]{1}).key());
	}
}
