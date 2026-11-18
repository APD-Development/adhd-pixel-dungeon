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

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

// A signed list of approved community content, meant to be served as a plain file from anywhere (a CDN, a mirror, a USB stick): the
// transport is never trusted, only the signatures are. Provider-neutral, read-only, nothing here talks to a network.
//
// One signature covers the whole catalogue (sequence, dates, every entry's key and package hash, every revocation), so an entry cannot be
// added, dropped or swapped. Each entry also carries its own approval signature (ContentVersion.approvalBytes) that is checked when the
// content is loaded. A higher sequence number always replaces a lower one; CatalogueKeeper refuses to go backwards.
//
// Text form, one tab-separated record per line (blank lines ignored):
//   apd.catalogue 1
//   seq N / issued MS / expires MS / key KEYID
//   entry contentId kind version packageHashHex rulesetId approvedAtMs approvalKeyId approvalSigBase64 authorId createdAtMs
//   revoked contentId@version
//   sig BASE64
public final class ContentCatalogue {

	public static final String HEADER = "apd.catalogue 1";

	public static final class Entry {
		public final ContentVersion version;
		public final long approvedAtMillis;
		public final String approvalKeyId;
		public final byte[] approvalSignature;

		Entry( ContentVersion version, long approvedAtMillis, String approvalKeyId, byte[] approvalSignature ){
			this.version = version;
			this.approvedAtMillis = approvedAtMillis;
			this.approvalKeyId = approvalKeyId;
			this.approvalSignature = approvalSignature;
		}

		//may these downloaded bytes be loaded as this entry? (hash match, valid approval, entry not revoked is checked by the caller via isRevoked)
		public boolean mayLoad( byte[] downloaded, TrustRoot trust, long nowMillis ){
			return ContentVersion.clientMayLoad(version, downloaded, approvedAtMillis, approvalKeyId, approvalSignature, trust, nowMillis);
		}
	}

	public enum Verdict { OK, BAD_SIGNATURE, UNKNOWN_KEY, KEY_NOT_ALLOWED, KEY_NOT_VALID, EXPIRED }

	public final long sequence, issuedAtMillis, expiresAtMillis;
	public final String keyId;
	private final List<Entry> entries;
	private final TreeSet<String> revoked;
	private final byte[] signature;

	public ContentCatalogue( long sequence, long issuedAtMillis, long expiresAtMillis, String keyId, List<Entry> entries, List<String> revoked, byte[] signature ){
		if (sequence < 1 || expiresAtMillis <= issuedAtMillis || keyId == null || keyId.isEmpty() || signature == null) throw new IllegalArgumentException("bad catalogue");
		this.sequence = sequence;
		this.issuedAtMillis = issuedAtMillis;
		this.expiresAtMillis = expiresAtMillis;
		this.keyId = keyId;
		this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
		this.revoked = new TreeSet<>(revoked);
		this.signature = signature.clone();
	}

	public static Entry entry( ContentVersion v, long approvedAtMillis, String approvalKeyId, byte[] approvalSignature ){
		return new Entry(v, approvedAtMillis, approvalKeyId, approvalSignature);
	}

	public List<Entry> entries(){
		return entries;
	}

	public boolean isRevoked( ContentVersion v ){
		return revoked.contains(v.key());
	}

	//the entries a client running ruleset 'rulesetId' may offer: right era, not revoked
	public List<Entry> available( String rulesetId ){
		List<Entry> out = new ArrayList<>();
		for (Entry e : entries){
			if (e.version.rulesetId.equals(rulesetId) && !isRevoked(e.version)) out.add(e);
		}
		return out;
	}

	//what the catalogue key signs: order of entries in the file never matters
	public static byte[] signedBytes( long sequence, long issued, long expires, String keyId, List<Entry> entries, List<String> revoked ){
		TreeSet<String> pairs = new TreeSet<>();
		for (Entry e : entries) pairs.add(e.version.key() + "#" + e.version.packageHashHex + "#" + e.version.rulesetId + "#" + e.version.kind);
		TreeSet<String> rev = new TreeSet<>(revoked);
		Canon c = new Canon("apd.content-catalogue", 1).num(sequence).num(issued).num(expires).str(keyId).num(pairs.size());
		for (String p : pairs) c.str(p);
		c.num(rev.size());
		for (String r : rev) c.str(r);
		return c.toBytes();
	}

	public Verdict verify( TrustRoot trust, long nowMillis ){
		TrustRoot.Check c = trust.check(keyId, TrustRoot.Purpose.CONTENT_APPROVAL, signedBytes(sequence, issuedAtMillis, expiresAtMillis, keyId, entries, new ArrayList<>(revoked)), signature, nowMillis);
		switch (c){
			case OK: break;
			case BAD_SIGNATURE: return Verdict.BAD_SIGNATURE;
			case UNKNOWN_KEY: return Verdict.UNKNOWN_KEY;
			case WRONG_PURPOSE: case REVOKED: return Verdict.KEY_NOT_ALLOWED;
			default: return Verdict.KEY_NOT_VALID;
		}
		return nowMillis > expiresAtMillis ? Verdict.EXPIRED : Verdict.OK;
	}

	public String toText(){
		StringBuilder sb = new StringBuilder(HEADER).append('\n');
		sb.append("seq\t").append(sequence).append('\n').append("issued\t").append(issuedAtMillis).append('\n').append("expires\t").append(expiresAtMillis).append('\n')
				.append("key\t").append(keyId).append('\n');
		for (Entry e : entries){
			ContentVersion v = e.version;
			sb.append("entry\t").append(v.contentId.value).append('\t').append(v.kind.name()).append('\t').append(v.versionText()).append('\t').append(v.packageHashHex).append('\t')
					.append(v.rulesetId).append('\t').append(e.approvedAtMillis).append('\t').append(e.approvalKeyId).append('\t')
					.append(Base64.getEncoder().encodeToString(e.approvalSignature)).append('\t').append(v.author.value).append('\t').append(v.createdAtMillis).append('\n');
		}
		for (String r : revoked) sb.append("revoked\t").append(r).append('\n');
		sb.append("sig\t").append(Base64.getEncoder().encodeToString(signature)).append('\n');
		return sb.toString();
	}

	//strict: any malformed line rejects the whole file (a half-read catalogue could hide a revocation)
	public static ContentCatalogue parse( String text ){
		long seq = -1, issued = -1, expires = -1;
		String key = null;
		byte[] sig = null;
		List<Entry> entries = new ArrayList<>();
		List<String> revoked = new ArrayList<>();
		boolean header = false;
		for (String line : text.split("\\r?\\n")){
			if (line.isEmpty()) continue;
			if (!header){
				if (!line.equals(HEADER)) throw new IllegalArgumentException("not a catalogue this version can read");
				header = true;
				continue;
			}
			String[] p = line.split("\t", -1);
			try {
				switch (p[0]){
					case "seq": seq = Long.parseLong(one(p)); break;
					case "issued": issued = Long.parseLong(one(p)); break;
					case "expires": expires = Long.parseLong(one(p)); break;
					case "key": key = one(p); break;
					case "sig": sig = Base64.getDecoder().decode(one(p)); break;
					case "revoked": revoked.add(one(p)); break;
					case "entry": {
						if (p.length != 11) throw new IllegalArgumentException("entry fields");
						String[] v = p[3].split("\\.");
						if (v.length != 3) throw new IllegalArgumentException("version");
						ContentVersion cv = new ContentVersion(Id.of(Id.Kind.CONTENT, p[1]), Id.of(Id.Kind.PLAYER, p[9]), ContentVersion.Kind.valueOf(p[2]),
								Integer.parseInt(v[0]), Integer.parseInt(v[1]), Integer.parseInt(v[2]), p[4], p[5], Long.parseLong(p[10]));
						entries.add(new Entry(cv, Long.parseLong(p[6]), p[7], Base64.getDecoder().decode(p[8])));
						break;
					}
					default: throw new IllegalArgumentException("unknown record " + p[0]);
				}
			} catch (RuntimeException e){
				throw new IllegalArgumentException("bad catalogue line: " + line, e);
			}
		}
		if (!header || key == null || sig == null) throw new IllegalArgumentException("incomplete catalogue");
		return new ContentCatalogue(seq, issued, expires, key, entries, revoked, sig);
	}

	private static String one( String[] p ){
		if (p.length != 2) throw new IllegalArgumentException("fields");
		return p[1];
	}
}
