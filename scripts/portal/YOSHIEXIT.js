function enter(pi) {
    var eim = pi.getEventInstance();
    if (eim != null) {
        eim.stopEventTimer();
        eim.dispose();
    }

    pi.playPortalSound();
    pi.warp(910000010);
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
    return true;
}