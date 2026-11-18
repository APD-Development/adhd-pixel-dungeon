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

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class BackendGroupsTest {

	private static Id p( String s ){
		return Id.of(Id.Kind.PLAYER, s);
	}

	private final AuditLog audit = new AuditLog();
	private final Groups groups = new Groups(audit, new java.security.SecureRandom());

	@Test
	public void platformPermissionsCanNeverComeFromAGroup(){
		for (Roles.InGroup r : Roles.InGroup.values()){
			for (Permission x : Roles.of(r)) assertFalse(r + " must not reach a platform permission: " + x, Roles.isPlatformPermission(x));
		}
		assertTrue(Roles.of(Roles.Platform.DEVELOPER).contains(Permission.ISSUE_DEVELOPER_GRANT));
		assertFalse("moderators cannot issue grants", Roles.of(Roles.Platform.MODERATOR).contains(Permission.ISSUE_DEVELOPER_GRANT));
		assertFalse("reviewers can only approve content", Roles.of(Roles.Platform.REVIEWER).contains(Permission.TRIAGE_REPORTS));
		for (Roles.Platform r : Roles.Platform.values()){
			for (Permission x : Roles.of(r)) assertTrue(Roles.isPlatformPermission(x));
		}
	}

	@Test
	public void membershipFollowsPermissionsAndKeepsAnOwner(){
		Id owner = p("owner"), a = p("a"), b = p("b");
		Id g = groups.create(owner, "creator-team", "Team", 1);
		assertEquals(Roles.InGroup.OWNER, groups.roleOf(g, owner));
		assertEquals("an outsider cannot add", Groups.Result.NOT_ALLOWED, groups.add(g, a, b, 2));
		assertEquals(Groups.Result.OK, groups.add(g, owner, a, 2));
		assertEquals(Groups.Result.ALREADY_MEMBER, groups.add(g, owner, a, 2));
		assertEquals("an ordinary member cannot add", Groups.Result.NOT_ALLOWED, groups.add(g, a, b, 3));
		assertEquals(Groups.Result.NOT_ALLOWED, groups.setRole(g, a, a, Roles.InGroup.OWNER, 3));
		assertEquals("nobody raises their own role, not even an owner", Groups.Result.NOT_ALLOWED, groups.setRole(g, owner, owner, Roles.InGroup.OWNER, 3));
		assertEquals(Groups.Result.OK, groups.setRole(g, owner, a, Roles.InGroup.MODERATOR, 4));
		assertEquals(Groups.Result.OK, groups.add(g, a, b, 5));
		assertEquals("a moderator removes an ordinary member", Groups.Result.OK, groups.remove(g, a, b, 6));
		assertEquals("but not the owner", Groups.Result.CANNOT_CHANGE_HIGHER_ROLE, groups.remove(g, a, owner, 7));
		assertEquals("the last owner cannot leave", Groups.Result.LAST_OWNER, groups.leave(g, owner, 8));
		assertEquals("the last owner cannot be demoted", Groups.Result.NOT_ALLOWED, groups.setRole(g, owner, owner, Roles.InGroup.MEMBER, 8));
		assertEquals(Groups.Result.OK, groups.setRole(g, owner, a, Roles.InGroup.OWNER, 9));
		assertEquals("with a second owner the first may leave", Groups.Result.OK, groups.leave(g, owner, 10));
		assertEquals(Roles.InGroup.NONE, groups.roleOf(g, owner));
		assertFalse(groups.may(g, owner, Permission.VIEW_GROUP));
		assertEquals(Groups.Result.UNKNOWN_GROUP, groups.add(Id.of(Id.Kind.GROUP, "gp_nope"), a, b, 11));
	}

	@Test
	public void everyChangeIsAudited(){
		Id owner = p("o"), a = p("a");
		Id g = groups.create(owner, "reviewers", "R", 1);
		groups.add(g, owner, a, 2);
		groups.setRole(g, owner, a, Roles.InGroup.MODERATOR, 3);
		groups.leave(g, a, 4);
		assertEquals(4, audit.entries().size());
		assertTrue(AuditLog.verify(audit.entries(), audit.headHashHex()));
		assertEquals("group.role", audit.entries().get(2).action);
	}

	@Test
	public void groupsHaveASizeLimit(){
		Id owner = p("o");
		Id g = groups.create(owner, "x", "X", 1);
		for (int i = 1; i < Groups.MAX_MEMBERS; i++) assertEquals(Groups.Result.OK, groups.add(g, owner, p("m" + i), 2));
		assertEquals(Groups.Result.GROUP_FULL, groups.add(g, owner, p("extra"), 3));
		assertEquals(Groups.MAX_MEMBERS, groups.size(g));
	}
}
