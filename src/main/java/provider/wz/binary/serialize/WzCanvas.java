package provider.wz.binary.serialize;

import provider.wz.binary.WzImage;
import provider.wz.binary.WzReader;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WzCanvas extends WzSerialize {
    private WzProperty property;

    public WzCanvas(WzImage parent, int offset) {
        super(parent, offset);
    }

    public WzProperty getProperty() {
        if (property == null) {
            property = readProperty();
        }
        return property;
    }

    private WzProperty readProperty() {
        final ByteBuffer buffer = parent.getBuffer(offset);
        WzReader.readStringBlock(parent, buffer); // "Canvas" UOL
        buffer.position(buffer.position() + 1);   // unused byte
        final boolean hasProperty = buffer.get() == 1;
        if (hasProperty) {
            buffer.position(buffer.position() + 2); // embedded Property header (00 00)
            Map<String, Object> items = WzProperty.readItems(parent, buffer);
            return new WzProperty(parent, offset, items);
        }
        return new WzProperty(parent, offset, new LinkedHashMap<>());
    }
}
