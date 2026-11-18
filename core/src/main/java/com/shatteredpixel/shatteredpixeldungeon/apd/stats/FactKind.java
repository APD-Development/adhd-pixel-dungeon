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

package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//The declaration of one KIND of observable fact: "the hero took a step", "a spell was cast", "a status effect began", "gold was spent".
//This is the whole contract between a mechanic and UIMSD. A character, item or system that wants its history kept declares its kinds
//(see FactKinds) and records them through the recorder; the log, the datasets, the filters, the question builder and the guide are all
//driven by these declarations, so UIMSD needs no change for a new kind.
//
//One observation has: a subject (what it was about, a class key such as a spell or an enemy, may be empty), an actor (who did it, empty
//= the hero), a few NUMBERS (named by the kind) and a few TEXT TAGS (named by the kind). The run, floor, turn, character, difficulty,
//challenges and so on come from the run's own timeline when the facts are read, they are not stored on every observation.
//
//A kind key ("area.name") and the order of its fields are part of the stored format: never reuse a key, never reorder or remove fields
//(a test checks the key manifest). New fields may only be added at the END.
public final class FactKind {

	//how a stored number is shown: PLAIN, or divided by 100 / 1000 (HUNDREDTHS, THOUSANDTHS)
	public static final int PLAIN = 0, HUNDREDTHS = 2, THOUSANDTHS = 3;

	public static final class Field {
		public final String key;
		public final String label;
		public final String description;
		public final int scale;

		Field( String key, String label, String description, int scale ){
			this.key = key;
			this.label = label;
			this.description = description;
			this.scale = scale;
		}
	}

	public final String key;
	public final String label;          //what one observation is called, singular: "Step", "Spell cast"
	public final String plural;         //"Steps", "Spell casts"
	public final String description;
	public final String area;           //the topic page it belongs to: "Movement", "Magic", "Economy"
	public final String subjectLabel;   //what the subject is: "Spell", "Item", "Status effect"; empty = this kind has no subject
	public final boolean hasWho;      //someone other than the hero can do it (enemies, allies)
	public final EventTypes.Visibility visibility;
	public final TelemetryVersion since;
	//observations are stored in delta-encoded series (high-frequency kinds): the same information, a few bytes each
	public final boolean series;
	private final List<Field> values;
	private final List<Field> tags;

	private FactKind( Builder b ){
		this.key = b.key;
		this.label = b.label;
		this.plural = b.plural != null ? b.plural : b.label + "s";
		this.description = b.description;
		this.area = b.area;
		this.subjectLabel = b.subjectLabel;
		this.hasWho = b.hasWho;
		this.visibility = b.visibility;
		this.since = b.since;
		this.series = b.series;
		this.values = Collections.unmodifiableList(new ArrayList<>(b.values));
		this.tags = Collections.unmodifiableList(new ArrayList<>(b.tags));
	}

	public List<Field> values(){
		return values;
	}

	public List<Field> tags(){
		return tags;
	}

	public boolean isInternal(){
		return visibility == EventTypes.Visibility.INTERNAL;
	}

	//the event type observations of this kind are stored in
	public int eventType(){
		return series ? (isInternal() ? EventTypes.FACT_SERIES_INTERNAL : EventTypes.FACT_SERIES)
				: (isInternal() ? EventTypes.FACT_INTERNAL : EventTypes.FACT);
	}

	public static Builder builder( String key, String label ){
		return new Builder(key, label);
	}

	public static final class Builder {
		private final String key;
		private final String label;
		private String plural;
		private String description = "";
		private String area = "Other";
		private String subjectLabel = "";
		private boolean hasWho;
		private EventTypes.Visibility visibility = EventTypes.Visibility.PUBLIC;
		private TelemetryVersion since = new TelemetryVersion(1, 8);
		private boolean series;
		private final List<Field> values = new ArrayList<>();
		private final List<Field> tags = new ArrayList<>();

		private Builder( String key, String label ){
			if (key == null || !key.matches("[a-z][a-zA-Z0-9]*(\\.[a-zA-Z0-9]+)+")) throw new IllegalArgumentException("bad fact kind key: " + key);
			this.key = key;
			this.label = label;
		}

		public Builder plural( String p ){ plural = p; return this; }
		public Builder description( String d ){ description = d; return this; }
		public Builder area( String a ){ area = a; return this; }
		public Builder subject( String label ){ subjectLabel = label; return this; }
		public Builder actor(){ hasWho = true; return this; }
		//hidden truth: never shown for a run that is not over
		public Builder internal(){ visibility = EventTypes.Visibility.INTERNAL; return this; }
		public Builder since( int major, int minor ){ since = new TelemetryVersion(major, minor); return this; }
		public Builder series(){ series = true; return this; }

		public Builder value( String key, String label ){
			return value(key, label, "", PLAIN);
		}

		public Builder value( String key, String label, String description, int scale ){
			values.add(new Field(key, label, description, scale));
			return this;
		}

		public Builder tag( String key, String label ){
			return tag(key, label, "");
		}

		public Builder tag( String key, String label, String description ){
			tags.add(new Field(key, label, description, PLAIN));
			return this;
		}

		public FactKind build(){
			return new FactKind(this);
		}
	}
}
