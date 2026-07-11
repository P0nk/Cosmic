function enter(pi) {
const player = pi.getPlayer();

    if (player.gmLevel() >= 2 || player.getReborns() >= 1 ) {
        pi.playPortalSound();
        pi.warp(211060800, "up00");
        return true;
    }
}