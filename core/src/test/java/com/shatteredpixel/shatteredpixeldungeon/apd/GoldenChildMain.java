package com.shatteredpixel.shatteredpixeldungeon.apd;

import java.util.List;

//Runs every golden case once in a fresh JVM and prints one "APDLINE" per level.
//
//Why a fresh JVM: a few places in SPD's level generation depend on the order of identity-hashed objects
//(HashMap/HashSet iteration), which varies with the history of the JVM. The golden file records which levels came out
//identical across many JVMs that were deliberately perturbed (argument 0 = number of throw-away hashCode() calls made
//before anything else), and only those levels are compared later.
//
//Optional second argument: "rec" runs with APD run recording switched on, "flow" additionally makes the hero arrive on
//every level (which saves the game), "flowrec" does both. "flow" and "flowrec" cover only the first FLOW_CASES cases
//and FLOW_DEPTH levels.
public final class GoldenChildMain {

	static final int FLOW_CASES = 3;
	static final int FLOW_DEPTH = 8;

	public static void main( String[] args ) throws Exception {
		int perturb = Integer.parseInt(args[0]);
		for (int i = 0; i < perturb; i++){
			new Object().hashCode();
		}
		String mode = args.length > 1 ? args[1] : "";
		boolean record = mode.equals("rec") || mode.equals("flowrec") || mode.equals("legacy-scrolls-rec");
		boolean flow = mode.equals("flow") || mode.equals("flowrec");

		HeadlessTestSupport.start();
		if (mode.startsWith("legacy-scrolls")){
			// Keep the historical pre-recorder baseline on its original twelve-item pool.
			com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category scrolls = com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.SCROLL;
			for (int i=0;i<scrolls.classes.length;i++) if (scrolls.classes[i] == com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfGathering.class){
				scrolls.defaultProbs[i]=0;scrolls.defaultProbs2[i]=0;scrolls.defaultProbsTotal[i]=0;
			}
		}

		LegacyGeneration.withoutApdWeapons();
		LevelGenerationHarness.historicalTutorialFingerprint = true;
		if (record){
			com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder.configure(
					new java.io.File(HeadlessTestSupport.tempDir(), "history"), true);
		}

		int caseNumber = 0;
		for (GoldenCases.Case c : GoldenCases.all()){
			if (flow && caseNumber++ >= FLOW_CASES) break;
			List<LevelGenerationHarness.LevelPrint> prints = flow
					? LevelGenerationHarness.generateFlow(c.seed, c.challenges, c.heroClass, c.context, FLOW_DEPTH)
					: LevelGenerationHarness.generate(c.seed, c.challenges, c.heroClass, c.context, GoldenCases.MAX_DEPTH);
			for (LevelGenerationHarness.LevelPrint p : prints){
				System.out.println("APDLINE " + c.label() + "|" + p.depth + "|" + p.line());
			}
		}
		System.out.flush();
		System.exit(0);
	}
}

