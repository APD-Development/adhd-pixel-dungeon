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

// What an actor may do on shared systems. Systems check permissions, never role names.
public enum Permission {
	//platform level (APD staff): never granted by gameplay or by group membership
	ISSUE_DEVELOPER_GRANT, APPROVE_CONTENT, REVOKE_CONTENT, TRIAGE_REPORTS, HIDE_SUBMISSION, VIEW_AUDIT_LOG,
	//group level (any kind of group a community feature defines)
	MANAGE_GROUP, MANAGE_MEMBERS, MODERATE_GROUP, POST_IN_GROUP, VIEW_GROUP
}
