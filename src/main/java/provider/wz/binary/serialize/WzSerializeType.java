package provider.wz.binary.serialize;

import java.util.Map;

public enum WzSerializeType {
    PROPERTY("Property"),
    CANVAS("Canvas"),
    VECTOR("Shape2D#Vector2D"),
    CONVEX("Shape2D#Convex2D"),
    POLYSHAPE("Shape2D#PolyShape2D"),
    SOUND("Sound_DX8"),
    UOL("UOL");

    private static final Map<String, WzSerializeType> typeMap = Map.of(
            PROPERTY.getUol(), PROPERTY,
            CANVAS.getUol(), CANVAS,
            VECTOR.getUol(), VECTOR,
            CONVEX.getUol(), CONVEX,
            POLYSHAPE.getUol(), POLYSHAPE,
            SOUND.getUol(), SOUND,
            UOL.getUol(), UOL
    );

    private final String uol;

    WzSerializeType(String uol) {
        this.uol = uol;
    }

    public final String getUol() {
        return uol;
    }

    public static WzSerializeType getByUol(String uol) {
        return typeMap.get(uol);
    }
}
