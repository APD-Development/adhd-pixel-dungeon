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

import com.google.android.play.agesignals.AgeSignalsAccessRequest;
import com.google.android.play.agesignals.AgeSignalsManager;
import com.google.android.play.agesignals.AgeSignalsManagerFactory;
import com.google.android.play.agesignals.AgeSignalsRequest;
import com.google.android.play.agesignals.model.AgeSignalsStatus;
import com.shatteredpixel.shatteredpixeldungeon.apd.age.CommunityAgeGate;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// Asks Google Play for the player's age range (Play Age Signals 0.0.4: request access, then check). Only the range is
// passed on; nothing is stored here. Any failure, refusal or region without age signals = no answer, and Community
// falls back to the birth month/year question.
final class PlayAgeSignals implements CommunityAgeGate.PlaySource {

	private final Activity activity;

	PlayAgeSignals(Activity activity){ this.activity = activity; }

	@Override
	public void request(Consumer<CommunityAgeGate.PlaySignal> done){
		AtomicBoolean answered = new AtomicBoolean(false);
		Consumer<CommunityAgeGate.PlaySignal> once = s -> { if (answered.compareAndSet(false, true)) done.accept(s); };
		try {
			AgeSignalsManager manager = AgeSignalsManagerFactory.create(activity.getApplicationContext());
			manager.requestAgeSignalsAccess(AgeSignalsAccessRequest.builder().setActivity(activity).build())
					.addOnSuccessListener(access -> {
						if (access.ageSignalsStatus() != AgeSignalsStatus.SHARED){
							once.accept(CommunityAgeGate.PlaySignal.none());
							return;
						}
						manager.checkAgeSignals(AgeSignalsRequest.builder().build())
								.addOnSuccessListener(r -> once.accept(new CommunityAgeGate.PlaySignal(r.ageLower(), r.ageUpper())))
								.addOnFailureListener(e -> once.accept(CommunityAgeGate.PlaySignal.none()));
					})
					.addOnFailureListener(e -> once.accept(CommunityAgeGate.PlaySignal.none()));
		} catch (RuntimeException | LinkageError e){
			once.accept(CommunityAgeGate.PlaySignal.none());
		}
	}
}
