# Build & smoke-test baseline (unmodified master)

Recorded by cosmic-qa on master `eb9b4453f` (PR #1 merged), Windows 11, JDK 21.0.12, local MySQL 8, fresh `cosmic` schema.
Update this file whenever a PR intentionally changes what "green" looks like.

## Build — `.\mvnw.cmd -B package`
| Result | Tests | Failures/Errors/Skipped | Duration |
|---|---|---|---|
| BUILD SUCCESS | 1890 | 0 / 0 / 0 | ~32 s (Maven total; 34 s wall) |

Expected noise (not errors): Mockito self-attach warning, "A Java agent has been loaded dynamically", HotSpot CDS sharing warning.

## Smoke test — `tools/agent/smoke_test.ps1`
Run: `powershell -ExecutionPolicy Bypass -File tools/agent/smoke_test.ps1` (add `SMOKE_ALLOW_ERRORS=1` to report new errors without failing).

| Check | Baseline |
|---|---|
| Server online | "Cosmic is now online after ~14.5 s" (first boot incl. 38 Liquibase changesets, 30 646 rows); script reported 15 s |
| Ports | 8484, 7575, 7576, 7577 all accept connections |
| Worlds | 1 world (Scania), 3 channels |
| Lines matching ` ERROR ` / `Exception` in stdout+stderr | **0** |
| stderr | empty |

### Known startup errors
**None.** Master boots clean, so there is nothing to list with a cause guess. If a future change legitimately introduces
a harmless startup error, add one regex per line to the block below (matched against the whole log line), with a
`#` comment giving a one-line cause guess. The smoke test fails on any ERROR/Exception line that matches none of them.

```baseline-patterns
# (empty - master has no known startup errors)
```

## Environment notes
- `mvnw.cmd` needs `JAVA_HOME`; the smoke script now falls back to the newest `C:\Program Files\Java\jdk-*` if unset. Plain `mvnw.cmd` calls from a shell without `JAVA_HOME` fail with "JAVA_HOME ... not defined correctly".
- DB password comes from the git-ignored `config.local.yaml` in the main checkout (or `COSMIC_DB_PASS`); never commit it.
- The smoke test only observes boot + port liveness (it does not log in a client), so runtime errors are out of scope.
