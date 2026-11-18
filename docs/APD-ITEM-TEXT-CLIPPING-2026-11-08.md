# Item text clipping correction

Owner screenshot19089 shows the Mirror Image scroll final line clipped above Drop/Throw/Read. MechanicsInfoPanel gave a short description viewport exactly body.height(), while ScrollPane truncates camera dimensions to integer pixels. It also provided no below-text padding. Corrected the description content to ceil(text height)+2 and used whole-pixel viewport heights; the same rule applies after relevant detail expansion. The action area stays separate, descriptions remain scrollable, and item actions/gameplay are unchanged.

Added pure geometry regression for integer/fractional and short/long content. Extended real Android item-inspection checks to verify final-line reachability with bottom padding. Added identified Mirror Image x2 in inventory to exercise its multiline title and actual Drop/Throw/Read controls. Screenshots will be reviewed before delivery. Shortened weapon wording remains included; Arcana/Lucky behavior unchanged.

Unpublished375 was superseded after the owner reported this additional UI bug; preserve its source tag/draft rather than rewrite it. It passed core/build and all native cases after a Developer emulator disconnection retry; first packaging selected incomplete duplicate-name evidence and failed. Exact-ID375 recovery also failed due artifact folder layout. These are not successful deliveries. Build376 includes both approved corrections, not unrelated gameplay. No claim of physical-phone verification.

Full local core suite2956/452 passed with zero failures/errors/skips; /workspace/apd376-local-full.log. Native376 verification pending.
