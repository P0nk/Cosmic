function enter(pi) {
const player = pi.getPlayer();

    if (player.gmLevel() >= 2 || player.getReborns() >= 1) {
        pi.playPortalSound();
        pi.warp(211060010, "west00");
        return true;
    }
    pi.playerMessage(5, "You need at least 1 rebirth to enter Lion King's Castle.");
    return false;
}