// botmonitor.js
// put in /scripts/npc/botmonitor.js and hook an NPC to it

var status = 0;
var targets = [];  // will hold the Character objects we can warp to

function start() {
    status = 0;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode != 1) {
        cm.dispose();
        return;
    }
    if (status == 0) {
        targets = [];
        var me = cm.getPlayer();
        var text = "📋 Select a player to warp to:\r\n";

        // get everyone in YOUR channel
//        var chars = me.getClient()
//                      .getChannelServer()
//                      .getPlayerStorage()
//                      .getAllCharacters();

        // Grab every channel in your current world
        var channels = cm.getChannels();

        for (var i = 0; i < channels.size(); i++) {
            var ch = channels.get(i);
            var chars = ch.getPlayerStorage().getAllCharacters();
            for (var j = 0; j < chars.size(); j++) {
                var chr = chars.get(j);
                // skip GMs and yourself
                if (!chr.isGM() && chr.getId() !== me.getId()) {
                    // stash for the selection handler
                    targets.push({ chr: chr, channel: ch.getId() });
                    // build a line like: [2] CoolPlayer – Henesys
                    text += "#L" + (targets.length - 1) + "#"
                         + "[" + ch.getId() + "] "
                         + chr.getName()
                         + " - "
                         + chr.getMap().getMapName()
                         + "#l\r\n";
                }
            }
        }

        if (targets.length == 0) {
            cm.sendOk("There are no other players in your channel right now.");
            cm.dispose();
        } else {
            cm.sendSimple(text);
            status++;
        }

    } else if (status == 1) {
        var sel = targets[selection];
        var targetChr = sel.chr;
        var targetChan = sel.channel;


        // If we're not already in the target's channel, switch first
        if (cm.getClient().getChannel() !== targetChan) {
            cm.sendOk("Switching you to channel " + targetChan + "…");
            cm.getClient().changeChannel(targetChan);
            cm.sleep(3000);
            try {
                for (var i = 0; i < 7; i++) {   // poll for a while until the player reconnects
                    if (cm.getPlayer().isLoggedinWorld()) {
                      break;
                    }
                    Thread.sleep(1777);
                }
            } catch (Error) {
            }

            var targetMap  = targetChr.getMap().getId();
            cm.getPlayer().dropMessage("Warping you to "
                               + targetChr.getName()
                               + " at "
                               + targetChr.getMap().getMapName()
                               + ".");
            cm.warp(targetMap);
//            action(1,0,selection)
            // after this your client will reconnect; you can then re-open the NPC if needed
        } else {
          // same channel: just warp straight there
          var targetMap  = targetChr.getMap().getId();
          cm.getPlayer().dropMessage("Warping you to "
                   + targetChr.getName()
                   + " at "
                   + targetChr.getMap().getMapName()
                   + ".");
          cm.warp(targetMap);
        }
        cm.dispose();
    }
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}