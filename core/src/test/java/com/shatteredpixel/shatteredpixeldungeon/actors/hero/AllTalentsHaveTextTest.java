package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Every talent has a title and a description, both normally and as a Metamorphosis pick. */
public class AllTalentsHaveTextTest {
	@Test public void everyTalentHasText(){
		HeadlessTestSupport.start();
		Hero old = Dungeon.hero;
		Dungeon.hero = new Hero();
		List<String> problems = new ArrayList<>();
		try {
			for (Talent t : Talent.values()){
				try {
					String title = t.title(), desc = t.desc(), meta = t.desc(true);
					if (title == null || title.isEmpty() || title.contains(Messages.NO_TEXT_FOUND)) problems.add(t + " title");
					if (desc == null || desc.isEmpty() || desc.contains(Messages.NO_TEXT_FOUND)) problems.add(t + " desc");
					if (meta == null || meta.isEmpty() || meta.contains(Messages.NO_TEXT_FOUND)) problems.add(t + " metamorph desc");
				} catch (Throwable th){
					problems.add(t + " threw " + th);
				}
			}
		} finally { Dungeon.hero = old; }
		assertTrue(problems.toString(), problems.isEmpty());
	}
}
