package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import java.util.Arrays;
import java.util.List;

/** Historical seed baselines predate the owner-approved katana-family blades and shields.
 * Zeroing only those entries reproduces the old weapon tables exactly. */
public final class LegacyGeneration {
	private LegacyGeneration(){}
	public static final List<Class<?>> APD_WEAPONS = Arrays.<Class<?>>asList(
			Tanto.class, Buckler.class, Wakizashi.class, Targe.class, Tachi.class, KiteShield.class, Nodachi.class);
	public static void withoutApdWeapons(){
		for (Generator.Category cat : new Generator.Category[]{Generator.Category.WEP_T1, Generator.Category.WEP_T2,
				Generator.Category.WEP_T3, Generator.Category.WEP_T4, Generator.Category.WEP_T5}){
			for (int i = 0; i < cat.classes.length; i++){
				if (APD_WEAPONS.contains(cat.classes[i])){
					cat.defaultProbs[i] = 0;
					if (cat.probs != null) cat.probs[i] = 0;
				}
			}
		}
	}
}
