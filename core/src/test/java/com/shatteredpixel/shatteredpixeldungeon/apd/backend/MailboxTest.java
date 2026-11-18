package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Collections;

public class MailboxTest {
	@Test public void fetchedNeverHasANullDetail(){
		Mailbox.Fetched f = new Mailbox.Fetched(true, Collections.<Envelope>emptyList(), null);
		assertTrue(f.ok);
		assertEquals("", f.detail);
		assertTrue(f.items.isEmpty());
		assertEquals("offline", new Mailbox.Fetched(false, Collections.<Envelope>emptyList(), "offline").detail);
	}

	@Test public void aMailboxIsJustARead(){
		Mailbox empty = () -> new Mailbox.Fetched(true, Collections.<Envelope>emptyList(), "");
		assertTrue(empty.fetch().ok);
	}
}
