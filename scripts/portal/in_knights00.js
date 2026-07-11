function enter(pi) {
    if (pi.isQuestStarted(31124) || pi.isQuestStarted(31125)) {
	    pi.forceCompleteQuest(31124);
	    pi.playPortalSound();
        pi.warp(271030010, "out00");
	    return true;
    }
    if (pi.isQuestCompleted(31125)) {
        pi.playPortalSound();
        pi.warp(271030000, "west00");
        return true;

    } else {
        pi.getPlayer().dropMessage(5, "I should speak to Chief Alex first before entering this area.");
        return false;
    }
}