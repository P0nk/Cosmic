/*  Author: Rulax
	NPC Name: 		Chief Alex (2142001)
	Map(s): 		Henesys Ruins
	Description: 	Scouting the Stronghold
*/

var status = -1;

function start(mode, type, selection) {
    status++;
    if (mode != 1) {
        if (type == 1 && mode == 0) {
            status -= 2;
        } else {
            qm.sendOk("If Henesys falls, who will be left...?");
            qm.dispose();
            return;
        }
    }
    if (status == 0) {
        qm.sendAcceptDecline("I've heard reports of movement in the Cygnus stronghold. They must be up to something. Will you help us?");
    } else if (status == 1) {
        qm.sendOk("You can reach the stronghold through the Crossroads of Ereve. Go scout it out, but be careful!");
        qm.forceStartQuest();
    } else if (status == 2) {
        qm.dispose();
    }
}