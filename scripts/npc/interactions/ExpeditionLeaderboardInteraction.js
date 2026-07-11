class ExpeditionLeaderboardInteraction {
    constructor(cm) {
        this.cm = cm;
        this.handlerRegistry = new InteractionHandlerRegistry();

        this.leaderboardTypeOptions = [ LeaderboardType.Global, LeaderboardType.Personal ];
        this.expeditionBossOptions = [ BossOption.PAPULATUS, BossOption.ZAKUM, BossOption.SCARGA, BossOption.KREXEL, BossOption.CASTELLAN,
                                       BossOption.HORNTAIL, BossOption.PINKBEAN, BossOption.VONLEON, BossOption.CYGNUS, BossOption.NLUCID,
                                       BossOption.NWILLSPIDER, BossOption.NDARKNELL, BossOption.NVERUS, BossOption.LUCID, BossOption.WILLSPIDER,
                                       BossOption.DARKNELL, BossOption.VERUS, BossOption.MAGNUS];

        this.ExpeditionLeaderboard = Java.type("server.expeditions.ExpeditionLeaderboard");
        this.BossLogEntry = Java.type("server.expeditions.ExpeditionBossLog.BossLogEntry");

        this.selectedLeaderboardTypeOption = null;
        this.selectedBossOption = null;

        this.lastStatus = null;

        this.initializeHandlers();
    }

    initializeHandlers() {
        this.handlerRegistry.register(1, null, this.showLeaderboardTypeSelectionMenu.bind(this));

        this.handlerRegistry.register(2, null, this.showBossSelectionMenu.bind(this));

        this.handlerRegistry.register(3, LeaderboardType.Global, this.showGlobalLeaderboard.bind(this));
        this.handlerRegistry.register(3, LeaderboardType.Personal, this.showPersonalLeaderboard.bind(this));
    }

    action(mode, type, selection, status) {
        if (this.cm.isEndChat(mode, type)) {
            return ScriptInteractionResult.Dispose;
        }

        try {
            if (status === 2) {
                if (this.lastStatus > status) {
                    this.selectedLeaderboardTypeOption = this.selectedLeaderboardTypeOption || selection;
                } else {
                    this.selectedLeaderboardTypeOption = selection;
                }
            }

            const handler = this.handlerRegistry.getHandler(status, this.selectedLeaderboardTypeOption);
            return handler.call(this, mode, type, selection, status);
        } catch (error) {
            this.cm.logToConsole(`[ExpeditionLeaderboardInteraction] HandlerError: ${error}`);
            return ScriptInteractionResult.HandlerError;
        }
    }

    showLeaderboardTypeSelectionMenu(mode, type, selection, status) {
        // set null to allow a new selection
        this.selectedLeaderboardTypeOption = null;
        let selectionString = "Select the type of leaderboard you would like to view.\r\n\r\n"

        for (let option of this.leaderboardTypeOptions) {
            selectionString += `#L${option}# ${LeaderboardType.StringMap[option]}\r\n`;
        }

        this.cm.sendSimple(selectionString);
        return status;
    }

    showBossSelectionMenu(mode, type, selection, status) {
        let selectionString = "Select the boss.\r\n\r\n";

        for (let option of this.expeditionBossOptions) {
            let bossName = BossOption.StringMap[option].replace("_", " "); // Replace underscores with spaces
            selectionString += `#L${option}# ${bossName}\r\n`;
        }

        this.cm.sendSimple(selectionString);
        return status;
    }

    showGlobalLeaderboard(mode, type, selection, status) {
        this.selectedBossOption = selection;

        let bossName = BossOption.StringMap[this.selectedBossOption].replace("_", " "); // Replace underscores with spaces
        const leaderboardRecords =
            this.ExpeditionLeaderboard.getGlobalLeaderboardStringForBoss(
                this.BossLogEntry.getBossEntryByName(bossName));

        this.cm.sendPrev(leaderboardRecords);
        return --status;
    }

    showPersonalLeaderboard(mode, type, selection, status) {
        this.selectedBossOption = selection;

        let bossName = BossOption.StringMap[this.selectedBossOption].replace("_", " "); // Replace underscores with spaces
        const leaderboardRecords =
            this.ExpeditionLeaderboard.getPersonalLeaderboardStringForBoss(this.cm.getPlayer().getId(),
                this.BossLogEntry.getBossEntryByName(bossName));

        this.cm.sendPrev(leaderboardRecords);
        return --status;
    }
}

const LeaderboardType = {
    StringMap: {
        1 : "Global",
        2 : "Personal",
    },
    Global: 1,
    Personal: 2
}

const BossOption = {
    StringMap: {
        1 : "PAPULATUS",
        2 : "ZAKUM",
        3 : "SCARGA",
        4 : "KREXEL",
        5 : "CASTELLAN",
        6 : "HORNTAIL",
        7 : "PINKBEAN",
        8 : "VONLEON",
        9 : "CYGNUS",
        10 : "NLUCID",
        11 : "NWILLSPIDER",
        12 : "NDARKNELL",
        13 : "NVERUS",
        14 : "LUCID",
        15 : "WILLSPIDER",
        16 : "DARKNELL",
        17 : "VERUS",
        18 : "MAGNUS"
    },
        PAPULATUS: 1,
        ZAKUM: 2,
        SCARGA: 3,
        KREXEL: 4,
        CASTELLAN: 5,
        HORNTAIL: 6,
        PINKBEAN: 7,
        VONLEON: 8,
        CYGNUS: 9,
        NLUCID: 10,
        NWILLSPIDER: 11,
        NDARKNELL: 12,
        NVERUS: 13,
        LUCID: 14,
        WILLSPIDER: 15,
        DARKNELL: 16,
        VERUS: 17,
        MAGNUS: 18
}
