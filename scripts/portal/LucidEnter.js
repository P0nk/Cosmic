function enter(pi) {
    const player = pi.getPlayer();
    
        if (player.getReborns() >= 3 && pi.isQuestCompleted(31161)) {
            pi.playPortalSound();
            pi.warp(450003600);
            return true;
        }
        pi.playerMessage(5, "You need to complete The Arcane Path quest first!.");
        return false;
    }