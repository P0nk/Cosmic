function enter(pi) {
    const player = pi.getPlayer();

    if (player.gmLevel() >= 2 || player.getReborns() >= 2) {
        pi.playPortalSound();
        pi.warp(271000000, 0);
        return true;
    }
    pi.playerMessage(5, "You need at least 2 rebirths to enter The Gate of Time.");
    return false;
}