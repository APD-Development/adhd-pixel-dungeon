package com.shatteredpixel.shatteredpixeldungeon.ui.changelist;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;

import java.util.ArrayList;

/** APD tester-build change history, SPD style. Content lives in assets/apd/changes.tsv. */
public class ApdChanges {

	public static final String ASSET = "apd/changes.tsv";

	public static String read(){
		try { return Gdx.files.internal(ASSET).readString("UTF-8"); } catch (Exception e){ return ""; }
	}

	/** Tab labels in file order. */
	public static ArrayList<String> tabs(String text){
		ArrayList<String> out = new ArrayList<>();
		for (String line : text.split("\n")){
			String[] f = line.replace("\r","").split("\t", -1);
			if (f.length >= 2 && f[0].equals("TAB")) out.add(f[1]);
		}
		return out;
	}

	public static void addTab(ArrayList<ChangeInfo> infos, String text, int tab){
		int current = -1;
		ChangeInfo info = null;
		for (String raw : text.split("\n")){
			String line = raw.replace("\r","");
			if (line.isEmpty() || line.startsWith("#")) continue;
			String[] f = line.split("\t", -1);
			if (f[0].equals("TAB")) { current++; continue; }
			if (current != tab) continue;
			if (f[0].equals("MAJOR") && f.length >= 2){
				info = new ChangeInfo(f[1], true, "");
				info.hardlight(f[1].startsWith("Coming") ? 0xCCCCCC : Window.TITLE_COLOR);
				infos.add(info);
			} else if (f[0].equals("BUILD") && f.length >= 2){
				info = new ChangeInfo(f[1], false, null);
				info.hardlight(Window.TITLE_COLOR);
				infos.add(info);
			} else if (f[0].equals("BTN") && f.length >= 4 && info != null){
				info.addButton(new ChangeButton(icon(f[1]), f[2], f[3].replace("\\n", "\n")));
			}
		}
	}

	static Image icon(String spec){
		try {
			if (spec.startsWith("i:")) return Icons.get(Icons.valueOf(spec.substring(2)));
			if (spec.startsWith("c:")) return ChangeIcons.valueOf(spec.substring(2)).get();
			if (spec.startsWith("s:")) return new ItemSprite(sprite(spec.substring(2)));
		} catch (Exception ignored){ }
		return Icons.get(Icons.INFO);
	}

	//explicit map: the sprite constants are inlined at compile time, so reflection can't be trusted after shrinking
	static int sprite(String name){
		switch (name){
			case "KATANA": return ItemSpriteSheet.KATANA;
			case "EXOTIC_KAUNAN": return ItemSpriteSheet.EXOTIC_KAUNAN;
			case "ARTIFACT_CLOAK": return ItemSpriteSheet.ARTIFACT_CLOAK;
			case "RING_SAPPHIRE": return ItemSpriteSheet.RING_SAPPHIRE;
			case "POTION_CRIMSON": return ItemSpriteSheet.POTION_CRIMSON;
			case "SEED_SUNGRASS": return ItemSpriteSheet.SEED_SUNGRASS;
			case "WATERSKIN": return ItemSpriteSheet.WATERSKIN;
			case "WAND_MAGIC_MISSILE": return ItemSpriteSheet.WAND_MAGIC_MISSILE;
			case "ARMOR_LEATHER": return ItemSpriteSheet.ARMOR_LEATHER;
			case "SWORD": return ItemSpriteSheet.SWORD;
			case "POTION_AMBER": return ItemSpriteSheet.POTION_AMBER;
			case "SCROLL_KAUNAN": return ItemSpriteSheet.SCROLL_KAUNAN;
			case "RING_RUBY": return ItemSpriteSheet.RING_RUBY;
			case "ARTIFACT_HOURGLASS": return ItemSpriteSheet.ARTIFACT_HOURGLASS;
			case "SEED_ROTBERRY": return ItemSpriteSheet.SEED_ROTBERRY;
			case "BOMB": return ItemSpriteSheet.BOMB;
			case "RATION": return ItemSpriteSheet.RATION;
			case "DART": return ItemSpriteSheet.DART;
			case "SPIRIT_BOW": return ItemSpriteSheet.SPIRIT_BOW;
			case "GOLD": return ItemSpriteSheet.GOLD;
			default: throw new IllegalArgumentException(name);
		}
	}
}
