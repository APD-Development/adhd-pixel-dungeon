# APD first launch goes to the title menu

## Stock behaviour (v4.0.0)
A brand-new install shows `WelcomeScene` (story intro, SPD logo, "Enter the Dungeon"), then goes
straight to `HeroSelectScene`. The title menu only appeared after the first finished run, because the
`intro` setting stays true until the first ranking exists. During that time hero select has no back
button, so Rankings, News, Changes, Settings and About were unreachable.

## APD change (`WelcomeScene.java`)
- Fresh install (`previousVersion == 0`): record the current version and go to `TitleScene` immediately.
- Any later launch of the same version also goes to `TitleScene`, even while `intro` is still true
  (stock would have shown the intro again every launch).
- The `intro` setting itself is untouched, so the first-run behaviour it controls elsewhere (e.g. hero
  select options hidden, no hunger during the intro) is unchanged. Hero select still has no back button
  until `intro` clears.
- The intro screen still appears after an app update (previous version != current), and now uses the
  plain APD text title instead of the SPD logo, torches and glow.
- No save-game (Bundle) changes. Only the existing `version` setting is written on first launch.
