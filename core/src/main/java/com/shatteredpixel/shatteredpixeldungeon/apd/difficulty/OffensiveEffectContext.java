package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
/** Lexical origin for an identified offensive effect, not for unrelated terrain/trap activation. */
public final class OffensiveEffectContext implements AutoCloseable{
 private static final ThreadLocal<OffensiveEffectContext> CURRENT=new ThreadLocal<>();
 private final OffensiveEffectContext previous;private final OffensiveDamageOrigin origin;private boolean closed;
 private OffensiveEffectContext(OffensiveDamageOrigin role){if(role==null)throw new IllegalArgumentException("Missing effect origin");origin=role;previous=CURRENT.get();CURRENT.set(this);}
 public static OffensiveEffectContext enter(OffensiveDamageOrigin role){return new OffensiveEffectContext(role);}
 public static OffensiveDamageOrigin current(){OffensiveEffectContext c=CURRENT.get();return c==null?OffensiveDamageOrigin.UNATTRIBUTED:c.origin;}
 @Override public void close(){if(closed)return;if(CURRENT.get()!=this)throw new IllegalStateException("Effect contexts must close in order");closed=true;if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
