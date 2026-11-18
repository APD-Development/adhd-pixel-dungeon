package com.shatteredpixel.shatteredpixeldungeon.levels.traps;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import org.junit.Before;
import org.junit.Test;

/** Every trap type: constructible, named, described, drawable and saved/restored correctly. */
public class AllTrapsTest {
	@SuppressWarnings("unchecked")
	static final Class<? extends Trap>[] TRAPS = new Class[]{
		AlarmTrap.class,
		BlazingTrap.class,
		BurningTrap.class,
		ChillingTrap.class,
		ConfusionTrap.class,
		CorrosionTrap.class,
		CursingTrap.class,
		DisarmingTrap.class,
		DisintegrationTrap.class,
		DistortionTrap.class,
		ExplosiveTrap.class,
		FlashingTrap.class,
		FlockTrap.class,
		FrostTrap.class,
		GatewayTrap.class,
		GeyserTrap.class,
		GnollRockfallTrap.class,
		GrimTrap.class,
		GrippingTrap.class,
		GuardianTrap.class,
		OozeTrap.class,
		PitfallTrap.class,
		PoisonDartTrap.class,
		RockfallTrap.class,
		ShockingTrap.class,
		StormTrap.class,
		SummoningTrap.class,
		TeleportationTrap.class,
		TenguDartTrap.class,
		ToxicTrap.class,
		WarpingTrap.class,
		WeakeningTrap.class,
		WornDartTrap.class
	};

	@Before public void setUp(){ HeadlessTestSupport.start(); }

	@Test public void everyTrapHasTextAndAValidLook(){
		for (Class<? extends Trap> c : TRAPS){
			Trap t = Reflection.newInstance(c);
			assertNotNull(c.getSimpleName(), t);
			String name = t.name(), desc = t.desc();
			assertFalse(c + " name", name.isEmpty() || name.contains(Messages.NO_TEXT_FOUND));
			assertFalse(c + " desc", desc.isEmpty() || desc.contains(Messages.NO_TEXT_FOUND));
			assertTrue(c + " color", t.color >= Trap.RED && t.color <= Trap.BLACK);
			assertTrue(c + " shape", t.shape >= Trap.DOTS && t.shape <= Trap.LARGE_DOT);
			assertTrue(c + " starts active", t.active);
		}
	}

	@Test public void inactiveTrapsSayTheyAreInactive(){
		for (Class<? extends Trap> c : TRAPS){
			Trap t = Reflection.newInstance(c);
			String active = t.desc();
			t.active = false;
			String inactive = t.desc();
			assertTrue(c.getSimpleName(), inactive.length() > active.length());
			assertTrue(c.getSimpleName(), inactive.endsWith(active));
		}
	}

	@Test public void positionVisibilityAndActivitySurviveASave(){
		for (Class<? extends Trap> c : TRAPS){
			Trap t = Reflection.newInstance(c).set(77);
			t.visible = true;
			t.active = false;
			Bundle b = new Bundle();
			t.storeInBundle(b);
			Trap back = Reflection.newInstance(c);
			back.restoreFromBundle(b);
			assertEquals(c.getSimpleName(), 77, back.pos);
			assertTrue(back.visible);
			assertFalse(back.active);
		}
	}

	@Test public void oldSavesWithoutTheActiveFlagLoadAsActive(){
		Bundle b = new Bundle();
		b.put("pos", 3);
		b.put("visible", false);
		Trap t = new AlarmTrap();
		t.restoreFromBundle(b);
		assertTrue(t.active);
		assertEquals(3, t.pos);
	}

	@Test public void everyTrapIsInTheBestiary(){
		for (Class<? extends Trap> c : TRAPS){
			//boss and quest variants of other traps are listed under their originals
			if (c == GnollRockfallTrap.class || c == TenguDartTrap.class) continue;
			assertTrue(c.getSimpleName(), Bestiary.TRAP.entities().contains(c));
		}
	}
}
