package server.partyquest.pyramid;

public enum PyramidDifficulty {
    EASY(0),
    NORMAL(1),
    HARD(2),
    HELL(3);

    final int mode;

    PyramidDifficulty(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }

    public static PyramidDifficulty getById(int id) {
        return PyramidDifficulty.values()[id];
    }
}