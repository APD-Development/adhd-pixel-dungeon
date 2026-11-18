## UIMSD raw-preview follow-up — 2026-10-10

Source tracing found that ordinary text wrapping never splits an overlong token. Raw queued JSON/IDs can therefore still overflow despite a max-width setting, and the default text renderer treats underscores/double stars as highlighting markers rather than literal payload characters.

Status/queue messages now use a raw variant of the existing scrolling UIMSD window. It preserves literal markers, breaks overlong tokens using the actual font width, keeps Unicode code points intact, and restores its raw/structured mode alongside the exact original payload. Presentation-only newlines do not change queued, exported or stored data. Structured report/help windows retain their existing formatting.

Local evidence: changed production classes compile; seven permanent wrapping tests cover unchanged normal whitespace, long IDs, proportional widths, Unicode, literal markers, oversized single glyphs and a 100,000-character input. The initial test compilation failed because the regex escape was generated incorrectly; it was corrected before the passing run. Native preview assertions now include a long ID and visibly rendered literal markers at narrow width, across two scene resets.

Earlier dialog-only source 68ae1fa35c56c7cc0361ee5cd0118ddb93d9127d completed the full core/build and API 34 workflow 38091430139 successfully. Its artifacts are staging evidence, not a delivered/signing-checked replacement for TEST26. Complete/native verification of the combined saved-view, Community and raw-preview changes is requested separately. Phone acceptance and process-death restoration remain unverified.

