package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Swarm;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;

// Called only for normal source loot, never wealth/lucky bonus drops.
public final class GuaranteedLootIdentification {
    private GuaranteedLootIdentification() {}
    public static boolean eligible(Class<?> source, Class<?> item) {
        return (source == Swarm.class || source == Bat.class || source == Necromancer.class)
                && (item == PotionOfHealing.class || item == PotionOfShielding.class);
    }
    public static void identifyNormalDrop(Mob source, Item item) {
        if (eligible(source.getClass(), item.getClass())) item.identify();
    }
}
