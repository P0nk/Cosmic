# "Feels like a live server" — Roadmap

Owner: `cosmic-lead` keeps this file current. Each epic becomes a parent Kanban card; the lead decomposes it into
child cards ≤ 1 day of agent work each, using `TASK_TEMPLATE.md`.

Priority order: **E0 → E1 → (E2, E3 in parallel) → E4 → E5**. The lead must not start E4/E5 cards until E0 is done.

---

## E0 — Foundations (cosmic-qa, cosmic-core)
Agents can't fix what they can't test.
- Baseline: run `.\mvnw.cmd -B package` and `powershell tools/agent/smoke_test.ps1` on `master`; record known startup
  errors in `docs/agents/BASELINE.md` so later smoke tests diff against it.
- Headless **protocol test client** (`src/test/java/.../TestClient` or `tools/agent/testclient/`): logs in with
  `admin/admin`, picks a character, enters a map, sends chat, moves, exits. Reuses `MapleAESOFB` + opcodes.
  This becomes the end-to-end test harness for every other epic.
- Bug intake: triage `docs/issues.txt`, `docs/leftover.txt` and upstream GitHub issues into `triage` cards.

## E1 — Content accuracy (cosmic-content, cosmic-core)
- Audit NPC scripts by region (Victoria → Ossyria → Ludus Lake → Leafre …): missing scripts, wrong text, broken
  rewards. One card per region.
- Quest chain audit: start/complete conditions, rewards, quest-item drops.
- Drop tables vs v83 sources; mob stats; boss mechanics (Zakum, Horntail, Papulatus, Pianus).
- Party quests: KPQ, LPQ, OPQ, LMPQ, Pirate PQ, Romeo&Juliet — run each end-to-end with the test client where feasible.

## E2 — Economy & social (cosmic-core)
- Free Market: hired merchants, player shops, Owl of Minerva search, Fredrick storage — correctness + dupe safety.
- Trading, Duey, storage, mesos caps; guilds, alliances, parties, buddies, family, marriage.
- Anti-dupe/exploit review of every item-moving handler (pairs with cosmic-reviewer).
- Economy telemetry: daily mesos created/destroyed, item price index from merchant listings → `docs/agents/economy.md`.

## E3 — Simulated players (cosmic-sim) — see ADR 0001
Phase 1 (server-side "residents"):
- Socketless `Client` + real `Character` rows flagged `is_sim`, spawned into towns/training maps on a schedule.
- Behaviour tree: idle/walk in town, train in level-appropriate maps (move + use attack skill packets broadcast
  to real players), occasional chat from a phrase bank, party invites to real players, sit in FM with hired
  merchants priced from the economy index.
- Population curve by hour of day (configurable), channel distribution, "megaphone" chatter.
- Hard limits: sims never trade real items out of thin air; their merchant stock is minted from a budget tracked in
  telemetry so they don't wreck the economy. All behind `SIM_PLAYERS_ENABLED: false` by default.
Phase 2: drive sims through the E0 protocol client instead (gives load testing for free).

## E4 — Live-ops & events (cosmic-liveops)
- Event calendar in `config.yaml` (2x EXP/drop windows, hot-time rewards, seasonal events like Maple Leaf / Christmas
  map decorations where v83 data exists).
- Automated GM events: Ola Ola, Fitness, Snowball, Coconut, OX quiz — scheduler + announcements.
- Scheduled boss/area-boss respawn announcements, world notices, "server maintenance" flow with countdown.
- GM tooling: new `@`/`!` commands for running events; audit log.

## E5 — Client polish (cosmic-client) — human-verified
- Server, client and agents are all on the same Windows machine now, so no IP/port-forwarding patch is needed for
  dev play-testing — `127.0.0.1` already works end to end.
- Quality-of-life exe patches (damage cap, window/resolution, etc.) only when a server feature needs them.
- WZ fixes found by E1 that need client data changes.
