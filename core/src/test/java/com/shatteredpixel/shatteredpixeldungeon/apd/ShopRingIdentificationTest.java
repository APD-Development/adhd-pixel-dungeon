package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression for suspected shop ring identity disclosure; never identifies stock. */
public class ShopRingIdentificationTest {
    private Object previousHandler;
    private int previousDepth;
    private com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero previousHero;
    @Before public void setup() {
        HeadlessTestSupport.start();
        previousHandler=Ring.apdHandler();
        previousDepth=Dungeon.depth;
        previousHero=Dungeon.hero;
        Dungeon.hero=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
        Ring.initGems();
        Dungeon.depth=6;
    }
    @After public void cleanup() {
        Ring.apdHandler(previousHandler);
        Dungeon.depth=previousDepth;
        Dungeon.hero=previousHero;
    }
    private Ring stock(Ring ring) {
        // Mirrors ShopRoom's ring stock generation: known uncursed status, NOT ring type.
        ring.level(0);ring.cursed=false;ring.cursedKnown=true;
        return ring;
    }
    @Test public void unknownShopRingsShowGemIdentityNotActualType() {
        for (Ring ring:new Ring[]{stock(new RingOfAccuracy()),stock(new RingOfHaste())}) {
            assertFalse(ring.isKnown());
            assertFalse(ring.isIdentified());
            assertNotEquals(Messages.get(ring.getClass(),"name"),ring.name());
            assertEquals(75,ring.value());
            assertEquals(750,Shopkeeper.sellPrice(ring)); // depth 6: 5*(6/5+1)
        }
    }
    @Test public void identifyingOneTypeOnlyRevealsThatTypeAndPersistsAcrossSave() {
        Ring accuracy=stock(new RingOfAccuracy());
        Ring haste=stock(new RingOfHaste());
        accuracy.identify(false);
        assertTrue(accuracy.isKnown());
        assertEquals(Messages.get(RingOfAccuracy.class,"name"),accuracy.name());
        assertFalse(haste.isKnown());
        assertNotEquals(Messages.get(RingOfHaste.class,"name"),haste.name());
        Bundle saved=new Bundle();
        Ring.save(saved);
        Ring.clearGems();
        Ring.restore(saved);
        assertTrue(new RingOfAccuracy().isKnown());
        assertFalse(new RingOfHaste().isKnown());
        assertEquals(75,stock(new RingOfHaste()).value());
    }
    @Test public void shopPricingDoesNotDependOnActualUnknownRingType() {
        Ring first=stock(new RingOfAccuracy()), second=stock(new RingOfHaste());
        assertEquals(first.value(),second.value());
        assertEquals(Shopkeeper.sellPrice(first),Shopkeeper.sellPrice(second));
    }
}
