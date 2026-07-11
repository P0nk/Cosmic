
/* Tana
 * 
 * @Author Rulax, Seeker1437
 * Helps players leave the map and logs weekly bosses
 * Tana
 */

const ExpeditionBossLog = Java.type("server.expeditions.ExpeditionBossLog");

var status;

function start() {
    status = -1;
    action(1, 0, 0)
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
    } else {
        if (mode == 0) {
            cm.dispose();
            return;
        }

        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        let eim = cm.getEventInstance();
        let mapId = cm.getMapId();
        let warpToMapId = getWarpToMapId(mapId);

        if (warpToMapId !== 0) {
            if (eim === null) {
                cm.sendYesNo("How did you even get in here without starting the expedition? Do you want to leave?");
            } else if (!eim.isEventCleared()) {
                cm.sendYesNo("If you leave now, you'll have to start over. \r\n\r\n" +
                    cm.getEventInstance().sendDmgDealt(cm.getPlayer().getWorld()) +
                    "Are you sure you want to leave?");
            } else {
                cm.sendYesNo("You guys finally overthrew such darkness! What a superb feat! Congratulations! \r\n\r\n" +
                    cm.getEventInstance().sendDmgDealt(cm.getPlayer().getWorld()) +
                    "Are you sure you want to leave now?");
            }
        } else {
            cm.sendYesNo("If you leave now, you'll have to start over. Are you sure you want to leave?");
        }

        if (status == 1) {
            if (eim.isEventCleared()) {
                let rewarded = eim.getProperty("rewarded") == "true";

                const players = eim.getPlayers();

                // Add item rewards based on the map or other conditions
                switch (mapId) {
                    case 280030000: //ZAKUM
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.ZAKUM, chr.getId());

                                playerInteraction.gainItem(4032133, 1);
                                playerInteraction.gainItem(2000005, 100);
                            })

                            eim.setProperty("rewarded", "true");
                        }
                        break;
                    case 240060200: //HORNTAIL
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.HORNTAIL, chr.getId());

                                playerInteraction.gainItem(4001094, 1);
                                playerInteraction.gainItem(2022179, 1);
                                playerInteraction.gainItem(2000005, 200);
                            })

                            eim.setProperty("rewarded", "true");
                        }
                        break;
                    case 211070100:  //VON LEON
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.VONLEON, chr.getId());

                                playerInteraction.gainItem(4001693, 1);

                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // 20% chance to get 2000005
                                    playerInteraction.gainItem(2000005, 400);
                                } else if (randomChance < 0.4) { // 20% chance to get 4000999
                                    playerInteraction.gainItem(4000999, 200);
                                } else if (randomChance < 0.6) { // 20% chance to get 2022282
                                    playerInteraction.gainItem(2022282, 1);
                                } else if (randomChance < 0.8) { // 20% chance to get 2022179
                                    playerInteraction.gainItem(2022179, 2);
                                } else if (randomChance < 0.84) { // 4% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 1);
                                } else if (randomChance < 0.88) { // 4% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 1);
                                } else if (randomChance < 0.92) { // 4% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 1);
                                } else if (randomChance < 0.96) { // 4% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 1);
                                } else { // 4% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 1);
                                }
                                
                                
                            });


                            eim.setProperty("rewarded", "true");
                        }
                        break;
                    case 271040100: //CYGNUS
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.CYGNUS, chr.getId());

                                // Always given items
                                playerInteraction.gainItem(4000659, 1);
                                playerInteraction.gainItem(4000999, 500);
                                playerInteraction.gainItem(2000005, 500);
                                playerInteraction.gainItem(2022282, 3);
                                playerInteraction.gainItem(2022179, 6);
                                playerInteraction.gainItem(2022283, 3);

                                // Randomized items - one of the following
                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // ~20% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 1);
                                } else if (randomChance < 0.4) { // ~20% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 1);
                                } else if (randomChance < 0.6) { // ~20% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 1);
                                } else if (randomChance < 0.8) { // ~20% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 1);
                                } else { // ~20% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 1);
                                }
                                
                            });

                            eim.setProperty("rewarded", "true");
                        }
                        break;



                    case 450007440: //WILL HARD
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.WILLSPIDER, chr.getId());

                                playerInteraction.gainItem(4000999, 2000);
                                playerInteraction.gainItem(4036536, 1);
                                playerInteraction.gainItem(4310000, 8);
                                playerInteraction.gainItem(4001890, 2);
                                playerInteraction.gainItem(2022282, 10);
                                playerInteraction.gainItem(2022179, 10);
                                playerInteraction.gainItem(2000005, 1000);
                                playerInteraction.gainItem(2022282, 10);

                                // Randomized items - one of the following
                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // ~20% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 5);
                                } else if (randomChance < 0.4) { // ~20% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 5);
                                } else if (randomChance < 0.6) { // ~20% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 5);
                                } else if (randomChance < 0.8) { // ~20% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 5);
                                } else { // ~20% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 5);
                                }
                            });

                            eim.setProperty("rewarded", "true");
                        }

                        break;
                    case 450010100: //VERUS HARD
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.VERUS, chr.getId());
                                
                                playerInteraction.gainItem(4000999, 2000);
                                playerInteraction.gainItem(4036538, 1);
                                playerInteraction.gainItem(4310000, 8);
                                playerInteraction.gainItem(4001893, 2);
                                playerInteraction.gainItem(2022282, 10);
                                playerInteraction.gainItem(2022282, 10);
                                playerInteraction.gainItem(2022179, 10);
                                playerInteraction.gainItem(2000005, 1000);

                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // ~20% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 5);
                                } else if (randomChance < 0.4) { // ~20% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 5);
                                } else if (randomChance < 0.6) { // ~20% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 5);
                                } else if (randomChance < 0.8) { // ~20% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 5);
                                } else { // ~20% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 5);
                                }
                            });

                            eim.setProperty("rewarded", "true");
                        }
                        break;

                    case 450012210: //DARKNELL HARD
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.DARKNELL, chr.getId());

                                playerInteraction.gainItem(4000999, 2000);
                                playerInteraction.gainItem(4036537, 1);
                                playerInteraction.gainItem(4310000, 8);
                                playerInteraction.gainItem(4001894, 2);
                                playerInteraction.gainItem(2022282, 10);
                                playerInteraction.gainItem(2022179, 10);
                                playerInteraction.gainItem(2000005, 1000);

                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // ~20% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 5);
                                } else if (randomChance < 0.4) { // ~20% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 5);
                                } else if (randomChance < 0.6) { // ~20% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 5);
                                } else if (randomChance < 0.8) { // ~20% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 5);
                                } else { // ~20% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 5);
                                }
                            });

                            eim.setProperty("rewarded", "true");
                        }
                        break;

                    case 450004750: //LUCID HARD
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.LUCID, chr.getId());

                                playerInteraction.gainItem(4000999, 2000);
                                playerInteraction.gainItem(4036535, 1);
                                playerInteraction.gainItem(4310000, 8);
                                playerInteraction.gainItem(4033732, 2);
                                playerInteraction.gainItem(2022282, 10);
                                playerInteraction.gainItem(2022179, 10);
                                playerInteraction.gainItem(2000005, 1000);
                                // Randomized items - one of the following
                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // ~20% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 5);
                                } else if (randomChance < 0.4) { // ~20% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 5);
                                } else if (randomChance < 0.6) { // ~20% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 5);
                                } else if (randomChance < 0.8) { // ~20% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 5);
                                } else { // ~20% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 5);
                                }

                            });

                            eim.setProperty("rewarded", "true");
                        }
                        break;



                        case 401060100: //MAGNUS
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.MAGNUS, chr.getId());

                                playerInteraction.gainItem(4000999, 4000);
                                playerInteraction.gainItem(4036540, 1);
                                playerInteraction.gainItem(4310000, 8);
                                playerInteraction.gainItem(4036543, 1);
                                playerInteraction.gainItem(2022282, 10);
                                playerInteraction.gainItem(2022179, 10);
                                playerInteraction.gainItem(2000005, 1000);
                                // Randomized items - one of the following
                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // ~20% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 7);
                                } else if (randomChance < 0.4) { // ~20% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 7);
                                } else if (randomChance < 0.6) { // ~20% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 7);
                                } else if (randomChance < 0.8) { // ~20% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 7);
                                } else { // ~20% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 7);
                                }

                            });

                            eim.setProperty("rewarded", "true");
                        }
                        break;






                    //NORMAL BOSSES

                    case 450007441: //WILL NORMAL
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.NWILLSPIDER, chr.getId());

                                playerInteraction.gainItem(4000999, 800);
                                playerInteraction.gainItem(4001890, 1);
                                playerInteraction.gainItem(2022282, 6);
                                playerInteraction.gainItem(2022283, 6);
                                playerInteraction.gainItem(2022179, 7);
                                playerInteraction.gainItem(2000005, 1000);

                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // ~20% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 3);
                                } else if (randomChance < 0.4) { // ~20% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 3);
                                } else if (randomChance < 0.6) { // ~20% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 3);
                                } else if (randomChance < 0.8) { // ~20% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 3);
                                } else { // ~20% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 3);
                                }
                            })

                            eim.setProperty("rewarded", "true");
                        }

                        break;
                    case 450010101: //VERUS NORMAL
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.NVERUS, chr.getId());

                                playerInteraction.gainItem(4000999, 800);
                                playerInteraction.gainItem(4001893, 1);
                                playerInteraction.gainItem(2022282, 6);
                                playerInteraction.gainItem(2022283, 6);
                                playerInteraction.gainItem(2022179, 7);
                                playerInteraction.gainItem(2000005, 1000);

                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // ~20% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 3);
                                } else if (randomChance < 0.4) { // ~20% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 3);
                                } else if (randomChance < 0.6) { // ~20% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 3);
                                } else if (randomChance < 0.8) { // ~20% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 3);
                                } else { // ~20% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 3);
                                }
                            })

                            eim.setProperty("rewarded", "true");
                        }
                        break;

                    case 450012211: //DARKNELL NORMAL
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.NDARKNELL, chr.getId());

                                playerInteraction.gainItem(4000999, 800);
                                playerInteraction.gainItem(4001894, 1);
                                playerInteraction.gainItem(2022282, 6);
                                playerInteraction.gainItem(2022283, 6);
                                playerInteraction.gainItem(2022179, 7);
                                playerInteraction.gainItem(2000005, 1000);
                                
                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // ~20% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 3);
                                } else if (randomChance < 0.4) { // ~20% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 3);
                                } else if (randomChance < 0.6) { // ~20% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 3);
                                } else if (randomChance < 0.8) { // ~20% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 3);
                                } else { // ~20% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 3);
                                }
                            })

                            eim.setProperty("rewarded", "true");
                        }
                        break;

                    case 450004751: //LUCID NORMAL
                        if (!rewarded) {
                            players.forEach((chr, index) => {
                                const playerInteraction = chr.getAbstractPlayerInteraction();
                                chr.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.NLUCID, chr.getId());

                                playerInteraction.gainItem(4000999, 800);
                                playerInteraction.gainItem(4033732, 1);
                                playerInteraction.gainItem(2022282, 6);
                                playerInteraction.gainItem(2022283, 6);
                                playerInteraction.gainItem(2022179, 7);
                                playerInteraction.gainItem(2000005, 1000);

                                const randomChance = Math.random();

                                if (randomChance < 0.2) { // ~20% chance to get 4036544
                                    playerInteraction.gainItem(4036544, 3);
                                } else if (randomChance < 0.4) { // ~20% chance to get 4036545
                                    playerInteraction.gainItem(4036545, 3);
                                } else if (randomChance < 0.6) { // ~20% chance to get 4036546
                                    playerInteraction.gainItem(4036546, 3);
                                } else if (randomChance < 0.8) { // ~20% chance to get 4036547
                                    playerInteraction.gainItem(4036547, 3);
                                } else { // ~20% chance to get 4036548
                                    playerInteraction.gainItem(4036548, 3);
                                }
                            })

                            eim.setProperty("rewarded", "true");
                        }
                        break;
                }

            } else {
                cm.dispose();
            }
            cm.warp(warpToMapId);
        }
    }
}

function getWarpToMapId(mapId) {
    let warpToMapId;

    switch (mapId) {
        case 280030000:
            warpToMapId = 211042400; //ZAKUM
            break;
        case 240060200:
            warpToMapId = 240050600; //HORNTAIL
            break;
        case 450007440:
            warpToMapId = 450007240; //WILL HARD
            break;
        case 450012210:
            warpToMapId = 450012200; //DARKNELL HARD
            break;
        case 450010100:
            warpToMapId = 450011500; //VERUS HARD
            break;
        case 541020800:
            warpToMapId = 541020700; //KREXEL
            break;
        case 211070100:
            warpToMapId = 211070000; //VON LEON
            break;
        case 271040100:
            warpToMapId = 271040000; //CYGNUS
            break;
        case 800040410:
            warpToMapId = 800040401; //CASTELLAN
            break;
        case 450004750:
            warpToMapId = 450003600; //LUCID HARD
            break;

        case 450004751:
            warpToMapId = 450003600; //LUCID NORMAL
            break;

        case 450007441:
            warpToMapId = 450007240; //WILL NORMAL
            break;

        case 450012211:
            warpToMapId = 450012200; //DARKNELL NORMAL
            break;

        case 450010101:
            warpToMapId = 450011500; //VERUS NORMAL
            break;

        
        case 401060100:
            warpToMapId = 401053002; //VERUS NORMAL
            break;

        // Add more cases for other map IDs as needed
        default:
            warpToMapId = 0; // Default warp destination if the map ID is not handled
    }

    return warpToMapId;
}