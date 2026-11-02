# scripts/ — JS scripting rules

- Scripts run on the server's JS engine; the API they call is `src/main/java/scripting/**` (start with
  `AbstractPlayerInteraction.java`, `npc/NPCConversationManager.java`, `event/EventManager.java`).
- File name = NPC / quest / portal / reactor id (e.g. `scripts/npc/9000020.js`). Look up ids in `handbook/*.txt`
  and in `wz/String.wz/*.xml`.
- NPC dialogs use the `status`/`action(mode, type, selection)` state machine pattern — copy an existing script in the
  same folder rather than inventing a new style. `mode == -1` = closed; always `cm.dispose()` on every exit path.
- Text must match v83 GMS wording where the NPC existed in GMS. Use `#b #k #r #e #n #L0# #l #v<id># #t<id># #i<id>#`
  colour/format codes as the client expects.
- No new Java API calls from JS unless the method exists — if you need one, open a child card for `cosmic-core`.
- Events in `scripts/event/` are owned by `cosmic-liveops`; quests/NPCs/portals/reactors by `cosmic-content`.
- Scripts are reloaded on server start; for the smoke test, `powershell tools/agent/smoke_test.ps1` is enough. Add a JS syntax
  check: `node --check <file>` catches parse errors (the engine is not Node, but syntax is compatible).
