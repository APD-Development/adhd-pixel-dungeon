package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Random;
/** Main-floor food and guaranteed upgrade scheduling; other sources and quest loot are untouched. */
public final class CustomGenerationHooks {
    private CustomGenerationHooks() { }
    public static int upgradeScrollsForFloor() {
        int selected=CustomCombatHooks.percentage(CustomDungeonRules.Slider.UPGRADE_SCROLLS);
        // This is deliberately the original RNG-consuming predicate at neutral settings.
        if(selected==UpgradeScrollPlan.NEUTRAL) return Dungeon.souNeeded() ? 1 : 0;
        return new UpgradeScrollPlan(selected).floorCount(Dungeon.depth);
    }
    public static int foodForFloor(int depth) {
        if(depth<1 || depth>24 || depth%5==0)return 0;
        int quota=CustomCombatHooks.percentage(CustomDungeonRules.Slider.FOOD)/25;
        int floor=(depth-1)%5;
        return quota/4+(floor<quota%4?1:0);
    }
    public static void addFloorFood(Level level,boolean largeFloorBonus) {
        int percent=CustomCombatHooks.percentage(CustomDungeonRules.Slider.FOOD);
        if(percent==100) {
            // Exact original generator call, including the original random sequence.
            level.addItemToSpawn(Generator.random(Generator.Category.FOOD));return;
        }
        int count;
        if(largeFloorBonus) {
            count=percent/100;
            if(percent%100!=0 && Random.Int(100)<percent%100)count++;
        } else count=foodForFloor(Dungeon.depth);
        for(int i=0;i<count;i++) {
            // At the minimum, the regional guarantee is a standard ration, not a random subtype.
            level.addItemToSpawn(percent==25 && !largeFloorBonus ? new Food() : Generator.random(Generator.Category.FOOD));
        }
    }
}
