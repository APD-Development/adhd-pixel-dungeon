package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import java.util.Locale;

/** Display and integer-position mapping only; no scoring or gameplay calculation. */
public final class CustomRulesPresentation {
    private CustomRulesPresentation() { }
    public static final String EXPERIMENTAL_HEADING = "EXPERIMENTAL FEATURE";
    public static final String EXPERIMENTAL_NOTICE = "Custom Dungeon Rules are experimental and will continue to evolve throughout APD's lifetime. Some or all difficulty sliders may have their minimum and maximum values adjusted in future updates as gameplay balance and player feedback are evaluated.";
    public static final String SCORING_NOTICE = "All six classes currently share the same provisional scoring multipliers. These experimental values will change as balancing continues.";
    public static final String SCORING_INFO_BUTTON = "About experimental scoring";
    public static final String SCORING_INFO_TITLE = "Experimental scoring";
    public static final String SCORING_VISION = "Today, all six classes use the same provisional lookup values, multiplied together to calculate the Custom Rules Multiplier.\n\nThe current scoring multipliers are provisional and will continue changing as APD develops. My long-term goal is to make scoring reflect how all 18 Custom Dungeon Rules interact, rather than simply assigning fixed values to individual sliders.\n\nI'm also exploring how different classes, subclasses, crown choices, and character progression should affect those calculations, including when guaranteed abilities become available.\n\nSome settings can reinforce each other, while others can partially cancel out or create unexpected advantages. I want the scoring system to recognize those differences without rewarding or penalizing players for random loot.\n\nThis is an extremely complicated system, and I'm still investigating how to make it as fair and meaningful as possible. Nothing about this future scoring approach is finalized.";
    /** The lifetime warning remains even when only the scoring paragraph changes after approval. */
    public static String noticeText(){return EXPERIMENTAL_NOTICE+"\n\n"+SCORING_NOTICE;}
    public static String label(CustomDungeonRules.Slider slider) {
        switch(slider) {
            case ENEMY_HP: return "Enemy HP";
            case ENEMY_DAMAGE: return "Enemy Damage";
            case ENEMY_DEFENSE: return "Enemy Defense";
            case ENEMY_ACCURACY: return "Enemy Accuracy";
            case ENEMY_EVASION: return "Enemy Evasion";
            case ENEMY_SPAWN: return "Enemy Spawn Frequency";
            case BOSS_HP: return "Boss HP";
            case BOSS_DAMAGE: return "Boss Damage";
            case PLAYER_HP: return "Player Maximum HP";
            case PLAYER_DAMAGE: return "Player Damage";
            case PLAYER_ACCURACY: return "Player Accuracy";
            case PLAYER_EVASION: return "Player Evasion";
            case PLAYER_DEFENSE: return "Player Defense";
            case FOOD: return "Food Availability";
            case HUNGER: return "Hunger Depletion";
            case HEALING: return "Healing Effectiveness";
            case EQUIPMENT: return "Equipment Availability";
            case UPGRADE_SCROLLS: return "Upgrade Scrolls";
            default: throw new IllegalArgumentException("Unknown slider");
        }
    }
    public static int positions(CustomDungeonRules.Slider slider) {return (slider.max-slider.min)/slider.step();}
    public static int value(CustomDungeonRules.Slider slider,int position) {
        if(position<0 || position>positions(slider))throw new IllegalArgumentException("Invalid slider position");
        return slider.min+position*slider.step();
    }
    public static int position(CustomDungeonRules.Slider slider,int value) {
        if(!slider.valid(value))throw new IllegalArgumentException("Invalid slider value");
        return (value-slider.min)/slider.step();
    }
    public static String multiplierText(CustomDungeonRules.Slider slider,int value){return CustomRuleMultipliers.format(CustomRuleMultipliers.individual(slider,value));}
    /** The locked run's factor must use its recorded calculation authority, not next-run preferences. */
    public static String multiplierTextForVersion(CustomDungeonRules.Slider slider,int value,String calculationVersion){
        return CustomRuleMultipliers.format(CustomRuleMultipliers.combinedForVersion(
                CustomDungeonRules.defaults().with(slider,value),calculationVersion));
    }
    public static String valueText(CustomDungeonRules.Slider slider,int value) {
        if(!slider.valid(value))throw new IllegalArgumentException("Invalid slider value");
        return slider==CustomDungeonRules.Slider.UPGRADE_SCROLLS
            ? String.format(Locale.ROOT,"%d (%.2f%%)",value,value*100.0/15.0) : value+"%";
    }
    /** Every listed alpha control has a gameplay implementation; active-run controls remain locked. */
    public static boolean gameplayImplemented(CustomDungeonRules.Slider slider) {
        return true;
    }
}
