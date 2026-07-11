function enter(pi) {
const player = pi.getPlayer();

    if (player.gmLevel() >= 2 || player.getReborns() >= 1 && pi.isQuestStarted(3167) || pi.isQuestCompleted(3167)) {
        pi.playPortalSound();
        pi.warp(211060601, "down00");
        return true;
    }
    pi.playerMessage(5, "You need to start the Key to the Third Tower quest to enter.");
    return false;
}