# APD age verification status — October 9, 2026

APD has no verified age provider. Its current policy state is UNAVAILABLE, so Adult humor is disabled even if an old preference was true. ELIGIBLE/INELIGIBLE/UNKNOWN/UNAVAILABLE are APD policy states, not invented Google API enum names. Eligibility is separate from the player's preference.

Official Google documentation reviewed today describes the beta Play Age Signals API, access enrollment, SDK com.google.android.play:age-signals:0.0.4 and Android 6/API 23 or later. The current two-stage flow requests sharing access, which returns SHARED, NOT_SHARED or VERIFICATION_REQUIRED. Only SHARED permits the separate age-signals check. Returned age bounds and their source must be evaluated under a defined eligibility policy; SHARED alone does not establish adulthood. A missing upper bound is not automatically an adult signal, and missing/unavailable data must fail closed.

Source tiers distinguish self-declared, parent-managed and increasingly assessed age information. Supported use and distribution conditions require review before integration. Play Console configuration, API access, appropriate consent/UX, policy for sufficient evidence and real Play-installed-device tests have not been completed or verified for APD. A sideloaded test APK must not infer adulthood from having no signal.

Sources (verified October 9, 2026):
- https://developer.android.com/google/play/age-signals/overview
- https://developer.android.com/google/play/age-signals/use-age-signals-api
- https://developer.android.com/google/play/age-signals/request-age-signals
- https://developer.android.com/google/play/age-signals/understand-age-signals-responses

Do not replace this with a checkbox age claim or invent an API response. No network provider or Play dependency was added in this sprint.
