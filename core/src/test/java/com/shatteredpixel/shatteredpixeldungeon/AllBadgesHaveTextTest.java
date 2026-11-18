package com.shatteredpixel.shatteredpixeldungeon;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Every badge a player can see has a title, a description and an image. */
public class AllBadgesHaveTextTest {
	@Test public void visibleBadgesHaveTextAndAnImage(){
		HeadlessTestSupport.start();
		List<String> problems = new ArrayList<>();
		for (Badges.Badge b : Badges.Badge.values()){
			if (b.type == Badges.BadgeType.HIDDEN) continue;
			String t = b.title(), d = b.desc();
			if (t == null || t.isEmpty() || t.contains(Messages.NO_TEXT_FOUND)) problems.add(b + " title");
			if (d == null || d.isEmpty() || d.contains(Messages.NO_TEXT_FOUND)) problems.add(b + " desc");
			if (b.image < 0) problems.add(b + " image");
		}
		assertTrue(problems.toString(), problems.isEmpty());
	}
}
