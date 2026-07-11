/* Nimble: Master Forger
 */

load("scripts/lib.js")
load("scripts/npc/interactions/common/base.js")

const MenuOption = {
    StringMap: {
        0 : "Crafting Equipment",
        1 : "Refining Minerals",
        2 : "Weapon Crafting"
    },
    CraftingEquipment: 0,
    RefiningMinerals: 1,
    WeaponCrafting: 2
}

const selectionMenuOptions = [ MenuOption.CraftingEquipment, MenuOption.RefiningMinerals ];

let currentInteraction;
let status;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (cm.isEndChat(mode, type)) {
        cm.dispose();
        return;
    }

    if (mode === 1) {
        status++;
    } else {
        status--;
    }

    if (status === 0) {
        let selStr = "#eYo! Im Nimble and welcome to my forge!#n\r\n\r\nI can craft you a wide variety of "
        selStr += "equipment and refine raw minerals! Of course, you would have to provide me with the "
        selStr += "#r#erequired materials#n#k.#b#e";

        for (let option of selectionMenuOptions) {
            selStr += `\r\n#L${option}# ${MenuOption.StringMap[option]}#l`;
        }

        cm.sendSimple(selStr);
        return;
    } else if (status === 1) {
        switch (selection) {
            case MenuOption.CraftingEquipment:
                load('scripts/npc/interactions/CraftingEquipment.js');
                currentInteraction = new CraftingEquipmentInteraction(cm);
                break;
            case MenuOption.RefiningMinerals:
                load('scripts/npc/interactions/RefiningMaterials.js');
                currentInteraction = new RefiningMineralsInteraction(cm);
                break;
            case MenuOption.WeaponCrafting:
                load('scripts/npc/interactions/WeaponCrafting.js');
                currentInteraction = new RefiningMineralsInteraction(cm);
                break;
            default:
                cm.logToConsole(`Unknown selection: ${selection}`);
                cm.dispose();
                return;
        }
    }

    interactOrDispose(mode, type, selection);
}

function interactOrDispose(mode, type, selection) {
    if (currentInteraction && status) {
        let result = currentInteraction.action(mode, type, selection, status);

        switch (result) {
            case ScriptInteractionResult.NoHandler:
                cm.logToConsole("Interaction Failed: No handler found for status.")
                cm.dispose();
                return;
            case ScriptInteractionResult.HandlerError:
                cm.logToConsole(`Interaction Failed: An error occurred while executing a handler for status ${status}`)
                cm.dispose();
                return;
            case ScriptInteractionResult.Dispose:
                cm.dispose();
                return;
        }

        status = result;
    } else {
        cm.dispose();
    }
}