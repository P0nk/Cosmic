/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
    TODO: Move current exp rate change to event script move out of item
    TODO: Make the other buffs controlled there too
    TODO: Remember all participants of a event
 */

var status;
var itemId = 2430033;
var npcId = 9906601;
var mesoCost = 25000000;

function start() {
    if (!verifyCanUseTotem())
    {
        im.dispose();
        return;
    }

    im.sendYesNo(`Are your sure you want use the item? It will cost you ${mesoCost} mesos.`, npcId);
}

function action(mode, type, selection) {
    if (mode > 0) {
        if (!verifyCanUseTotem())
        {
            im.dispose();
            return;
        }

        const player = im.getPlayer();
        const playerMesos = player.getMeso();

        if (playerMesos < mesoCost) {
            player.dropMessage(1, `You do not have enough mesos to use this item! (${mesoCost} mesos)`);
            im.dispose();
            return;
        }

        player.gainMeso(-mesoCost, false);
        im.removeOne(itemId);

        im.spawnNpc(npcId);
        im.getMap().setExpRateMultiplier(3);
        im.getEventManager("SpawnTotemEvent").startTotemInstance(im.getPlayer(), npcId);
        im.dispose();
    } else {
        im.dispose();
    }
}

function verifyCanUseTotem()
{
    const player = im.getPlayer();
    const map = player.getMap();

    if (map.getEventInstance()) {
        player.dropMessage(1, "This item cannot be used. (Map is already a participant of another event instance.)");
        return false;
    }

    if (player.getEventInstance()) {
        player.dropMessage(1, "This item cannot be used. (Player is already a participant of another event instance.)");
        return false;
    }

    if (player.totemIsCooling(npcId)) {
        const remainingTimeMs = player.getTotemCooldownTimeRemaining(npcId);
    
        const remainingSeconds = Math.floor(remainingTimeMs / 1000);
        const minutes = Math.floor(remainingSeconds / 60);
        const seconds = remainingSeconds % 60;
    
        player.dropMessage(1, `This item cannot be used. (Cooldown is active, ${minutes} minutes and ${seconds} seconds remaining.)`);
        return false;
    }

    return true;
}

// if (mode == -1) {
//     im.dispose();
// } else {
//     if (mode == 0 && type > 0) {
//         im.dispose();
//         return;
//     }
//     if (mode == 1) {
//         status++;
//     } else {
//         status--;
//     }
//
//     if (status == 0) {
//         if (im.getMapId() == 106020300) {
//             var portal = im.getMap().getPortal("obstacle");
//             if (portal != null && portal.getPosition().distance(im.getPlayer().getPosition()) < 210) {
//                 if (!(im.isQuestStarted(100202) || im.isQuestCompleted(100202))) {
//                     im.startQuest(100202);
//                 }
//                 im.removeAll(2430014);
//
//                 im.message("You have used the Killer Mushroom Spore to open the way.");
//             }
//         }
//
//         im.dispose();
//     }
// }