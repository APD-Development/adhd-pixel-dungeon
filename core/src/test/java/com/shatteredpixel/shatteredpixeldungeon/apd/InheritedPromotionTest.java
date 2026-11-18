package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class InheritedPromotionTest {
    static class PickupHero extends Hero {
        float spent; int actions;
        @Override public void spendAndNext(float time) {spent+=time;actions++;}
    }
    @Test public void bossKeyKeepsNormalPickupWithEitherLegacyPreference() throws Exception {
        HeadlessTestSupport.start();
        Hero previous=Dungeon.hero;
        java.util.Set<Class> discovered=
                new java.util.HashSet<>(com.shatteredpixel.shatteredpixeldungeon.Statistics.itemTypesDiscovered);
        boolean nagged=SPDSettings.supportNagged();
        java.lang.reflect.Field records=Notes.class.getDeclaredField("records");records.setAccessible(true);
        Object notes=records.get(null);Notes.reset();
        java.lang.reflect.Field field=GameScene.class.getDeclaredField("scene");field.setAccessible(true);
        Object scene=field.get(null);field.set(null,null);
        try {
            for(boolean legacy:new boolean[]{false,true}) {
                SPDSettings.supportNagged(legacy);
                PickupHero hero=new PickupHero();Dungeon.hero=hero;
                WornKey key=new WornKey(5);int before=Notes.keyCount(key);
                assertTrue(key.doPickUp(hero,0));
                assertEquals(before+1,Notes.keyCount(key));
                assertEquals(key.pickupDelay(),hero.spent,0);
                assertEquals(1,hero.actions);
                assertEquals("pickup does not rewrite old preferences",legacy,SPDSettings.supportNagged());
            }
        } finally {
            records.set(null,notes);field.set(null,scene);Dungeon.hero=previous;SPDSettings.supportNagged(nagged);
            com.shatteredpixel.shatteredpixeldungeon.Statistics.itemTypesDiscovered.clear();
            com.shatteredpixel.shatteredpixeldungeon.Statistics.itemTypesDiscovered.addAll(discovered);
        }
    }

    @Test public void runtimePromotionLinksAndLocalizedAppealsAreAbsentButCreditsRemain() throws Exception {
        Path base=Paths.get("src/main");if(!Files.isDirectory(base))base=Paths.get("core/src/main");
        String gameSource=System.getProperty("apd.game.src");
        if(gameSource!=null)base=Paths.get(gameSource).toAbsolutePath().getParent().getParent().getParent().getParent();
        assertTrue("real production source root",Files.isDirectory(base));
        Path project=base.toAbsolutePath().getParent().getParent().getParent();
        try(java.util.stream.Stream<Path> files=Files.walk(project)) {
            for(Path p:(Iterable<Path>)files.filter(x->x.toString().replace('\\','/').contains("/src/main/") && (x.toString().endsWith(".java")||x.toString().endsWith(".properties")))::iterator) {
                String s=Files.readString(p,StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                assertFalse(p.toString(),s.contains("patreon.com")||s.contains("paypal.me")||s.contains("buymeacoffee.com")||s.contains("ko-fi.com"));
                if(p.toString().endsWith(".properties"))assertFalse(p.toString(),s.contains("scenes.supporterscene.")||s.contains("scenes.titlescene.patreon_")||s.contains("windows.wndvictorycongrats.support_prompt="));
            }
        }
        String credits=Files.readString(base.resolve("java/com/shatteredpixel/shatteredpixeldungeon/scenes/AboutScene.java"));
        for(String required:new String[]{"Evan Debenham","Watabou","libGDX","GNU General Public License"})assertTrue(required,credits.contains(required));
    }
}
