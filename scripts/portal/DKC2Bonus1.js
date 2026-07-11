function enter(pi) {
    if (!pi.isDoorOpen()) {
        var remainingTime = pi.timeUntilNextOpen();
        pi.message("The door is currently closed and will open in " + remainingTime + " minutes.");
        return false;
    }

    var em = pi.getEventManager("DKBONUS1");
    if (!em.startInstance(pi.getPlayer())) {
        pi.message("The map is occupied, please try again later!");
        return false;
    }

    pi.playPortalSound();
    
    // Randomly choose between 2210044 and 2210042
    var itemId = Math.random() < 0.5 ? 2210044 : 2210042;
    pi.useItem(itemId);

    return true;
}