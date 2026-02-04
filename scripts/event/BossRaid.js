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
    { name: "Deo", mobId: 3220001, mapId: 260010201 }
];

function init() {
    scheduleNextRaid();
}

function scheduleNextRaid() {
    // Schedule random times: Morning (10-12), Afternoon (4-6), Evening (8-10)
    // For now, let's just schedule it to try starting every hour and check probability, 
    // or just fixed random intervals.
    // Simpler approach for "Randomly a few times a day":
    // Schedule next run in 4-8 hours.

    var randomHours = 4 + Math.floor(Math.random() * 5); // 4 to 8 hours
    var nextTime = randomHours * 60 * 60 * 1000;

    em.schedule("startRaid", nextTime);

    // Log for server console/debugging
    // java.lang.System.out.println("[BossRaid] Next raid scheduled in " + randomHours + " hours.");
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
        // Broadcast to channel only
        em.getChannelServer().broadcastPacket(
            PacketCreator.serverNotice(6, "[Boss Raid] " + currentBoss.name + "s are spawning rapidly in this channel at " + getMapName(currentBoss.mapId) + "! The raid will last for 5 minutes!")
        );
        // Also show scrolling header if possible
        em.getChannelServer().broadcastPacket(
            PacketCreator.serverNotice(5, "[Boss Raid] " + currentBoss.name + " invasion at " + getMapName(currentBoss.mapId) + "!")
        );
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
        em.getChannelServer().broadcastPacket(
            PacketCreator.serverNotice(6, "[Boss Raid] The invasion of " + currentBoss.name + "s has ended.")
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
            em.getChannelServer().broadcastPacket(
                PacketCreator.serverNotice(6, "[Boss Raid] " + currentBoss.name + "s are spawning rapidly in this channel at " + getMapName(currentBoss.mapId) + "! The raid will last for 5 minutes!")
            );
            em.getChannelServer().broadcastPacket(
                PacketCreator.serverNotice(5, "[Boss Raid] " + currentBoss.name + " invasion at " + getMapName(currentBoss.mapId) + "!")
            );
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
