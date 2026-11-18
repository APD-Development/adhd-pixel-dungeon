Confirmed known-empty filter chooser gap, locally fixed; not committed/delivered yet.

UimsdModel.choicesFor discarded known empty strings, while WndUimsdFilter's typed single-value route refused blank input. The engine correctly distinguishes a known empty value from an unrecorded value. Fact actor columns explicitly use empty for the hero, so those values need an intentional selectable route.

Fix offers known empty text as '(empty)', retaining its original empty comparison value. Unknown cells and hidden information remain excluded by the existing guards. No record, formula, schema, operator or limit changed.

Two new permanent model/real-query/saved-view cases failed old source0.036s (known-empty choice absent), then passed0.112s. Newly compiled new cases plus existing UimsdModelTest,SavedViewsTest,QueryEngineTest:53unique passes1.410s. Corrected an initial local fixture that named nonexistent SavedViews APIs; used its actual save/store/list/query methods before meaningful baseline checks. Existing empty-string codec fix fromTEST90 remains unchanged and is required for persistence.

Android screen fixture strengthened to select '(empty)' by actual pointer events and verify only the known-empty row matches. Full Android/build pending. Original positive/negative threshold settings and game save data unchanged. Whole UIMSD audit unfinished.
