# Headless v83 test client

A window-less client that speaks the real v83 protocol to a locally running server. Source lives in
`src/test/java/testclient/` (compiled by `mvnw package`, never run by surefire: no class name ends in `Test`).

## Run it

Everything (boot server, run scenario, assert on server log, stop server):

```
powershell tools/agent/run_testclient.ps1            # scenario login-enter-map, account admin/admin
```

Against a server you already have running (login 8484):

```
.\mvnw.cmd -B package -DskipTests
java -cp "target\Cosmic.jar;target\test-classes" testclient.TestClientMain login-enter-map
java -cp "target\Cosmic.jar;target\test-classes" testclient.TestClientMain --list
```

Flags: `--host --port --user --pass --world --channel` (channel is 1-based). Success prints
`SCENARIO <name> PASS ...` and exits 0; failure prints `FAIL` + stack trace and exits 1.

`run_testclient.ps1` switches: `-Scenario <name>`, `-NoServer` (don't start/stop, no log assertions),
`-KeepServer`, `-Account/-Password` (an unknown account auto-registers and has no character, which exercises
character creation: a Beginner named `QaBot`). It starts the server through `smoke_test.ps1` with `KEEP_UP=1`, so it
shares the smoke test's fixed ports and MySQL schema (the smoke mutex is only held while the server boots; don't run
two of these at once). It also turns on `USE_DEBUG_SHOW_RCVD_PACKET` for the server only (config overlay in
`target/testclient-config.yaml`, handed to `smoke_test.ps1` via `SMOKE_SERVER_LOCAL_CONFIG`) so the server log shows
which opcodes arrived. Log assertions: `Received packet id 20` (PLAYER_LOGGEDIN), `Attempting to save chr` after we
close the socket (clean logout), and no `Error in packet handler`.

## How the protocol works (the hard-to-find bits)

1. **Hello (unencrypted, 16 bytes)** sent by the server on connect, to both login and channel servers:
   `0E 00` (length) `53 00` (version 83) `01 00 31` (patch string "1") `<4 bytes recvIv>` `<4 bytes sendIv>` `08` (locale).
   Names are from the *server's* point of view: the first IV is what the server uses to *receive*, so it is the
   client's **send** IV; the second is the client's **receive** IV.
2. **Ciphers**: `MapleAESOFB(iv, version)` from the server code is reused unchanged, mirrored:
   client send = `MapleAESOFB(helloIv1, 83)`, client receive = `MapleAESOFB(helloIv2, 0xFFFF - 83)`.
   (`InitializationVector` has a private constructor; `HeadlessClient` builds one reflectively.)
3. **Packet framing**: 4-byte header `getPacketHeader(len)` computed from the IV *before* encrypting, then body.
   Body encryption order on send: `MapleCustomEncryption.encryptData` (Shanda) then `crypt` (AES-OFB, which also
   advances the IV). Receive is the reverse. Header is read as a big-endian int and validated with `isValidHeader`.
   Inside the body: little-endian opcode short, then fields; strings are `short length` + ASCII, no terminator.
4. **Login sequence** (opcodes from `RecvOpcode`/`SendOpcode`):
   `LOGIN_PASSWORD(user, pass, 6 zero bytes, 4 hwid bytes)` -> `LOGIN_STATUS` (first byte 0 = ok; 23 = must accept ToS)
   -> `SERVERLIST_REQUEST` -> `SERVERLIST` per world, ended by id `0xFF` -> `CHARLIST_REQUEST(2, world, channel0)` ->
   `CHARLIST` -> `CHAR_SELECT(charId, macs, "<12 hex mac>_<8 hex hwid>")` (the host string format is regex-checked,
   `Hwid.fromHostString`) -> `SERVER_IP(short 0, ip[4], port, charId)`.
   PIN/PIC are not enforced by the server for plain `CHAR_SELECT`, so the client skips them.
5. **Channel**: new socket + new hello, send `PLAYER_LOGGEDIN(charId)`; the server replies with a pile of packets
   whose important one is `SET_FIELD` (0x7D, built by `PacketCreator.getCharInfo`) - that is "you are in the map".
   Server `PING`s must be answered with `PONG` (`expect()` does it) or the idle handler drops you after ~30 s.
6. **Logout**: there is no handler for `PLAYER_DC`; the real client just closes the socket and the server saves the
   character on `channelInactive`. The test client does the same.
7. The char list is only partially parsed (first entry's id, name, map id); the avatar-look block is variable length.

## Adding a scenario (design note)

- `HeadlessClient` = one encrypted connection (`send`, `read`, `expect(SendOpcode)`). `PacketWriter`/`PacketReader` =
  little-endian field helpers. `Scenario` = one method `run(ScenarioContext)` returning a one-line summary and
  throwing on failure. `Scenarios` = the name -> scenario registry.
- To add one: create `FooScenario implements Scenario` in `src/test/java/testclient/`, register it in `Scenarios`
  (`ALL.put("foo", new FooScenario())`). Reuse the login part rather than copying it: extract the first half of
  `LoginEnterMapScenario` into a helper that returns the connected `HeadlessClient` on the channel (part 2 of this epic
  will do that for walking/chat).
- Use `ScenarioContext.check(cond, msg)` for assertions and `ctx.log` for output; no `Thread.sleep` - wait on a packet
  with `expect`.
- Server-side assertions go in `run_testclient.ps1` (grep the log); add a `-Scenario`-specific block next to the
  `login-enter-map` one.
- Use packet constants from `net.opcodes.*` so a renumbering breaks compilation, not silently.
