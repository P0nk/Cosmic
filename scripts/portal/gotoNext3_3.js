function enter(pi) {
const player = pi.getPlayer();

    if (player.gmLevel() >= 2 || player.getReborns() >= 1 && player.haveItem(4032834) || pi.isQuestCompleted(3167)) {
        pi.playPortalSound();
        pi.warp(211060620, "in00");
        return true;
    }
    pi.playerMessage(5, "You need to have the Key to the Third Tower to proceed.");
    return false;
}