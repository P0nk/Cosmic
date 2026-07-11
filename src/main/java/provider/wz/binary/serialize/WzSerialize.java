package provider.wz.binary.serialize;

import provider.wz.binary.WzImage;

public abstract class WzSerialize {
    protected final WzImage parent;
    protected final int offset;

    protected WzSerialize(WzImage parent, int offset) {
        this.parent = parent;
        this.offset = offset;
    }
}
