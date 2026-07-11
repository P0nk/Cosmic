/*  Von Leon (boss spawner)
    Audience room (211070100)
    Talkable NPC that spawns the boss.
 */

    var status;
    const ExpeditionType = Java.type('server.expeditions.ExpeditionType');
    var exped = ExpeditionType.DARKNELL;

    function start() {
            status = -1;
            action(1, 0, 0);
    }
    
    function action(mode, type, selection) {
            if (mode == -1) {
                    cm.dispose();
            } else {
                    if (mode == 0 && type > 0) {
                            cm.dispose();
                            return;
                    }
                    if (mode == 1)
                            status++;
                    else
                            status--;
    
                    if(status == 0) {
                        expedition = cm.getExpedition(exped);
                        if (expedition.isLeader(cm.getPlayer())) {
                            cm.sendYesNo("?????????????????.....");
                        }
                    } else if (status == 1) {
                            cm.getMap().spawnMonsterOnGroundBelow(8645009, 359, 29);
                            cm.getMap().destroyNPC(3003921);
                            cm.dispose();
                    }
    
            }
    }