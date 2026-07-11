/* Nimble the Forger
	Maxcloud, Seeker1437
 */
load('scripts//lib.js');
load('scripts/npc/lib/ItemEnhancer.js');

const ItemInformationProvider = Java.type('server.ItemInformationProvider');
const ItemConstants = Java.type('constants.inventory.ItemConstants');
const InventoryType = Java.type('client.inventory.InventoryType');
const Stream = Java.type('java.util.stream.Stream');
const Collectors = Java.type('java.util.stream.Collectors')

// Global variables
let status;
let option_select;

// Enhancing Equipment variables
let selected_recipe_items = [];
let selected_crystals = [];
let selected_enhancement_item = null;
let eligibleItems = [];
let eligibleCrystalItemIds = [];

let selected_set;
let selected_material;
let amount;
let clazz = ["Beginner", "Warrior", "Magician", "Bowman", "Thief", "Pirate", "Aran"];
let clazz_sets = ["Warrior Gear Set", "Magician Gear Set", "Bowman Gear Set", "Thief Gear Set", "Pirate Gear Set"];

// material selection
let item;
let mats;
let qty;
let cost;

// total materials amount
let total_amount;

const raw_materials = getJson('raw_materials.json');
const gear_sets = getJson('gear_sets.old.json');

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === 1) {
        status ++;
    } else {
        status--;
    }
    if (mode === 0) {
        cm.dispose();
        return;
    }

    let selStr = ""

    if (status === 0) {
        //cm.getPlayer().setCS(true);
        selStr += "#eYo! I am Nimble and welcome to my forge!#n\r\n\r\nI can craft you a wide variety of "
        selStr += "equipment and refine raw minerals! Of course, you would have to provide me with the "
        selStr += "#r#erequired materials#n#k.#b#e";
        const options = ["Crafting Equipment", "Refining Minerals"];
        for (let i = 0; i < options.length; i++)
            selStr += "\r\n#L" + i + "# " + options[i] + "#l";

        cm.sendSimple(selStr);
    } else if (status === 1) {
        option_select = selection;

        let selStr = "";
        let options  = [];

        switch (option_select) {
            case MenuOption.CraftingEquipment:
                selStr += "My selection of armour, weaponry and other accessories are fit for all types of combat!#b#e";
                options = ["Set Gear", "Other Accessories"];
                for (let i = 0; i < options.length; i++)
                    selStr += "\r\n#L" + i + "# " + options[i] + "#l";

                cm.sendSimple(selStr);
                break;
            case MenuOption.RefiningMinerals:
                selStr += "Let me take a look at what minerals you have...#b#e";
                for (let i = 0; i < raw_materials.length; i++) {
                    const item_id = raw_materials[i].item;
                    selStr += "\r\n#L" + i + "# #i" + item_id + "# #z" + item_id + "##l";
                }

                cm.sendSimple(selStr);
                break;
            case MenuOption.EnhancingEquipment:
                const player = cm.getPlayer();
                const ii = ItemInformationProvider.getInstance();

                options = [];
                const playerInventory = player.getInventory(InventoryType.EQUIP).list();

                const eligibleItemsStream = playerInventory.stream().filter((item) => {
                    const itemId = item.getItemId();
                    const equipLevelRequirement = ii.getEquipLevelReq(itemId);

                    return equipLevelRequirement >= 150;
                });

                eligibleItems = eligibleItemsStream.collect(Collectors.toList());

                if (eligibleItems.length === 0) {
                    selStr = "You do not have any eligible items to enhance at this time.";
                    cm.sendOk(selStr);
                    break;
                }

                selStr = "Keep in mind I can only enhance equipment in your inventory that are #r#eover Lv. 150#k#n!#b#e";

                let itemIndex = 0;
                eligibleItems.forEach((element) => {
                    const itemId = element.getItemId();

                    selStr += "\r\n#L" + itemIndex + "# #i" + itemId + "# #z" + itemId + "##l";
                    itemIndex += 1;
                });

                cm.sendSimple(selStr);
                break;
        }
    } else if (status === 2) {
        let selStr = "";

        switch (option_select) {
            case MenuOption.CraftingEquipment:
                switch (selection) {
                    case CraftingMenuOption.SetGear:
                        selStr += "I have unique equipment per class!#b#e";
                        for (let i = 0; i < clazz_sets.length; i++)
                            selStr += "\r\n#L" + i + "# #z" + clazz_sets[i] + "##l";

                        cm.sendSimple(selStr);
                        break;

                    case CraftingMenuOption.OtherAccessories:
                        selStr += "Browse through my fine collection of accessories!#b#e"
                        let options = ["Pendants", "Medals", "Face Accessories", "Eye Accessories", "Ear Accessories", "Rings"];
                        for (let i = 0; i < options.length; i++)
                            selStr += "\r\n#L" + i + "# #z" + options[i] + "##l";

                        cm.sendSimple(selStr);
                        break;
                }
                break;

            case MenuOption.RefiningMinerals:
                selected_material = selection;
                item = raw_materials[selection].item;
                cm.sendGetNumber("How many #i"+item+"# #b#z"+item+"#(s)#k would you like to refine?", 1, 1, 1000);
                break;

            case MenuOption.EnhancingEquipment:
                getCrystalSelectionMenu(selection, status)
                break;
        }
    } else if (status === 3) {
        switch (option_select) {
            case MenuOption.CraftingEquipment:
                amount = selection;

                if (amount < 1 || amount > 1000) {
                    cm.sendOk("You must enter a valid quantity.");
                    cm.dispose();
                }
                break;

            case MenuOption.RefiningMinerals:
                item = raw_materials[selected_material].item;
                mats = raw_materials[selected_material].materials;
                qty = raw_materials[selected_material].quantity;
                cost = raw_materials[selected_material].cost;

                let selStr = "You require the following materials: \r\n\r\n";
                for (let i = 0; i < mats.length; i++) {
                    const item_id = mats[i][0];
                    const quantity = (mats[i][1] * amount);
                    selStr += " #i"+ item_id + "# #b#z"+ item_id + "##k (#rx" + cm.format(quantity) + "#k) \r\n";
                }
                selStr += "\r\nIt costs " + cm.format(cost * amount) + " mesos. Do you want to proceed?\r\n\r\n";
                selStr += "#fUI/UIWindow.img/QuestIcon/4/0\r\n\r\nYou will obtain #i" + item + "# #b"+cm.format(qty * amount)+" #b#z" + item + "#(s)#k."

                cm.sendYesNo(selStr);
                break;

            case MenuOption.EnhancingEquipment:
                switch (selection) {
                    case EnhanceNowOption:
                        // TODO: Handle result
                        try {
                            const itemEnhancer = new ItemEnhancer(cm, selected_enhancement_item, selected_recipe_items);
                            const result = itemEnhancer.tryEnhanceItem(selected_enhancement_item, selected_recipe_items);

                            switch (result) {
                                case CraftingResult.Success:
                                    cm.sendOk(CraftingResult.StringMap[result]);
                                    break;
                                case CraftingResult.OddItemFault:
                                    cm.sendOk(CraftingResult.StringMap[result]);
                                    break;
                                case CraftingResult.CreateFailed:
                                    cm.sendOk(itemEnhancer.createStatus !== undefined ? CreateStatus.Map[`${itemEnhancer.createStatus}`] : CraftingResult.StringMap[result]);
                                    break;
                            }
                        } catch (e) {
                            cm.logToConsole("" + e);
                            cm.dispose();
                        }

                        break;
                    case CancelEnhancementOption:
                        cm.sendOk("You have cancelled this operation.");
                        break;
                    default:
                        getCrystalSelectionMenu(selection, status);
                        status--;
                }
                break;
        }

    } else if (status === 4) {

        switch (option_select) {
            case MenuOption.CraftingEquipment:
                break;
            case MenuOption.RefiningMinerals:
                const total_quantity = (qty * amount);
                const total_cost = (cost * amount);

                selStr = "";
                let completed = true;
                if (cm.getMeso() < total_cost) {
                    selStr += "You do not have enough mesos."
                }

                selStr = "You are missing the following materials: \r\n\r\n";
                for (let i = 0; i < mats.length; i++) {
                    const item_id = mats[i][0];
                    const quantity = (mats[i][1] * amount);

                    if (!cm.haveItem(item_id, quantity)) {
                        completed = false;

                        let item_quantity = (cm.itemQuantity(item_id));
                        selStr += " #i"+ item_id + "# #b#z"+ item_id + "##k (#r" + cm.format(item_quantity) + "#k/#b" + cm.format(quantity) + ") \r\n";
                    }
                }

                if (!completed) {
                    cm.sendOk(selStr);
                    cm.dispose(); return;
                }

                for (let i = 0; i < mats.length; i++) {
                    const item_id = mats[i][0];
                    const quantity = (mats[i][1] * amount * -1);
                    cm.gainItem(item_id, quantity);
                }

                cm.gainItem(item, total_quantity);
                cm.gainMeso(cost * amount * -1);

                cm.sendOk("You have successfully refined " + cm.format(total_quantity) + " #i" + item + "# #b#z" + item + "#(s)#k.");
                cm.dispose();
                break;
            case MenuOption.EnhancingEquipment:
                cm.dispose();
                break;
        }
    }
}

const CraftingMenuOption = {
    StringMap: {
        0 : "Set Gear",
        1 : "Other Accessories",
    },
    SetGear: 0,
    OtherAccessories: 1
}

const MenuOption = {
    StringMap: {
        0 : "Crafting Equipment",
        1 : "Refining Minerals",
        2 : "Enhancing Equipment"
    },
    CraftingEquipment: 0,
    RefiningMinerals: 1,
    EnhancingEquipment: 2
}

const Crystals= {
    ItemMap: {
        0: [4250000, 4250001, 4250002],
        1: [4250100, 4250101, 4250102],
        2: [4250200, 4250201, 4250202],
        3: [4250300, 4250301, 4250302],
        4: [4250500, 4250501, 4250502],
        5: [4250400, 4250401, 4250402],
        6: [4250600, 4250601, 4250602],
        7: [4250700, 4250701, 4250702],
        8: [4251300, 4251301, 4251302],
        9: [4251400, 4251401, 4251402],
        10: [4250800, 4250801, 4250802],
        11: [4251100, 4251101, 4251102],
        12: [4250900, 4250901, 4250902],
        13: [4251000, 4251001, 4251002]
    },
    Diamond:0,
    Sapphire:1,
    Garnet:2,
    Opal:3,
    Aquamarine:4,
    Amethyst:5,
    Topaz:6,
    Emerald:7,
    BlackCrystal:8,
    DarkCrystal:9,
    PowerCrystal:10,
    DEXCrystal:11,
    WisdomCrystal:12,
    LUKCrystal:13
}

const EnhanceNowOption = 9999998;
const CancelEnhancementOption = 9999999;

function getCrystalSelectionMenu(selection, status) {
    switch (status) {
        case 2:
            selected_enhancement_item = eligibleItems[selection].getItemId();
            break;
        case 3:
            const selectedCrystalId = eligibleCrystalItemIds[selection];

            selected_recipe_items.push(selectedCrystalId);
            selected_crystals.push(getCrystalFromItemId(selectedCrystalId));
    }

    const isWeapon = ItemConstants.isWeapon(selected_enhancement_item);

    const selectedCrystalCount = selected_crystals.length;

    let crystalOptions = [ ];

    if (selectedCrystalCount !== 3) {
        let weaponOnlyCrystalOptions = [ Crystals.Diamond, Crystals.Sapphire ]

        crystalOptions = [ Crystals.DarkCrystal, Crystals.BlackCrystal,
            Crystals.PowerCrystal, Crystals.DEXCrystal, Crystals.WisdomCrystal,
            Crystals.LUKCrystal, Crystals.Garnet, Crystals.Opal, Crystals.Aquamarine,
            Crystals.Amethyst, Crystals.Topaz, Crystals.Emerald ]

        if (isWeapon) {
            crystalOptions.push(...weaponOnlyCrystalOptions);
        }

        crystalOptions = crystalOptions.filter(crystalOption => !selected_crystals.includes(crystalOption))
    }

    let selStr = "I can enhance your equipment for a fee in headpats! I mean.. mesos.";

    eligibleCrystalItemIds = [];

    crystalOptions.forEach((option) => {
        Crystals.ItemMap[option].forEach((itemId) => {
            eligibleCrystalItemIds.push(itemId);
        });
    });

    let crystalIndex = 0;
    eligibleCrystalItemIds.forEach((itemId) => {
        selStr += "\r\n#L" + crystalIndex + "# #i" + itemId + "# #z" + itemId + " ##l";

        crystalIndex++;
    });

    selStr += "\r\n\r\n#L9999998# Craft Now";
    selStr += "\r\n\r\n#L9999999# Cancel";

    cm.sendSimple(selStr)
}

function getCrystalFromItemId(itemId) {
    return parseInt(findKeyByValue(Crystals.ItemMap, itemId));
}

function findKeyByValue(object, value) {
    return Object.keys(object).find(key => object[key].includes(value));
}

// People would check the list of gear sets and then jobs

// click on the gear set to show all the equipments in the set
// then the user would select their class, to that specific set that was previously selected

