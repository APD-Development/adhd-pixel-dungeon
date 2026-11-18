# RESOLVED: existing-tag release creation

The documented existing-tag request omits unused target_commitish and requires a pre-existing exact source tag, verified before/after. It creates or updates no Git reference. With unchanged Actions Contents-write permissions, run42280704000/job128108597214 succeeded; verified368 fallback was privately published at2026-11-06T19:35:44Z and downloaded back/checksummed at19:35:51.893311Z. No token extraction or permission change occurred. Configured normal gh CLI was also independently verified usable and created the369 PRIVATE DRAFT; no owner action is required. Earlier reports of a current CLI401/manual-owner blocker are superseded by this observed success.

The earlier failure below remains evidence of the request containing the unnecessary target_commitish field, rather than proof that all available publication paths were blocked. Exact source tags remain unchanged.369 publication awaits full native acceptance, not owner permission.

# Confirmed release authorization boundary

Diagnostic reuse run42279432373/job128104219010 reverified exact368 package, source/APK digests, prior signing/test evidence, and full source/history scans. Release creation returned HTTP403: `Resource not accessible by integration`, request ID `EC54:B342F:298C9B:898911:6AEE29C1` at2026-11-06T19:25:53Z. No APK was rebuilt and no permissions/credentials were changed.

GitHub's current official Create a release documentation (https://docs.github.com/en/rest/releases/releases?apiVersion=2022-11-28#create-a-release), read2026-11-06, states that a target_commitish modifying .github/workflows relative to the default branch requires Workflows repository permission(write), that some authentication paths return this exact403, and that Actions GITHUB_TOKEN cannot be authorized for this. The exact369 source differs from origin/main in124 workflow paths. This documents a permission boundary rather than proving the internal GitHub policy evaluation.

Keep the exact build tag/source pinned. The owner has been asked to create a private draft release using existing exact tag apd-alpha-preview-369 through GitHub UI, after which asset attachment can be attempted with existing content permissions. Publishing/attachment remains unverified until actually completed. No substitute target, token extraction or permission bypass is authorized. No further blind retry.
