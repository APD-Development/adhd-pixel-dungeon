package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
/** A lexical callback scope; never retains actors after the callback completes. */
public final class OffensiveInvocation implements AutoCloseable {
 private static final ThreadLocal<OffensiveInvocation> CURRENT=new ThreadLocal<>();
 private final OffensiveInvocation previous;private final Char actor;private final OffensiveDamageOrigin origin;
 private boolean closed;
 private OffensiveInvocation(Char actor,OffensiveDamageOrigin origin){if(origin==null)throw new IllegalArgumentException("Missing invocation origin");this.actor=actor;this.origin=origin;previous=CURRENT.get();CURRENT.set(this);}
 public static OffensiveInvocation enter(Char actor,OffensiveDamageOrigin origin){return new OffensiveInvocation(actor,origin);}
 static OffensiveDamageOrigin captured(Char actor){for(OffensiveInvocation scope=CURRENT.get();scope!=null;scope=scope.previous)if(scope.actor==actor)return scope.origin;return null;}
 @Override public void close(){if(closed)return;if(CURRENT.get()!=this)throw new IllegalStateException("Invocation scopes must close in order");closed=true;if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
