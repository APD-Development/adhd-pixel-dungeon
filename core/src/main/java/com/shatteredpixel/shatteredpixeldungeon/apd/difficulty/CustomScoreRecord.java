package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.watabou.utils.Bundle;

/** Earned-score preservation while custom scoring calibration remains unapproved. No experimental reward calculation. */
public final class CustomScoreRecord {
    public enum Status { NEUTRAL_IDENTITY, UNRATED_PENDING_CALIBRATION, HISTORICAL_SETTINGS_UNKNOWN }
    public static final int RECORD_VERSION=1;
    public final CustomDungeonRules rules;
    public final int originalScore;
    public final Status status;
    private CustomScoreRecord(CustomDungeonRules rules, int originalScore) {
        if (originalScore < 0) throw new IllegalArgumentException("Negative original score");
        this.rules=rules; this.originalScore=originalScore;
        status=rules==null ? Status.HISTORICAL_SETTINGS_UNKNOWN
            : rules.isNeutral() ? Status.NEUTRAL_IDENTITY : Status.UNRATED_PENDING_CALIBRATION;
    }
    public static CustomScoreRecord preserve(CustomDungeonRules rules, int originalScore) {
        return new CustomScoreRecord(rules,originalScore);
    }
    public Double multiplier() { return status==Status.NEUTRAL_IDENTITY ? 1.0 : null; }
    public Integer adjustedScore() { return status==Status.NEUTRAL_IDENTITY ? originalScore : null; }
    public int earnedScore() { return originalScore; }
    public String calculationVersion() { return status==Status.NEUTRAL_IDENTITY ? "neutral-identity-v1" : ""; }
    public void store(Bundle bundle) {
        bundle.put("custom_score_record_version",RECORD_VERSION);
        bundle.put("custom_original_score",originalScore);
        bundle.put("custom_score_rules_known",rules!=null);
        if (rules!=null) { Bundle settings=new Bundle(); rules.store(settings); bundle.put("custom_score_rules",settings); }
    }
    public static CustomScoreRecord restore(Bundle bundle) {
        if (bundle.getInt("custom_score_record_version")!=RECORD_VERSION || !bundle.contains("custom_original_score") || !bundle.contains("custom_score_rules_known"))
            throw new IllegalArgumentException("Unsupported or incomplete score record");
        if (bundle.getBoolean("custom_score_rules_known") && !bundle.contains("custom_score_rules"))
            throw new IllegalArgumentException("Missing recorded score rules");
        CustomDungeonRules rules=bundle.getBoolean("custom_score_rules_known")
            ? CustomDungeonRules.restore(bundle.getBundle("custom_score_rules")) : null;
        return preserve(rules,bundle.getInt("custom_original_score"));
    }
}
