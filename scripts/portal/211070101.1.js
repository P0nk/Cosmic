function enter(pi) {
    const interaction = pi.getPlayer().getAbstractPlayerInteraction();
    const player = pi.getPlayer();

    if (pi.getPlayer().haveItem(4032860)) {
        pi.gainItem(4032860, -1);
        pi.playPortalSound();
        pi.warp(211070100, 0);
        return true;
    }
    pi.playerMessage(5, "You must retrieve 1 Prison Key to return to the fight");
    return false;
}