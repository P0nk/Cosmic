/* NPC: Donation Box (9000041)
	Victoria Road : Henesys

	NPC Bazaar:
        * @author Ronan Lana
	* Then by PumpkinPie
*/

const InventoryType = Java.type('client.inventory.InventoryType');
const YamlConfig = Java.type('config.YamlConfig');

let options = ["EQUIP","USE","SET-UP","ETC"];
let status;
let invType = 0;
let inv;
let x = 0;
let first = 1, last = 96;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    status++;
    if (mode !== 1) {
        cm.dispose();
        return;
    }

    if (status === 0) {
        // if (!YamlConfig.config.server.USE_ENABLE_CUSTOM_NPC_SCRIPT) {
        //     cm.sendOk("The medal ranking system is currently unavailable...");
        //     cm.dispose();
        //     return;
        // }

        let selStr = "Hello, I am the #bDonation Box#k! I can clear your inventory of junk or glitched items by selling them all. ";
        for (let i = 0; i < options.length; i++) {
            selStr += "\r\n#L" + i + "# " + options[i] + "#l";
        }

        cm.sendSimple(selStr);
    }

    else if (status === 1) {
        invType = InventoryType.getByType(selection + 1);
        inv = cm.getPlayer().getInventory(invType);
        let format = 0;
        let fStr = "Which slot in your inventory do you want to start selling from?\r\n";
        for(x; x <= last; x++) {
            if(inv.getItem(x) != null){
                fStr += "#L"+x+"##i" + inv.getItem(x).getItemId() + "#\t";
                format++;
                if ( format % 4 === 0) fStr+="\r\n";
            }
        }
        cm.sendSimple(fStr);
    }

    else if (status === 2){
        first = selection;
        let format = 0;
        let lStr = "Which slot in your inventory do you want to stop selling at?\r\n";
        for(x; x >= first+1; x--) {
            if (inv.getItem(x) != null) {
                lStr += "#L"+x+"##i" + inv.getItem(x).getItemId() + "#\t";
                format++;
                if(format % 4 === 0) lStr+="\r\n";
            }
        }
        cm.sendSimple(lStr);
    }

    else if (status === 3) {
        last = selection;
        const res = cm.getPlayer().sellAllPosLast(invType, first, last);

        if (res > -1) {
            cm.sendOk("Transaction complete! You received #r" + cm.numberWithCommas(res) + " mesos#k from this action.");
        } else {
            cm.sendOk("There was an error in your message, please try again.");
        }

        cm.dispose();
    }
}