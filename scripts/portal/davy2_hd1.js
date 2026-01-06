var windowTime = 10 * 1000;

function enter(pi) {
    var eim = pi.getEventInstance();
    // Parse the property to an Integer immediately
    var level = parseInt(eim.getProperty("level"));

    if (eim.getProperty("stage2b") == "0") {
        // Now 'level' is a number, matching the Java method signature
        pi.getMap(925100202).spawnAllMonstersFromMapSpawnList(level, true);
        eim.setProperty("stage2b", "1");
    }

    pi.playPortalSound();
    pi.warp(925100202, 0);
    return true;
}