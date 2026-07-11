function enter(pi) {
    const player = pi.getPlayer();
    
        if (player.getReborns() >= 3 && pi.isQuestCompleted(31163)) {
            pi.playPortalSound();
            pi.warp(450012600);
            return true;
        }
        pi.playerMessage(5, "You need to complete The Arcane Path Part 3 quest first!.");
        return false;
    }