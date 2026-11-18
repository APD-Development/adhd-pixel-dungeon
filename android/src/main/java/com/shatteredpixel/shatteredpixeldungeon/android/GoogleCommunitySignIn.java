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

package com.shatteredpixel.shatteredpixeldungeon.android;

import android.app.Activity;
import android.os.CancellationSignal;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.shatteredpixel.shatteredpixeldungeon.apd.community.CommunityController;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// "Sign in with Google" for Community, through Android's Credential Manager. Only the Google ID token goes to the
// Community server, which checks it against Google's keys. Cancel or any failure = null (back to the sign-in screen).
final class GoogleCommunitySignIn implements CommunityController.GoogleSource {

	private final Activity activity;
	private final String serverClientId;

	GoogleCommunitySignIn(Activity activity, String serverClientId){
		this.activity = activity;
		this.serverClientId = serverClientId;
	}

	@Override
	public void request(Consumer<String> done){
		AtomicBoolean answered = new AtomicBoolean(false);
		Consumer<String> once = t -> { if (answered.compareAndSet(false, true)) done.accept(t); };
		try {
			GetGoogleIdOption option = new GetGoogleIdOption.Builder()
					.setFilterByAuthorizedAccounts(false)
					.setServerClientId(serverClientId)
					.build();
			GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(option).build();
			CredentialManager.create(activity).getCredentialAsync(activity, request, new CancellationSignal(),
					Executors.newSingleThreadExecutor(),
					new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
						@Override public void onResult(GetCredentialResponse response){
							Credential c = response.getCredential();
							if (c instanceof CustomCredential && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(c.getType())){
								try { once.accept(GoogleIdTokenCredential.createFrom(c.getData()).getIdToken()); }
								catch (RuntimeException e){ once.accept(null); }
							} else once.accept(null);
						}
						@Override public void onError(GetCredentialException e){ once.accept(null); }
					});
		} catch (RuntimeException | LinkageError e){
			once.accept(null);
		}
	}
}
