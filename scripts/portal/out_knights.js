function enter(pi) {
    if (pi.isQuestCompleted(31125)) {
        pi.playPortalSound();
        pi.warp(271030000, "in00");
        return true;
    } else {
        pi.playPortalSound();
        pi.warp(271030010,"in00");
        return true;
    }
}