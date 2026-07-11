function enter(pi) {
    if (!pi.isDoorOpen()) {
        var remainingTime = pi.timeUntilNextOpen();
        pi.message("The door is currently closed and will open in " + remainingTime + " minutes.");
        return false;
    }
    
    var em = pi.getEventManager("YOSHIJQ");
    if (!em.startInstance(pi.getPlayer())) {
        pi.message("The map is occupied, please try again later!");
        return false;
    }

    pi.playPortalSound();
    
    // Randomly select an item ID between 2210045 and 2210054
    var randomItemId = 2210045 + Math.floor(Math.random() * 10);
    pi.useItem(randomItemId);

    return true;
}
