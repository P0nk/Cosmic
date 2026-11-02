---
name: cosmic-packet-work
description: Safely add or change a v83 packet handler or packet builder in Cosmic (opcodes, PacketCreator, channel handlers). Use whenever a change touches bytes sent to or read from the client.
version: 1.0.0
metadata:
  hermes:
    tags: [cosmic, protocol, netty, v83]
    related_skills: [cosmic-build-test]
---
# v83 packet work

## When to Use
Editing `net/opcodes`, `net/server/**/handlers`, `tools/PacketCreator.java`, `tools/packets/*`, or anything that
calls `OutPacket`/`InPacket`.

## Procedure
1. Find the opcode in `net/opcodes/SendOpcode.java` / `RecvOpcode.java`. Find every existing builder for it
   (`grep -n "SendOpcode.<NAME>" -r src/main/java`) — reuse/extend before adding new ones.
2. Write the struct down first, in the PR body:
   `OPCODE 0xNN NAME: int charId, short x, short y, byte stance, ...`
3. Implement. Reads in handlers: bounds-check every index/quantity; reject impossible values and log with the
   character name (possible exploit).
4. Test: add a unit test asserting exact bytes (see `src/test/java/net/packet/ByteBufOutPacketTest.java`,
   `testutil/Packets.java`). For handlers, extend `testutil/HandlerTest`.
5. If the client must change to accept it → create a linked `cosmic-client` card; do not merge server first.

## Pitfalls
- v83 uses little-endian; strings are `short length + ASCII`. Booleans are 1 byte.
- Movement/attack packets are broadcast to the map — exclude the source player where the original does.
- Don't do DB I/O on the Netty thread.

## Verification
Byte-exact unit test passes; smoke test passes; if possible, E0 test client exercises the path.
