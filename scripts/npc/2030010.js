/*
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/* Amon
 *
 * @Author Stereo
 * Adobis's Mission I : Breath of Lava <Level 1> (280020000)
 * Adobis's Mission I : Breath of Lava <Level 2> (280020001)
 * Last Mission : Zakum's Altar (280030000)
 * Zakum Quest NPC
 * Helps players leave the map and reset reactors (only if Zakum is dead)
 */
var status = 0;
var zakumAlive = false;

function start() {
    zakumAlive = cm.getPlayer().getMap().getMonsterById(8800000) !== null;

    if (cm.getMapId() === 280030000 || (cm.getMapId() >= 280030100 && cm.getMapId() <= 280030130)) {
        if (zakumAlive) {
            cm.sendSimple(
                "Zakum is still alive.\r\nWhat would you like to do?\r\n" +
                "#b#L0#Leave the map#l"
            );
        } else {
            cm.sendSimple(
                "Zakum has been defeated.\r\nWhat would you like to do?\r\n" +
                "#b#L0#Leave the map#l\r\n" +
                "#L1#Let me spawn Zakum again#l"
            );
        }
    } else {
        cm.sendYesNo("If you leave now, you'll have to start over. Are you sure you want to leave?");
    }
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }

    var mapId = cm.getPlayer().getMapId();

    // Handle non-Zakum maps (just leave)
    if (mapId !== 280030000 && (mapId < 280030100 || mapId > 280030130)) {
        cm.getPlayer().getClient().getChannelServer().removeMiniDungeon(mapId);
        cm.warp(211042300, 0);
        cm.dispose();
        return;
    }

    if (selection === 0) {
        player = cm.getPlayer();
        map = player.getMap();
        players = map.countPlayers();
        channel = player.getClient().getChannelServer()
        if(players <= 1 || player.isPartyLeader()) {
            channel.removeMiniDungeon(mapId);
            map.clearMapObjects();
            map.warpEveryone(211042300, 0);
        } else {
            cm.warp(211042300, 0);
        }
        cm.dispose();
    } else if (selection === 1) {
        // Attempt to reset reactors only if Zakum is dead
        zakumAlive = cm.getPlayer().getMap().getMonsterById(8800000) !== null;
        if (zakumAlive) {
            cm.sendOk("You cannot reset reactors while Zakum is still alive.");
        } else {
            cm.getPlayer().getMap().resetReactors();
            cm.sendOk("You can drop an Eye of Fire again.");
        }
        cm.dispose();
    }
}
