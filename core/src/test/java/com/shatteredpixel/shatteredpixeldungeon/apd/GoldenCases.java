package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;

import java.util.ArrayList;
import java.util.List;

//The dungeons the golden baseline covers.
public final class GoldenCases {

	private GoldenCases(){}

	public static final int MAX_DEPTH = 26;

	public static final class Case {
		public final String name;
		public final long seed;
		public final int challenges;
		public final HeroClass heroClass;
		public final LevelGenerationHarness.Context context;

		Case( String name, long seed, int challenges, HeroClass heroClass, LevelGenerationHarness.Context context ){
			this.name = name;
			this.seed = seed;
			this.challenges = challenges;
			this.heroClass = heroClass;
			this.context = context;
		}

		public String label(){
			return name + " " + heroClass.name() + " " + context.name();
		}
	}

	public static List<Case> all(){
		LevelGenerationHarness.Context fresh = LevelGenerationHarness.Context.FRESH;
		LevelGenerationHarness.Context veteran = LevelGenerationHarness.Context.VETERAN;
		List<Case> cases = new ArrayList<>();
		cases.add(new Case("plain-1", 1L, 0, HeroClass.WARRIOR, fresh));
		cases.add(new Case("plain-12345", 12345L, 0, HeroClass.WARRIOR, fresh));
		cases.add(new Case("plain-987654321", 987654321L, 0, HeroClass.WARRIOR, fresh));
		cases.add(new Case("plain-5e9", 5000000000L, 0, HeroClass.WARRIOR, fresh));
		cases.add(new Case("plain-123456789012", 123456789012L, 0, HeroClass.WARRIOR, fresh));
		//same seed, different class: generation should not depend on the hero class
		cases.add(new Case("mage-12345", 12345L, 0, HeroClass.MAGE, fresh));
		//all challenges on
		cases.add(new Case("challenges-4242", 4242L, Challenges.MAX_VALUE, HeroClass.WARRIOR, fresh));
		//daily-style seed, outside the range of user-enterable seeds
		cases.add(new Case("daily-style", DungeonSeed.TOTAL_SEEDS + 20260101L, 0, HeroClass.WARRIOR, fresh));
		//the same dungeons for a veteran journal (intro off, guide pages found), which changes generation
		cases.add(new Case("veteran-12345", 12345L, 0, HeroClass.WARRIOR, veteran));
		cases.add(new Case("veteran-challenges-4242", 4242L, Challenges.MAX_VALUE, HeroClass.WARRIOR, veteran));
		cases.add(new Case("veteran-daily-style", DungeonSeed.TOTAL_SEEDS + 20260101L, 0, HeroClass.WARRIOR, veteran));
		return cases;
	}
}
