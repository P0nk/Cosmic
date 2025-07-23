var status;

function start() {
    const YamlConfig = Java.type('config.YamlConfig');
    if (YamlConfig.config.server.USE_ENABLE_CUSTOM_NPC_SCRIPT) {
        cm.dispose();
        cm.openNpc(9201088, "FaceEyeNPC");
        return;
    }

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
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0) {
            // do nothing
            cm.dispose();
        }
    }
}