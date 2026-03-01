/*
    Swarm Boss Raid System
    Randomly selects an Area Boss and swarms their map for 5 minutes.
*/

var setupTask;
var raidActive = false;
var currentBoss = null;

// Boss Configuration
var raidBosses = [
    { name: "Mano", mobId: 2220000, mapId: 104000400 },
    { name: "Stumpy", mobId: 3220000, mapId: 101030404 },
    { name: "Deo", mobId: 3220001, mapId: 260010201 },
    { name: "Pianus", mobId: 8510000, mapId: 230040420 },
    { name: "Pianus", mobId: 8520000, mapId: 230040420 },
    { name: "Griffey", mobId: 8180001, mapId: 240020101 },
    { name: "Manon", mobId: 8180000, mapId: 240020401 },
    { name: "Giant Centipede", mobId: 5220004, mapId: 251010102 },
    { name: "Seruf", mobId: 4220001, mapId: 230020100 },
    { name: "Leviathan", mobId: 9500333, mapId: 240040401 },
    { name: "Tae Roon", mobId: 7220000, mapId: 250010304 },
    { name: "King Sage Cat", mobId: 7220002, mapId: 250010504 },
    { name: "Stumpy", mobId: 3220000, mapId: 101030404 },
    { name: "Jr.Balrog", mobId: 8130100, mapId: 105090900 },
    { name: "Dyle", mobId: 6220000, mapId: 107000300 },
    { name: "Blue Mushmom", mobId: 8220007, mapId: 800010100 },
    { name: "Mushmom", mobId: 6130101, mapId: 100000005 },
    { name: "Female Boss", mobId: 9400121, mapId: 801040003 },
    { name: "Nine-Tailed Fox", mobId: 7220001, mapId: 222010310 },
    { name: "Snowman", mobId: 8220001, mapId: 211040101 },
    { name: "Eliza", mobId: 8220000, mapId: 200010300 },
    { name: "Deet and Roi", mobId: 8090000, mapId: 261010102 },
    { name: "Security Camera", mobId: 7090000, mapId: 261020401 },
    { name: "D.Roy", mobId: 7110300, mapId: 261020500 },
    { name: "Chimera", mobId: 8220002, mapId: 261030000 },
    { name: "Snack Bar", mobId: 8220009, mapId: 105090310 }
];

function init() {
    // java.lang.System.out.println("[BossRaid] Script loaded. Initializing schedule...");
    scheduleNextRaid();
}

function scheduleNextRaid() {
    // Randomize minutes between 60 minutes (1 hour) and 180 minutes (3 hours)
    // This gives a more natural distribution than just flat 1, 2, or 3 hours.
    // var minMinutes = 60;
    // var maxMinutes = 180;
    // var randomMinutes = Math.floor(Math.random() * (maxMinutes - minMinutes + 1)) + minMinutes;

    var nextTime = 30 * 60 * 1000; // 30 minutes

    // java.lang.System.out.println("[BossRaid] Scheduling next raid in " + nextTime + "ms (" + (nextTime / 60000) + " minutes).");
    setupTask = em.schedule("startRaid", nextTime);

    // Log for server console/debugging
    // java.lang.System.out.println("[BossRaid] Next raid scheduled in " + randomMinutes + " minutes (" + (randomMinutes / 60).toFixed(1) + " hours).");
    java.lang.System.out.println("[BossRaid] Next raid scheduled in 30 seconds (TEST MODE).");
}

function startRaid() {
    if (raidActive) {
        return; // Already running?
    }

    // 1. Pick Random Boss
    var randIndex = Math.floor(Math.random() * raidBosses.length);
    currentBoss = raidBosses[randIndex];

    raidActive = true;
    em.setProperty("state", "active");
    em.setProperty("map", currentBoss.mapId);

    // 2. Announce
    var mapFactory = em.getChannelServer().getMapFactory();
    if (mapFactory) {
        // Broadcast to World (Global Announce)
        var chId = em.getChannelServer().getId();
        em.getWorldServer().broadcastPacket(
            PacketCreator.serverNotice(5, "[Boss Raid] Channel " + chId + ": " + currentBoss.name + "s are spawning rapidly at " + getMapName(currentBoss.mapId) + "! The raid will last for 5 minutes!")
        );
//        em.getWorldServer().broadcastPacket(
//            PacketCreator.serverNotice(5, "[Boss Raid] Channel " + chId + ": " + currentBoss.name + " invasion at " + getMapName(currentBoss.mapId) + "!")
//        );

        // Add Map Timer
        var map = em.getChannelServer().getMapFactory().getMap(currentBoss.mapId);
        if (map) {
            map.broadcastMessage(PacketCreator.getClock(300));
        }
    }

    // 3. Start Wave Loop
    em.schedule("spawnWave", 1000); // Start in 1s

    // 4. Schedule End
    em.schedule("endRaid", 5 * 60 * 1000); // 5 Minutes
}

function spawnWave() {
    if (!raidActive || currentBoss == null) {
        return;
    }

    var map = em.getChannelServer().getMapFactory().getMap(currentBoss.mapId);
    if (map == null) {
        return; // Should not happen
    }

    // Count existing bosses
    var currentCount = map.countMonster(currentBoss.mobId);
    var cap = 8;

    if (currentCount < cap) {
        // Spawn until cap
        var toSpawn = cap - currentCount;
        for (var i = 0; i < toSpawn; i++) {
            var mob = LifeFactory.getMonster(currentBoss.mobId);
            if (mob != null) {
                // Spawn at random spawnpoint in the map
                var randomSpawn = map.getRandomPlayerSpawnpoint();
                if (randomSpawn != null) {
                    map.spawnMonsterOnGroundBelow(mob, randomSpawn.getPosition());
                }
            }
        }
    }

    // Schedule next wave check
    em.schedule("spawnWave", 15000); // Check every 15 seconds
}

function endRaid() {
    raidActive = false;
    em.setProperty("state", "inactive");
    if (currentBoss != null) {
        em.getWorldServer().broadcastPacket(
            PacketCreator.serverNotice(5, "[Boss Raid] Channel " + em.getChannelServer().getId() + ": The invasion of " + currentBoss.name + "s has ended.")
        );
    }
    currentBoss = null;

    // Schedule next one
    scheduleNextRaid();
}

function getMapName(mapId) {
    // Helper to get map name, might need map factory
    // This is purely cosmetic
    var map = em.getChannelServer().getMapFactory().getMap(mapId);
    return map.getMapName();
}


function forceStart(specificBossName) {
    if (raidActive) {
        cancelSchedule();
    }

    if (specificBossName) {
        for (var i = 0; i < raidBosses.length; i++) {
            if (raidBosses[i].name.toLowerCase() == specificBossName.toLowerCase()) {
                currentBoss = raidBosses[i];
                break;
            }
        }
    }

    // If no specific boss or not found, startRaid will pick random
    if (currentBoss == null) {
        startRaid();
    } else {
        // Start raid with the pre-selected boss
        raidActive = true;
        em.setProperty("state", "active");
        em.setProperty("map", currentBoss.mapId);

        var mapFactory = em.getChannelServer().getMapFactory();
        if (mapFactory) {
            var chId = em.getChannelServer().getId();
            em.getWorldServer().broadcastPacket(
                PacketCreator.serverNotice(6, "[Boss Raid] Channel " + chId + ": " + currentBoss.name + "s are spawning rapidly at " + getMapName(currentBoss.mapId) + "! The raid will last for 5 minutes!")
            );
            em.getWorldServer().broadcastPacket(
                PacketCreator.serverNotice(5, "[Boss Raid] Channel " + chId + ": " + currentBoss.name + " invasion at " + getMapName(currentBoss.mapId) + "!")
            );

            // Add Map Timer
            var map = mapFactory.getMap(currentBoss.mapId);
            if (map) {
                map.broadcastMessage(PacketCreator.getClock(300));
            }
        }
        em.schedule("spawnWave", 1000);
        em.schedule("endRaid", 5 * 60 * 1000);
    }
}

function cancelSchedule() {
    if (setupTask != null) {
        setupTask.cancel(true);
    }
    raidActive = false;
}

// Required Event Methods
function setup(eim, leaderid) { }
function monsterValue(eim, mobid) { return 0; }
function disbandParty(eim, player) { }
function playerDisconnected(eim, player) { }
function playerEntry(eim, player) { }
function monsterKilled(mob, eim) { }
function scheduledTimeout(eim) { }
function afterSetup(eim) { }
function changedLeader(eim, leader) { }
function playerExit(eim, player) { }
function leftParty(eim, player) { }
function clearPQ(eim) { }
function allMonstersDead(eim) { }
function playerUnregistered(eim, player) { }
function dispose() { }
