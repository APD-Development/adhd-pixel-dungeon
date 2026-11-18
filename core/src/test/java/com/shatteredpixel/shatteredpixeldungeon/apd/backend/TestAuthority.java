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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

//The test stand-in for developer/server authority. It holds a PRIVATE key, which the app never does: this class lives in the test sources on
//purpose (BackendArchitectureTest checks that nothing in the shipped code can sign).
final class TestAuthority {

	final KeyPair pair;
	final String keyId;

	TestAuthority( String keyId ){
		try {
			KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
			g.initialize(new ECGenParameterSpec("secp256r1"));
			pair = g.generateKeyPair();
			this.keyId = keyId;
		} catch (Exception e){
			throw new IllegalStateException(e);
		}
	}

	byte[] sign( byte[] message ){
		try {
			Signature s = Signature.getInstance(Verifier.PublicKeyVerifier.ALGORITHM);
			s.initSign(pair.getPrivate());
			s.update(message);
			return s.sign();
		} catch (Exception e){
			throw new IllegalStateException(e);
		}
	}

	String publicKeyBase64(){
		return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
	}

	TrustRoot trustFor( TrustRoot.Purpose purpose, long from, long until ){
		return new TrustRoot().add(keyId, purpose, Verifier.PublicKeyVerifier.fromBase64(publicKeyBase64()), from, until);
	}

	AchievementGrant grant( String grantId, Id player, String achievement, long at ){
		Id g = Id.of(Id.Kind.GRANT, grantId), a = Id.of(Id.Kind.ACHIEVEMENT, achievement);
		return new AchievementGrant(g, player, a, at, keyId, sign(AchievementGrant.signedBytes(g, player, a, at, keyId)));
	}
}
