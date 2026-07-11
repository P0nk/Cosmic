function enter(pi) {
    var eim = pi.getEventInstance();
    if (eim != null) {
        eim.stopEventTimer();
        eim.dispose();
    }

    pi.playPortalSound();
    pi.warp(910000010);
    pi.cancelItem(2210044);
    pi.cancelItem(2210042);
    return true;
}