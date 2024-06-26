package server.partyquest.pyramid;

public enum PyramidRank {
    S((byte) 0),
    A((byte) 1),
    B((byte) 2),
    C((byte) 3),
    D((byte) 4);

    final byte code;

    PyramidRank(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return this.code;
    }
}
