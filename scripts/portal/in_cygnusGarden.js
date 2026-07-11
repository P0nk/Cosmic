function enter(pi) {
    if (pi.isQuestStarted(31149)) {
	    pi.forceCompleteQuest(31149);
	    pi.getPlayer().dropMessage(5, "I found the Cygnus Garden, I should report back to Chief Alex.");
	    return true;
    }
    if (pi.haveItem(4032923)) {
       pi.playPortalSound();
       pi.warp(271040000, "out00");
       pi.gainItem(4032923, -1);
       return true;
    } else {
       pi.getPlayer().dropMessage(5, "I need a Dream Key in order to enter.");
       return false;
    }
}