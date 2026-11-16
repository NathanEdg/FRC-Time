# Verification

The upstream Gradle 7.3.3 build failed on the workspace's default JDK 21 with `Unsupported class file major version 65`. The updated Gradle 8.14.3 build and JavaFX 21 compile on JDK 21. Packaged previews launch on JDK 17 and JDK 21.

Completed checks (18 headless tests and 2 native UI tests passed):

- Headless tests: minimal schema creation, idempotent configuration, safe invalid-header rejection, legacy column mapping, registration fields, duplicate IDs, numeric IDs and leading zeroes, exact time accumulation, failed saves, and spreadsheet-link validation.
- Native JavaFX checks: desktop, narrow, and wide layouts, typed numeric entry with leading zeroes, button and Enter submission, busy/error state, suppression of repeated submissions while saving, registration validation, and closing the registration window.
- Google client request tests use an in-process mock HTTP transport to check cross-sheet A1 ranges, append-only grid growth, and sanitized API failures.
- `check`, `uiTest`, distribution generation, and preview launch pass on the development platform.

Native screenshots are generated to build/review and are not versioned. Preview creates no credentials, configuration, accounts, or attendance.

Live OAuth consent, a real team's sheet permissions/data, and other operating systems were not tested because no live Google configuration was supplied. Releases contain native JavaFX libraries for their build platform.

Independent Java and code/UI reviews approved the final change with no outstanding material findings in their reviewed scope.

Original license and attribution are preserved. A few pre-existing source comments are formatted by the required repository formatter. Obsolete GrizzlyTime update/registration notices and assets are removed; the fork does not advertise upstream binaries as updates.
