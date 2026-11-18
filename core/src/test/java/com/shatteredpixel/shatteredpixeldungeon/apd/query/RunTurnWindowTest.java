package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.research.RunTurnWindow;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunTurnWindowTest {

    @Test public void strictlyFollowingHasExclusiveAnchorAndInclusiveEnd() {
        RunTurnWindow w=RunTurnWindow.strictlyFollowing("run-A",10,10);
        assertEquals(10,w.anchorTurn);assertEquals(11,w.firstTurn);assertEquals(20,w.lastTurn);assertEquals(10,w.turnCount);
        assertFalse(w.contains("run-A",10));assertTrue(w.contains("run-A",11));assertTrue(w.contains("run-A",20));assertFalse(w.contains("run-A",21));
    }

    @Test public void runIdentityIsExactAndCaseSensitive() {
        RunTurnWindow w=RunTurnWindow.strictlyFollowing("Run-A",3,2);
        assertTrue(w.contains("Run-A",4));assertFalse(w.contains("run-a",4));assertFalse(w.contains("Run-A ",4));assertFalse(w.contains("",4));
    }

    @Test public void applyRetainsDatasetScopeDisclosureAndExistingFilters() {
        Scope scope=new Scope(Scope.Rules.ALL,"frozen",true,false,true,false);
        Query base=Query.of("target").withScope(scope).withShowInternal(true).addFilter(Filter.num("target.kind",Op.EQ,7));
        Query q=RunTurnWindow.strictlyFollowing("A",10,5).apply(base,"target.run","target.turn");
        assertEquals("target",q.dataset);assertEquals(scope,q.scope);assertTrue(q.showInternal);assertEquals(3,q.filters.size());
        assertEquals(Filter.num("target.kind",Op.EQ,7),q.filters.get(0));
        assertEquals(Filter.text("target.run",Op.GROUP_TEXT,"A"),q.filters.get(1));
        assertEquals(Filter.num("target.turn",Op.BETWEEN,11,15),q.filters.get(2));
        assertEquals(1,base.filters.size());
    }

    @Test public void appliedWindowSurvivesExactQueryCodecRoundTrip() {
        Query q=RunTurnWindow.strictlyFollowing("A",Long.MAX_VALUE-10,10)
                .apply(Query.of("damage"),"damage.run","damage.turn");
        Query restored=QueryCodec.decode(QueryCodec.encode(q));
        assertEquals(q,restored);
        assertArrayEquals(new long[]{Long.MAX_VALUE-9,Long.MAX_VALUE},restored.filters.get(1).nums);
    }

    @Test public void invalidWindowsAndMissingQueryPartsFailClosed() {
        invalid(null,1,1);invalid("",1,1);invalid("A",-1,1);invalid("A",1,0);invalid("A",Long.MAX_VALUE,1);invalid("A",Long.MAX_VALUE-1,2);
        RunTurnWindow w=RunTurnWindow.strictlyFollowing("A",1,1);
        try{w.apply(null,"run","turn");fail();}catch(IllegalArgumentException expected){}
        try{w.apply(Query.of("x"),"","turn");fail();}catch(IllegalArgumentException expected){}
        try{w.describeStrictlyFollowing(" ");fail();}catch(IllegalArgumentException expected){}
    }

    @Test public void sharedDescriptionPinsTheSameExactBoundaries() {
        assertEquals("run A, strictly after equipment turn 10 through turn 20 inclusive (10 turns)",
                RunTurnWindow.strictlyFollowing("A",10,10).describeStrictlyFollowing("equipment"));
    }

    private static void invalid(String run,long anchor,long turns) {
        try{RunTurnWindow.strictlyFollowing(run,anchor,turns);fail();}catch(IllegalArgumentException expected){}
    }
}
