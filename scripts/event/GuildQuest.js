/*
    Sharenian Guild PQ (Refactored)
*/

var isPq = true;
var minPlayers = 2, maxPlayers = 30;
var minLevel = 1, maxLevel = 255;
var entryMap = 990000000;
var exitMap = 990001100;
var recruitMap = 101030104;
var clearMap = 990001000;

var minMapId = 990000000;
var maxMapId = 990001101;

var waitTime = 0.5;
var eventTime = 90;
var bonusTime = 0.5;

const maxLobbies = 1;

function init() {
    setEventRequirements();
}

function getMaxLobbies() {
    return maxLobbies;
}

function setEventRequirements() {
    var reqStr = "";
    reqStr += "\r\n    Number of players: " + (maxPlayers - minPlayers >= 1 ? minPlayers + " ~ " + maxPlayers : minPlayers);
    reqStr += "\r\n    Level range: " + (maxLevel - minLevel >= 1 ? minLevel + " ~ " + maxLevel : minLevel);
    reqStr += "\r\n    All members of the same guild";
    reqStr += "\r\n    Time limit: " + eventTime + " minutes";
    em.setProperty("party", reqStr);
}

function setEventExclusives(eim) {
    eim.setExclusiveItems([1032033, 4001024, 4001025, 4001026, 4001027, 4001028, 4001029, 4001030, 4001031, 4001032, 4001033, 4001034, 4001035, 4001037]);
}

function setEventRewards(eim) {
    eim.setEventRewards(1, [], []);
    eim.setEventClearStageExp([]);
}

function getEligibleParty(party) {
    var eligible = [];
    var hasLeader = false;
    var guildId = 0;

    if (party.size() > 0) {
        var partyList = party.toArray();

        for (var i = 0; i < party.size(); i++) {
            var ch = partyList[i];
            if (ch.isLeader()) {
                guildId = ch.getGuildId();
                break;
            }
        }

        for (var i = 0; i < party.size(); i++) {
            var ch = partyList[i];
            if (ch.getMapId() == recruitMap && ch.getLevel() >= minLevel && ch.getLevel() <= maxLevel && ch.getGuildId() == guildId) {
                if (ch.isLeader()) hasLeader = true;
                eligible.push(ch);
            }
        }
    }

    if (!hasLeader) eligible = [];
    return Java.to(eligible, Java.type('net.server.world.PartyCharacter[]'));
}

function setup(level, lobbyid) {
    var eim = em.newInstance("Guild" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("guild", 0);
    eim.setProperty("canJoin", 1);
    eim.setProperty("canRevive", 0);

    var maps = [990000000, 990000100, 990000200, 990000300, 990000301, 990000400, 990000401,
                990000410, 990000420, 990000430, 990000431, 990000440, 990000500, 990000501,
                990000502, 990000600, 990000610, 990000611, 990000620, 990000630, 990000631,
                990000640, 990000641, 990000700, 990000800, 990000900, 990001000, 990001100, 990001101];

    for (var m of maps) eim.getInstanceMap(m).resetPQ(level);

    respawnStages(eim);
    eim.setProperty("entryTimestamp", "" + (Date.now() + (60000 * waitTime)));
    eim.startEventTimer(waitTime * 60000);

    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function isTeamAllJobs(eim) {
    var eventJobs = eim.getEventPlayersJobs();
    var rangeJobs = parseInt('111110', 2);
    return ((eventJobs & rangeJobs) == rangeJobs);
}

function afterSetup(eim) {
    var leader = em.getChannelServer().getPlayerStorage().getCharacterById(eim.getLeaderId());
    if (leader != null) {
        eim.setProperty("guild", "" + leader.getGuildId());
    }
}

function playerEntry(eim, player) {
    var map = eim.getMapInstance(entryMap);
    player.changeMap(map, map.getPortal(0));
}

function scheduledTimeout(eim) {
    if (eim.isEventCleared()) {
        eim.warpEventTeam(990001100);
    } else {
        if (eim.getIntProperty("canJoin") == 1) {
            eim.setProperty("canJoin", 0);

            if (eim.checkEventTeamLacking(true, minPlayers)) {
                end(eim);
            } else {
                eim.startEventTimer(eventTime * 60000);
                if (isTeamAllJobs(eim)) {
                    var rnd = Math.floor(Math.random() * 4);
                    eim.applyEventPlayersItemBuff(2023000 + rnd);
                }
            }
        } else {
            end(eim);
        }
    }
}

function playerUnregistered(eim, player) {
    player.cancelEffect(2023000);
    player.cancelEffect(2023001);
    player.cancelEffect(2023002);
    player.cancelEffect(2023003);
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, 0);
}

function changedMap(eim, player, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        if (eim.isEventTeamLackingNow(true, minPlayers, player) && eim.getIntProperty("canJoin") == 0) {
            eim.unregisterPlayer(player);
            end(eim);
        } else {
            eim.unregisterPlayer(player);
        }
    }
}

function afterChangedMap(eim, player, mapid) {
    if (mapid == 990000100) {
        var text = "So, here is the brief. You guys should be warned that, once out on the fortress outskirts, anyone that would not be equipping the #b#t1032033##k will die instantly due to the deteriorated state of the air around there. That being said, once your team moves out, make sure to #bhit the glowing rocks#k in that region and #bequip the dropped item#k before advancing stages. That will protect you thoroughly from the air sickness. Good luck!";
        player.getAbstractPlayerInteraction().npcTalk(9040000, text);
    }
}

function playerDead(eim, player) {
    if (player.getMapId() == 990000900) {
        if (player.getMap().countAlivePlayers() == 0 && player.getMap().countMonsters() > 0) {
            end(eim);
        }
    }
}

function playerRevive(eim, player) {
    if (eim.getIntProperty("canRevive") == 0) {
        if (eim.isEventTeamLackingNow(true, minPlayers, player) && eim.getIntProperty("canJoin") == 0) {
            player.respawn(eim, exitMap);
            end(eim);
        } else {
            player.respawn(eim, exitMap);
        }
        return false;
    }
    return true;
}

function playerDisconnected(eim, player) {
    if (eim.isEventTeamLackingNow(true, minPlayers, player) && eim.getIntProperty("canJoin") == 0) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.unregisterPlayer(player);
    }
}

function end(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        playerExit(eim, party.get(i));
    }
    eim.dispose();
}

function clearPQ(eim) {
    eim.stopEventTimer();
    eim.setEventCleared();
    eim.warpEventTeam(clearMap);
    eim.startEventTimer(bonusTime * 60000);
}

function dispose(eim) {
    em.schedule("reopenGuildQuest", em.getLobbyDelay() * 1.5 * 1000);
}

function reopenGuildQuest() {
    em.attemptStartGuildInstance();
}

// ---------- FILLER FUNCTIONS ----------
function respawnStages(eim) {}
function changedLeader(eim, leader) {}
function leftParty(eim, player) {}
function disbandParty(eim) {}
function monsterValue(eim, mobId) { return 1; }
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function monsterKilled(mob, eim) {}
function allMonstersDead(eim) {}
function cancelSchedule() {}