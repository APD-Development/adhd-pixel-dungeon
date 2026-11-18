package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.SeedReuseScore;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/** Prevents a higher build number from silently dropping the owner's R7 line. */
public class R7IntegrationCompletenessTest {
    private static final String ROOT = "com.shatteredpixel.shatteredpixeldungeon.";
    private static Class<?> required(String name) {
        try { return Class.forName(ROOT + name, false, R7IntegrationCompletenessTest.class.getClassLoader()); }
        catch (ClassNotFoundException missing) { fail("Required R7 feature missing: " + name); return null; }
    }

    @Test public void requiredR7ClassesCannotDisappearFromACombinedBuild() throws Exception {
        for (String name : new String[]{"Tanto", "Wakizashi", "Tachi", "Nodachi",
                "Buckler", "Targe", "KiteShield"}) required("items.weapon.melee." + name);
        required("apd.qol.FullPackSwap");
        required("apd.qol.ThrowAim");
        required("items.spells.GreaterMetamorphosis");
        required("items.spells.GreaterMetamorphosis$Recipe");
        required("ui.Toolbar").getDeclaredMethod("apdWallSearchUseful");
        required("apd.qol.SearchControlContext").getDeclaredMethod("noHiddenDoorRoom",
                required("levels.Level"), int.class);
        required("scenes.GameScene").getDeclaredMethod("wellEmitter");
        required("scenes.GameScene").getDeclaredMethod("normalSpeedEmitter");
    }

    @Test public void exactR7WeaponTiersGuardScalingSpritesAndDropWeightsArePreserved() throws Exception {
        HeadlessTestSupport.start();
        String[] names={"Tanto","Wakizashi","Tachi","Katana","Nodachi","Buckler","Targe","KiteShield"};
        String[] sprites={"TANTO","WAKIZASHI","TACHI","KATANA","NODACHI","BUCKLER","TARGE","KITE_SHIELD"};
        int[] tiers={1,2,3,4,5,1,2,4};
        int[] guards={1,1,2,3,4,2,3,5};
        for(int i=0;i<names.length;i++) {
            Class<?> type=required("items.weapon.melee."+names[i]);
            MeleeWeapon weapon=(MeleeWeapon)type.getDeclaredConstructor().newInstance();
            assertEquals(names[i]+" tier",tiers[i],weapon.tier);
            assertEquals(names[i]+" sprite",ItemSpriteSheet.class.getField(sprites[i]).getInt(null),weapon.image);
            Method block=type.getMethod("DRMax",int.class);
            for(int level:new int[]{-3,0,1,2,3,4,9}) {
                int positive=Math.max(0,level);
                int growth=i==6 ? 3*positive/4 : i==7 ? 3*positive/2 : positive/2;
                assertEquals(names[i]+" guard at "+level,guards[i]+growth,((Number)block.invoke(weapon,level)).intValue());
            }
            Generator.Category category=Generator.Category.valueOf("WEP_T"+tiers[i]);
            int position=-1;
            for(int j=0;j<category.classes.length;j++) if(category.classes[j]==type) position=j;
            assertTrue(names[i]+" must be registered in its real generator deck",position>=0);
            assertEquals(names[i]+" original R7 generation weight",2f,category.defaultProbs[position],0f);
        }
    }

    @Test public void combinedBuildKeepsExactC485RationalScoringAndR7RecipeCost() throws Exception {
        assertEquals(6,SeedReuseScore.VERSION);
        assertEquals(255571,SeedReuseScore.calculate(330393,7,9,26,true,new int[]{4,4,4,4,4,4,9}).earned);
        assertEquals(219228,SeedReuseScore.calculate(330393,7,9,26,true).earned);
        assertEquals(1,SeedReuseScore.calculate(1,100,26,26,true).earned);
        Object recipe=required("items.spells.GreaterMetamorphosis$Recipe").getDeclaredConstructor().newInstance();
        Method cost=recipe.getClass().getMethod("cost",java.util.ArrayList.class);
        assertEquals(12,((Number)cost.invoke(recipe,new Object[]{null})).intValue());
    }
}
