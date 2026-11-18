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

/** Correlated, read-only equipment-to-damage question over existing public observations. */
public final class EquipmentWindow {
    private EquipmentWindow(){}
    public static ResearchDocument answer(Catalog catalog,QueryResult anchors,Row anchor,int turns,String rules){
        ResearchDocument d=new ResearchDocument("Damage after equipping");
        if(anchors!=null && !anchors.problems.isEmpty()){
            d.notes.add("Cannot interpret this equipment query: "+String.join("; ",anchors.problems));return d;
        }
        if(anchors==null || !anchors.problems.isEmpty() || anchors.isGrouped() || !EventDatasets.EQUIPMENT.equals(anchors.def.id)
                || !anchors.rows.contains(anchor) || anchor.imported || anchor.duplicate
                || anchor.runId.isEmpty() || !ResearchData.exact(anchors,anchor,"equip.start")){
            d.notes.add("Select a recorded equipment occurrence with an exact start turn in the current filtered Equipment table. Imported or duplicate records cannot establish this sequence.");return d;
        }
        Long start=ResearchData.number(anchors,anchor,"equip.start");
        RunTurnWindow window;
        try{
            if(start==null || turns>1000)throw new IllegalArgumentException();
            window=RunTurnWindow.strictlyFollowing(anchor.runId,start,turns);
        }catch(IllegalArgumentException bad){
            d.notes.add("The turn window is invalid or exceeds the recorded clock range.");return d;
        }
        d.notes.add("Interpreted question: total observed hero-target damage in "+window.describeStrictlyFollowing("equipment")+". Equipment: "+ResearchData.label(anchors,anchor,"equip.item")+". The equip turn is excluded because within-turn ordering is not established by these table rows. Later equipment changes do not end this time window.");
        d.notes.add("Damage is the recorded displayed amount, including shield absorption, not just HP lost. This is an observed lower bound, not a claim of complete recording, armor effectiveness or causation. Missing historical events, gaps and unsupported recording versions are never reconstructed. Scope and disclosure are retained; the anchor already satisfied the Equipment filters. Equipment-only filters are not reinterpreted as damage filters.");
        Dataset data=catalog==null ? null:catalog.dataset(EventDatasets.DAMAGE);boolean covered=false;
        if(data!=null)for(Dataset.RunInfo info:data.runs)if(anchor.runId.equals(info.runId) && info.covered && !info.duplicate)covered=true;
        if(!covered){d.notes.add("Damage recording coverage for this run is unavailable. Older logs may record equipment without recording damage. The total is unknown, not zero.");return d;}
        Query q=window.apply(Query.of(EventDatasets.DAMAGE).withScope(anchors.query.scope).withShowInternal(anchors.query.showInternal),
                        "damage.run","damage.turn")
                .addFilter(Filter.num("damage.targetKind",Op.EQ,CombatEvents.KIND_HERO));
        QueryResult damage=ResearchData.select(catalog,q,rules);
        if(damage==null){d.notes.add("Damage observations are unavailable, or the schema cannot apply this question. No numeric answer can be established.");return d;}
        List<Row> contributing=new ArrayList<>();long total=0;int excluded=0;boolean overflow=false;
        for(Row r:ResearchData.distinct(damage)){
            Long n=ResearchData.number(damage,r,"damage.amount");
            if(n==null || n<0 || !ResearchData.exact(damage,r,"damage.amount")
                    || !ResearchData.exact(damage,r,"damage.turn") || !ResearchData.exact(damage,r,"damage.targetKind")){excluded++;continue;}
            contributing.add(r);
            try{total=Math.addExact(total,n);}catch(ArithmeticException bad){overflow=true;}
        }
        int undecidable=0;for(QueryResult.FilterReport f:damage.stats.filters)undecidable+=f.unknown+f.uncertain+f.hidden;
        String shown=overflow ? "?":total+" observed (at least)";
        String detail="Exact contributing damage records="+contributing.size()+"; matched uncertain/invalid amounts or clocks excluded="+excluded
                +"; filter evaluations excluded for unknown/uncertain/hidden fields="+undecidable
                +". Zero observed means no qualifying exact positive damage was recorded, not proof that no damage happened."
                +(overflow ? " The sum exceeds the integer range; no numeric total is reported.":"");
        d.cards.add(new ResearchDocument.Card("Observed damage, next "+turns+" turns",shown,overflow ? CellText.UNKNOWN:CellText.LOWER,detail,
                new ResearchDocument.Evidence("Anchor equipment",anchors,Collections.singletonList(anchor)),
                new ResearchDocument.Evidence("Contributing damage records",damage,contributing)));
        return d;
    }
}
