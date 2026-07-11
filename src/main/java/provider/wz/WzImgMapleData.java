package provider.wz;

import provider.Data;
import provider.DataEntity;
import provider.wz.binary.serialize.WzCanvas;
import provider.wz.binary.serialize.WzConvex;
import provider.wz.binary.serialize.WzPolyShape;
import provider.wz.binary.serialize.WzProperty;
import provider.wz.binary.serialize.WzSound;
import provider.wz.binary.serialize.WzUol;
import provider.wz.binary.serialize.WzVector;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WzImgMapleData implements Data {
    private final String name;
    private final Object value;

    public WzImgMapleData(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DataType getType() {
        if (value == null) {
            return DataType.IMG_0x00;
        }
        if (value instanceof WzProperty) {
            return DataType.PROPERTY;
        }
        if (value instanceof WzCanvas) {
            return DataType.CANVAS;
        }
        if (value instanceof WzVector) {
            return DataType.VECTOR;
        }
        if (value instanceof WzUol) {
            return DataType.UOL;
        }
        if (value instanceof WzSound) {
            return DataType.SOUND;
        }
        if (value instanceof WzConvex || value instanceof WzPolyShape) {
            return DataType.CONVEX;
        }
        if (value instanceof Short) {
            return DataType.SHORT;
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Byte) {
            return DataType.INT;
        }
        if (value instanceof Float) {
            return DataType.FLOAT;
        }
        if (value instanceof Double) {
            return DataType.DOUBLE;
        }
        if (value instanceof String) {
            return DataType.STRING;
        }
        return DataType.NONE;
    }

    @Override
    public Object getData() {
        if (value instanceof WzVector) {
            WzVector v = (WzVector) value;
            return new Point(v.getX(), v.getY());
        }
        if (value instanceof WzUol) {
            return ((WzUol) value).getUol();
        }
        if (value instanceof WzProperty
                || value instanceof WzCanvas
                || value instanceof WzSound
                || value instanceof WzConvex
                || value instanceof WzPolyShape) {
            return null;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Byte) {
            return ((Byte) value).intValue();
        }
        return value;
    }

    @Override
    public List<Data> getChildren() {
        Map<String, Object> items = childItems(value);
        if (items == null) {
            return new ArrayList<>();
        }
        List<Data> ret = new ArrayList<>(items.size());
        for (Map.Entry<String, Object> e : items.entrySet()) {
            ret.add(new WzImgMapleData(e.getKey(), e.getValue()));
        }
        return ret;
    }

    @Override
    public Data getChildByPath(String path) {
        if (path == null || path.isEmpty()) {
            return this;
        }
        Map<String, Object> items = childItems(value);
        if (items == null) {
            return null;
        }
        String[] parts = path.split("/", 2);
        if (!items.containsKey(parts[0])) {
            return null;
        }
        Data wrapped = new WzImgMapleData(parts[0], items.get(parts[0]));
        if (parts.length == 1) {
            return wrapped;
        }
        return wrapped.getChildByPath(parts[1]);
    }

    @Override
    public DataEntity getParent() {
        return null;
    }

    @Override
    public Iterator<Data> iterator() {
        return getChildren().iterator();
    }

    private static Map<String, Object> childItems(Object v) {
        if (v instanceof WzProperty) {
            return ((WzProperty) v).getItems();
        }
        if (v instanceof WzCanvas) {
            return ((WzCanvas) v).getProperty().getItems();
        }
        return null;
    }
}
