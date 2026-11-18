# TEST119 experimental custom-rules warning candidate

Replaces the earlier short scoring-only notice with the owner's exact lifetime experimental warning and separate scoring paragraph. EXPERIMENTAL FEATURE uses compact native heading styling. The full warning is first in the existing scroll area, above all controls; this avoids clipping narrow viewports or reserving so much fixed height that sliders become inaccessible. No acknowledgment, modal warning or extra action is required. The heading remains visible, and reopening starts at the warning.

Exact body:
Custom Dungeon Rules are experimental and will continue to evolve throughout APD's lifetime. Some or all difficulty sliders may have their minimum and maximum values adjusted in future updates as gameplay balance and player feedback are evaluated.

The scoring system for Custom Dungeon Rules is still being developed. These settings currently do not affect your earned score.

Lifetime warning and scoring paragraph are separate constants, so a later approved scoring implementation can update only its paragraph. No slider boundary, gameplay effect, score calculation, saved configuration, historical record or UIMSD recording code changes in this package. Previously saved rules remain untouched.

Updated the existing Android notice test for scrolling text above controls, actual slider touch/cancellation, unchanged active rules and original score, and narrow read-only rendering. Required screenshots remain custom-rules-notice.png and custom-rules-notice-narrow.png. Existing two presentation tests pass0.038s and both changed core classes compile locally. Android verification pending; do not claim these new pictures are verified until retrieved and inspected.

Previous produced APK118: source03393816054f4f3ccaa3e5fd85fae754e4f6e6e4/run38390168067. Its core/build job passed and private APK artifact11808407560 exists; its Android job was still pending when this report was prepared. It contains the older scoring-only notice, not this replacement. TEST119 requires1354core/22API34/24validPNG/debug and screenshot review. No private release/distribution or physical-phone claim.

The experimental warning does not authorize changing limits or rewriting old runs. Eleven gameplay effects are implemented through117, seven remain unfinished and disabled. The local boss-health prototype is not in this UI package. Offensive attribution groundwork is in118, but full offensive integration remains unfinished. UIMSD remains paused as requested and the eighteen-slider early alpha is not yet complete.


Verification38391217821passed: actual1354core/22API34 XML, zero failed/error/skipped;24validPNG/debug. Manual normal/narrow warning review completed; narrow-scrolled follow-up capture added in120. No private distribution or physical-phone claim.
