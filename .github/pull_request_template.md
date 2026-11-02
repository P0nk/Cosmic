## Kanban card
<!-- t_xxxx — title -->

## What changed
<!-- 2-5 bullets -->

## v83 accuracy / sources
<!-- How do we know this matches GMS v83? Links, WZ paths, captures. "Custom" features: name the config flag. -->

## Verification
- [ ] `.\mvnw.cmd -B package`
- [ ] `powershell tools/agent/smoke_test.ps1` (paste PASS line or explain why not needed)
- [ ] Tests added/updated:
- [ ] Packet layout changed? If yes, opcode + struct documented below
- [ ] Needs client change? Link `client-patches/` PR / card

## Risk
<!-- dupes, crashes, DB migration, performance, merge-conflict hot files (PacketCreator, config.yaml) -->
