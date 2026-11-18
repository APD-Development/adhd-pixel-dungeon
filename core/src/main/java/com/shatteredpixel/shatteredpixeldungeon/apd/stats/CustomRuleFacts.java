package com.shatteredpixel.shatteredpixeldungeon.apd.stats;



/** Derives user-visible requested rules from the permanent raw snapshot, never current settings. */
public final class CustomRuleFacts {
    public static final String PREFIX="custom.";
    public static final String VERSION=PREFIX+"version";
    public static final String IDENTITY=PREFIX+"identity";
    public static final String SCROLL_PERCENT=PREFIX+"scrollPercentHundredths";
    public static final String SCROLL_PERCENT_NUMERATOR=PREFIX+"scrollPercentNumerator";
    public static final String SCROLL_PERCENT_DENOMINATOR=PREFIX+"scrollPercentDenominator";
    public static final String SCORE_STATUS=PREFIX+"scoreStatus";
    public static final String GENERATED_UPGRADES=PREFIX+"generatedUpgradeScrolls";
    public static final String GENERATED_EQUIPMENT=PREFIX+"generatedEquipment";
    public static final String ORIGINAL_SCORE=PREFIX+"originalScore";
    public static final String MULTIPLIER_EXACT=PREFIX+"multiplierExact";
    public static final String SCORING_RECORD_VERSION=PREFIX+"scoringRecordVersion";
    public static final String MULTIPLIER=PREFIX+"multiplierHundredths";
    public static final String CALCULATION_VERSION=PREFIX+"calculationVersion";
    public static final String ADJUSTED_SCORE=PREFIX+"adjustedScore";
    private CustomRuleFacts() { }
    public static String key(String slider) { return PREFIX+slider; }
    public static void record(RunFacts facts, LifecycleEvents.RunStart start) {
        String snapshot=start.customRules;
        if (snapshot==null || snapshot.isEmpty()) return;
        final RecordedRuleSettings rules;
        try { rules=RecordedRuleSettings.parse(snapshot); }
        catch (IllegalArgumentException invalid) {
            facts.text(SCORE_STATUS,"invalid recorded configuration",Fact.PROV_RECORDED);
            return;
        }
        facts.num(VERSION,RecordedRuleSettings.VERSION,Fact.PROV_RECORDED);
        facts.text(IDENTITY,rules.identityText(),Fact.PROV_RECORDED);
        for (String slider : RecordedRuleSettings.KEYS)
            facts.num(key(slider),rules.get(slider),Fact.PROV_RECORDED);
        // Exact rational percentage, rather than rounding a repeating decimal in permanent history.
        facts.num(SCROLL_PERCENT_NUMERATOR,100L*rules.get("UPGRADE_SCROLLS"),Fact.PROV_RECORDED);
        facts.num(SCROLL_PERCENT_DENOMINATOR,15,Fact.PROV_RECORDED);
        facts.num(SCROLL_PERCENT,Math.round(10000.0*rules.get("UPGRADE_SCROLLS")/15),Fact.PROV_RECORDED);
        try{
            RecordedRuleSettings.Scoring scoring=rules.scoring(start.customScoreVersion,start.customScoreStatus,start.customScoreCalculation,start.customScoreMultiplier);
            if(scoring==null){facts.text(SCORE_STATUS,"scoring metadata not recorded",Fact.PROV_RECORDED);return;}
            facts.num(SCORING_RECORD_VERSION,start.customScoreVersion,Fact.PROV_RECORDED);
            facts.text(SCORE_STATUS,scoring.displayStatus(),Fact.PROV_RECORDED);
            facts.text(CALCULATION_VERSION,scoring.calculationVersion,Fact.PROV_RECORDED);
            if(!scoring.multiplierExact.isEmpty()){facts.text(MULTIPLIER_EXACT,scoring.multiplierExact,Fact.PROV_RECORDED);facts.num(MULTIPLIER,new java.math.BigDecimal(scoring.multiplierExact).multiply(java.math.BigDecimal.valueOf(100)).setScale(0,java.math.RoundingMode.HALF_UP).longValueExact(),Fact.PROV_RECORDED);}
        }catch(IllegalArgumentException invalid){facts.text(SCORE_STATUS,"invalid recorded scoring metadata",Fact.PROV_RECORDED);}
    }
    public static void preserveScore(RunFacts facts,LifecycleEvents.RunEnd end){
        facts.num(ORIGINAL_SCORE,end.score,Fact.PROV_RECORDED);
        String multiplier=facts.textOr(MULTIPLIER_EXACT,"");
        if(end.customAdjustedKnown&&!multiplier.isEmpty()&&end.score>=0){
            try{
                long expected=ProvisionalScoreLookup.adjusted(end.score,new java.math.BigDecimal(multiplier));
                if(end.customAdjustedScore==expected)facts.num(ADJUSTED_SCORE,end.customAdjustedScore,Fact.PROV_RECORDED);
            }catch(IllegalArgumentException|ArithmeticException invalidRecordedScore){
                // Preserve the raw original; malformed or overflowing adjusted values remain unknown.
            }
        }
    }
}
