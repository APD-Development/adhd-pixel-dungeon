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

package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ByteSink;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LogFormat;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LogHeader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.PayloadReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.PositionedStringLookup;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StringLookup;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

//Makes a copy of a run log that holds only what the player could know, for the archive of a run that is NOT finished.
//
//Hidden (INTERNAL) information of a run that is still going must never leave the app in a readable form, so an archive carries such
//a run only as this copy: every INTERNAL event is left out, the clock of the rest is kept exactly, and the strings that only the
//left-out events used are blanked (their ids stay, so nothing else has to change). One PUBLIC event mixes in hidden facts, the
//run start, which holds the dungeon seed: it is rewritten without the seed and the custom seed text.
//
//Events of a type that is not on the audited list below are treated as INTERNAL, whatever their registered visibility, so a new
//event type can never slip into an archive by accident (a test checks that every PUBLIC type is on the list).
public final class LogRedactor {

	private LogRedactor(){}

	//what a redacted log's header says about the build that wrote it, so a reader can tell it is a redacted copy
	public static final String REDACTED_MARK = com.shatteredpixel.shatteredpixeldungeon.apd.stats.LogHeader.REDACTED_PREFIX;

	public static boolean isRedacted( String writerBuild ){
		return writerBuild != null && writerBuild.startsWith(REDACTED_MARK);
	}

	//the PUBLIC event types whose payload has been audited: every field is something the player could know, except the run start's
	//seed (handled here). Adding a PUBLIC type means auditing its fields and adding it here and to decode().
	public static boolean isAudited( int type ){
		switch (type){
			case EventTypes.GAP: case EventTypes.RUN_START: case EventTypes.RUN_END: case EventTypes.SAVE: case EventTypes.LEVEL_ENTER:
			case EventTypes.LEVEL_EXIT: case EventTypes.SESSION_START: case EventTypes.ROLLBACK: case EventTypes.RULESET_CHANGE:
			case EventTypes.ITEM_SEEN: case EventTypes.ITEM_ACQUIRED: case EventTypes.ITEM_LEFT: case EventTypes.ITEM_EQUIPPED:
			case EventTypes.ITEM_UNEQUIPPED: case EventTypes.ITEM_KNOWLEDGE: case EventTypes.ITEM_CLASS_KNOWN:
			case EventTypes.ATTACK: case EventTypes.DAMAGE: case EventTypes.HEAL: case EventTypes.MAX_HP_CHANGE: case EventTypes.SHIELD_GAIN:
			case EventTypes.HUNGER_CHANGE: case EventTypes.HUNGER_STATE: case EventTypes.XP_GAIN: case EventTypes.LEVEL_UP:
			case EventTypes.KILL: case EventTypes.HERO_DEATH: case EventTypes.HERO_REVIVED: case EventTypes.HP_CHANGE_INFERRED:
			case EventTypes.LEGACY_RECORD: case EventTypes.LEGACY_TOTALS: case EventTypes.LEGACY_DAILY_SCORE:
			//generic facts: a PUBLIC kind only ever carries what its declaration says the player could know (a kind with hidden truth is INTERNAL
			//and uses the other event types, which are never kept)
			case EventTypes.FACT: case EventTypes.FACT_SERIES:
				return true;
			default:
				return false;
		}
	}

	//A kept event: registered PUBLIC and audited
	static boolean keeps( int type ){
		return EventTypes.visibility(type) == EventTypes.Visibility.PUBLIC && isAudited(type);
	}

	//Decodes an event of an audited type through 'lookup' (so the lookup sees every string id the payload uses). Throws if it is damaged.
	static void decode( ApdEvent e, StringLookup lookup ){
		switch (e.type){
			case EventTypes.GAP: break;   //numbers only
			case EventTypes.RUN_START: LifecycleEvents.decodeRunStart(e, lookup); break;
			case EventTypes.RUN_END: LifecycleEvents.decodeRunEnd(e, lookup); break;
			case EventTypes.SAVE: LifecycleEvents.decodeSave(e); break;
			case EventTypes.LEVEL_ENTER: LifecycleEvents.decodeLevelEnter(e, lookup); break;
			case EventTypes.LEVEL_EXIT: LifecycleEvents.decodeLevelExit(e); break;
			case EventTypes.SESSION_START: LifecycleEvents.decodeSessionStart(e, lookup); break;
			case EventTypes.ROLLBACK: LifecycleEvents.decodeRollback(e); break;
			case EventTypes.RULESET_CHANGE: LifecycleEvents.decodeRulesetChange(e, lookup); break;
			case EventTypes.ITEM_SEEN: ItemEvents.decodeSeen(e, lookup); break;
			case EventTypes.ITEM_ACQUIRED: ItemEvents.decodeAcquired(e, lookup); break;
			case EventTypes.ITEM_LEFT: ItemEvents.decodeLeft(e, lookup); break;
			case EventTypes.ITEM_EQUIPPED: case EventTypes.ITEM_UNEQUIPPED: ItemEvents.decodeEquip(e, lookup); break;
			case EventTypes.ITEM_KNOWLEDGE: ItemEvents.decodeKnowledge(e, lookup); break;
			case EventTypes.ITEM_CLASS_KNOWN: ItemEvents.decodeClassKnown(e, lookup); break;
			case EventTypes.ATTACK: CombatEvents.decodeAttack(e, lookup); break;
			case EventTypes.DAMAGE: CombatEvents.decodeDamage(e, lookup); break;
			case EventTypes.HEAL: CombatEvents.decodeHeal(e, lookup); break;
			case EventTypes.MAX_HP_CHANGE: CombatEvents.decodeMaxHpChange(e, lookup); break;
			case EventTypes.SHIELD_GAIN: CombatEvents.decodeShieldGain(e, lookup); break;
			case EventTypes.HUNGER_CHANGE: CombatEvents.decodeHungerChange(e, lookup); break;
			case EventTypes.HUNGER_STATE: CombatEvents.decodeHungerState(e, lookup); break;
			case EventTypes.XP_GAIN: CombatEvents.decodeXpGain(e, lookup); break;
			case EventTypes.LEVEL_UP: CombatEvents.decodeLevelUp(e, lookup); break;
			case EventTypes.KILL: CombatEvents.decodeKill(e, lookup); break;
			case EventTypes.HERO_DEATH: CombatEvents.decodeHeroDeath(e, lookup); break;
			case EventTypes.HERO_REVIVED: CombatEvents.decodeHeroRevived(e, lookup); break;
			case EventTypes.HP_CHANGE_INFERRED: CombatEvents.decodeHpChangeInferred(e, lookup); break;
			case EventTypes.FACT: case EventTypes.FACT_SERIES: com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents.decode(e, lookup); break;
			case EventTypes.LEGACY_RECORD: LegacyEvents.decodeRecord(e, lookup); break;
			case EventTypes.LEGACY_TOTALS: LegacyEvents.decodeTotals(e, lookup); break;
			case EventTypes.LEGACY_DAILY_SCORE: LegacyEvents.decodeDailyScore(e, lookup); break;
			default: throw new IllegalStateException("no decoder for audited event type " + e.type);
		}
	}

	//a lookup that remembers which string ids were asked for, and where in the payload their references ended
	private static final class Recording implements PositionedStringLookup {
		final StringLookup inner;
		final java.util.List<int[]> hits = new java.util.ArrayList<>();

		Recording( StringLookup inner ){
			this.inner = inner;
		}

		@Override
		public String get( int id ){
			return inner.get(id);
		}

		@Override
		public String get( int id, int endPosition ){
			hits.add(new int[]{ id, endPosition });
			return inner.get(id);
		}
	}

	//a lookup that answers with the id itself, to read the ids out of a payload
	private static final class Ids implements StringLookup {
		@Override
		public String get( int id ){
			return Integer.toString(id);
		}
	}

	public static final class Result {
		public byte[] bytes;
		public long eventsKept;
		public long eventsLeftOut;
		//events of an audited type that could not be read and were left out too
		public long eventsUnreadable;
	}

	//The redacted copy of a whole log file, or null when the file is not a readable log at all.
	//The copy's string table is renumbered to hold only the strings its own events use (in order of first use), so neither a hidden
	//string's text nor how many hidden strings there are can be read from it: it is a function of what the player could know.
	public static Result redact( byte[] log ){
		ReadResult r = RunLogReader.parse(log);
		if (r.header == null) return null;
		Result out = new Result();

		int tableSize = r.strings.size();
		int emptyId = tableSize;   //an id past the table: the empty string the redacted run start points to
		for (int i = 0; i < tableSize; i++) if (r.strings.get(i) != null && r.strings.get(i).length() == 0){
			emptyId = i;
			break;
		}

		java.util.Map<Integer, Integer> newIds = new java.util.LinkedHashMap<>();
		java.util.List<ApdEvent> kept = new java.util.ArrayList<>();
		for (ApdEvent e : r.events){
			if (!keeps(e.type)){
				out.eventsLeftOut++;
				continue;
			}
			try {
				byte[] payload = e.type == EventTypes.RUN_START ? redactRunStart(e.payload, emptyId) : e.payload;
				Recording rec = new Recording(r);
				decode(new ApdEvent(e.index, e.type, e.flags, e.clock, payload), rec);
				kept.add(new ApdEvent(e.index, e.type, e.flags, e.clock, renumber(payload, rec.hits, newIds)));
			} catch (RuntimeException bad){
				out.eventsUnreadable++;
			}
		}

		//A clean encoding of a damaged prefix must not turn incomplete counts into exact facts.
		//Do not expose discarded byte sizes, hidden-event counts or hidden-only clocks.
		if (!r.status.isClean() || out.eventsUnreadable > 0){
			long clock = kept.isEmpty() ? 0 : kept.get(kept.size() - 1).clock;
			byte[] gap = new ByteSink().writeUnsigned(EventTypes.GAP_REDACTED_INCOMPLETE).writeUnsigned(0).toByteArray();
			kept.add(new ApdEvent(r.eventCount, EventTypes.GAP, 0, clock, gap));
		}

		ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.max(1024, log.length / 4));
		try {
			bytes.write(LogHeader.encode(r.header.telemetryVersion, r.header.createdAtMillis, r.header.runId, REDACTED_MARK + r.header.writerBuild));
			String[] table = new String[newIds.size()];
			for (java.util.Map.Entry<Integer, Integer> m : newIds.entrySet()){
				String s = m.getKey() < tableSize ? r.strings.get(m.getKey()) : null;
				table[m.getValue()] = s == null ? "" : s;
			}
			for (int id = 0; id < table.length; id++) bytes.write(LogFormat.stringFrame(id, table[id]));
			long prev = 0;
			int since = 128;   //forces a clock frame before the first event, as the writer does
			for (ApdEvent e : kept){
				if (since >= 128){
					bytes.write(LogFormat.clockFrame(prev));
					since = 0;
				}
				bytes.write(LogFormat.eventFrame(e.type, e.flags, e.clock - prev, e.payload));
				prev = e.clock;
				since++;
			}
			if (r.sealed) bytes.write(LogFormat.sealFrame(kept.size(), prev));
		} catch (java.io.IOException impossible){
			throw new IllegalStateException(impossible);
		}
		out.bytes = bytes.toByteArray();
		out.eventsKept = kept.size();
		return out;
	}

	//the payload with each string reference replaced by its new id (assigned in order of first use across the kept events)
	private static byte[] renumber( byte[] payload, java.util.List<int[]> hits, java.util.Map<Integer, Integer> newIds ){
		if (hits.isEmpty()) return payload;
		ByteSink o = new ByteSink(payload.length + 8);
		int copied = 0;
		for (int[] h : hits){
			int oldId = h[0], end = h[1];
			int start = end - varintLength(oldId);
			if (start < copied || start < 0) throw new IllegalStateException("string reference out of order");
			o.writeBytes(payload, copied, start - copied);
			Integer id = newIds.get(oldId);
			if (id == null){
				id = newIds.size();
				newIds.put(oldId, id);
			}
			o.writeUnsigned(id);
			copied = end;
		}
		o.writeBytes(payload, copied, payload.length - copied);
		return o.toByteArray();
	}

	private static int varintLength( long v ){
		return new ByteSink(10).writeUnsigned(v).size();
	}

	//The run start without its seed: the seed becomes 0 and the custom seed text the empty string; every other field is unchanged.
	//The field order is that of LifecycleEvents.emitRunStart (a test pins it).
	static byte[] redactRunStart( byte[] payload, int emptyStringId ){
		return runStartPayload(payload, true, emptyStringId);
	}

	static byte[] runStartPayload( byte[] payload, boolean redact, int emptyStringId ){
		PayloadReader p = new ApdEvent(0, EventTypes.RUN_START, 0, 0, payload).reader(new Ids());
		ByteSink o = new ByteSink(payload.length + 8);
		o.writeUnsigned(id(p.string()));           //runId
		o.writeUnsigned(p.unsigned());             //segment
		o.writeUnsigned(p.unsigned());             //startKind
		o.writeUnsigned(p.unsigned());             //apdVersionCode
		o.writeUnsigned(id(p.string()));           //apdVersionName
		o.writeUnsigned(id(p.string()));           //baseGame
		o.writeUnsigned(p.unsigned());             //initialVersion
		long seed = p.signed();
		o.writeSigned(redact ? 0 : seed);          //seed
		o.writeUnsigned(p.unsigned());             //seedType
		int custom = id(p.string());
		o.writeUnsigned(redact ? emptyStringId : custom);   //customSeedText
		o.writeUnsigned(p.unsigned());             //challenges
		o.writeUnsigned(id(p.string()));           //heroClass
		o.writeUnsigned(id(p.string()));           //rulesetId
		o.writeUnsigned(id(p.string()));           //rulesetText
		o.writeUnsigned(p.unsigned());             //exoticChance
		o.writeBoolean(p.bool());                  //introActive
		o.writeBoolean(p.bool());                  //externalModsPresent
		o.writeUnsigned(id(p.string()));           //platform
		o.writeUnsigned(p.unsigned());             //realTimeMillis
		//telemetry 1.7 appended the run's difficulty and balance revision (not hidden information: copied as they are); older starts end above
		if (p.hasRemaining()){
			o.writeUnsigned(p.unsigned());         //difficulty
			if (p.hasRemaining()) o.writeUnsigned(p.unsigned());   //difficultyBalance
		}
		// Telemetry1.11: user-selected rules are public; retain their string reference.
		if (p.hasRemaining()) o.writeUnsigned(id(p.string()));
        if(p.hasRemaining())o.writeUnsigned(p.unsigned());
        for(int i=0;i<3&&p.hasRemaining();i++)o.writeUnsigned(id(p.string()));
		if (p.hasRemaining()) throw new IllegalStateException("run start has fields this redactor does not know");
		return o.toByteArray();
	}

	private static int id( String s ){
		return Integer.parseInt(s);
	}
}
