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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Generic group with members and roles. Knows nothing about what a group is for. Permission-checked, always has an owner, no self-promotion, audited.
public final class Groups {

	public enum Result { OK, NOT_ALLOWED, UNKNOWN_GROUP, UNKNOWN_MEMBER, ALREADY_MEMBER, LAST_OWNER, CANNOT_CHANGE_HIGHER_ROLE, GROUP_FULL }

	public static final int MAX_MEMBERS = 500;

	private static final class Group {
		final String kind, name;
		final Map<Id, Roles.InGroup> members = new HashMap<>();

		Group( String kind, String name ){
			this.kind = kind;
			this.name = name;
		}
	}

	private final Map<Id, Group> groups = new HashMap<>();
	private final AuditLog audit;
	private final java.security.SecureRandom rnd;

	public Groups( AuditLog audit, java.security.SecureRandom rnd ){
		this.audit = audit;
		this.rnd = rnd;
	}

	//'kind' is a label the feature chooses ("creator-team", ...); the creator becomes the owner
	public Id create( Id creator, String kind, String name, long now ){
		Id id = Id.random(Id.Kind.GROUP, rnd);
		Group g = new Group(kind, name);
		g.members.put(creator, Roles.InGroup.OWNER);
		groups.put(id, g);
		audit.append(now, creator.value, "group.create", id.value, kind);
		return id;
	}

	public Roles.InGroup roleOf( Id group, Id player ){
		Group g = groups.get(group);
		Roles.InGroup r = g == null ? null : g.members.get(player);
		return r == null ? Roles.InGroup.NONE : r;
	}

	public boolean may( Id group, Id player, Permission p ){
		return Roles.of(roleOf(group, player)).contains(p);
	}

	public int size( Id group ){
		Group g = groups.get(group);
		return g == null ? 0 : g.members.size();
	}

	//an actor with MANAGE_MEMBERS adds a player as an ordinary member
	public Result add( Id group, Id actor, Id player, long now ){
		Group g = groups.get(group);
		if (g == null) return Result.UNKNOWN_GROUP;
		if (!may(group, actor, Permission.MANAGE_MEMBERS)) return Result.NOT_ALLOWED;
		if (g.members.containsKey(player)) return Result.ALREADY_MEMBER;
		if (g.members.size() >= MAX_MEMBERS) return Result.GROUP_FULL;
		g.members.put(player, Roles.InGroup.MEMBER);
		audit.append(now, actor.value, "group.add", group.value, player.value);
		return Result.OK;
	}

	//a player may always leave, except the last owner (who must hand over or delete the group)
	public Result leave( Id group, Id player, long now ){
		Group g = groups.get(group);
		if (g == null) return Result.UNKNOWN_GROUP;
		Roles.InGroup r = g.members.get(player);
		if (r == null) return Result.UNKNOWN_MEMBER;
		if (r == Roles.InGroup.OWNER && owners(g) <= 1) return Result.LAST_OWNER;
		g.members.remove(player);
		audit.append(now, player.value, "group.leave", group.value, player.value);
		return Result.OK;
	}

	//an actor with MANAGE_MEMBERS removes someone whose role is LOWER than the actor's (owners can only leave or be changed by another owner)
	public Result remove( Id group, Id actor, Id player, long now ){
		Group g = groups.get(group);
		if (g == null) return Result.UNKNOWN_GROUP;
		if (!may(group, actor, Permission.MANAGE_MEMBERS)) return Result.NOT_ALLOWED;
		Roles.InGroup target = g.members.get(player);
		if (target == null) return Result.UNKNOWN_MEMBER;
		if (target.ordinal() >= roleOf(group, actor).ordinal()) return Result.CANNOT_CHANGE_HIGHER_ROLE;
		g.members.remove(player);
		audit.append(now, actor.value, "group.remove", group.value, player.value);
		return Result.OK;
	}

	//only an owner (MANAGE_GROUP) sets roles; nobody changes their own role; an owner changing another owner is allowed, but the group keeps one
	public Result setRole( Id group, Id actor, Id player, Roles.InGroup role, long now ){
		Group g = groups.get(group);
		if (g == null) return Result.UNKNOWN_GROUP;
		if (!may(group, actor, Permission.MANAGE_GROUP) || actor.equals(player) || role == Roles.InGroup.NONE) return Result.NOT_ALLOWED;
		Roles.InGroup old = g.members.get(player);
		if (old == null) return Result.UNKNOWN_MEMBER;
		if (old == Roles.InGroup.OWNER && role != Roles.InGroup.OWNER && owners(g) <= 1) return Result.LAST_OWNER;
		g.members.put(player, role);
		audit.append(now, actor.value, "group.role", group.value, player.value + "=" + role);
		return Result.OK;
	}

	public List<Id> members( Id group ){
		Group g = groups.get(group);
		List<Id> out = new ArrayList<>();
		if (g != null) out.addAll(g.members.keySet());
		java.util.Collections.sort(out);
		return out;
	}

	private static int owners( Group g ){
		int n = 0;
		for (Roles.InGroup r : g.members.values()) if (r == Roles.InGroup.OWNER) n++;
		return n;
	}
}
