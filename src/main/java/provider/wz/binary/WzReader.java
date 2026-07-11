package provider.wz.binary;

import provider.wz.binary.serialize.WzCanvas;
import provider.wz.binary.serialize.WzConvex;
import provider.wz.binary.serialize.WzPolyShape;
import provider.wz.binary.serialize.WzProperty;
import provider.wz.binary.serialize.WzSerialize;
import provider.wz.binary.serialize.WzSerializeType;
import provider.wz.binary.serialize.WzSound;
import provider.wz.binary.serialize.WzUol;
import provider.wz.binary.serialize.WzVector;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class WzReader {
    public static int readCompressedInt(ByteBuffer buffer) {
        final byte value = buffer.get();
        if (value == Byte.MIN_VALUE) {
            return buffer.getInt();
        } else {
            return value;
        }
    }

    public static String readString(ByteBuffer buffer) {
        int length = buffer.get();
        if (length < 0) {
            if (length == Byte.MIN_VALUE) {
                length = buffer.getInt();
            } else {
                length = -length;
            }
            if (length > 0) {
                final byte[] data = new byte[length];
                buffer.get(data);
                WzCrypto.cryptAscii(data);
                return new String(data, StandardCharsets.US_ASCII);
            }
        } else if (length > 0) {
            if (length == Byte.MAX_VALUE) {
                length = buffer.getInt();
            }
            if (length > 0) {
                length = length * 2; // UTF16
                final byte[] data = new byte[length];
                buffer.get(data);
                WzCrypto.cryptUnicode(data);
                return new String(data, StandardCharsets.UTF_16LE);
            }
        }
        return "";
    }

    public static String readStringBlock(WzImage parent, ByteBuffer buffer) {
        final byte stringType = buffer.get();
        switch (stringType) {
            case 0x00:
            case 0x73: {
                return WzReader.readString(buffer);
            }
            case 0x01:
            case 0x1B: {
                final int stringOffset = buffer.getInt();
                final int originalPosition = buffer.position();
                buffer.position(parent.getOffset() + stringOffset);
                final String string = WzReader.readString(buffer);
                buffer.position(originalPosition);
                return string;
            }
            default:
                throw new WzReaderError("Unknown string block type : %d", stringType);
        }
    }

    public static WzSerialize readPropertyItem(WzImage parent, ByteBuffer buffer, int offset) {
        final String itemUol = WzReader.readStringBlock(parent, buffer);
        final WzSerializeType type = WzSerializeType.getByUol(itemUol);
        if (type == null) {
            throw new WzReaderError("Unknown property item UOL : %s", itemUol);
        }
        switch (type) {
            case PROPERTY:
                return new WzProperty(parent, offset);
            case CANVAS:
                return new WzCanvas(parent, offset);
            case VECTOR: {
                final int x = readCompressedInt(buffer);
                final int y = readCompressedInt(buffer);
                return new WzVector(parent, offset, x, y);
            }
            case CONVEX:
                return new WzConvex(parent, offset);
            case POLYSHAPE:
                return new WzPolyShape(parent, offset);
            case SOUND:
                return new WzSound(parent, offset);
            case UOL: {
                buffer.position(buffer.position() + 1);
                final String uol = readStringBlock(parent, buffer);
                return new WzUol(parent, offset, uol);
            }
            default:
                throw new WzReaderError("Unhandled property item type : %s", type);
        }
    }
}
