package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import java.math.BigDecimal;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ProvisionalScoreLookup;
/** Game configuration adapter to the single immutable, engine-independent scoring authority. */
public final class CustomRuleMultipliers {
 public static final String VERSION=ProvisionalScoreLookup.VERSION,INTERIM_VERSION=ProvisionalScoreLookup.INTERIM_VERSION,PREVIOUS_VERSION=ProvisionalScoreLookup.PREVIOUS_VERSION,MODEL=ProvisionalScoreLookup.MODEL;
 private CustomRuleMultipliers(){}
 public static BigDecimal individual(CustomDungeonRules.Slider slider,int value){if(!slider.valid(value))throw new IllegalArgumentException("Invalid scoring position");return ProvisionalScoreLookup.individual(slider.ordinal(),value);}
 public static BigDecimal combined(CustomDungeonRules rules){if(rules==null)throw new IllegalArgumentException("Unknown scoring rules");return ProvisionalScoreLookup.combined(rules.values());}
 public static BigDecimal combinedForVersion(CustomDungeonRules rules,String version){if(rules==null)throw new IllegalArgumentException("Unknown scoring rules");return ProvisionalScoreLookup.combinedForVersion(rules.values(),version);}
 public static long adjusted(long original,BigDecimal multiplier){return ProvisionalScoreLookup.adjusted(original,multiplier);}
 public static String format(BigDecimal multiplier){return ProvisionalScoreLookup.format(multiplier);}
}
