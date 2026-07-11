package provider.wz.binary;

import java.nio.ByteBuffer;

public interface WzReadable {
    ByteBuffer getBuffer(int offset);
}
