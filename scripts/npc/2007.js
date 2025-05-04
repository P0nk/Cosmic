function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.sendNext("Welcome to your new adventure! Here's a trusty wooden bat to get you started. Make friends, join a guild, and most importantly—play fair! If you ever run into any issues, feel free to reach out to me anytime. Enjoy your journey, and happy Mapling!");
        cm.gainItem(1402009, 1);
        cm.dispose();
    } else {
        if (status == 0 && mode == 0) {
            cm.sendNext("Welcome to your new adventure! Here's a trusty wooden bat to get you started. Make friends, join a guild, and most importantly—play fair! If you ever run into any issues, feel free to reach out to me anytime. Enjoy your journey, and happy Mapling!");
            cm.gainItem(1402009, 1);
            cm.dispose();
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }
        if (status == 0) {
            cm.sendYesNo("Hi there! I'm Merogie — thanks for playing on our server! Would you like to skip the tutorials and head straight to Lith Harbor? I can send you there right away, though it’s not really recommended for new adventurers!");



        } else if (status == 1) {
            cm.warp(104000000, 0);
            cm.dispose();
        }
    }
}