/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.watabou.input;

import com.badlogic.gdx.Gdx;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Timing-only correlation for intermittent Android Back latency. Disabled by
 * default and enabled by the Android launcher only for the private developer
 * build. It never accepts, rejects, delays or dispatches input itself.
 */
public final class BackTrace {

	private static final int MAX_RECENT = 64;
	private static final ArrayDeque<String> recent = new ArrayDeque<>();
	private static final ThreadLocal<Long> dispatch = new ThreadLocal<>();

	private static boolean enabled;
	private static long nextRoute;
	private static long acceptedRoute;
	private static long acceptedAt;

	private BackTrace(){}

	public static synchronized void setEnabled(boolean value){
		enabled = value;
		if (!value) {
			recent.clear();
			acceptedRoute = acceptedAt = 0;
			dispatch.remove();
		}
	}

	public static synchronized boolean enabled(){
		return enabled;
	}

	public static long route(String source){
		if (!enabled()) return 0;
		long now = System.nanoTime();
		synchronized (BackTrace.class) {
			long id = ++nextRoute;
			record(id, "route", source, now);
			return id;
		}
	}

	public static void accepted(long route){
		if (route == 0) return;
		long now = System.nanoTime();
		synchronized (BackTrace.class) {
			if (!enabled) return;
			acceptedRoute = route;
			acceptedAt = now;
			record(route, "accepted", "", now);
		}
	}

	public static void rejected(long route, String reason){
		stage(route, "rejected", reason);
	}

	public static void stage(long route, String stage, String detail){
		if (route == 0) return;
		long now = System.nanoTime();
		synchronized (BackTrace.class) {
			if (enabled) record(route, stage, detail, now);
		}
	}

	public static void beginDispatch(long route){
		if (route == 0 || !enabled()) return;
		dispatch.set(route);
		stage(route, "render-dispatch-begin", "");
	}

	public static void endDispatch(long route){
		if (route == 0) return;
		stage(route, "render-dispatch-end", "");
		dispatch.remove();
	}

	public static long currentDispatch(){
		Long route = dispatch.get();
		return route == null ? 0 : route;
	}

	public static synchronized List<String> snapshot(){
		return new ArrayList<>(recent);
	}

	public static synchronized void resetForTest(){
		enabled = false;
		nextRoute = acceptedRoute = acceptedAt = 0;
		recent.clear();
		dispatch.remove();
	}

	private static void record(long route, String stage, String detail, long now){
		long deltaNanos = route == acceptedRoute && acceptedAt != 0 ? Math.max(0, now - acceptedAt) : -1;
		String line = "route=" + route
				+ " stage=" + stage
				+ " monotonic_ns=" + now
				+ " delta_ms=" + (deltaNanos < 0 ? "n/a" : Long.toString(deltaNanos / 1_000_000L))
				+ (detail == null || detail.isEmpty() ? "" : " detail=" + detail);
		if (recent.size() == MAX_RECENT) recent.removeFirst();
		recent.addLast(line);
		if (Gdx.app != null) Gdx.app.log("APD-BACK", line);
	}
}
