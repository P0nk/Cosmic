package provider.wz.binary.serialize;

import provider.wz.binary.WzImage;

public final class WzUol extends WzSerialize {
    private final String uol;

    public WzUol(WzImage parent, int offset, String uol) {
        super(parent, offset);
        this.uol = uol;
    }

    public String getUol() {
        return uol;
    }
}
