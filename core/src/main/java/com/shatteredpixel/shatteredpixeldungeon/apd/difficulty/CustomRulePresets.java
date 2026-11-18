package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

/** Quick choices for the existing eighteen sliders. Never modifies an active run. */
public final class CustomRulePresets {
    private CustomRulePresets(){}
    public enum Preset {
        EASIEST("All easiest"), HARDEST("All hardest"),
        MAXIMUM("All max"), MINIMUM("All min"), DEFAULT("Default");
        public final String label;
        Preset(String label){this.label=label;}
    }
    /** True means a higher value helps the player, according to the slider's defined effect. */
    private static boolean playerBenefit(CustomDungeonRules.Slider slider){
        switch(slider){
            case PLAYER_HP: case PLAYER_DAMAGE: case PLAYER_ACCURACY:
            case PLAYER_EVASION: case PLAYER_DEFENSE: case FOOD:
            case HEALING: case EQUIPMENT: case UPGRADE_SCROLLS: return true;
            case ENEMY_HP: case ENEMY_DAMAGE: case ENEMY_DEFENSE:
            case ENEMY_ACCURACY: case ENEMY_EVASION: case ENEMY_SPAWN:
            case BOSS_HP: case BOSS_DAMAGE: case HUNGER: return false;
            default: throw new IllegalArgumentException("Unclassified slider "+slider);
        }
    }
    public static CustomDungeonRules apply(Preset preset){
        if(preset==null)throw new IllegalArgumentException("Missing preset");
        int[] result=new int[CustomDungeonRules.Slider.values().length];
        for(CustomDungeonRules.Slider slider:CustomDungeonRules.Slider.values()){
            int value;
            switch(preset){
                case EASIEST: value=playerBenefit(slider)?slider.max:slider.min;break;
                case HARDEST: value=playerBenefit(slider)?slider.min:slider.max;break;
                case MAXIMUM: value=slider.max;break;
                case MINIMUM: value=slider.min;break;
                case DEFAULT: value=slider.defaultValue();break;
                default: throw new IllegalArgumentException("Unknown preset");
            }
            result[slider.ordinal()]=value;
        }
        return new CustomDungeonRules(result);
    }
}
