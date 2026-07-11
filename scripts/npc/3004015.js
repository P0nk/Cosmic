var status = -1; 
var selectionChoices = [];

function start() {
    selectionChoices = getSelectionChoices();
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else {
        status--;
    }

    if (status == 0) {
        cm.sendOk(`The following options will show your expedition attempts.\r\n${generateMenu(selectionChoices)}`);
    } else {
        cm.dispose();
    }
}

function generateMenu(array) {
    var menu = "";
    for (var i = 0; i < array.length; i++) {
        menu += array[i] + "#l\r\n";
    }
    return menu;
}

function getSelectionChoices() {
    var choices = ["#eDaily Bosses", " "]; // Start with the daily header

    // Define the desired order of bosses
    var bossOrder = [
        "PAPULATUS",
        "SCARGA",
        "ZAKUM",
        "KREXEL",
        "CASTELLAN",
        "HORNTAIL",
        "PINKBEAN",
        "VONLEON",
        "CYGNUS",
        "NORMAL_LUCID",
        "NORMAL_WILL",
        "NORMAL_VERUS",
        "NORMAL_DARKNELL",
        "HARD_LUCID",
        "HARD_WILL",
        "HARD_VERUS",
        "HARD_DARKNELL",
        "MAGNUS"
    ];

    // Create the menu based on the defined order
    var menuItems = bossOrder.map((bossKey) => {
        var boss = BossLog.Bosses[bossKey];
        var attempts = cm.getBossLogEntries(boss.id - 1);
        var maxAttempts = boss.maxAttempts;
        var entryText;

        if (attempts >= maxAttempts) {
            entryText = `#d${boss.name} #e[COMPLETED]`;
        } else {
            entryText = `#r${boss.name}#k#b ${attempts} #kout of #b${maxAttempts} #kattempts.`;
        }

        return entryText;
    });

    // Add the menu items to the choices
    choices = choices.concat(menuItems);
    return choices;
}



var BossLog = {
    Bosses: {
        ZAKUM: { name: "#fItem/Consume/0238.img/02388023/info/iconRaw# ZAKUM", maxAttempts: 25, type: "daily", id: 1 },
        HORNTAIL: { name: "#fItem/Consume/0238.img/02388024/info/iconRaw# HORNTAIL", maxAttempts: 20, type: "daily", id: 2 },
        PINKBEAN: { name: "#fItem/Consume/0238.img/02388043/info/iconRaw# PINKBEAN", maxAttempts: 20, type: "daily", id: 3 },
        SCARGA: { name: "#fItem/Consume/0238.img/02388023/info/icon# SCARGA", maxAttempts: 10, type: "daily", id: 4 },
        PAPULATUS: { name: "#fItem/Consume/0238.img/02388022/info/iconRaw# PAPULATUS", maxAttempts: 30, type: "daily", id: 5 },
        VONLEON: { name: "#fItem/Consume/0238.img/02388146/info/iconRaw# VONLEON", maxAttempts: 18, type: "daily", id: 6 },
        CYGNUS: { name: "\r\n\r\n#fItem/Consume/0238.img/02388152/info/iconRaw# CYGNUS", maxAttempts: 16, type: "daily", id: 7 },
        HARD_WILL: { name: "#fItem/Consume/0238.img/02388161/info/iconRaw# HARD WILL", maxAttempts: 8, type: "daily", id: 8 },
        HARD_VERUS: { name: "#fItem/Consume/0238.img/02388163/info/iconRaw# HARD VERUS", maxAttempts: 8, type: "daily", id: 9 },
        HARD_DARKNELL: { name: "#fItem/Consume/0238.img/02388162/info/iconRaw# HARD DARKNELL", maxAttempts: 8, type: "daily", id: 10 },
        KREXEL: { name: "#fItem/Consume/0238.img/02388023/info/icon# KREXEL", maxAttempts: 15, type: "daily", id: 11 },
        CASTELLAN: { name: "#fItem/Consume/0238.img/02388159/info/iconRaw# CASTELLAN", maxAttempts: 20, type: "daily", id: 12 },
        HARD_LUCID: { name: "\r\n\r\n#fItem/Consume/0238.img/02388160/info/iconRaw# HARD LUCID", maxAttempts: 8, type: "daily", id: 13 },
        NORMAL_LUCID: { name: "#fItem/Consume/0238.img/02388160/info/iconRaw# NORMAL LUCID", maxAttempts: 12, type: "daily", id: 14 },
        NORMAL_WILL: { name: "#fItem/Consume/0238.img/02388161/info/iconRaw# NORMAL WILL", maxAttempts: 12, type: "daily", id: 15 },
        NORMAL_VERUS: { name: "#fItem/Consume/0238.img/02388163/info/iconRaw# NORMAL VERUS", maxAttempts: 12, type: "daily", id: 16 },
        NORMAL_DARKNELL: { name: "#fItem/Consume/0238.img/02388162/info/iconRaw# NORMAL DARKNELL", maxAttempts: 12, type: "daily", id: 17 },
        MAGNUS: { name: "#fItem/Consume/0238.img/02388164/info/iconRaw# MAGNUS", maxAttempts: 8, type: "daily", id: 18 }
    }
};
