/*  Author: Rulax
	NPC Name: 		Neinheart (2143001)
	Map(s): 		Secret Grove
	Description: 	Rescuing Neinheart
*/

var status = -1;

function end(mode, type, selection) {
    status++;
        qm.forceCompleteQuest(31146);
        qm.dispose();
        return;
}