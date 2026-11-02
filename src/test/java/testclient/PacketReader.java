package testclient;

import java.nio.charset.StandardCharsets;

/** Little-endian reader over one decrypted server packet. The first short is the send opcode. */
public final class PacketReader {
    private final byte[] data;
    private int pos;

    public PacketReader(byte[] data) {
        this.data = data;
    }

    public int opcode() {
        return (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
    }

    /** Position the cursor just after the opcode. */
    public PacketReader afterOpcode() {
        pos = 2;
        return this;
    }

    public int available() {
        return data.length - pos;
    }

    public int readByte() {
        return data[pos++] & 0xFF;
    }

    public int readShort() {
        return readByte() | (readByte() << 8);
    }

    public int readInt() {
        return readShort() | (readShort() << 16);
    }

    public byte[] readBytes(int n) {
        byte[] b = new byte[n];
        System.arraycopy(data, pos, b, 0, n);
        pos += n;
        return b;
    }

    public void skip(int n) {
        pos += n;
    }

    public String readString() {
        return new String(readBytes(readShort()), StandardCharsets.US_ASCII);
    }

    /** Fixed-width, NUL-padded string (character names are 13 bytes). */
    public String readFixedString(int n) {
        String s = new String(readBytes(n), StandardCharsets.US_ASCII);
        int nul = s.indexOf('\0');
        return nul < 0 ? s : s.substring(0, nul);
    }

    public byte[] bytes() {
        return data;
    }
}
