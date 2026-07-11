function enter(pi) {
    const player = pi.getPlayer();

    if (player.gmLevel() >= 2 || player.getReborns() >= 3) {
        pi.playPortalSound();
        pi.warp(450001003, 0);
        return true;
    }
    pi.playerMessage(5, "You need at least 3 rebirths to enter Arcane River.");
    return false;
}