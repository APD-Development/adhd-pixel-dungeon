package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
/** Frozen scoring authority. Versioned run-owned authority; legacy runs are never repriced. */
public final class CustomScoreSnapshot {
 public enum Status { NEUTRAL_IDENTITY, PENDING_CALIBRATION, EXPERIMENTAL }
 public static final int VERSION=2;
 public final int recordVersion;
 private static final String VERSION_KEY="apd_custom_score_version",STATUS_KEY="apd_custom_score_status",CALC_KEY="apd_custom_score_calculation",MULT_KEY="apd_custom_score_multiplier";
 public final Status status;
 public final String calculationVersion,multiplierExact;
 private CustomScoreSnapshot(int version,Status status,String calculation,String multiplier){recordVersion=version;this.status=status;calculationVersion=calculation;multiplierExact=multiplier;}
 public static CustomScoreSnapshot forNewRun(CustomDungeonRules rules){return rules==null?null:new CustomScoreSnapshot(VERSION,Status.EXPERIMENTAL,CustomRuleMultipliers.VERSION,CustomRuleMultipliers.combined(rules).toPlainString());}
 public String displayStatus(){return status==Status.EXPERIMENTAL?"experimental provisional":status==Status.NEUTRAL_IDENTITY?"neutral identity":"legacy unadjusted";}
 public Long adjustedIdentityScore(long original){if(status==Status.EXPERIMENTAL)return CustomRuleMultipliers.adjusted(original,new java.math.BigDecimal(multiplierExact));if(status==Status.NEUTRAL_IDENTITY)return original;return null;}
 public void store(Bundle b){b.put(VERSION_KEY,recordVersion);b.put(STATUS_KEY,status.name());b.put(CALC_KEY,calculationVersion);b.put(MULT_KEY,multiplierExact);}
 public static void clearStored(Bundle b){b.remove(VERSION_KEY);b.remove(STATUS_KEY);b.remove(CALC_KEY);b.remove(MULT_KEY);}
 public static CustomScoreSnapshot restore(Bundle b,CustomDungeonRules rules){
  boolean v=b.contains(VERSION_KEY),s=b.contains(STATUS_KEY),c=b.contains(CALC_KEY),m=b.contains(MULT_KEY);if(!v&&!s&&!c&&!m)return null;
  if(!v||!s||!c||!m)throw new IllegalArgumentException("Incomplete recorded scoring authority; preserve save");
  return recorded(b.getInt(VERSION_KEY),b.getString(STATUS_KEY),b.getString(CALC_KEY),b.getString(MULT_KEY),rules);
 }
 public static CustomScoreSnapshot recorded(int version,String status,String calculation,String multiplier,CustomDungeonRules rules){
  if(status==null||calculation==null||multiplier==null)throw new IllegalArgumentException("Missing scoring metadata");
  if(version==0&&status.isEmpty()&&calculation.isEmpty()&&multiplier.isEmpty())return null;
  if((version!=1&&version!=VERSION)||rules==null)throw new IllegalArgumentException("Unsupported scoring snapshot; preserve history");
  Status s=Status.valueOf(status);
  if(s==Status.EXPERIMENTAL){if(version!=VERSION||!multiplier.equals(CustomRuleMultipliers.combinedForVersion(rules,calculation).toPlainString()))throw new IllegalArgumentException("Invalid experimental scoring snapshot");}
  else if(version!=1)throw new IllegalArgumentException("Legacy status requires legacy version");
  else if(s==Status.NEUTRAL_IDENTITY){if(!rules.isNeutral()||!calculation.equals("neutral-identity-v1")||!multiplier.equals("1.00"))throw new IllegalArgumentException("Invalid neutral scoring snapshot");}
  else if(rules.isNeutral()||!calculation.equals("pending-calibration-v1")||!multiplier.isEmpty())throw new IllegalArgumentException("Invalid pending scoring snapshot");
  return new CustomScoreSnapshot(version,s,calculation,multiplier);
 }
 public void recordTo(LifecycleEvents.RunStart s){s.customScoreVersion=recordVersion;s.customScoreStatus=status.name();s.customScoreCalculation=calculationVersion;s.customScoreMultiplier=multiplierExact;}
}
