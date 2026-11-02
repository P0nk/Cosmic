# "Feels like a live server" — Roadmap

Owner: `cosmic-lead` keeps this file current. Each epic becomes a parent Kanban card; the lead decomposes it into
child cards ≤ 1 day of agent work each, using `TASK_TEMPLATE.md`.

Priority order: **E0 → E1 → E2 → E4 → E5 → E3 (deferred)**. The lead must not start E4/E5 cards until E0 is done.
E3 (simulated players) is DEFERRED by the human (2026-11-02): low priority, large effort. No E3 cards until the human
re-activates it. E2 no longer runs in parallel with E3.

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

## E3 — Simulated players (cosmic-sim) — see ADR 0001 — **DEFERRED, do not start**
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

---

## Go-public checklist (sharing the server with other people) - NOT STARTED
Do this only after E0 is done and the server is stable. Everything today is set to 127.0.0.1 (this PC only).
Needs the human for the steps marked [human].

Security first:
- [ ] Change the default `admin` / `admin` password (and review every default account)
- [ ] Decide on `AUTOMATIC_REGISTER` (config.yaml): true lets anyone create an account
- [ ] Never expose MySQL port 3306 to the internet - game ports only
- [ ] Set a real MySQL root password and put it in config.yaml (do not commit it)

Network:
- [ ] [human] Find the public IP, or set up a stable address (dynamic DNS) - home IPs change
- [ ] Set `HOST` in config.yaml (WAN IPv4) to that address
- [ ] [human] Router port forwarding: login 8484 + the channel ports (7575-7577 for Scania; more for other worlds)
- [ ] [human] Windows Firewall allows the server program on those ports
- [ ] Check the internet provider allows incoming connections

Client:
- [ ] Card for cosmic-client: patched exe pointing at the public address (spec in client-patches/exe/)
- [ ] [human] Apply the patch and test from a different network (e.g. phone hotspot)
- [ ] Decide how friends get the client and WZ files (do not commit client binaries to the repo)

Running it:
- [ ] Server PC stays on; auto-start MySQL and the server; log rotation
- [ ] Backups of the `cosmic` database (daily)
- [ ] GM tools and ban process ready (E4)
- [ ] Rules/abuse plan: who can GM, what to do about dupers

## Concurrency cap (set by human 2026-11-03)
Max 2 cards `running`/`ready` at once (token budget). Decompose cards must not auto-start children: create
sub-cards blocked, and the lead unblocks them 1-2 at a time in E0 -> E1 -> E2 order. E3/E5 stay paused.
