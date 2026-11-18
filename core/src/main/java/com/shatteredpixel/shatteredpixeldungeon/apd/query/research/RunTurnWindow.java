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

import com.shatteredpixel.shatteredpixeldungeon.apd.query.Filter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Op;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;

/**
 * Exact same-run turn boundaries for correlated, read-only questions.
 *
 * The canonical stored range is inclusive. Factories name any excluded boundary
 * explicitly and convert it once, so consumers cannot accidentally disagree
 * about wording and query arithmetic.
 */
public final class RunTurnWindow {

    public final String runId;
    public final long anchorTurn;
    public final long firstTurn;
    public final long lastTurn;
    public final long turnCount;

    private RunTurnWindow(String runId, long anchorTurn, long firstTurn, long lastTurn, long turnCount) {
        this.runId = runId;
        this.anchorTurn = anchorTurn;
        this.firstTurn = firstTurn;
        this.lastTurn = lastTurn;
        this.turnCount = turnCount;
    }

    /** Strictly after anchorTurn through anchorTurn+turnCount inclusive. */
    public static RunTurnWindow strictlyFollowing(String runId, long anchorTurn, long turnCount) {
        if (runId == null || runId.isEmpty() || anchorTurn < 0 || turnCount < 1
                || anchorTurn == Long.MAX_VALUE || anchorTurn > Long.MAX_VALUE - turnCount) {
            throw new IllegalArgumentException("Invalid exact run/turn window");
        }
        return new RunTurnWindow(runId, anchorTurn, anchorTurn + 1, anchorTurn + turnCount, turnCount);
    }

    /**
     * Adds exact, case-sensitive run identity and inclusive turn bounds without
     * replacing the caller's dataset, scope, disclosure choice or other filters.
     */
    public Query apply(Query base, String runColumn, String turnColumn) {
        if (base == null || runColumn == null || runColumn.isEmpty() || turnColumn == null || turnColumn.isEmpty()) {
            throw new IllegalArgumentException("A base query and run/turn columns are required");
        }
        return base.addFilter(Filter.text(runColumn, Op.GROUP_TEXT, runId))
                .addFilter(Filter.num(turnColumn, Op.BETWEEN, firstTurn, lastTurn));
    }

    public boolean contains(String candidateRunId, long candidateTurn) {
        return runId.equals(candidateRunId) && candidateTurn >= firstTurn && candidateTurn <= lastTurn;
    }

    public String describeStrictlyFollowing(String anchorLabel) {
        if (anchorLabel == null || anchorLabel.trim().isEmpty()) {
            throw new IllegalArgumentException("An anchor label is required");
        }
        return "run " + runId + ", strictly after " + anchorLabel + " turn " + anchorTurn
                + " through turn " + lastTurn + " inclusive (" + turnCount + " turns)";
    }
}
