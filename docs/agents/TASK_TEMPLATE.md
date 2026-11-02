# Kanban card template

Use this for every card body (`hermes kanban create ... --body "$(cat card.md)"`). Workers start with no memory of
the planning conversation — the body must stand alone.

```markdown
## Goal
One sentence: the observable behaviour after this card is done.

## Context
- Epic: E2 Economy & social
- Relevant files: src/main/java/server/maps/..., scripts/npc/9030000.js
- Known facts / sources: (v83 behaviour, wiki link, packet capture, upstream issue #)

## Acceptance criteria
- [ ] `.\mvnw.cmd -B package` passes
- [ ] New/updated unit test: <name>
- [ ] `powershell tools/agent/smoke_test.ps1` passes (if server behaviour changed)
- [ ] Behaviour X verified via <test client / log line / unit test>
- [ ] PR opened against `master` of the fork, linked in kanban_complete summary

## Out of scope
Things the worker must NOT touch (other lanes, refactors).

## Lane
cosmic-core | cosmic-content | cosmic-sim | cosmic-liveops | cosmic-client | cosmic-qa
```

Sizing: if a card needs more than ~1 day or touches more than 2 lanes, split it and link with `--parent`.
