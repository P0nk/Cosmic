# net/ — protocol rules

- The client is an unmodifiable **GMS v83** binary (unless `cosmic-client` ships an exe patch). Every outgoing packet
  must match byte-for-byte what v83 expects. A wrong field length = client crash (error 38) or silent disconnect.
- Opcodes live in `net/opcodes` (`RecvOpcode`, `SendOpcode`). Don't renumber existing ones.
- Handlers: `net/server/channel/handlers/*Handler.java`, registered in `PacketProcessor`. Validate everything from the
  client — assume a malicious or buggy client (negative quantities, out-of-range slots, wrong map, item dupes).
- Use `InPacket`/`OutPacket` (`net/packet`). Unit-test new packet builders with `testutil/Packets` and the existing
  `ByteBuf*PacketTest` style: assert exact bytes.
- Never block Netty event-loop threads with DB calls or sleeps; hand off to `ThreadManager` / `TimerManager`.
- Concurrency: many handlers touch shared `MapleMap`/`Character` state. Follow the existing locking pattern of the
  class you are editing; don't introduce new global locks without an ADR in `docs/agents/decisions/`.
- Load `cosmic-packet-work` skill before changing any packet layout.
