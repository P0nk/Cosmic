package testclient;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Little-endian packet builder (opcode first, then fields), mirroring net.packet.OutPacket. */
public final class PacketWriter {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public PacketWriter(int opcode) {
        writeShort(opcode);
    }

    public PacketWriter writeByte(int v) {
        out.write(v);
        return this;
    }

    public PacketWriter writeShort(int v) {
        out.write(v);
        out.write(v >>> 8);
        return this;
    }

    public PacketWriter writeInt(int v) {
        writeShort(v);
        writeShort(v >>> 16);
        return this;
    }

    public PacketWriter writeBytes(byte[] b) {
        out.write(b, 0, b.length);
        return this;
    }

    /** MapleString: short length, then ASCII bytes (no terminator). */
    public PacketWriter writeString(String s) {
        byte[] b = s.getBytes(StandardCharsets.US_ASCII);
        writeShort(b.length);
        return writeBytes(b);
    }

    public byte[] toBytes() {
        return out.toByteArray();
    }
}
