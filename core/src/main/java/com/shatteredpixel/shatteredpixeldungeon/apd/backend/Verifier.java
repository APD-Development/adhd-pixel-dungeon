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

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

//Checks a signature. The app only ever holds PUBLIC keys, so it can recognise developer/server authority but cannot create it; there is
//deliberately no signing code anywhere in the app (BackendArchitectureTest fails if there is). The scheme is ECDSA P-256 with SHA-256,
//available on every Android version APD supports.
public interface Verifier {

	boolean verify( byte[] message, byte[] signature );

	final class PublicKeyVerifier implements Verifier {

		public static final String ALGORITHM = "SHA256withECDSA";

		private final PublicKey key;

		public PublicKeyVerifier( PublicKey key ){
			this.key = key;
		}

		//a key as published: base64 of the X.509 SubjectPublicKeyInfo encoding
		public static PublicKeyVerifier fromBase64( String x509Base64 ){
			try {
				byte[] der = Base64.getDecoder().decode(x509Base64.trim());
				return new PublicKeyVerifier(KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der)));
			} catch (Exception e){
				throw new IllegalArgumentException("not a usable public key", e);
			}
		}

		@Override
		public boolean verify( byte[] message, byte[] signature ){
			try {
				Signature s = Signature.getInstance(ALGORITHM);
				s.initVerify(key);
				s.update(message);
				return s.verify(signature);
			} catch (Exception e){
				return false;
			}
		}
	}
}
