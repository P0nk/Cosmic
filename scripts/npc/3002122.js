/*  Von Leon (boss spawner)
    Audience room (211070100)
    Talkable NPC that spawns the boss.
 */

    var status;
    const ExpeditionType = Java.type('server.expeditions.ExpeditionType');
    var exped = ExpeditionType.LUCID;

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
                            cm.sendYesNo("Sparkles can be seen coming out of the misterious robe, would you like to reveal who is in there?");
                        }
                    } else if (status == 1) {
                            cm.getMap().spawnMonsterOnGroundBelow(8880150, 1059, 48);
                            cm.spawnMonster(8880172, 1500, 48);
                            cm.spawnMonster(8880165, 420, 48); 
                            cm.getMap().destroyNPC(3002122);
                            cm.dispose();
                    }
    
            }
    }