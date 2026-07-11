function enter(pi) {
    if (pi.haveItem(4032922) && pi.isQuestStarted(31127)) {
    pi.playPortalSound();
	pi.warp(271030100,"out00");
	return true;
	} else {
         pi.getPlayer().dropMessage(5, "I have scouted the area, I should report back to Chief Alex.");
         return false;
    }
}