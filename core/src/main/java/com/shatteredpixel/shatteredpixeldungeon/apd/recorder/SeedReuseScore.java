package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.watabou.utils.Bundle;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Owner-approved exact rational per-floor familiarity:
 * R = (1/C) * sum(f=1..C, (3/5)^n_f).
 * C is current deepest floor, n_f is distinct earlier runs reaching that floor.
 * Score = max(1, roundHalfUp(base * R)) for positive base; zero stays zero.
 * A zero/missing earlier history is never fabricated.
 */
public final class SeedReuseScore {
    private SeedReuseScore() { }

    public static final int VERSION = 6;
    private static final int V1 = 1, V2 = 2, V3 = 3, V4 = 4, V5 = 5;
    private static final long CUBIC_DENOMINATOR = 15625L;
    public static final String VERSION_KEY = "apd_seed_reuse_score_version";
    public static final String BASE_KEY = "apd_seed_reuse_base_score";
    public static final String EARNED_KEY = "apd_seed_reuse_earned_score";
    public static final String ATTEMPTS_KEY = "apd_seed_reuse_prior_attempts";
    public static final String PRIOR_DEPTH_KEY = "apd_seed_reuse_prior_depth";
    public static final String CURRENT_DEPTH_KEY = "apd_seed_reuse_current_depth";
    public static final String RETENTION_KEY = "apd_seed_reuse_retention_bps";
    public static final String KNOWN_KEY = "apd_seed_reuse_history_known";
    public static final String EXACT_KEY = "apd_seed_reuse_individual_depths_known";
    public static final String DEPTHS_KEY = "apd_seed_reuse_individual_depths";

    public static final class Result {
        public final int priorAttempts, deepestBefore, currentDepth, retentionBasisPoints;
        public final long before, earned, deduction;
        public final boolean known, individualDepthsKnown;
        public final int[] individualDepths;
        private Result(int attempts, int prior, int current, int bps,
                       long before, long earned, boolean known, int[] depths) {
            priorAttempts=attempts; deepestBefore=prior; currentDepth=current;
            retentionBasisPoints=bps; this.before=before; this.earned=earned;
            deduction=before-earned; this.known=known;
            individualDepthsKnown=depths!=null;
            individualDepths=depths==null ? null : depths.clone();
        }
        public void store(Bundle data) {
            if (data==null) return;
            data.put(VERSION_KEY,VERSION);
            data.put(BASE_KEY,before);
            data.put(EARNED_KEY,earned);
            data.put(ATTEMPTS_KEY,priorAttempts);
            data.put(PRIOR_DEPTH_KEY,deepestBefore);
            data.put(CURRENT_DEPTH_KEY,currentDepth);
            data.put(RETENTION_KEY,retentionBasisPoints);
            data.put(KNOWN_KEY,known);
            data.put(EXACT_KEY,individualDepthsKnown);
            if (individualDepthsKnown) data.put(DEPTHS_KEY,individualDepths);
        }
    }

    private static long roundRatio(BigInteger numerator, long denominator) {
        if (denominator<=0 || numerator.signum()<0) throw new IllegalArgumentException("Invalid ratio");
        return numerator.add(BigInteger.valueOf(denominator/2))
                .divide(BigInteger.valueOf(denominator)).longValueExact();
    }

    /** Legacy signature: only summary A / previous maximum D, so conservative fallback. */
    public static Result calculate(long score, int attempts, int previousDeepest, int currentDeepest,
                                   boolean historyKnown) {
        return calculate(score,attempts,previousDeepest,currentDeepest,historyKnown,null);
    }

    /**
     * Accurate mode uses each previous attempt's recorded deepest floor.
     * If those individual depths are not all provable, null means conservative
     * fallback: assume every previous attempt reached the historical maximum D.
     * Current run is excluded from the list.
     */
    public static Result calculate(long score, int attempts, int previousDeepest, int currentDeepest,
                                   boolean historyKnown, int[] previousDepths) {
        if (score<0 || attempts<0) throw new IllegalArgumentException("Negative score or attempts");
        if (previousDepths!=null) {
            if (!historyKnown || previousDepths.length!=attempts)
                throw new IllegalArgumentException("Incomplete individual depth list");
            int maximum=-1;
            for (int depth : previousDepths) {
                if (depth<1) throw new IllegalArgumentException("Unknown depth in exact list");
                maximum=Math.max(maximum,depth);
            }
            if (attempts>0 && maximum!=previousDeepest)
                throw new IllegalArgumentException("Inconsistent historical maximum");
        }
        int current=Math.max(0,Math.min(26,currentDeepest));
        int bps=10000;
        long earned=score;
        if (historyKnown && attempts>0 && current>0 && previousDeepest>=1) {
            // Sum 3^n * 5^(A-n) over floors, denominator C*5^A.
            // Work with integers only, including one final HALF_UP score rounding.
            BigInteger powerFive=BigInteger.valueOf(5).pow(attempts);
            BigInteger[] powersThree=new BigInteger[attempts+1];
            BigInteger[] powersFive=new BigInteger[attempts+1];
            powersThree[0]=BigInteger.ONE;
            powersFive[0]=BigInteger.ONE;
            for (int i=1;i<=attempts;i++) {
                powersThree[i]=powersThree[i-1].multiply(BigInteger.valueOf(3));
                powersFive[i]=powersFive[i-1].multiply(BigInteger.valueOf(5));
            }
            BigInteger weight=BigInteger.ZERO;
            for (int floor=1;floor<=current;floor++) {
                int visits=0;
                if (previousDepths!=null) {
                    for (int depth:previousDepths) if (depth>=floor) visits++;
                } else if (floor<=previousDeepest) {
                    // Proven summary only: do not invent exact individual depths.
                    visits=attempts;
                }
                weight=weight.add(powersThree[visits].multiply(powersFive[attempts-visits]));
            }
            BigInteger denominator=powerFive.multiply(BigInteger.valueOf(current));
            // Half-up: floor((2*N+D)/(2*D)), for nonnegative N.
            BigInteger scoreNumerator=BigInteger.valueOf(score).multiply(weight);
            earned=scoreNumerator.shiftLeft(1).add(denominator)
                    .divide(denominator.shiftLeft(1)).longValueExact();
            if (score>0 && earned==0) earned=1;
            bps=weight.multiply(BigInteger.valueOf(10000)).shiftLeft(1)
                    .add(denominator).divide(denominator.shiftLeft(1)).intValueExact();
        }
        return new Result(attempts,previousDeepest,currentDeepest,bps,score,earned,
                historyKnown,previousDepths);
    }

    /** Authenticate v5 Math.exp(-0.5*n) receipts using their original floating-point steps. */
    private static long[] oldV5(long score, int attempts, int previousDeepest,
                                int currentDeepest, boolean historyKnown, int[] previousDepths) {
        int current=Math.max(0,Math.min(26,currentDeepest));
        if (!historyKnown || attempts==0 || current<=0 || previousDeepest<1)
            return new long[]{10000,score};
        double credits=0.0;
        for (int floor=1;floor<=current;floor++) {
            int visits=0;
            if (previousDepths!=null) {
                for (int depth:previousDepths) if (depth>=floor) visits++;
            } else if (floor<=previousDeepest) visits=attempts;
            credits+=Math.exp(-0.5d*visits);
        }
        double ratio=credits/current;
        long earned=BigDecimal.valueOf(ratio).multiply(BigDecimal.valueOf(score))
                .setScale(0,RoundingMode.HALF_UP).longValueExact();
        int bps=BigDecimal.valueOf(ratio).multiply(BigDecimal.valueOf(10000))
                .setScale(0,RoundingMode.HALF_UP).intValueExact();
        return new long[]{bps,earned};
    }

    /** Exact v4 integer-quarter credit, solely to authenticate historical receipts. */
    private static long[] oldV4(long base, int attempts, int prior, int depth, boolean known,
                                int[] depths) {
        if (!known || attempts==0 || depth<=0 || prior<1) return new long[]{10000,base};
        int current=Math.max(0,Math.min(26,depth));
        long quarters=0;
        for (int floor=1;floor<=current;floor++) {
            int visits=0;
            if (depths!=null) {
                for (int reached:depths) if (reached>=floor) visits++;
            } else if (floor<=prior) visits=attempts;
            quarters+=Math.max(0,4-visits);
        }
        long denominator=4L*current;
        return new long[]{
                roundRatio(BigInteger.valueOf(quarters).multiply(BigInteger.valueOf(10000)),denominator),
                roundRatio(BigInteger.valueOf(base).multiply(BigInteger.valueOf(quarters)),denominator)};
    }

    private static int oldBps(int version, int attempts, int prior, int current, boolean known) {
        if (!known || attempts==0) return 10000;
        int tier=10000-Math.min(attempts,4)*2500;
        int old=Math.max(0,Math.min(25,prior));
        int now=Math.max(0,Math.min(25,current));
        int improved=Math.max(0,now-old);
        int previous=(tier*Math.min(25,25-old+improved)+12)/25;
        if (version==V1) return previous;
        int extra=Math.max(0,Math.min(26,current)-Math.max(0,Math.min(26,prior)));
        return Math.max(previous,(2500*Math.min(25,extra)+12)/25);
    }

    /** Validate old saved receipts against their original formula before migrating. */
    public static Result restore(Bundle data) {
        if (data==null || !data.contains(VERSION_KEY)) return null;
        int version=data.getInt(VERSION_KEY);
        if (version!=V1 && version!=V2 && version!=V3 && version!=V4 && version!=V5 && version!=VERSION) return null;
        if (!data.contains(BASE_KEY) || !data.contains(EARNED_KEY)
                || !data.contains(ATTEMPTS_KEY) || !data.contains(PRIOR_DEPTH_KEY)
                || !data.contains(CURRENT_DEPTH_KEY) || !data.contains(RETENTION_KEY)
                || !data.contains(KNOWN_KEY)) return null;
        long before=data.getLong(BASE_KEY), stored=data.getLong(EARNED_KEY);
        int attempts=data.getInt(ATTEMPTS_KEY), prior=data.getInt(PRIOR_DEPTH_KEY),
                current=data.getInt(CURRENT_DEPTH_KEY), bps=data.getInt(RETENTION_KEY);
        boolean known=data.getBoolean(KNOWN_KEY);
        if (before<0 || stored<0 || stored>before || attempts<0 || bps<0 || bps>10000
                || (!known && before!=stored)) return null;
        if (version==VERSION) {
            if (!data.contains(EXACT_KEY)) return null;
            boolean exact=data.getBoolean(EXACT_KEY);
            if (exact && !data.contains(DEPTHS_KEY)) return null;
            if (!exact && data.contains(DEPTHS_KEY)) return null;
            int[] depths=exact ? data.getIntArray(DEPTHS_KEY) : null;
            try {
                Result checked=calculate(before,attempts,prior,current,known,depths);
                return checked.retentionBasisPoints==bps && checked.earned==stored ? checked : null;
            } catch (IllegalArgumentException invalid) {
                return null;
            }
        }
        if (version==V5) {
            if (!data.contains(EXACT_KEY)) return null;
            boolean exact=data.getBoolean(EXACT_KEY);
            if (exact && !data.contains(DEPTHS_KEY)) return null;
            if (!exact && data.contains(DEPTHS_KEY)) return null;
            int[] depths=exact ? data.getIntArray(DEPTHS_KEY) : null;
            try {
                Result corrected=calculate(before,attempts,prior,current,known,depths);
                long[] previous=oldV5(before,attempts,prior,current,known,depths);
                return previous[0]==bps && previous[1]==stored ? corrected : null;
            } catch (IllegalArgumentException invalid) {
                return null;
            }
        }
        if (version==V4) {
            if (!data.contains(EXACT_KEY)) return null;
            boolean exact=data.getBoolean(EXACT_KEY);
            if (exact && !data.contains(DEPTHS_KEY)) return null;
            if (!exact && data.contains(DEPTHS_KEY)) return null;
            int[] depths=exact ? data.getIntArray(DEPTHS_KEY) : null;
            try {
                // Verify exact recorded depths and the original quarter-credit receipt
                // before recalculating. Preserve complete v4 depths during migration.
                Result corrected=calculate(before,attempts,prior,current,known,depths);
                long[] original=oldV4(before,attempts,prior,current,known,depths);
                return original[0]==bps && original[1]==stored ? corrected : null;
            } catch (IllegalArgumentException invalid) {
                return null;
            }
        }
        if (version==V1 || version==V2) {
            if (bps!=oldBps(version,attempts,prior,current,known)) return null;
            long oldEarned=BigInteger.valueOf(before).multiply(BigInteger.valueOf(bps))
                    .divide(BigInteger.valueOf(10000)).longValueExact();
            if (oldEarned!=stored) return null;
        } else {
            // v3: 100 - 75*(D/25)^3 - 5*(A-1); earned truncated at the end.
            BigInteger numerator=BigInteger.valueOf(100*CUBIC_DENOMINATOR);
            if (known && attempts>0) {
                int d=Math.max(0,Math.min(25,prior));
                numerator=numerator.subtract(BigInteger.valueOf(75L*d*d*d))
                        .subtract(BigInteger.valueOf(5*CUBIC_DENOMINATOR)
                                .multiply(BigInteger.valueOf((long)attempts-1)));
                if (numerator.signum()<0) numerator=BigInteger.ZERO;
            }
            int legacyBps=numerator.multiply(BigInteger.valueOf(100))
                    .add(BigInteger.valueOf(CUBIC_DENOMINATOR/2))
                    .divide(BigInteger.valueOf(CUBIC_DENOMINATOR)).intValueExact();
            long legacyEarned=BigInteger.valueOf(before).multiply(numerator)
                    .divide(BigInteger.valueOf(100*CUBIC_DENOMINATOR)).longValueExact();
            if (legacyBps!=bps || legacyEarned!=stored) return null;
        }
        // Historical v1-v3 snapshots did not record individual depths.
        // Use A / previous maximum D as an explicit conservative fallback.
        return calculate(before,attempts,prior,current,known);
    }

    public static boolean migrate(Bundle data) {
        if (data==null || !data.contains(VERSION_KEY)
                || data.getInt(VERSION_KEY)==VERSION) return false;
        Result corrected=restore(data);
        if (corrected==null) return false;
        corrected.store(data);
        return true;
    }

    public static long earnedScore(Bundle data,long normalScore) {
        Result result=restore(data);
        return result==null || result.before!=normalScore ? normalScore : result.earned;
    }
}
