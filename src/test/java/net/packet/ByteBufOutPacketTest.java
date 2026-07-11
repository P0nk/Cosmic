package net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.opcodes.SendOpcode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteBufOutPacketTest {
    private ByteBufOutPacket outPacket;

    @BeforeEach
    void reset() {
        outPacket = new ByteBufOutPacket(SendOpcode.ADMIN_SHOP); // Any opcode will do
    }

    private static ByteBuf wrapExplicitlyWrittenBytes(OutPacket outPacket) {
        byte[] packetBytes = outPacket.getBytes();
        ByteBuf byteBuf = Unpooled.copiedBuffer(packetBytes);
        byteBuf.readShortLE(); // Skip over opcode
        return byteBuf;
    }

    @Test
    void whenInstantiatingNew_shouldWriteOpcode() {
        byte[] packetBytes = new ByteBufOutPacket(SendOpcode.NPC_TALK).getBytes();
        assertEquals(2, packetBytes.length);
    }

    @Test
    void getBytes() {
        ByteBufOutPacket outPacket = new ByteBufOutPacket(SendOpcode.PING); // This opcode has value 0x11 = 17 in decimal
        outPacket.writeByte(10);
        outPacket.writeByte(20);
        outPacket.writeByte(30);

        byte[] bytes = outPacket.getBytes();

        assertArrayEquals(new byte[]{(byte) 17, (byte) 0, (byte) 10, (byte) 20, (byte) 30}, bytes);
    }

    @Test
    void writeByte() {
        final byte writtenByte = 19;
        outPacket.writeByte(writtenByte);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);
        byte readByte = wrapped.readByte();

        assertEquals(writtenByte, readByte);
    }

    @Test
    void writeByteFromInt() {
        final int writtenInt = 123;
        outPacket.writeByte(writtenInt);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);
        byte readByte = wrapped.readByte();

        assertEquals(writtenInt, readByte);
    }

    @Test
    void whenWritingByteFromInt_shouldOnlyWrite1Byte() {
        final int writtenInt = Integer.MAX_VALUE;
        outPacket.writeByte(writtenInt);

        byte[] bytes = outPacket.getBytes();
        assertEquals(2 + 1, bytes.length); // 2 for opcode
    }

    @Test
    void writeBytes() {
        byte[] writtenBytes = {101, 102, 103};
        outPacket.writeBytes(writtenBytes);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);

        assertEquals(101, wrapped.readByte());
        assertEquals(102, wrapped.readByte());
        assertEquals(103, wrapped.readByte());
    }

    @Test
    void writeShort() {
        final short writtenShort = 4312;
        outPacket.writeShort(writtenShort);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);
        short readShort = wrapped.readShortLE();

        assertEquals(writtenShort, readShort);
    }

    @Test
    void whenWritingShortFromInt_shouldOnlyWrite2Bytes() {
        final int writtenInt = Integer.MAX_VALUE;
        outPacket.writeShort(writtenInt);

        byte[] bytes = outPacket.getBytes();
        assertEquals(2 + 2, bytes.length); // 2 for opcode
    }

    @Test
    void writeShortFromInt() {
        final int writtenInt = 34_567;
        outPacket.writeShort(writtenInt);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);
        short readShort = wrapped.readShortLE();

        assertEquals((short) writtenInt, readShort);
    }

    @Test
    void writeInt() {
        final int writtenInt = 1_010_101_010;
        outPacket.writeInt(writtenInt);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);
        int readInt = wrapped.readIntLE();

        assertEquals(writtenInt, readInt);
    }

    @Test
    void writeLong() {
        final long writtenLong = 100_200_300_400_500_600L;
        outPacket.writeLong(writtenLong);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);
        long readLong = wrapped.readLongLE();

        assertEquals(writtenLong, readLong);
    }

    @Test
    void writeBoolean_true() {
        outPacket.writeBool(true);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);
        byte readByte = wrapped.readByte();

        assertEquals(1, readByte);
    }

    @Test
    void writeBoolean_false() {
        outPacket.writeBool(false);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);
        byte readByte = wrapped.readByte();

        assertEquals(0, readByte);
    }

    @Test
    void writeString() {
        final String writtenString = "You've been weakened, making you unable to jump.";
        outPacket.writeString(writtenString);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);
        int length = wrapped.readShortLE();
        byte[] stringBytes = new byte[length];
        wrapped.readBytes(stringBytes);
        String readString = new String(stringBytes, StandardCharsets.US_ASCII);

        assertEquals(writtenString, readString);
    }

    @Test
    void writePosition() {
        final Point writtenPoint = new Point(23, 42);
        outPacket.writePos(writtenPoint);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);
        short readX = wrapped.readShortLE();
        short readY = wrapped.readShortLE();

        assertEquals((short) writtenPoint.getX(), readX);
        assertEquals((short) writtenPoint.getY(), readY);
    }

    @Test
    void whenSkipping_shouldWriteZeroes() {
        final byte firstWrittenByte = 9;
        final byte secondWrittenByte = 11;
        outPacket.writeByte(firstWrittenByte);
        outPacket.skip(2);
        outPacket.writeByte(secondWrittenByte);

        ByteBuf wrapped = wrapExplicitlyWrittenBytes(outPacket);

        assertEquals(firstWrittenByte, wrapped.readByte());
        assertEquals(0, wrapped.readByte());
        assertEquals(0, wrapped.readByte());
        assertEquals(secondWrittenByte, wrapped.readByte());
    }
}