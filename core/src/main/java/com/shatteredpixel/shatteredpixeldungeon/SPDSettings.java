/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
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

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.GameSettings;
import com.watabou.utils.Point;

import java.util.Locale;

public class SPDSettings extends GameSettings {
	
	//Version info
	
	public static final String KEY_VERSION      = "version";
	
	public static void version( int value)  {
		put( KEY_VERSION, value );
	}
	
	public static int version() {
		return getInt( KEY_VERSION, 0 );
	}
	
	//Display
	
	public static final String KEY_FULLSCREEN	= "fullscreen"; //used to hide navbars on mobile
	public static final String KEY_LANDSCAPE	= "force_landscape";
	public static final String KEY_ZOOM			= "zoom";
	public static final String KEY_BRIGHTNESS	= "brightness";
	public static final String KEY_GRID 	    = "visual_grid";
	public static final String KEY_CAMERA_FOLLOW= "camera_follow";
	public static final String KEY_SCREEN_SHAKE = "screen_shake";
	
	public static void fullscreen( boolean value ) {
		put( KEY_FULLSCREEN, value );
		
		ShatteredPixelDungeon.updateSystemUI();
	}
	
	public static boolean fullscreen() {
		return getBoolean( KEY_FULLSCREEN, true );
	}

	public static void landscape( boolean value ){
		put( KEY_LANDSCAPE, value );
		((ShatteredPixelDungeon)ShatteredPixelDungeon.instance).updateDisplaySize();
	}

	public static boolean landscape(){
		return getBoolean(KEY_LANDSCAPE, false);
	}
	
	public static void zoom( int value ) {
		put( KEY_ZOOM, value );
	}
	
	public static int zoom() {
		return getInt( KEY_ZOOM, 0 );
	}
	
	public static void brightness( int value ) {
		put( KEY_BRIGHTNESS, value );
		GameScene.updateFog();
	}
	
	public static int brightness() {
		return getInt( KEY_BRIGHTNESS, 0, -1, 1 );
	}
	
	public static void visualGrid( int value ){
		put( KEY_GRID, value );
		GameScene.updateMap();
	}
	
	public static int visualGrid() {
		return getInt( KEY_GRID, 0, -1, 2 );
	}

    public static boolean ratKingQuickSearch(){return getBoolean("apd_rat_king_quick_search",false);}
    public static void ratKingQuickSearch(boolean enabled){put("apd_rat_king_quick_search",enabled);}
    public static int searchedWallColor(){
        return com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallAppearance.normalize(getInt("apd_searched_wall_color",0));
    }
    public static void searchedWallColor(int index){
        put("apd_searched_wall_color",com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallAppearance.normalize(index));
        GameScene.updateSearchedWallAppearance();
    }

    public static boolean enhancedTileVisibility() { return getBoolean("apd_enhanced_tile_visibility", false); }
    public static void enhancedTileVisibility(boolean value) { put("apd_enhanced_tile_visibility", value); GameScene.updateMap(); }
    public static boolean stairNavigationShortcut() { return getBoolean("apd_stair_navigation_shortcut",false); }
    public static void stairNavigationShortcut(boolean value) { put("apd_stair_navigation_shortcut", value); GameScene.updateTags=true; }
    // Opt-in: the stair shortcut walks normally, then uses that direction's stairs.
    // A manual map tap or ordinary movement never triggers this preference.
    public static boolean autoEnterStairs() { return getBoolean("apd_auto_enter_stairs", false); }
    public static void autoEnterStairs(boolean value) { put("apd_auto_enter_stairs", value); }

	public static void cameraFollow( int value ){
		put( KEY_CAMERA_FOLLOW, value );
	}

	public static int cameraFollow() {
		return getInt( KEY_CAMERA_FOLLOW, 4, 1, 4 );
	}

	public static void screenShake( int value ){
		put( KEY_SCREEN_SHAKE, value );
	}

	public static int screenShake() {
		return getInt( KEY_SCREEN_SHAKE, 2, 0, 4 );
	}
	
	//Interface

	public static final String KEY_UI_SIZE 	    = "full_ui";
	public static final String KEY_SCALE		= "scale";
	public static final String KEY_QUICK_SWAP	= "quickslot_swapper";
	public static final String KEY_FLIPTOOLBAR	= "flipped_ui";
	public static final String KEY_FLIPTAGS 	= "flip_tags";
	public static final String KEY_BARMODE		= "toolbar_mode";
	public static final String KEY_SLOTWATERSKIN= "quickslot_waterskin";
	public static final String KEY_SYSTEMFONT	= "system_font";
	public static final String KEY_VIBRATION    = "vibration";

	public static final String KEY_GAMES_SORT    = "games_sort";

	//0 = mobile, 1 = mixed (large without inventory in main UI), 2 = large
	public static void interfaceSize( int value ){
		put( KEY_UI_SIZE, value );
	}

	public static int interfaceSize(){
		int size = getInt( KEY_UI_SIZE, DeviceCompat.isDesktop() ? 2 : 0 );
		if (size > 0){
			//force mobile UI if there is not enough space for full UI
			float wMin = Game.width / PixelScene.MIN_WIDTH_FULL;
			float hMin = Game.height / PixelScene.MIN_HEIGHT_FULL;
			if (Math.min(wMin, hMin) < 2*Game.density){
				size = 0;
			}
		}
		return size;
	}

	public static void scale( int value ) {
		put( KEY_SCALE, value );
	}

	public static int scale() {
		return getInt( KEY_SCALE, 0 );
	}
	
	public static void quickSwapper(boolean value ){ put( KEY_QUICK_SWAP, value ); }
	
	public static boolean quickSwapper(){ return getBoolean( KEY_QUICK_SWAP, true); }
	
	public static void flipToolbar( boolean value) {
		put(KEY_FLIPTOOLBAR, value );
	}
	
	public static boolean flipToolbar(){ return getBoolean(KEY_FLIPTOOLBAR, false); }
	
	public static void flipTags( boolean value) {
		put(KEY_FLIPTAGS, value );
	}
	
	public static boolean flipTags(){ return getBoolean(KEY_FLIPTAGS, false); }
	
	public static void toolbarMode( String value ) {
		put( KEY_BARMODE, value );
	}
	
	public static String toolbarMode() {
		return getString(KEY_BARMODE, PixelScene.landscape() ? "GROUP" : "SPLIT");
	}

	public static void quickslotWaterskin( boolean value ){
		put( KEY_SLOTWATERSKIN, value);
	}

	public static boolean quickslotWaterskin(){
		return getBoolean( KEY_SLOTWATERSKIN, true );
	}

	// -1: legacy first-empty; 0..5: slots 1-6; 6: Off. Default slot 2.
	// 6: never auto-assign. Separate from the legacy yes/no preference so
	// updating an older installation does not silently move its Waterskin.
	public static final String KEY_WATERSKIN_START_SLOT = "apd_waterskin_start_slot";
	public static int waterskinStartingSlot() {
		return getInt(KEY_WATERSKIN_START_SLOT, 1, -1, 6);
	}
	public static void waterskinStartingSlot(int position) {
		if (position < -1 || position > 6) throw new IllegalArgumentException("waterskin starting slot");
		put(KEY_WATERSKIN_START_SLOT, position);
	}

	public static void systemFont(boolean value){
		put(KEY_SYSTEMFONT, value);
	}

	public static boolean systemFont(){
		return getBoolean(KEY_SYSTEMFONT,
				(language() == Languages.CHI_SMPL || language() == Languages.CHI_TRAD
						|| language() == Languages.KOREAN || language() == Languages.JAPANESE));
	}

	public static void vibration(boolean value){
		put(KEY_VIBRATION, value);
	}

	public static boolean vibration(){
		return getBoolean(KEY_VIBRATION, true);
	}

	public static String gamesInProgressSort(){
		return getString(KEY_GAMES_SORT, "level");
	}

	public static void gamesInProgressSort(String value){
		put(KEY_GAMES_SORT, value);
	}

	//Game State
	
	public static final String KEY_LAST_CLASS	= "last_class";
	public static final String KEY_CHALLENGES	= "challenges";
	public static final String KEY_CUSTOM_SEED	= "custom_seed";
	public static final String KEY_LAST_DAILY	= "last_daily";
	public static final String KEY_INTRO		= "intro";

	public static final String KEY_SUPPORT_NAGGED= "support_nagged";
	public static final String KEY_VICTORY_NAGGED= "victory_nagged";
	public static final String KEY_VAULT_INJURE_WARNS = "vault_injure_warns";
	
	public static void intro( boolean value ) {
		put( KEY_INTRO, value );
	}
	
	public static boolean intro() {
		//APD: a floor replay uses the reference context, not this install's tutorial flag
		if (com.shatteredpixel.shatteredpixeldungeon.apd.replay.ReplayContext.active()) return com.shatteredpixel.shatteredpixeldungeon.apd.replay.ReplayContext.TUTORIAL;
		return getBoolean( KEY_INTRO, true );
	}
	
	public static void lastClass( int value ) {
		put( KEY_LAST_CLASS, value );
	}
	
	public static int lastClass() {
		return getInt( KEY_LAST_CLASS, 0, 0, 3 );
	}
	
	public static void challenges( int value ) {
		put( KEY_CHALLENGES, value );
	}
	
	public static int challenges() {
		return getInt( KEY_CHALLENGES, 0, 0, Challenges.MAX_VALUE );
	}

	public static void customSeed( String value ){
		put( KEY_CUSTOM_SEED, value );
	}

	public static String customSeed() {
		return getString( KEY_CUSTOM_SEED, "", 20);
	}

	public static void lastDaily( long value ){
		put( KEY_LAST_DAILY, value );
	}

	public static long lastDaily() {
		return getLong( KEY_LAST_DAILY, 0);
	}

	public static void supportNagged( boolean value ) {
		put( KEY_SUPPORT_NAGGED, value );
	}

	public static boolean supportNagged() {
		return getBoolean(KEY_SUPPORT_NAGGED, false);
	}

	public static void victoryNagged( boolean value ) {
		put( KEY_VICTORY_NAGGED, value );
	}

	public static boolean victoryNagged() {
		return getBoolean(KEY_VICTORY_NAGGED, false);
	}

	public static void vaultInjureWarns( int value ) {
		put( KEY_VAULT_INJURE_WARNS, value );
	}

	public static int vaultInjureWarns() {
		return getInt(KEY_VAULT_INJURE_WARNS, 0);
	}

	//APD

    // -1 preserves normal one-tap drinking; 0..100 are inclusive HP thresholds.
    public static int healingConfirmationThreshold() { return getInt("apd_healing_confirmation", -1, -1, 100); }
    public static void healingConfirmationThreshold(int value) { put("apd_healing_confirmation", Math.max(-1, Math.min(100, value))); }

	public static final String KEY_APD_EXOTIC_CHANCE    = "apd_exotic_chance";
	public static final String KEY_APD_CLERIC_ATTEMPTS  = "apd_cleric_attempts";

	public static final int APD_EXOTIC_CHANCE_MAX = 80;
	//APD: difficulty 1-5 for NEW runs (3 = the game as it is); an existing run keeps the difficulty it started with
	public static final String KEY_APD_DIFFICULTY       = "apd_difficulty";

	//APD: the UIMSD tour has been offered once (taken or declined); the Guide stays available whatever this says
	public static final String KEY_APD_UIMSD_GUIDE_SEEN = "apd_uimsd_guide_seen";

	public static void apdUimsdGuideSeen( boolean value ){
		put( KEY_APD_UIMSD_GUIDE_SEEN, value );
	}

	public static boolean apdUimsdGuideSeen(){
		return getBoolean( KEY_APD_UIMSD_GUIDE_SEEN, false );
	}

	//UIMSD Demo Unlock: both flags are permanent (nothing ever sets them back), so deleting runs or demo data cannot lock UIMSD again or bring the demo button back
	public static final String KEY_APD_UIMSD_UNLOCKED = "apd_uimsd_unlocked";
	public static final String KEY_APD_DEMO_UNLOCK_USED = "apd_demo_unlock_used";

	public static boolean apdUimsdUnlocked(){
		return getBoolean( KEY_APD_UIMSD_UNLOCKED, false );
	}

	public static void apdUimsdUnlocked( boolean value ){
		put( KEY_APD_UIMSD_UNLOCKED, value );
	}

	//the guided tour opens only after ten GENUINE runs; the demo route never sets this
	public static final String KEY_APD_UIMSD_TOUR_UNLOCKED = "apd_uimsd_tour_unlocked";

	public static boolean apdUimsdTourUnlocked(){
		return getBoolean( KEY_APD_UIMSD_TOUR_UNLOCKED, false );
	}

	public static void apdUimsdTourUnlocked( boolean value ){
		put( KEY_APD_UIMSD_TOUR_UNLOCKED, value );
	}

	//the one-time "go to UIMSD now" prompt after the tenth genuine run: waiting to be shown, and already shown
	public static final String KEY_APD_TENTH_PENDING = "apd_tenth_run_prompt_pending";
	public static final String KEY_APD_TENTH_SHOWN = "apd_tenth_run_prompt_shown";

	public static boolean apdTenthPending(){
		return getBoolean( KEY_APD_TENTH_PENDING, false );
	}

	public static void apdTenthPending( boolean value ){
		put( KEY_APD_TENTH_PENDING, value );
	}

	public static boolean apdTenthShown(){
		return getBoolean( KEY_APD_TENTH_SHOWN, false );
	}

	public static void apdTenthShown( boolean value ){
		put( KEY_APD_TENTH_SHOWN, value );
	}

	//Community (Nov 18): the server address (empty until the server is live), the signed-in session, and the 16+ gate's
	//single pass/fail answer. The birth date itself is never stored anywhere.
	public static final String COMMUNITY_SERVER_DEFAULT = "https://apd-community.charltonchris29.workers.dev";
	//Google sign-in: the OAuth "web" client id the Community server accepts (set once Chris creates it). Empty = no Google button.
	public static final String COMMUNITY_GOOGLE_CLIENT_ID = "1075731906356-tr7miehsnl77ld9ru05h3ijv84cd60jb.apps.googleusercontent.com";
	public static String communityServer(){ return getString("apd_community_server", COMMUNITY_SERVER_DEFAULT, 200); }
	public static void communityServer(String url){ put("apd_community_server", url == null ? "" : url); }
	public static String communityToken(){ String t = getString("apd_community_token", "", 200); return t.isEmpty() ? null : t; }
	public static void communityToken(String token){ put("apd_community_token", token == null ? "" : token); }
	public static String communityAgeGate(){ String v = getString("apd_community_age", "", 8); return v.isEmpty() ? null : v; }
	public static void communityAgeGate(String passOrFail){ put("apd_community_age", passOrFail == null ? "" : passOrFail); }

	//Nov 18: every APD setting now starts off for NEW players. Players who already had the game keep what they had:
	//the settings that used to default to on are saved as on for them, unless they already saved a choice.
	static final String[] OLD_ON_BY_DEFAULT = {
		"apd_stair_navigation_shortcut", "apd_adaptive_search_choice", "apd_dot_overlay",
		"apd_auto_grass_dew", "apd_confirm_waterskin", "apd_confirm_known_traps" };
	static final String DEFAULTS_MIGRATED = "apd_defaults_v2_kept";

	//Runs once per install, before any screen reads settings. version() is the last build that ran on this phone
	//(0 on a fresh install). Returns how many settings were kept on.
	public static int keepOldDefaultsForExistingPlayers(){
		if (contains(DEFAULTS_MIGRATED)) return 0;
		int kept = 0;
		if (version() > 0){
			for (String key : OLD_ON_BY_DEFAULT){
				if (!contains(key)){ put(key, true); kept++; }
			}
		}
		put(DEFAULTS_MIGRATED, true);
		return kept;
	}

	//Every APD setting's saved value ("unset" = nothing saved, the default applies), for bug reports.
	public static String apdSettingsReport(){
		StringBuilder b = new StringBuilder();
		String[] keys = { "apd_stair_navigation_shortcut", "apd_auto_enter_stairs", "apd_adaptive_search_choice",
			"apd_dot_overlay", "apd_auto_grass_dew", "apd_auto_dew_with_enemies", "apd_confirm_waterskin",
			"apd_confirm_known_traps", "apd_single_tap_search", "apd_continue_trap_discovery", "apd_rat_king_quick_search",
			"apd_trophy_outlines", "apd_enhanced_tile_visibility", "apd_wait_stop_full", "apd_wait_stop_starving",
			"apd_wait_stop_critical", KEY_APD_ADULT_HUMOR, KEY_APD_RECORDING, DEFAULTS_MIGRATED };
		for (String k : keys){
			b.append(k).append('=').append(contains(k) ? String.valueOf(getBoolean(k, false)) : "unset").append('\n');
		}
		return b.toString();
	}

	//One shared preference. Everyone defaults to Clean; Adult is opt-in.
	public static final String KEY_APD_ADULT_HUMOR = "apd_adult_humor";

	public static boolean apdAdultHumor(){
		com.shatteredpixel.shatteredpixeldungeon.apd.age.AgeEligibility.State age =
				com.shatteredpixel.shatteredpixeldungeon.apd.age.AgeEligibility.current();
		return com.shatteredpixel.shatteredpixeldungeon.apd.age.AgeEligibility.adultHumor(
				age, getBoolean( KEY_APD_ADULT_HUMOR,
						com.shatteredpixel.shatteredpixeldungeon.apd.age.AgeEligibility.defaultAdultHumor(age) ));
	}

	public static void apdAdultHumor( boolean value ){
		if (!value || com.shatteredpixel.shatteredpixeldungeon.apd.age.AgeEligibility.permitsAdult(
				com.shatteredpixel.shatteredpixeldungeon.apd.age.AgeEligibility.current())){
			put( KEY_APD_ADULT_HUMOR, value );
		}
	}

	//The Cleric grudge's preferences (frequency and the five location switches) are kept as small text values under keys of the grudge's own
	//choosing (see GrudgePrefs); "" when a value was never set.
	public static String apdGrudgePref( String key ){
		return getString( key, "", 20 );
	}

	public static void apdGrudgePref( String key, String value ){
		put( key, value );
	}

	public static boolean apdDemoUnlockUsed(){
		return getBoolean( KEY_APD_DEMO_UNLOCK_USED, false );
	}

	public static void apdDemoUnlockUsed( boolean value ){
		put( KEY_APD_DEMO_UNLOCK_USED, value );
	}

	public static final String KEY_APD_SENDING = "apd_sending";
	public static final String KEY_APD_SERVER = "apd_server";

	//Sending queued runs to a server: off unless the player turns it on, and it needs a server address too
	public static void apdSending( boolean value ){
		put( KEY_APD_SENDING, value );
	}

	public static boolean apdSending(){
		return getBoolean( KEY_APD_SENDING, false );
	}

	public static void apdServer( String value ){
		put( KEY_APD_SERVER, value == null ? "" : value );
	}

	public static String apdServer(){
		return getString( KEY_APD_SERVER, "", 200 );
	}


    //APD hotbar preferences are separate from the original per-run QuickSlot placements.
    public static int apdClericQuickSlots() { return getInt("apd_cleric_quick_slots", 1, 1, 4); }
    public static void apdClericQuickSlots(int value) { put("apd_cleric_quick_slots", Math.max(1, Math.min(4, value))); }
    public static String apdClericQuickSpell(int slot) {
        if (slot < 0 || slot >= 4) throw new IllegalArgumentException("Cleric quick slot");
        return getString("apd_cleric_quick_spell_" + slot, "");
    }
    public static void apdClericQuickSpell(int slot, String value) {
        if (slot < 0 || slot >= 4) throw new IllegalArgumentException("Cleric quick slot");
        put("apd_cleric_quick_spell_" + slot, value == null ? "" : value);
    }
    public static int apdHotbarMode() { return getInt("apd_hotbar_mode", 0, 0, 4); }
    public static void apdHotbarMode(int value) { put("apd_hotbar_mode", Math.max(0, Math.min(4, value))); }
    public static int apdHotbarCategory(int slot) { return getInt("apd_hotbar_category_" + slot, slot, 0, 8); }
    public static void apdHotbarCategory(int slot, int value) { put("apd_hotbar_category_" + slot, Math.max(0, Math.min(8, value))); }
    public static String apdHotbarItem(int slot) { return getString("apd_hotbar_item_" + slot, ""); }
    public static void apdHotbarItem(int slot, String value) { put("apd_hotbar_item_" + slot, value == null ? "" : value); }
    public static boolean apdFoodInHealing() { return getBoolean("apd_food_in_healing", false); }
    public static void apdFoodInHealing(boolean value) { put("apd_food_in_healing", value); }

	public static final String KEY_APD_RECORDING = "apd_recording";

	//APD run history recording, on by default
	public static void apdRecording( boolean value ){
		put( KEY_APD_RECORDING, value );
	}

	public static boolean apdRecording(){
		return getBoolean( KEY_APD_RECORDING, true );
	}

	//percent chance (0-80) that a generated potion/scroll is exotic, 0 is the vanilla behaviour
	public static void apdExoticChance( int value ){
		put( KEY_APD_EXOTIC_CHANCE, value );
	}


    // Next-run preferences only. The active run owns an immutable snapshot.
    public static com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules apdCustomRules() {
        String text = getString("apd_custom_rules_v1", "");
        if (text.isEmpty()) return com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults();
        return com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.fromPreferenceText(text);
    }

    public static void apdCustomRules(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules rules) {
        if (rules == null) throw new IllegalArgumentException("Missing next-run custom rules");
        put("apd_custom_rules_v1", rules.preferenceText());
    }

    // Six player-owned configuration slots, pre-labelled by class. No game save slots are used.
    public static final int APD_CUSTOM_RULE_SLOT_COUNT = 6;
    private static final String[] APD_CUSTOM_RULE_SLOT_DEFAULT_NAMES = {"Warrior","Mage","Rogue","Huntress","Duelist","Cleric"};
    public static String apdCustomRuleSlotName(int slot){
        if(slot<1||slot>APD_CUSTOM_RULE_SLOT_COUNT)throw new IllegalArgumentException("Preset slot out of range");
        String saved=getString("apd_custom_rules_slot_name_"+slot,"");
        return saved.isEmpty()?APD_CUSTOM_RULE_SLOT_DEFAULT_NAMES[slot-1]:saved;
    }
    public static void apdCustomRuleSlotName(int slot,String name){
        if(slot<1||slot>APD_CUSTOM_RULE_SLOT_COUNT)throw new IllegalArgumentException("Preset slot out of range");
        if(name==null)throw new IllegalArgumentException("Missing name");
        String clean=name.trim();
        if(clean.length()>24)throw new IllegalArgumentException("Name too long");
        // An empty name restores the built-in class label.
        put("apd_custom_rules_slot_name_"+slot,clean);
    }
    public static boolean apdCustomRuleSlotOccupied(int slot){
        if(slot<1||slot>APD_CUSTOM_RULE_SLOT_COUNT)throw new IllegalArgumentException("Preset slot out of range");
        return !getString("apd_custom_rules_slot_"+slot,"").isEmpty();
    }
    public static com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules apdCustomRuleSlot(int slot){
        if(slot<1||slot>APD_CUSTOM_RULE_SLOT_COUNT)throw new IllegalArgumentException("Preset slot out of range");
        String value=getString("apd_custom_rules_slot_"+slot,"");
        return value.isEmpty()?null:com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.fromPreferenceText(value);
    }
    public static void apdCustomRuleSlot(int slot,com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules rules){
        if(slot<1||slot>APD_CUSTOM_RULE_SLOT_COUNT)throw new IllegalArgumentException("Preset slot out of range");
        if(rules==null)throw new IllegalArgumentException("Missing saved slider configuration");
        put("apd_custom_rules_slot_"+slot,rules.preferenceText());
    }

    /** Private, locally stored consent; logging is disabled on every fresh install. */
    public static boolean apdExtendedBugLogging() {
        return getBoolean("apd_extended_bug_logging", false);
    }
    public static void apdExtendedBugLogging(boolean allowed) {
        put("apd_extended_bug_logging", allowed);
        com.watabou.utils.SupportActionJournal.setEnabled(allowed);
    }

	public static void apdDifficulty( int value ){
		put( KEY_APD_DIFFICULTY, com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.Difficulty.normalize( value ) );
	}

	public static int apdDifficulty(){
		return getInt( KEY_APD_DIFFICULTY, com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.Difficulty.DEFAULT,
				com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.Difficulty.MIN, com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.Difficulty.MAX );
	}

	//the player's setting: what the NEXT run will be started with
	public static int apdExoticSetting(){
		return getInt( KEY_APD_EXOTIC_CHANCE, 0, 0, APD_EXOTIC_CHANCE_MAX );
	}

	//the exotic chance in force for gameplay: a floor replay uses the run it replays; a run uses the one it was started with (never the setting as
	//it is now); only a run that never recorded one (saved by an older version) or no run at all falls back to the setting
	public static int apdExoticChance(){
		if (com.shatteredpixel.shatteredpixeldungeon.apd.replay.ReplayContext.active()) return com.shatteredpixel.shatteredpixeldungeon.apd.replay.ReplayContext.exoticChance();
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.apdExoticKnown) return com.shatteredpixel.shatteredpixeldungeon.Dungeon.apdExotic;
		return apdExoticSetting();
	}

	public static void apdClericAttempts( int value ){
		put( KEY_APD_CLERIC_ATTEMPTS, value );
	}

	public static int apdClericAttempts(){
		return getInt( KEY_APD_CLERIC_ATTEMPTS, 0, 0, Integer.MAX_VALUE );
	}

	//Input

	public static final String KEY_CONTROLLER_SENS  = "controller_sens";
	public static final String KEY_MOVE_SENS        = "move_sens";

	public static void controllerPointerSensitivity( int value ){
		put( KEY_CONTROLLER_SENS, value );
	}

	public static int controllerPointerSensitivity(){
		return getInt(KEY_CONTROLLER_SENS, 5, 1, 10);
	}

	public static void movementHoldSensitivity( int value ){
		put( KEY_MOVE_SENS, value );
	}

	public static int movementHoldSensitivity(){
		return getInt(KEY_MOVE_SENS, 3, 0, 4);
	}

	//Connectivity

	public static final String KEY_NEWS     = "news";
	public static final String KEY_UPDATES	= "updates";
	public static final String KEY_BETAS	= "betas";
	public static final String KEY_WIFI     = "wifi";

	public static final String KEY_NEWS_LAST_READ = "news_last_read";

	public static void news(boolean value){
		put(KEY_NEWS, value);
	}

	public static boolean newsDefault = true;

	public static boolean news(){
		return getBoolean(KEY_NEWS, newsDefault);
	}

	public static void updates(boolean value){
		put(KEY_UPDATES, value);
	}

	public static boolean updates(){
		return getBoolean(KEY_UPDATES, true);
	}

	public static void betas(boolean value){
		put(KEY_BETAS, value);
	}

	public static boolean betas(){
		return getBoolean(KEY_BETAS, Game.version.contains("BETA") || Game.version.contains("RC"));
	}

	public static void WiFi(boolean value){
		put(KEY_WIFI, value);
	}

	public static boolean WiFi(){
		return getBoolean(KEY_WIFI, true);
	}

	public static void newsLastRead(long lastRead){
		put(KEY_NEWS_LAST_READ, lastRead);
	}

	public static long newsLastRead(){
		return getLong(KEY_NEWS_LAST_READ, 0);
	}

	//Audio
	
	public static final String KEY_MUSIC		= "music";
	public static final String KEY_MUSIC_VOL    = "music_vol";
	public static final String KEY_SOUND_FX		= "soundfx";
	public static final String KEY_SFX_VOL      = "sfx_vol";
	public static final String KEY_IGNORE_SILENT= "ignore_silent";
	public static final String KEY_MUSIC_BG     = "music_bg";
	
	public static void music( boolean value ) {
		Music.INSTANCE.enable( value );
		put( KEY_MUSIC, value );
	}
	
	public static boolean music() {
		return getBoolean( KEY_MUSIC, true );
	}
	
	public static void musicVol( int value ){
		Music.INSTANCE.volume(value*value/100f);
		put( KEY_MUSIC_VOL, value );
	}
	
	public static int musicVol(){
		return getInt( KEY_MUSIC_VOL, 10, 0, 10 );
	}
	
	public static void soundFx( boolean value ) {
		Sample.INSTANCE.enable( value );
		put( KEY_SOUND_FX, value );
	}
	
	public static boolean soundFx() {
		return getBoolean( KEY_SOUND_FX, true );
	}
	
	public static void SFXVol( int value ) {
		Sample.INSTANCE.volume(value*value/100f);
		put( KEY_SFX_VOL, value );
	}
	
	public static int SFXVol() {
		return getInt( KEY_SFX_VOL, 10, 0, 10 );
	}

	public static void ignoreSilentMode( boolean value ){
		put( KEY_IGNORE_SILENT, value);
		Game.platform.setHonorSilentSwitch(!value);
	}

	public static boolean ignoreSilentMode(){
		return getBoolean( KEY_IGNORE_SILENT, false);
	}

	public static void playMusicInBackground( boolean value ){
		put( KEY_MUSIC_BG, value);
	}

	public static boolean playMusicInBackground(){
		return getBoolean( KEY_MUSIC_BG, true);
	}
	
	//Languages
	
	public static final String KEY_LANG         = "language";
	
	public static void language(Languages lang) {
		put( KEY_LANG, lang.code());
	}
	
	public static Languages language() {
		String code = getString(KEY_LANG, null);
		if (code == null){
			return Languages.matchLocale(Locale.getDefault());
		} else {
			return Languages.matchCode(code);
		}
	}

	//Window management (desktop only atm)
	
	public static final String KEY_WINDOW_WIDTH     = "window_width";
	public static final String KEY_WINDOW_HEIGHT    = "window_height";
	public static final String KEY_WINDOW_MAXIMIZED = "window_maximized";
	public static final String KEY_FULLSCREEN_MONITOR = "fullscreen_monitor";

	public static void windowResolution( Point p ){
		put(KEY_WINDOW_WIDTH, p.x);
		put(KEY_WINDOW_HEIGHT, p.y);
	}
	
	public static Point windowResolution(){
		return new Point(
				getInt( KEY_WINDOW_WIDTH, 800, 720, Integer.MAX_VALUE ),
				getInt( KEY_WINDOW_HEIGHT, 600, 400, Integer.MAX_VALUE )
		);
	}
	
	public static void windowMaximized( boolean value ){
		put( KEY_WINDOW_MAXIMIZED, value );
	}
	
	public static boolean windowMaximized(){
		return getBoolean( KEY_WINDOW_MAXIMIZED, false );
	}

	public static void fulLScreenMonitor( int value ){
		put( KEY_FULLSCREEN_MONITOR, value);
	}

	public static int fulLScreenMonitor(){
		return getInt( KEY_FULLSCREEN_MONITOR, 0 );
	}
    public static boolean waitStopFullHealth(){return getBoolean("apd_wait_stop_full",true);}
    public static void waitStopFullHealth(boolean value){put("apd_wait_stop_full",value);}
    public static boolean waitStopStarving(){return getBoolean("apd_wait_stop_starving",true);}
    public static void waitStopStarving(boolean value){put("apd_wait_stop_starving",value);}
    public static boolean waitStopCritical(){return getBoolean("apd_wait_stop_critical",false);}
    public static void waitStopCritical(boolean value){put("apd_wait_stop_critical",value);}
    public static int waitCriticalPercent(){int value=getInt("apd_wait_critical_percent",20);return value>=10&&value<=50&&value%10==0?value:20;}
    public static void waitCriticalPercent(int value){put("apd_wait_critical_percent",value>=10&&value<=50&&value%10==0?value:20);}
    public static boolean autoEatWhileWaiting(){return getBoolean("apd_wait_auto_eat",false);}
    public static void autoEatWhileWaiting(boolean value){put("apd_wait_auto_eat",value);}
    public static boolean autoEatWhenHungry(){return getBoolean("apd_wait_eat_hungry",false);}
    public static void autoEatWhenHungry(boolean value){put("apd_wait_eat_hungry",value);}
    public static boolean continueOnTrapDiscovery(){return getBoolean("apd_continue_trap_discovery",false);}
    public static void continueOnTrapDiscovery(boolean enabled){put("apd_continue_trap_discovery",enabled);}
    /** Chooser preference only: never performs a search or changes its range/cost. */
    public static boolean adaptiveSearchChoice(){return getBoolean("apd_adaptive_search_choice",false);}
    public static void adaptiveSearchChoice(boolean enabled){put("apd_adaptive_search_choice",enabled);}
    public static boolean singleTapSearch(){return getBoolean("apd_single_tap_search",false);}
    public static void singleTapSearch(boolean enabled){put("apd_single_tap_search",enabled);}
    public static int playbackSpeed() {
        int value = getInt("apd_playback_speed", 1);
        return com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup.allowed(value) ? value : 1;
    }
    public static void playbackSpeed(int value) {
        put("apd_playback_speed", com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup.allowed(value) ? value : 1);
    }
    //Trophy outlines on enemies in the dungeon (Chris, Nov 18: a player setting, OFF by default). The Bestiary always shows them.
    public static boolean trophyOutlines(){return getBoolean("apd_trophy_outlines",false);}
    public static void trophyOutlines(boolean enabled){put("apd_trophy_outlines",enabled);}
    public static boolean dotOverlayEnabled(){return getBoolean("apd_dot_overlay",false);}
    public static void dotOverlayEnabled(boolean enabled){put("apd_dot_overlay",enabled);}
    public static boolean autoCollectGrassDew(){return getBoolean("apd_auto_grass_dew",false);}
    public static void autoCollectGrassDew(boolean enabled){put("apd_auto_grass_dew",enabled);}
    public static boolean autoCollectDewWithEnemies(){return getBoolean("apd_auto_dew_with_enemies",false);}
    public static void autoCollectDewWithEnemies(boolean enabled){put("apd_auto_dew_with_enemies",enabled);}
    public static boolean confirmWaterskin(){return getBoolean("apd_confirm_waterskin",false);}
    public static void confirmWaterskin(boolean enabled){put("apd_confirm_waterskin",enabled);}
    public static boolean confirmKnownTraps(){return getBoolean("apd_confirm_known_traps",false);}
    public static void confirmKnownTraps(boolean enabled){put("apd_confirm_known_traps",enabled);}
}
