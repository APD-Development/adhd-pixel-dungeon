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

package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

// Roles and their permissions in one table. Platform (staff) and group roles are separate scopes; a group role can never reach a platform permission.
public final class Roles {

	public enum Platform { NONE, REVIEWER, MODERATOR, DEVELOPER }

	public enum InGroup { NONE, MEMBER, MODERATOR, OWNER }

	private static final Map<Platform, Set<Permission>> PLATFORM = new EnumMap<>(Platform.class);
	private static final Map<InGroup, Set<Permission>> GROUP = new EnumMap<>(InGroup.class);
	static {
		PLATFORM.put(Platform.NONE, EnumSet.noneOf(Permission.class));
		PLATFORM.put(Platform.REVIEWER, EnumSet.of(Permission.APPROVE_CONTENT));
		PLATFORM.put(Platform.MODERATOR, EnumSet.of(Permission.TRIAGE_REPORTS, Permission.HIDE_SUBMISSION, Permission.REVOKE_CONTENT));
		PLATFORM.put(Platform.DEVELOPER, EnumSet.of(Permission.ISSUE_DEVELOPER_GRANT, Permission.APPROVE_CONTENT, Permission.REVOKE_CONTENT, Permission.TRIAGE_REPORTS,
				Permission.HIDE_SUBMISSION, Permission.VIEW_AUDIT_LOG));
		GROUP.put(InGroup.NONE, EnumSet.noneOf(Permission.class));
		GROUP.put(InGroup.MEMBER, EnumSet.of(Permission.VIEW_GROUP, Permission.POST_IN_GROUP));
		GROUP.put(InGroup.MODERATOR, EnumSet.of(Permission.VIEW_GROUP, Permission.POST_IN_GROUP, Permission.MODERATE_GROUP, Permission.MANAGE_MEMBERS));
		GROUP.put(InGroup.OWNER, EnumSet.of(Permission.VIEW_GROUP, Permission.POST_IN_GROUP, Permission.MODERATE_GROUP, Permission.MANAGE_MEMBERS, Permission.MANAGE_GROUP));
	}

	private Roles(){}

	public static Set<Permission> of( Platform r ){
		return Collections.unmodifiableSet(PLATFORM.get(r));
	}

	public static Set<Permission> of( InGroup r ){
		return Collections.unmodifiableSet(GROUP.get(r));
	}

	public static boolean isPlatformPermission( Permission p ){
		switch (p){
			case ISSUE_DEVELOPER_GRANT: case APPROVE_CONTENT: case REVOKE_CONTENT: case TRIAGE_REPORTS: case HIDE_SUBMISSION: case VIEW_AUDIT_LOG:
				return true;
			default:
				return false;
		}
	}
}
