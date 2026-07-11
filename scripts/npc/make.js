//Thanks for helping out Chronos! 



let status = 0;
let stats;
let id;
let keys;
let tempSel;

function start() {
    if (cm.getPlayer().gmLevel() < 3) {
        cm.sendOk("#r#eSwiggity swooty I'm coming for that booty!");
        cm.dispose();
        return;
    }
    cm.sendGetNumber(cm.letters("Stat Editor") + "\r\n\r\nProvide a valid equip id:", 1302000, 1000000, 1999999);
}

function action(m, t, s) {
    if (m !== 1) {
        cm.dispose();
    } else {
        if (id === undefined) {
            id = s;
        }
        if (!cm.isEquip(id)) {
            cm.sendOk(cm.letters("Error") + "\r\n\r\nThe id you entered is not an equip!\r\nYour input was: #r" + id + "\r\n#kPotential item name: #r#z" + id);
            cm.dispose();
            return;
        } else if (stats === undefined) {
            initStats(cm.getEquipById(id));
        }

        status++;
        if (status === 1) {
            let str = "Selected equip: #i" + id + "# #r#z" + id + "# #k(#b" + id + "#k)\r\n";
            for (let i = 0; i < keys.length; i++) {
                str += "#L" + i + "#" + keys[i] + ": #r" + stats[keys[i]] + "#k#l\r\n";
            }
            str += "\r\n#L100##r#eCreate item!#l";
            cm.sendSimple(str);
        } else if (status === 2) {
            if (s !== 100) {
                tempSel = s;
                cm.sendGetNumber(cm.letters(keys[s]) + "\r\n\r\nNew value:", stats[keys[s]], 0, 32767);
            } else {
                createItem();
                cm.sendOk("Swiggity swooty I'm coming for that booty!");
                cm.dispose();
            }
        } else if (status === 3) {
            stats[keys[tempSel]] = s;
            status = 0;
            action(1, 0, 0);
        }
    }
}

function initStats(equip) {
    stats = {
        "Str": equip.getStr(),
        "Dex": equip.getDex(),
        "Luk": equip.getLuk(),
        "Int": equip.getInt(),
        "Watk": equip.getWatk(),
        "Matk": equip.getMatk(),
        "Acc": equip.getAcc(),
        "Avo": equip.getAvoid(),
        "Jump": equip.getJump(),
        "Speed": equip.getSpeed(),
        "Wdef": equip.getWdef(),
        "Mdef": equip.getMdef(),
        "HP": equip.getHp(),
        "MP": equip.getMp(),
        "Slots": equip.getUpgradeSlots()
    };
    keys = Object.keys(stats);
}

function createItem() {
    cm.gainStatItem(
        id, // itemid
        stats["Str"], // str
        stats["Dex"], // dex
        stats["Luk"], // luk
        stats["Int"], // int
        stats["Matk"], // matk
        stats["Watk"], // watk
        stats["Acc"], // acc
        stats["Avo"], // avo
        stats["Jump"], // jump
        stats["Speed"], // speed
        stats["Wdef"], // wdef
        stats["Mdef"], // mdef
        stats["HP"], // hp
        stats["MP"], // mp
        stats["Slots"] // slots
    );
}