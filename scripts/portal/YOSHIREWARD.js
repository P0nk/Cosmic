function enter(pi) {
    if (pi.getPlayer().haveItem(4036551) && pi.getPlayer().haveItem(4036552)) {
        var eim = pi.getEventInstance();
        if (eim != null) {
            eim.stopEventTimer();
            eim.dispose();
        }
        pi.playPortalSound();
        pi.warp(910000000);
        pi.cancelItem(2210045);
        pi.cancelItem(2210046);
        pi.cancelItem(2210047);
        pi.cancelItem(2210048);
        pi.cancelItem(2210049);
        pi.cancelItem(2210050);
        pi.cancelItem(2210051);
        pi.cancelItem(2210052);
        pi.cancelItem(2210053);
        pi.cancelItem(2210054);
        pi.gainItem(4036551, -1);
        pi.gainItem(4036552, -1);
        pi.gainItem(4310000, 10);
        return true;
    } else {
        pi.playerMessage(5, "This door can only be unlocked with both keys...");
        return false;
    }
}
