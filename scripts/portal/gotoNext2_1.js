function enter(pi) {
const player = pi.getPlayer();

    if (player.gmLevel() >= 2 || player.getReborns() >= 1 && player.haveItem(4032833) || pi.isQuestCompleted(3166)) {
        pi.playPortalSound();
        pi.warp(211060500, "west00");
        return true;
    }
    pi.playerMessage(5, "You need the Key to the Second Tower to proceed.");
    return false;
}