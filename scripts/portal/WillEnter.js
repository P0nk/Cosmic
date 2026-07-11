function enter(pi) {
const player = pi.getPlayer();

    if (player.getReborns() >= 3 && pi.isQuestCompleted(31162)) {
        pi.playPortalSound();
        pi.warp(450007240);
        return true;
    }
    pi.playerMessage(5, "You need to complete The Arcane Path Part 2 quest first!.");
    return false;
}