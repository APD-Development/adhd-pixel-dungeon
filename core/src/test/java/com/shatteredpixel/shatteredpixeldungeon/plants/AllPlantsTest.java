package com.shatteredpixel.shatteredpixeldungeon.plants;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import org.junit.Before;
import org.junit.Test;

/** Every plant: in the Bestiary, has text, and keeps its position across a save. */
public class AllPlantsTest {
	@SuppressWarnings("unchecked")
	static final Class<? extends Plant>[] PLANTS = new Class[]{
		Blindweed.class, Earthroot.class, Fadeleaf.class, Firebloom.class, Icecap.class, Mageroyal.class,
		Rotberry.class, Sorrowmoss.class, Starflower.class, Stormvine.class, Sungrass.class, Swiftthistle.class
	};

	@Before public void setUp(){ HeadlessTestSupport.start(); }

	@Test public void everyPlantHasTextAndIsListed(){
		for (Class<? extends Plant> c : PLANTS){
			Plant p = Reflection.newInstance(c);
			String name = p.name(), desc = p.desc();
			assertFalse(c.getSimpleName(), name.isEmpty() || name.contains(Messages.NO_TEXT_FOUND));
			assertFalse(c.getSimpleName(), desc.isEmpty() || desc.contains(Messages.NO_TEXT_FOUND));
			assertTrue(c.getSimpleName(), Bestiary.PLANT.entities().contains(c));
		}
	}

	@Test public void positionSurvivesASave(){
		for (Class<? extends Plant> c : PLANTS){
			Plant p = Reflection.newInstance(c);
			p.pos = 42;
			Bundle b = new Bundle();
			p.storeInBundle(b);
			Plant back = Reflection.newInstance(c);
			back.restoreFromBundle(b);
			assertEquals(c.getSimpleName(), 42, back.pos);
		}
	}
}
