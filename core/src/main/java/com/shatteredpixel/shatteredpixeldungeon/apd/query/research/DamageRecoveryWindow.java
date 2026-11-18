/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.apd.query.research;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import java.util.*;

/** Correlated, read-only hero-damage-to-healing question over existing public observations. */
public final class DamageRecoveryWindow {
    private DamageRecoveryWindow(){}

    public static ResearchDocument answer(Catalog catalog, QueryResult anchors, Row anchor, int turns, String rules) {
        ResearchDocument d=new ResearchDocument("Healing after damage");
        if(anchors!=null && !anchors.problems.isEmpty()){
            d.notes.add("Cannot interpret this damage query: "+String.join("; ",anchors.problems));
            return d;
        }
        if(anchors==null || anchors.isGrouped() || !EventDatasets.DAMAGE.equals(anchors.def.id)
                || !anchors.rows.contains(anchor) || anchor.imported || anchor.duplicate
                || anchor.runId.isEmpty()
                || !ResearchData.exact(anchors,anchor,"damage.turn")
                || !ResearchData.exact(anchors,anchor,"damage.targetKind")){
            d.notes.add("Select a recorded, exact hero-damage occurrence in the current filtered Damage table. Imported or duplicate records cannot establish this sequence.");
            return d;
        }
        Long start=ResearchData.number(anchors,anchor,"damage.turn");
        Long targetKind=ResearchData.number(anchors,anchor,"damage.targetKind");
        if(targetKind==null || targetKind!=CombatEvents.KIND_HERO){
            d.notes.add("This question starts only from damage recorded against the hero.");
            return d;
        }
        RunTurnWindow window;
        try{
            if(start==null || turns>1000)throw new IllegalArgumentException();
            window=RunTurnWindow.strictlyFollowing(anchor.runId,start,turns);
        }catch(IllegalArgumentException bad){
            d.notes.add("The turn window is invalid or exceeds the recorded clock range.");
            return d;
        }
        d.notes.add("Interpreted question: total observed HP healing in "+window.describeStrictlyFollowing("hero-damage")
                +". Damage source: "+ResearchData.label(anchors,anchor,"damage.source")+". The damage turn is excluded because within-turn ordering is not established by these table rows.");
        d.notes.add("Healing is recorded HP recovery, not shielding, max-HP increases or an inferred claim that the selected damage caused a heal. This is an observed lower bound. Missing historical events, gaps and unsupported recording versions are never reconstructed. Scope and disclosure are retained; Damage-only filters are not reinterpreted as Healing filters.");

        Dataset data=catalog==null ? null:catalog.dataset(EventDatasets.HEALS);
        boolean covered=false;
        if(data!=null)for(Dataset.RunInfo info:data.runs)
            if(anchor.runId.equals(info.runId) && info.covered && !info.duplicate)covered=true;
        if(!covered){
            d.notes.add("Healing recording coverage for this run is unavailable. The total is unknown, not zero.");
            return d;
        }

        Query q=window.apply(Query.of(EventDatasets.HEALS)
                        .withScope(anchors.query.scope).withShowInternal(anchors.query.showInternal),
                "heal.run","heal.turn");
        QueryResult heals=ResearchData.select(catalog,q,rules);
        if(heals==null){
            d.notes.add("Healing observations are unavailable, or the schema cannot apply this question. No numeric answer can be established.");
            return d;
        }
        List<Row> contributing=new ArrayList<>();
        long total=0;
        int excluded=0;
        boolean overflow=false;
        for(Row r:ResearchData.distinct(heals)){
            Long n=ResearchData.number(heals,r,"heal.amount");
            if(n==null || n<0 || !ResearchData.exact(heals,r,"heal.amount")
                    || !ResearchData.exact(heals,r,"heal.turn")){
                excluded++;
                continue;
            }
            contributing.add(r);
            try{total=Math.addExact(total,n);}catch(ArithmeticException bad){overflow=true;}
        }
        int undecidable=0;
        for(QueryResult.FilterReport f:heals.stats.filters)undecidable+=f.unknown+f.uncertain+f.hidden;
        String shown=overflow ? "?":total+" observed (at least)";
        String detail="Exact contributing healing records="+contributing.size()
                +"; matched uncertain/invalid amounts or clocks excluded="+excluded
                +"; filter evaluations excluded for unknown/uncertain/hidden fields="+undecidable
                +". Zero observed means no qualifying exact HP healing was recorded, not proof that no recovery happened."
                +(overflow ? " The sum exceeds the integer range; no numeric total is reported.":"");
        d.cards.add(new ResearchDocument.Card("Observed healing, next "+turns+" turns",shown,
                overflow ? CellText.UNKNOWN:CellText.LOWER,detail,
                new ResearchDocument.Evidence("Anchor hero damage",anchors,Collections.singletonList(anchor)),
                new ResearchDocument.Evidence("Contributing healing records",heals,contributing)));
        return d;
    }
}
