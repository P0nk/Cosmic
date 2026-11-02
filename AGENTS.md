# Cosmic — Agent Guide

Cosmic is a **GMS v83 MapleStory server emulator** (Java 21, Maven, MySQL 8, Netty, JS scripts via GraalJS).
This fork's goal: a **private server that feels like a live GMS server** — populated, with a working economy,
accurate v83 content, and live-ops events. Read `docs/agents/ROADMAP.md` for the epics.

## Ground rules (all agents)
1. **One Kanban card = one branch = one PR.** Branch name: `agent/<card-id>-<short-slug>`. Never push to `master`.
2. **Stay in your lane.** Only edit the paths your profile owns (see "Ownership" below). If a fix needs another lane,
   `kanban_block` or create a child card for that profile — don't silently edit their area.
3. **Prove it.** Every PR must pass `.\mvnw.cmd -B package` (compile + tests). Gameplay changes also need the smoke
   test (`tools/agent/smoke_test.ps1`) and, where practical, a unit test using `src/test/java/testutil/*`.
4. **v83 accuracy beats invention.** When behaviour is in question, match GMS v83 (2009-era) behaviour. Cite your
   source (WZ data, packet captures, BasilMS/HeavenMS history, wiki) in the PR description. Custom features must be
   behind a `config.yaml` flag defaulting to the GMS-like behaviour.
5. **Never break the client contract.** Packet structure changes require the `cosmic-packet-work` skill and must be
   matched by an opcode/struct note in the PR. The client is a fixed v83 binary — we can't change what it expects
   unless the `cosmic-client` lane ships a matching exe patch.
6. **Database changes = new Liquibase changesets** in `src/main/resources/db/extensions/` (auto-included, filename
   order — prefix `YYYYMMDD-HHMM-`). Never edit existing changesets/seed files or hand-edit a live schema.
7. **Don't commit:** MySQL data directories, logs, `target/`, credentials, client binaries, or `.wz` binaries.
8. Finish every card with `kanban_complete` (with a summary + PR URL) or `kanban_request_review`. If blocked on a
   human decision (gameplay design, anything needing manual play-testing), use `kanban_block` and ask a precise
   question.

## Build / test / run (native Windows dev machine)
Agents run everything through Hermes' terminal tool, which executes via **Git Bash** — but the commands below are
the native Windows tools (`mvnw.cmd`, a PowerShell smoke test), not a Linux/WSL toolchain. One-time human setup:
install MySQL Community Server for Windows and create an empty `cosmic` schema (`CREATE DATABASE cosmic;`) — the
server creates its own tables and seed data on first boot. `config.yaml` already defaults to `DB_HOST: localhost`,
`DB_USER: root`, `DB_PASS: ""`; update `DB_PASS` to match your local MySQL root password if you set one.

| What | Command |
|---|---|
| Compile + unit tests (same as CI, which runs on `windows-latest`) | `.\mvnw.cmd -B package` |
| Unit tests only | `.\mvnw.cmd -B test` |
| One test class | `.\mvnw.cmd -B test -Dtest=NoteServiceTest` |
| Full server + MySQL smoke test | `powershell tools/agent/smoke_test.ps1` (serialised with a named mutex; ~2–4 min) |
| Run server for manual poking | `.\mvnw.cmd clean package` then double-click `launch.bat`, or `java -Dwz-path=wz -jar target\Cosmic.jar` |

Ports: login `8484`, Scania channels `7575-7577` (see `config.yaml` world list for others). Ready signal in logs:
`Cosmic is now online after N ms.` Default admin login: `admin` / `admin`.

## Map of the code
| Path | What lives there |
|---|---|
| `src/main/java/net/` | Netty server, encryption (`MapleAESOFB`), opcodes, packet handlers (`net/server/channel/handlers`) |
| `src/main/java/tools/PacketCreator.java` | Outgoing packet builders (huge; search, don't read whole) |
| `src/main/java/client/` | `Character`, `Client`, inventory, skills, GM commands (`client/command/commands`) |
| `src/main/java/server/` | Maps, life (mobs/NPCs), events, party quests, expeditions, shops, trade, cash shop, timers |
| `src/main/java/scripting/` | Java side of the JS script engine (`AbstractPlayerInteraction` = API scripts call) |
| `src/main/java/database/`, `src/main/resources/db` | DAOs (JDBI) + Liquibase migrations |
| `scripts/` | JS for NPCs, quests, portals, reactors, items, events (`scripts/event`) |
| `wz/` | **Server-side XML export** of WZ data (maps, mobs, items, strings) |
| `config.yaml` | Server rates, world/channel config, feature flags |
| `src/test/java/` | JUnit 5 + Mockito. Reuse `testutil/Mocks`, `Packets`, `HandlerTest` |
| `client-patches/` | (this fork) Client WZ change specs and exe byte patches — owned by `cosmic-client` |
| `docs/agents/` | Roadmap, task template, ADRs (architecture decisions) |

## Ownership (lanes)
| Profile | Owns |
|---|---|
| `cosmic-core` | `src/main/java/{net,client,server,database,service,model,tools,constants,config}`, `src/main/resources/db` — engine, protocol, economy & social systems (FM, trade, guilds, parties, buddies) |
| `cosmic-content` | `scripts/{npc,quest,portal,reactor,item,map}`, `wz/` XML, drop/shop data (via new changesets in `src/main/resources/db/extensions/`), `handbook/` |
| `cosmic-sim` | `src/main/java/sim/` (simulated players), its tests and config section |
| `cosmic-liveops` | `scripts/event/`, `server/events`, GM commands, schedulers, rates/announcement config |
| `cosmic-client` | `client-patches/` only |
| `cosmic-qa` | `src/test/`, `tools/agent/` (incl. `smoke_test.ps1`) |
| `cosmic-reviewer` | read-only; reviews PRs |
| `cosmic-lead` | `docs/agents/` — plans and creates cards, does not write game code |

Shared files (`config.yaml`, `pom.xml`, `PacketCreator.java`) — keep edits minimal and additive to avoid merge conflicts.

Deeper rules load automatically from `AGENTS.md` files in subfolders (`src/main/java/net`, `scripts`, `wz`, `client-patches`).
