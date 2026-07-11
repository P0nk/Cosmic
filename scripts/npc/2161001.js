var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0) {
            if (cm.isQuestStarted(3173)
            || cm.isQuestStarted(3174)
            || cm.isQuestStarted(3175)
            || cm.isQuestStarted(3176)) {
                cm.warp(211070000, "west00");
                cm.dispose();
            } else {
                cm.sendOk("Please help me...");
                cm.dispose();
                return;
            }
        }
    }
}
