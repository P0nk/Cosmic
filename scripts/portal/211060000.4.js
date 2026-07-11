function enter(pi) {
    const player = pi.getPlayer();

    if (player.gmLevel() >= 2 || player.getReborns() >= 1) {
        pi.playPortalSound();
        pi.warp(211060010, 0);
        return true;
    }
    pi.playerMessage(5, "You need at least 1 rebirth to enter Lion's Castle.");
    return false;
}