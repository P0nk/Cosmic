package provider.wz.binary.serialize;

import provider.wz.binary.WzImage;

public final class WzVector extends WzSerialize {
    private final int x;
    private final int y;

    public WzVector(WzImage parent, int offset, int x, int y) {
        super(parent, offset);
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
