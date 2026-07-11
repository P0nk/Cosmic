var status = 0;
var maps = [410000100];
var cost = [1000];
var selectedMap = -1;
var mesos;

function start() {
    cm.sendNext("Hmmmmm gods like us only care about one thing, and that is mesos...");
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (status == 1 && mode == 0) {
            cm.dispose();
            return;
        } else if (status >= 2 && mode == 0) {
            cm.sendNext("There's a lot to see in this town, too. Come back and find us when you need to go to a different town.");
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }
        if (status == 1) {
            var selStr = "";
            if (cm.getJobId() == 0) {
                selStr += "We have a special 90% discount for beginners.";
            }
            selStr += "To get inside the tree you gotta pay a fee!#b";
            for (var i = 0; i < maps.length; i++) {
                selStr += "\r\n#L" + i + "##m" + maps[i] + "# (" + (cm.getJobId() == 0 ? cost[i] / 10 : cost[i]) + " mesos)#l";
            }
            cm.sendSimple(selStr);
        } else if (status == 2) {
            cm.sendYesNo("I see, well going up #b#m" + maps[selection] + "##k? will cost you #b" + (cm.getJobId() == 0 ? cost[selection] / 10 : cost[selection]) + " mesos#k.");
            selectedMap = selection;
        } else if (status == 3) {
            if (cm.getJobId() == 0) {
                mesos = cost[selectedMap] / 10;
            } else {
                mesos = cost[selectedMap];
            }

            if (cm.getMeso() < mesos) {
                cm.sendNext("Your loss.");
                cm.dispose();
                return;
            }

            cm.gainMeso(-mesos);
            cm.warp(maps[selectedMap], 0);
            cm.dispose();
        }
    }
}