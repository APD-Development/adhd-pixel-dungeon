package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * APD: gemstone dust, the ring version of arcane resin.
 * Alchemy turns a known-uncursed ring into dust; dust raises an identified, uncursed ring by one level up to +3.
 */
public class RingDust extends Item {

	{
		image = ItemSpriteSheet.ARCANE_RESIN;
		stackable = true;
		defaultAction = AC_APPLY;
		bones = true;
	}

	private static final String AC_APPLY = "APPLY";
	private static final ItemSprite.Glowing SPARKLE = new ItemSprite.Glowing(0x66CCFF);

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_APPLY);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_APPLY)) {
			curUser = hero;
			GameScene.selectItem(itemSelector);
		}
	}

	@Override public ItemSprite.Glowing glowing() { return SPARKLE; }
	@Override public boolean isUpgradable() { return false; }
	@Override public boolean isIdentified() { return true; }
	@Override public int value() { return 30 * quantity(); }

	@Override
	public String desc() {
		StringBuilder costs = new StringBuilder();
		for (int level = 0; level < applicationLevelLimit(); level++) {
			if (level > 0) costs.append(level == applicationLevelLimit() - 1 ? " or " : ", ");
			costs.append(applicationCost(level));
		}
		return Messages.get(this, "desc", applicationLevelLimit(), costs.toString());
	}

	public static int applicationLevelLimit() { return 3; }
	public static int applicationCost(int currentLevel) { return Math.max(1, currentLevel + 1); }

	/** Whether dust can be applied to this ring at all (ignores how much dust you hold). */
	public static boolean canApply(Item item) {
		return item instanceof Ring && item.isIdentified() && !item.cursed && item.level() < applicationLevelLimit();
	}

	/** Applies dust from this stack to the ring. Returns false when nothing changed. */
	public boolean applyTo(Ring ring) {
		if (!canApply(ring)) return false;
		int cost = applicationCost(ring.level());
		if (quantity() < cost) return false;
		if (cost < quantity()) quantity(quantity() - cost);
		else if (Dungeon.hero != null) detachAll(Dungeon.hero.belongings.backpack);
		else quantity(0);
		ring.upgrade();
		Item.updateQuickslot();
		return true;
	}

	private final WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {
		@Override public String textPrompt() { return Messages.get(RingDust.class, "prompt"); }
		@Override public boolean itemSelectable(Item item) { return item instanceof Ring && item.isIdentified() && !item.cursed; }
		@Override
		public void onSelect(Item item) {
			if (!(item instanceof Ring)) return;
			Ring ring = (Ring) item;
			if (ring.level() >= applicationLevelLimit()) { GLog.w(Messages.get(RingDust.class, "level_too_high")); return; }
			if (quantity() < applicationCost(ring.level())) { GLog.w(Messages.get(RingDust.class, "not_enough")); return; }
			if (!applyTo(ring)) return;
			curUser.sprite.operate(curUser.pos);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			curUser.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
			curUser.spendAndNext(Actor.TICK);
			GLog.p(Messages.get(RingDust.class, "apply"));
		}
	};

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe {

		public static int energyCost() { return 5; }

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			return ingredients.size() == 1
					&& ingredients.get(0) instanceof Ring
					&& ingredients.get(0).cursedKnown
					&& !ingredients.get(0).cursed;
		}

		@Override public int cost(ArrayList<Item> ingredients) { return energyCost(); }

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			Ring r = (Ring) ingredients.get(0);
			Item result = new RingDust().quantity(dustQuantity(r.level()));
			r.quantity(0);
			return result;
		}

		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			Ring r = (Ring) ingredients.get(0);
			return r.levelKnown ? new RingDust().quantity(dustQuantity(r.level())) : new RingDust();
		}

		/** Same yield rule as arcane resin: 2 x (level + 1), at least 2. */
		public static int dustQuantity(int ringLevel) { return 2 * Math.max(1, ringLevel + 1); }
	}
}
