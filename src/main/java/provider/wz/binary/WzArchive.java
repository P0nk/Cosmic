package provider.wz.binary;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

public final class WzArchive implements WzReadable, Closeable {
    private final ByteBuffer buffer;
    private WzImage image;

    public WzArchive(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public WzImage getImage() {
        if (image == null) {
            image = new WzImage(this, 0);
        }
        return image;
    }

    @Override
    public ByteBuffer getBuffer(int offset) {
        final ByteBuffer result = buffer.duplicate();
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.position(offset);
        return result;
    }

    @Override
    public void close() {
        // Heap-backed buffer holds no OS resources; nothing to release.
    }

    public static WzArchive from(String path) throws IOException {
        return from(new File(path));
    }

    public static WzArchive from(Path path) throws IOException {
        return from(path.toFile());
    }

    public static WzArchive from(File file) throws IOException {
        // Read the entire file into a heap byte[] so the OS handle is released immediately.
        // Memory-mapping (FileChannel.map) keeps the file locked on Windows even after close().
        final long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("img file too large to load: " + file);
        }
        final byte[] bytes = new byte[(int) length];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.readFully(bytes);
        }
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return new WzArchive(buffer);
    }
}
