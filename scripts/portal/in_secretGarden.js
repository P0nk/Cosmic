function enter(pi) {
    if (pi.isQuestStarted(31145)) {
        pi.forceCompleteQuest(31145);
	    pi.playPortalSound();
        pi.warp(271030410, "out00");
        pi.getPlayer().dropMessage(5, "I have found Neinheart in the Secret Grove. Let's report back to Chief Alex.");
	    return true;
    }
    if (pi.isQuestStarted(31146) || pi.isQuestCompleted(31145)) {
        pi.playPortalSound();
        pi.warp(271030410, "out00");
        return true;
    } else {
         pi.getPlayer().dropMessage(5, "I should talk to the Informant before proceeding.");
         return false;
    }
}