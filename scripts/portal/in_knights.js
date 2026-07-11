function enter(pi) {
    if (pi.haveItem(4032922)) {
    pi.playPortalSound();
	pi.warp(271030100,"out00");
	return true;
	} else {
         pi.getPlayer().dropMessage(5, "You need to have the Cygnus Knight Emblem to proceed.");
         return false;
    }
}