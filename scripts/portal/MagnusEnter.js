function enter(pi) {
    const player = pi.getPlayer();
    
        if (player.getReborns() >= 3 && pi.isQuestCompleted(31166)) {
            pi.playPortalSound();
            pi.warp(401053002);
            return true;
        }
        pi.playerMessage(5, "You need to complete New Beginnings quest first!.");
        return false;
    }