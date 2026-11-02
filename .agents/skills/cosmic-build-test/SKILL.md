---
name: cosmic-build-test
description: Build, unit-test and smoke-test the Cosmic v83 server natively on Windows. Use before every PR and when diagnosing build or startup failures.
version: 2.0.0
platforms: [windows]
metadata:
  hermes:
    tags: [cosmic, java, maven, windows, testing]
    related_skills: [cosmic-pr]
---
# Cosmic build & test

## When to Use
Any code, script, config or WZ XML change in the Cosmic repo — before `kanban_complete` / opening a PR.

## Prerequisite (one-time, human-set-up)
MySQL Community Server for Windows running locally, with an empty `cosmic` schema (`CREATE DATABASE cosmic;`).
The server creates its own tables and seed data on first boot. `config.yaml` defaults to `DB_HOST: localhost`,
`DB_USER: root`, `DB_PASS: ""` — matches a fresh MySQL install with no root password set.

## Quick Reference
- Compile + all tests (= CI, which runs on `windows-latest`): `.\mvnw.cmd -B package`
- Tests only: `.\mvnw.cmd -B test` · single class: `.\mvnw.cmd -B test -Dtest=ClassName`
- Full boot check: `powershell tools/agent/smoke_test.ps1` (serialised by a named mutex; other agents may hold it — just wait)
- Keep running for manual checks: `$env:KEEP_UP="1"; powershell tools/agent/smoke_test.ps1`, then stop the `java` process manually
- JS syntax: `node --check scripts/npc/<id>.js`
- XML well-formedness: `python -c "import xml.dom.minidom as m,sys; m.parse(sys.argv[1])" wz/<file>.xml`

## Procedure
1. Run the narrowest check first (single test, `node --check`, the XML check), then `.\mvnw.cmd -B package`.
2. If server behaviour changed (handlers, scripts, events, config, DB, WZ), run the smoke test.
3. Compare smoke-test errors against `docs/agents/BASELINE.md`. Only *new* errors are yours to fix.
4. Paste the final PASS line into the PR's Verification section.

## Pitfalls
- The smoke test's mutex only serialises *within one checkout*. Two agents in two different git worktrees still
  share the SAME MySQL `cosmic` schema and the SAME fixed ports — the mutex is named globally
  (`Global\CosmicSmokeTest`) specifically to serialise across worktrees too. Don't bypass it by running the jar
  by hand while another smoke test might be running.
- Netty `tcnative` classifier warnings during build are expected.
- Don't "fix" a failing test by deleting or `@Disabled`-ing it. Block the card instead.
- If MySQL isn't reachable the smoke test fails fast with a clear message — don't try to start/stop the MySQL
  Windows service yourself from a card; that's host setup, not per-card work.

## Verification
`.\mvnw.cmd -B package` ends with `BUILD SUCCESS`; smoke test prints `[smoke] PASS`.
