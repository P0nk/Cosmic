function enter(pi) {
const player = pi.getPlayer();

    if (player.gmLevel() >= 2 || player.getReborns() >= 1 && player.haveItem(4032832) || pi.isQuestCompleted(3165)) {
        pi.playPortalSound();
        pi.warp(211060300, "west00");
        return true;
    }
    pi.playerMessage(5, "You need the Key to the First Tower to proceed.");
    return false;
}