function enter(pi) {
    const player = pi.getPlayer();
    
        if (player.getReborns() >= 3 && pi.isQuestCompleted(31164)) {
            pi.playPortalSound();
            pi.warp(450011660);
            return true;
        }
        pi.playerMessage(5, "You need to complete The Arcane Path Part 4 quest first!.");
        return false;
    }