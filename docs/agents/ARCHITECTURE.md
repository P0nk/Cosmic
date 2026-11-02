# Cosmic architecture - a plain-language guide

Audience: the project owner, who is new to this codebase. Technical terms are explained when first used.
Confidence: everything below was read from the repo (file names, counts, a few entry points). Nothing has been run
yet, because Java and MySQL are not installed. Statements about *how things behave* are inferred from file names and
the project docs, and are marked "inferred" where it matters.

## 1. What this thing is, in one paragraph
MapleStory is an online game. The real servers belong to Nexon. Cosmic is a community-built replacement server that
speaks the same network language as the 2009 game client (version 83, "v83"). You run the old game client on your PC,
point it at this server, and play. The server holds all the rules: monsters, drops, quests, shops, trades, chat.
The client is just a window onto it.

## 2. The big picture
    Game client (v83 .exe)            <- what you play; we cannot change it (except via small patches)
          |  encrypted network packets (TCP)
          v
    Login server (port 8484)          <- checks username/password, shows world + character list
          |
          v
    Channel servers (7575, 7576 ...)  <- the actual game world: maps, monsters, other players
          |
          +--> Scripts (scripts/*.js) <- NPC dialog, quests, portals, events (the "content")
          +--> Game data (wz/ XML)    <- maps, monsters, items, skills, strings
          +--> MySQL database         <- accounts, characters, inventories, guilds, drops, shops

Everything runs inside ONE Java program. Starting point: `Server.main()` in `src/main/java/net/server/Server.java`.

## 3. Words you will see (glossary)
- Packet: one small network message, e.g. "player moved" or "player said hello". The client and server swap thousands.
- Opcode: the number at the front of a packet that says what kind it is. Lists live in `net/opcodes`.
- Handler: Java code that reacts to one kind of incoming packet (147 of them in `net/server/channel/handlers`).
- World / channel: a world is a whole server (Scania, Bera...). Each world has several channels, like parallel copies
  of the maps. `config.yaml` defines 21 worlds, 3 channels each.
- WZ: the game's data files. The client has them as binary `.wz`. The server uses an XML copy in `wz/`.
- Script: a JavaScript file that controls an NPC, quest, portal, reactor (a smashable/triggerable object) or event.
- Liquibase: a tool that creates and upgrades the database tables automatically on startup.
- Netty: the library that handles network connections.
- Epic / card / lane: see ROADMAP.md. Epic = big goal, card = one small task, lane = which agent does it.

## 4. What happens when the server starts (inferred from Server.java)
1. Run database migrations (Liquibase builds/updates tables).
2. Load skills, items and other game data in parallel in the background.
3. Create each world from `config.yaml`, and open its channels.
4. Start the login port, start timers for background jobs.
5. Log "Cosmic is now online after N ms". That is the "ready" signal.

## 5. What happens when a player plays (inferred)
- Client connects to login (8484) -> sends username/password -> `LoginPasswordHandler` checks it.
- Player picks world, channel, character -> server hands the connection to that channel.
- From then on, every action (walk, attack, chat, talk to an NPC) is a packet. Netty receives it, `PacketProcessor`
  looks up the handler by opcode, the handler changes game state, and the server sends packets back (built in
  `tools/PacketCreator.java`) to that player and to everyone nearby.
- Clicking an NPC runs that NPC's script from `scripts/npc/<npcId>.js`. The script calls Java helpers
  (`NPCConversationManager`) to show text, give items, warp, etc.

## 6. Folder guide (what lives where)
| Folder | In plain terms | Size |
|---|---|---|
| `src/main/java/net` | Networking, encryption, packet handlers, opcodes | 147 handlers |
| `src/main/java/client` | The player character, inventory, skills, GM commands | `Character.java` is 10,900 lines |
| `src/main/java/server` | Game rules: maps, monsters, shops, trade, cash shop, party quests, events | maps/, life/, events/ ... |
| `src/main/java/scripting` | The bridge that lets JS scripts control the game | |
| `src/main/java/tools` | Packet builders (`PacketCreator`, 7,460 lines) and many small helper utilities | |
| `src/main/java/database`, `resources/db` | Database access and migrations | |
| `scripts/` | Game content as JS: 708 NPCs, 253 quests, 458 portals, 292 reactors, 108 events | |
| `wz/` | XML copy of game data (maps, mobs, items, text) | very large |
| `config.yaml` | Rates (EXP/drop/meso x10), worlds, feature switches | |
| `src/test/java` | 16 test files (JUnit) - a thin safety net for ~857 source files | |
| `client-patches/` | Specs for patching the game client | |
| `docs/agents/` | Roadmap, task template, triage, this report | |
| `tools/agent/` | `smoke_test.ps1` - boots the server and checks it starts | |

## 7. The risky parts (where bugs hide)
- Huge files: `Character.java`, `PacketCreator.java`, `Server.java`. Many people editing them at once causes merge
  conflicts, so only one card at a time may touch each.
- Packets must be byte-exact. One wrong byte can crash the client. Hence the `cosmic-packet-work` skill.
- Concurrency: many players act at the same moment. The old issue list mentions deadlocks after long uptime.
- Item duplication: any code that moves items (trade, shops, Fredrick, storage) can be exploited.
- Test coverage is thin (16 test files). So "it compiles" proves little, which is why E0 builds a test client.

## 8. Where the project stands today (2026-11-02)
- Branch `master`, clean, one extra commit of agent scaffolding. Never built or run on this machine.
- Missing on this PC: JDK 21, MySQL. Present: Python 3.14, the Hermes Kanban tool.
- Kanban board (default): 22 triage cards, 1 baseline card, test-client card, 5 epic-decompose cards.
  See section 9 for the issues found.
- Deferred by the owner: E3 simulated players.

## 9. Board state and problems found (UNCONFIRMED where noted)
- `t_08de931a` baseline card is `blocked`: the dispatcher tried twice and failed because the board has no default
  repo folder ("workspace_kind=worktree but no workspace_path"). Fix: set the board default workdir to this repo, or
  recreate the card with `--workspace worktree:C:/Users/joshs/Documents/GitHub/Cosmic`. Needs Java/MySQL anyway.
- The five "Decompose epic" cards and the test-client card all wait on the baseline card.
- The E3 decompose card `t_b1fe0504` is still `todo` (Kanban would not let me block a todo card). I added a comment
  saying it is deferred; it stays gated behind the baseline card. Do not let it run.
- The earlier triage run (`t_37526409`) created 22 cards. It crashed once and retried, then finished. Its cards
  overlap with `docs/agents/TRIAGE.md` (my own notes). The board is the source of truth; TRIAGE.md is a reference.

## 10. How we keep this report current
Updated by cosmic-lead whenever a card merges that changes structure, or weekly with the ROADMAP digest.
