function enter(pi) {
const player = pi.getPlayer();

    if (player.gmLevel() >= 2 || player.getReborns() >= 1 && player.haveItem(4032858) || pi.isQuestCompleted(3165)) {
        pi.gainItem(4032858,-1);
        pi.playPortalSound();
        pi.warp(211060201, "down00");
        return true;
    }
    pi.playerMessage(5, "You need the Temporary Key to the First Tower to enter.");
    return false;
}