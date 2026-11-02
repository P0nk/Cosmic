package testclient;

import net.encryption.InitializationVector;
import net.encryption.MapleAESOFB;
import net.encryption.MapleCustomEncryption;
import net.opcodes.RecvOpcode;
import net.opcodes.SendOpcode;
import tools.HexTool;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * One headless v83 connection (login server or channel server). Protocol notes: docs/agents/testclient.md.
 * <p>
 * Uses the server's own cipher classes, mirrored: what the server sends with (sendIv, version 0xFFFF-83) the
 * client receives with, and what the server receives with (recvIv, version 83) the client sends with.
 */
public final class HeadlessClient implements Closeable {
    public static final short VERSION = 83;
    private static final int SOCKET_TIMEOUT_MS = 15_000;

    private final Socket socket;
    private final DataInputStream in;
    private final OutputStream out;
    private final MapleAESOFB sendCypher;
    private final MapleAESOFB recvCypher;
    private final Consumer<String> log;

    public HeadlessClient(String host, int port, Consumer<String> log) throws IOException {
        this.log = log;
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), SOCKET_TIMEOUT_MS);
        socket.setSoTimeout(SOCKET_TIMEOUT_MS);
        in = new DataInputStream(socket.getInputStream());
        out = socket.getOutputStream();

        // Unencrypted hello, 16 bytes: [len=0x0E short][version short][patch-string len short=1]["1"]
        //                              [server recvIv 4][server sendIv 4][locale byte]
        byte[] hello = new byte[16];
        in.readFully(hello);
        PacketReader h = new PacketReader(hello);
        int len = h.readShort();
        int version = h.readShort();
        h.skip(3);
        byte[] serverRecvIv = h.readBytes(4); // server's receive IV = OUR send IV
        byte[] serverSendIv = h.readBytes(4); // server's send IV    = OUR receive IV
        int locale = h.readByte();
        if (len != 0x0E || version != VERSION) {
            throw new IOException("Unexpected hello: len=" + len + " version=" + version);
        }
        sendCypher = new MapleAESOFB(ivOf(serverRecvIv), VERSION);
        recvCypher = new MapleAESOFB(ivOf(serverSendIv), (short) (0xFFFF - VERSION));
        log.accept("connected " + host + ":" + port + " version=" + version + " locale=" + locale
                + " clientSendIv=" + HexTool.toHexString(serverRecvIv) + " clientRecvIv=" + HexTool.toHexString(serverSendIv));
    }

    /** InitializationVector has a private constructor (the server only generates random ones); build one from bytes. */
    private static InitializationVector ivOf(byte[] bytes) {
        try {
            var ctor = InitializationVector.class.getDeclaredConstructor(byte[].class);
            ctor.setAccessible(true);
            return ctor.newInstance((Object) bytes);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("InitializationVector constructor changed", e);
        }
    }

    /** Encrypts and sends one packet: header (from the current IV), then Shanda, then AES-OFB over the body. */
    public synchronized void send(PacketWriter p) throws IOException {
        byte[] body = p.toBytes();
        byte[] header = sendCypher.getPacketHeader(body.length);
        MapleCustomEncryption.encryptData(body);
        sendCypher.crypt(body);
        out.write(header);
        out.write(body);
        out.flush();
    }

    public void send(RecvOpcode op, Consumer<PacketWriter> fill) throws IOException {
        PacketWriter w = new PacketWriter(op.getValue());
        fill.accept(w);
        send(w);
    }

    /** Reads and decrypts one packet. Throws EOFException if the server closed the socket. */
    public PacketReader read() throws IOException {
        int header = in.readInt(); // 4 raw header bytes as a big-endian int, exactly like PacketDecoder
        if (!recvCypher.isValidHeader(header)) {
            throw new IOException("Invalid packet header " + Integer.toHexString(header));
        }
        byte[] body = new byte[MapleAESOFB.getPacketLength(header)];
        in.readFully(body);
        recvCypher.crypt(body);
        MapleCustomEncryption.decryptData(body);
        return new PacketReader(body);
    }

    /**
     * Reads packets until one with the wanted opcode arrives (cursor positioned after the opcode). Server pings are
     * answered with a pong; every other packet is skipped and logged.
     */
    public PacketReader expect(SendOpcode wanted) throws IOException {
        while (true) {
            PacketReader p = read();
            if (p.opcode() == wanted.getValue()) {
                return p.afterOpcode();
            }
            if (p.opcode() == SendOpcode.PING.getValue()) {
                send(RecvOpcode.PONG, w -> { });
            } else {
                log.accept("  (skipping server packet 0x" + Integer.toHexString(p.opcode()) + ", " + p.bytes().length + " bytes)");
            }
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // closing the socket is the logout; the server cleans up on channelInactive
        }
    }
}
