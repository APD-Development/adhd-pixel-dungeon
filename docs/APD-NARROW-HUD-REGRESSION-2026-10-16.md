# Narrow-toolbar message visibility — October 16, 2026

## Confirmed visual regression and correction
Android16 TEST253 capture hotbar-controls.png from run38946476567 shows the raised quickslot page switch covering part of the dungeon message log. The page control itself now works and the72control-layout cases passed, but the message baseline still used the bottom toolbar edge and did not reserve the raised control's height. This manual visual finding rejects TEST253 for owner delivery; passing control bounds alone is insufficient.

TEST254 adds Toolbar.messageTop(), reserving only the height of a visible raised pager. GameScene positions messages two logical pixels above that boundary and refreshes that layout when toolbar preferences change. Existing wide layouts and normal classic controls retain their original baseline. The original button graphics, sizes, page behavior, HUD tags, gameplay, RNG, scoring, inventory and saves are unchanged.

## Regression coverage and verification boundary
The existing72native width/layout/side/page scenarios now assert both the message anchor and the bottoms of actual rendered message entries remain above the pager. The changed production classes compile locally. Required TEST254 gate:1736unique core methods, debug assembly, exact preceding actually produced TEST253 history,36API34methods and49valid reviewed captures; further Android16 and six-class acceptance must reuse the same new254APK. No254verification or publication is claimed at this source checkpoint.

## Retained unsuccessful preparation
TEST253's original producer/core/debug/history succeeded, but its first native run passed35of36: test setup left an empty-hotbar message modal over the HUD, blocking the new tap check. Corrected cleanup closes only newly created test-owned windows. An isolated-method rerun then passed31of36 on Android14; five existing fixtures read preferences/platform before launching an Activity, so cold processes violated their setup assumptions. Android16 passed7of8, with one live-drag input assertion not observed. Restore ordinary complete-suite process semantics and sample the realized camera, acknowledge actual DOWN delivery, and retain the before-UP live multiplier assertion. No gameplay code or coefficient was changed to accommodate tests; all results remain documented separately.

The earlier TEST252 Android16 setup failed for insufficient runner disk (7036.54MB available versus7372.80MB required). A diagnostic heredoc script also failed before running tests and was corrected to a script file. Source252isolated acceptance passed6of7 and identified the original2.5pixel Inventory overlap;253corrected that control placement but exposed the new message overlap during visual review.

## Delivery safeguards and remaining work
Preserve the published TEST250 compatible APK and all older assets. Never deliver rejected253as a fully verified fix. Source254 must pass actual build/native/visual/signature/applicationID/checksum checks before private owner publication. Physical-phone install-over/current-save continuity, physical touch and full human-played boss strategies remain unverified; no phone is connected. UIMSD's broader audit, original armor playthrough and upstream audio delay remain open. No public release, automatic tester distribution, paid service, unfinished online activation or player-data clearing.
