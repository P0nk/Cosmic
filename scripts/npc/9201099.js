/**
 * 9201098 - Mo
 * @author Ronan
 * @author Ponk
 */

let status = 0;
let selectedItem = undefined;

/*
References:
- https://www.youtube.com/watch?v=g6y2zmCGglI
- https://www.youtube.com/watch?v=CttmlVWLJKM
*/
function start() {
    if (cm.getQuestStatus(8224) !== 2) {
        cm.sendDefault();
        cm.dispose();
        return;
    }

    cm.sendSimple("Name's Mo. I've got Mo' items for Mo' mesos. What business do you bring me?\r\n#L0##bI'd like to buy some items#k");
}

function action(action, type, selection) {
    if (!action) {
        cm.dispose();
        return;
    }

    if (status === 0) {
        let index = 0;
        const selections = "#e" + shopItems()
            .map(i => {
                const mesoText = i.quantity === 1 ? "meso" : `meso per ${i.quantity} arrows`;
                return `\r\n#L${index++}##i${i.itemId}# #z${i.itemId}# #b${i.cost} ${mesoText}#k`;
            })
            .join("");
        cm.sendSimple("An ally of the Raven Ninja Clan is welcome to buy from me!" + selections);
        status++;
    } else if (status === 1 && selection !== -1) {
        selectedItem = shopItems()[selection];
        cm.sendAcceptDecline("Are you sure you want to buy it?");
        status++;
    } else if (status === 2) {
        if (!selectedItem) {
            cm.dispose();
            return;
        }
        if (!cm.hasMeso(selectedItem.cost)) {
            cm.sendOk("You don't have enough mesos.");
            cm.dispose();
            return;
        }
        if (!cm.canHold(selectedItem.itemId, selectedItem.quantity)) {
            cm.sendOk("There's no room in your inventory.");
            cm.dispose();
            return;
        }

        cm.loseMeso(selectedItem.cost);
        cm.gainItem(selectedItem.itemId, selectedItem.quantity);
        cm.dispose();
    }
}

function shopItems() {
    return [
        {itemId: 2050004, quantity: 1, cost: 400}, // All-Cure Potion
        {itemId: 2050000, quantity: 1, cost: 200}, // Antidote
        {itemId: 2020012, quantity: 1, cost: 4500}, // Melting Cheese
        {itemId: 2020013, quantity: 1, cost: 5000}, // Reindeer Milk
        {itemId: 2020014, quantity: 1, cost: 8100}, // Sunrise Dew
        {itemId: 2020015, quantity: 1, cost: 9690}, // Sunset Dew
        {itemId: 2050001, quantity: 1, cost: 200}, // Eyedrop
        {itemId: 2050002, quantity: 1, cost: 300}, // Tonic
        {itemId: 2050003, quantity: 1, cost: 500}, // Holy Water
        {itemId: 2022000, quantity: 1, cost: 1650}, // Pure Water
        {itemId: 2002017, quantity: 1, cost: 5000}, // Warrior Elixir
        {itemId: 2060004, quantity: 2000, cost: 40_000}, // Diamond Arrow for Bow
        {itemId: 2061004, quantity: 2000, cost: 40_000}, // Diamond Arrow for Crossbow
        {itemId: 2070010, quantity: 1, cost: 2000}, // Icicle
        {itemId: 2022003, quantity: 1, cost: 1100}, // Unagi
        {itemId: 2000006, quantity: 1, cost: 620}, // Mana Elixir
        {itemId: 2022002, quantity: 1, cost: 1000}, // Cider
        {itemId: 2030020, quantity: 1, cost: 400}, // Return to New Leaf City Scroll
    ]
}
