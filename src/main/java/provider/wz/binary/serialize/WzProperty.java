package provider.wz.binary.serialize;

import provider.wz.binary.WzImage;
import provider.wz.binary.WzReader;
import provider.wz.binary.WzReaderError;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WzProperty extends WzSerialize {
    private Map<String, Object> items;

    public WzProperty(WzImage parent, int offset) {
        super(parent, offset);
    }

    public WzProperty(WzImage parent, int offset, Map<String, Object> items) {
        super(parent, offset);
        this.items = items;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) getItems().get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        final T result = get(key);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    public Object getItem(String path) {
        final String[] split = path.split("/", 2);
        final Object item = getItems().get(split[0]);
        if (split.length == 1) {
            return item;
        }
        if (item instanceof WzProperty) {
            return ((WzProperty) item).getItem(split[1]);
        }
        throw new WzReaderError("Tried to access path : %s which does not exist", path);
    }

    public Map<String, Object> getItems() {
        if (items == null) {
            items = readProperty();
        }
        return items;
    }

    private Map<String, Object> readProperty() {
        final ByteBuffer buffer = parent.getBuffer(offset);
        WzReader.readStringBlock(parent, buffer);
        buffer.position(buffer.position() + 2); // reserved
        return readItems(parent, buffer);
    }

    public static Map<String, Object> readItems(WzImage parent, ByteBuffer buffer) {
        final LinkedHashMap<String, Object> items = new LinkedHashMap<>();
        final int size = WzReader.readCompressedInt(buffer);
        for (int i = 0; i < size; i++) {
            final String itemName = WzReader.readStringBlock(parent, buffer);
            final byte itemType = buffer.get();
            switch (itemType) {
                case 0: {
                    items.put(itemName, null);
                    break;
                }
                case 2:
                case 18: {
                    final short shortValue = buffer.getShort();
                    items.put(itemName, shortValue);
                    break;
                }
                case 3:
                case 19: {
                    final int intValue = WzReader.readCompressedInt(buffer);
                    items.put(itemName, intValue);
                    break;
                }
                case 20: {
                    final long value = buffer.get();
                    if (value == Byte.MIN_VALUE) {
                        items.put(itemName, buffer.getLong());
                    } else {
                        items.put(itemName, value);
                    }
                    break;
                }
                case 4: {
                    final byte floatType = buffer.get();
                    if (floatType == 0x00) {
                        items.put(itemName, 0f);
                    } else if (floatType == (byte) 0x80) {
                        final float floatValue = buffer.getFloat();
                        items.put(itemName, floatValue);
                    } else {
                        throw new WzReaderError("Unknown float type : %d", floatType);
                    }
                    break;
                }
                case 5: {
                    final double doubleValue = buffer.getDouble();
                    items.put(itemName, doubleValue);
                    break;
                }
                case 8: {
                    final String stringValue = WzReader.readStringBlock(parent, buffer);
                    items.put(itemName, stringValue);
                    break;
                }
                case 9: {
                    final int itemSize = buffer.getInt();
                    final int itemOffset = buffer.position();
                    items.put(itemName, WzReader.readPropertyItem(parent, buffer, itemOffset));
                    buffer.position(itemSize + itemOffset);
                    break;
                }
                default:
                    throw new WzReaderError("Unknown property item type : %d", itemType);
            }
        }
        return Collections.unmodifiableMap(items);
    }
}
