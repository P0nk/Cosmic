load('scripts/npc/lib/ItemCrafter.js');

class MakerCraftingMenuSubInteraction {
    constructor(cm) {
        this.cm = cm;
        this.handlerRegistry = new InteractionHandlerRegistry();
        this.startingStatus = 0;

        this.MakerItemFactory = Java.type('server.MakerItemFactory');
        this.ItemInformationProvider = Java.type('server.ItemInformationProvider');
        this.ii = this.ItemInformationProvider.getInstance();
        this.NpcMakerCraftingService = Java.type('scripting.npc.NpcMakerCraftingService');
        this.ItemConstants = Java.type('constants.inventory.ItemConstants');
        this.InventoryType = Java.type('client.inventory.InventoryType');
        this.Stream = Java.type('java.util.stream.Stream');
        this.Collectors = Java.type('java.util.stream.Collectors');
        this.HashMap = Java.type('java.util.HashMap');
        this.PacketCreator = Java.type('tools.PacketCreator');

        this.weaponOnlyCrystalOptions = [ Crystals.Diamond, Crystals.Sapphire ];
        this.allCrystalOptions = [ Crystals.PowerCrystal, Crystals.DEXCrystal, Crystals.WisdomCrystal,
            Crystals.LUKCrystal, Crystals.Diamond, Crystals.Sapphire, Crystals.BlackCrystal, Crystals.DarkCrystal,
            Crystals.Amethyst, Crystals.Garnet, Crystals.Opal, Crystals.Aquamarine, Crystals.Topaz, Crystals.Emerald ];

        this.initialized = false;
        this.recipeInitialized = false;

        this.selectedRecipe = null;
        this.selectedRecipeAmount = null;

        this.availableCrystalOptions = null;
        this.activeCrystalItemOptions = null;

        this.isUsingStimulant = false;
        this.availableStimulant = null;
        this.reagentSlotCount = 0;
        this.reagentItems = null;
        this.selectedReagentSlot = null;
        this.starForceItemTargetSlot = null;

        this.selectedCraftingMenuOption = null;
    }

    action(mode, type, selection, status) {
        try {
            if (status === (this.startingStatus + 1)) {
                this.selectedCraftingMenuOption = selection;
            }

            const handler = this.handlerRegistry.getHandler(status, this.selectedCraftingMenuOption);
            return handler.call(this, mode, type, selection, status);
        } catch (error) {
            this.cm.logToConsole(`[MakerCraftingMenuSubInteraction] HandlerError: ${error}`);
            return ScriptInteractionResult.HandlerError;
        }
    }

    setupInteraction(startingStatus) {
        if (this.initialized)
            return;

        this.startingStatus = startingStatus;

        this.#initializeHandlers();

        this.initialized = true;
    }

    setSelectedRecipe(recipe, amount) {
        if (this.recipeInitialized) {
            return;
        }

        this.selectedRecipe = recipe;

        // sanity check for haxors, if any material in the recipe will process StarForce, amount is 1.
        this.selectedRecipeAmount = this.selectedRecipe.materials.some((material) => material.processStarforce)
            ? 1
            : amount;

        if (this.selectedRecipe) {
            this.reagentSlotCount = this.selectedRecipe.slots;
            this.reagentItems = this.reagentItems || new SlotManager(this.reagentSlotCount);
            this.availableStimulant = this.selectedRecipe.stimulant;

            this.selectedRecipe.materials = this.selectedRecipe.materials.sort(this.materialSortComparisonFunction);

            this.#setupAvailableCrystalOptions();
        }

        this.recipeInitialized = true;
    }

    #initializeHandlers() {
        this.handlerRegistry.clear();

        let statusIndex = this.startingStatus;

        this.handlerRegistry.register(statusIndex, null, this.showFullMakerSelectionMenu.bind(this));

        statusIndex += 1;
        this.handlerRegistry.register(statusIndex, CraftingSelectionMenuOption.Slot1, this.showSlotSelectionMenu.bind(this));
        this.handlerRegistry.register(statusIndex, CraftingSelectionMenuOption.Slot2, this.showSlotSelectionMenu.bind(this));
        this.handlerRegistry.register(statusIndex, CraftingSelectionMenuOption.Slot3, this.showSlotSelectionMenu.bind(this));
        this.handlerRegistry.register(statusIndex, CraftingSelectionMenuOption.StarForce, this.showStarForceSelectionMenu.bind(this));
        this.handlerRegistry.register(statusIndex, CraftingSelectionMenuOption.Stimulant, this.toggleStimulant.bind(this));
        this.handlerRegistry.register(statusIndex, CraftingSelectionMenuOption.CraftNow, this.tryCraftingRecipe.bind(this));

        this.handlerRegistry.register(statusIndex, null, this.handleInvalidSelectionOrCancel.bind(this));

        statusIndex += 1;
        this.handlerRegistry.register(statusIndex, CraftingSelectionMenuOption.Slot1, this.handleSlotMenuSelection.bind(this));
        this.handlerRegistry.register(statusIndex, CraftingSelectionMenuOption.Slot2, this.handleSlotMenuSelection.bind(this));
        this.handlerRegistry.register(statusIndex, CraftingSelectionMenuOption.Slot3, this.handleSlotMenuSelection.bind(this));
        this.handlerRegistry.register(statusIndex, CraftingSelectionMenuOption.StarForce, this.handleStarForceSlotSelection.bind(this));
    }

    #setupAvailableCrystalOptions() {
        if (!this.ItemConstants.isEquipment(this.selectedRecipe.itemId))
        {
            this.availableCrystalOptions = [];

            return;
        }

        const isWeapon = this.ItemConstants.isWeapon(this.selectedRecipe.itemId);

        let crystalOptions = this.allCrystalOptions;

        if (!isWeapon) {
            crystalOptions = crystalOptions.filter(slot => !this.weaponOnlyCrystalOptions.includes(slot));
        }

        this.availableCrystalOptions = crystalOptions;
    }

    showFullMakerSelectionMenu(mode, type, selection, status) {
        if (isNaN(this.selectedRecipeAmount) || this.selectedRecipeAmount < 1 || this.selectedRecipeAmount > 1000) {
            this.cm.sendOk("You must enter a valid quantity.");
            return ScriptInteractionResult.Dispose;
        }

        let selectionMenuString = 'Are you sure this is what you want?\r\n\r\n';

        selectionMenuString += '#fUI/UIWindow.img/Quest/basic#\r\n'
        selectionMenuString += `#i${this.selectedRecipe.itemId}:# #z${this.selectedRecipe.itemId}# #e x${this.cm.numberWithCommas(this.selectedRecipe.quantity * this.selectedRecipeAmount)}#n\r\n\r\n`;
        selectionMenuString += `#fUI/UIWindow.img/Quest/icon0# #fUI/UIWindow.img/UserInfo/ReqLv#. #r${this.selectedRecipe.requiredLevel}#k\r\n\r\n`;
        selectionMenuString += '#fUI/UIWindow.img/Quest/icon0# #b#eRequired Materials:#n#k\r\n';

        for (let material of this.selectedRecipe.materials) {
            selectionMenuString += material.processStarforce
                ? this.printStarForceRecipeMaterial(material)
                : this.printStandardRecipeMaterial(material);
        }

        if (this.reagentSlotCount > 0) {
            selectionMenuString += '\r\n';
            selectionMenuString += '#b#eCrystals:#n#k\r\n'
            for (let index = 1; index <= this.reagentItems.maxSlots; index++) {
                selectionMenuString += `#L${index}# `;

                let itemId = this.reagentItems.getSlot(index);

                if (itemId === null || itemId === SlotManagerResult.InvalidSlot) {
                    selectionMenuString += `#fUI/UIWindow.img/Messenger/question# Select Crystal#l\r\n`;
                    continue;
                }

                let playerMaterialQuantity = this.cm.getItemQuantity(itemId);
                let isBelowQuantity = playerMaterialQuantity < this.selectedRecipeAmount;

                let playerQuantityString = isBelowQuantity
                    ? `#r${this.cm.numberWithCommas(playerMaterialQuantity)}#k`
                    : `${this.cm.numberWithCommas(playerMaterialQuantity)}`

                selectionMenuString += `#i${itemId}:# #z${itemId}# ( ${playerQuantityString} / ${this.cm.numberWithCommas(this.selectedRecipeAmount)} )#l\r\n`;
            }
        }

        if (this.availableStimulant !== null) {
            selectionMenuString += '\r\n';
            selectionMenuString += '#b#eStimulant:#n#k\r\n';
            selectionMenuString += '#L10# '

            let playerMaterialQuantity = this.cm.getItemQuantity(this.availableStimulant);
            let isBelowQuantity = playerMaterialQuantity < this.selectedRecipeAmount;

            let playerQuantityString = isBelowQuantity
                ? `#r#c${this.availableStimulant}##k`
                : `${this.cm.numberWithCommas(playerMaterialQuantity)}`

            selectionMenuString += this.isUsingStimulant
                ? `#i${this.availableStimulant}# #z${this.availableStimulant}# ( ${playerQuantityString} / ${this.cm.numberWithCommas(this.selectedRecipeAmount)} )`
                : `#i${this.availableStimulant}# Use Stimulant`

            selectionMenuString += '#l\r\n';
        }

        const fee = this.getCraftingFee();

        selectionMenuString += '\r\n#e#bCost:#n\r\n';
        selectionMenuString += `#fUI/UIWindow.img/Shop/meso# ${this.cm.numberWithCommas(fee)} mesos\r\n\r\n`;
        selectionMenuString += `#L${CraftingSelectionMenuOption.CraftNow}##eCraft Now\r\r`;
        selectionMenuString += `#L${CancelOption}##rCancel#n#k`;

        this.cm.sendSimple(selectionMenuString);
        return status;
    }

    handleInvalidSelectionOrCancel(mode, type, selection, status) {
        return ScriptInteractionResult.Dispose;
    }

    // ================= StarForce Slot Handlers =======================
    // TODO: show the item name
    // TODO: allow remove and cancel
    // TODO: show current slot if selected
    showStarForceSelectionMenu(mode, type, selection, status) {
        let selectionMenuString = 'Select the item you\'d like to use for crafting this item.\r\n\r\n';

        const starForceMaterial = this.getStarForceTargetMaterial();

        selectionMenuString += this.cm.getEligibleEquipSelectionString(starForceMaterial.itemId);

        this.cm.sendSimple(selectionMenuString);
        return status;
    }

    handleStarForceSlotSelection(mode, type, selection, status) {
        this.starForceItemTargetSlot = selection;

        cm.sendOk("Slot updated with selected item.");
        return this.goToMainMenu();
    }

    // ================= Reagent Slot Handlers =========================
    showSlotSelectionMenu(mode, type, selection, status) {
        this.selectedReagentSlot = selection;
        let selectionMenuString = '';

        selectionMenuString += "I can enhance your equipment for a fee in headpats! I mean.. mesos.\r\n\r\n";

        const reagentItemId = this.reagentItems.getSlot(this.selectedReagentSlot);

        if (reagentItemId) {
            let playerMaterialQuantity = this.cm.getItemQuantity(reagentItemId);

            let isBelowQuantity = playerMaterialQuantity < this.selectedRecipeAmount;

            let playerQuantityString = isBelowQuantity
                ? `#r#c${reagentItemId}##k`
                : `#c${reagentItemId}#`

            selectionMenuString += '#b#eCurrently Selected:#k#n\r\n'
            selectionMenuString += `#i${reagentItemId}:# #z${reagentItemId}# ( ${playerQuantityString} / ${this.cm.numberWithCommas(this.selectedRecipeAmount)} )\r\n\r\n`;
            selectionMenuString += `#eUse the options below to replace or remove this crystal.#n\r\n\r\n`;
        }

        const slots = this.reagentItems.getSlots();

        const selectedCrystals = slots.filter(slot => slot !== null && slot !== reagentItemId).map(slot => this.getCrystalFromItemId(slot));

        let crystalOptions = this.availableCrystalOptions;

        crystalOptions = crystalOptions.filter(crystalOption => !selectedCrystals.includes(crystalOption))

        let eligibleCrystalItemIds = [];

        crystalOptions.forEach((option) => {
            Crystals.ItemMap[option].forEach((itemId) => {
                if (itemId === reagentItemId)
                    return;

                eligibleCrystalItemIds.push(itemId);
            });
        });

        this.activeCrystalItemOptions = eligibleCrystalItemIds;

        let crystalIndex = 0;
        this.activeCrystalItemOptions.forEach((itemId) => {
            selectionMenuString += "\r\n#L" + crystalIndex + "# #i" + itemId + ":# #z" + itemId + "##l";

            crystalIndex++;
        });

        selectionMenuString += `\r\n\r\n#L${RemoveOption}##rRemove#k`;
        selectionMenuString += `\r\n#L${CancelOption}#Cancel`;

        this.cm.sendSimple(selectionMenuString);
        return status;
    }

    handleSlotMenuSelection(mode, type, selection, status) {
        // handle special cases first
        switch (selection) {
            case RemoveOption:
                this.reagentItems.clearSlot(this.selectedReagentSlot);
                this.cm.sendNext('You cleared this slot.');
                return this.goToMainMenu();
            case CancelOption:
                this.cm.sendNext('You have canceled editing this slot.');
                return this.goToMainMenu();
        }

        const selectedCrystalItemId = this.activeCrystalItemOptions[selection];
        if (!selectedCrystalItemId) {
            // Kill the interaction immediately
            return ScriptInteractionResult.Dispose;
        }

        this.reagentItems.setSlot(this.selectedReagentSlot, selectedCrystalItemId);

        let playerMaterialQuantity = this.cm.getItemQuantity(selectedCrystalItemId);

        let isBelowQuantity = playerMaterialQuantity < this.selectedRecipeAmount;

        let playerQuantityString = isBelowQuantity
            ? `#r#c${selectedCrystalItemId}##k`
            : `#c${selectedCrystalItemId}#`

        let nextString = `#b#eYou have selected:#k#n\r\n`
        nextString += `#i${selectedCrystalItemId}:# #z${selectedCrystalItemId}# ( ${playerQuantityString} / ${this.cm.numberWithCommas(this.selectedRecipeAmount)} )`;

        this.cm.sendOk(nextString);
        return this.goToMainMenu();
    }

    // ================= Stimulant Slot Handlers =======================
    toggleStimulant(mode, type, selection, status) {
        this.isUsingStimulant = !this.isUsingStimulant;

        this.cm.sendOk(`Stimulant is ${this.isUsingStimulant ? 'enabled' : 'disabled'}.`);
        return this.goToMainMenu();
    }

    // ==================== Crafting Handlers ==========================
    tryCraftingRecipe(mode, type, selection, status) {
        if (!this.selectedRecipe) {
            this.cm.sendOk('No recipe selected! (haxor?)');
            return ScriptInteractionResult.Dispose;
        }

        let missingMaterialRequirements = [];
        for (let material of this.selectedRecipe.materials) {
            let playerMaterialQuantity = this.cm.getItemQuantity(material.itemId);
            let requiredAmount = material.amount * this.selectedRecipeAmount;

            if (material.processStarforce) {
                if (!this.starForceItemTargetSlot) {
                    missingMaterialRequirements.push(new MissingMaterialRequirement(material.itemId, requiredAmount, 0));
                    continue;
                }
            }

            if (playerMaterialQuantity < requiredAmount) {
                missingMaterialRequirements.push(new MissingMaterialRequirement(material.itemId, requiredAmount, playerMaterialQuantity));
            }
        }

        if (this.availableStimulant !== null && this.isUsingStimulant) {
            let playerMaterialQuantity = this.cm.getItemQuantity(this.availableStimulant);

            if (!this.cm.hasItem(this.availableStimulant, this.selectedRecipeAmount)) {
                missingMaterialRequirements.push(new MissingMaterialRequirement(this.availableStimulant, this.selectedRecipeAmount, playerMaterialQuantity));
            }
        }

        const reagentItems = this.getReagentItemIds();

        for (let reagentId of reagentItems) {
            let playerMaterialQuantity = this.cm.getItemQuantity(reagentId);
            let requiredAmount = this.selectedRecipeAmount;
            if (playerMaterialQuantity < requiredAmount) {
                missingMaterialRequirements.push(new MissingMaterialRequirement(reagentId, requiredAmount, playerMaterialQuantity));
            }
        }

        let message = '';

        if (missingMaterialRequirements.length > 0) {
            message += 'Hey! You\'re missing some materials.';
            if (this.cm.getLevel() < this.selectedRecipe.requiredLevel) {
                 if (message.length > 0) {
                 message += '\r\n\r\n';
               }

                message += `#fUI/UIWindow.img/Quest/icon0# #fUI/UIWindow.img/UserInfo/ReqLv#. #r${this.selectedRecipe.requiredLevel}#k`;
            }
            if (this.NpcMakerCraftingService.getMakerSkillLevel(this.cm.getPlayer()) < this.selectedRecipe.requiredMakerLevel) {
                if (message.length > 0) {
                message += '\r\n\r\n';
              }
              message += `#fUI/UIWindow.img/Quest/icon0# #bMaker Skill#k must be at #rLv. ${this.selectedRecipe.requiredMakerLevel}#k too.`;
            }

            message += '\r\n\r\n#fUI/UIWindow.img/Quest/icon0# #r#eMissing Materials:#n#k\r\n';
            for (let requirement of missingMaterialRequirements) {
                message += `#i${requirement.itemId}:# #z${requirement.itemId}# ( #r${this.cm.numberWithCommas(requirement.actualAmount)}#k / ${this.cm.numberWithCommas(requirement.requiredAmount)} )\r\n`;
            }
        }

        let playerMesos = this.cm.getMeso();
        let requiredMesos = this.getCraftingFee();
        if (playerMesos < requiredMesos) {
            if (message.length > 0) {
                message += '\r\n';
            }

            message += `Are you trying to trick me? You don\'t have enough mesos! Just because I\'m a cat doesn\'t mean I can\'t count.\r\n\r\n(Missing #fUI/UIWindow.img/Shop/meso# #r${this.cm.numberWithCommas(requiredMesos - playerMesos)}#k mesos.)`;
        }

        if (!this.cm.canHold(this.selectedRecipe.itemId, this.selectedRecipe.quantity * this.selectedRecipeAmount)) {
            if (message.length > 0) {
                message += '\r\n';
            }

            message += 'Please clear up some space in your inventory.';
        }

        if (message.length > 0) {
            this.cm.sendOk(message);
            return this.goToMainMenu();
        }

        const itemCrafter = new ItemCrafter(
            this.cm,
            this.selectedRecipe,
            this.selectedRecipeAmount,
            reagentItems,
            this.starForceItemTargetSlot,
            this.isUsingStimulant);

        const results = itemCrafter.tryCrafting();

        const resultKeys = results.map(([key]) => parseInt(key, 10));
        const overallSuccess = resultKeys.includes(CraftingResult.Success) || resultKeys.includes(CraftingResult.SuccessWithoutEnhancement);

        try {
            this.cm.getPlayer().getClient().sendPacket(this.PacketCreator.showMakerEffect(overallSuccess));
        } catch (e) {
            this.cm.logToConsole(`[MakerCrafting] Failed to send player packet. Continuing script execution as it is safe in this case. Error: ${e}`);
        }

        try {
            const player = this.cm.getPlayer();
            this.cm.getMap().broadcastMessage(player, this.PacketCreator.showForeignMakerEffect(player.getId(), overallSuccess), false);
        } catch (e) {
            this.cm.logToConsole(`[MakerCrafting] Failed to send broadcast packet. Continuing script execution as it is safe in this case. Error: ${e}`);
        }

        message = `#fUI/UIWindow.img/QuestIcon/4/0#\r\n\r\nCrafting ${overallSuccess ? '#e#bSucceeded#n#k' : '#e#rFaied#n#k' } for #i${this.selectedRecipe.itemId}:# #b#z${this.selectedRecipe.itemId}##k #ex${(this.selectedRecipe.quantity * this.selectedRecipeAmount)}\r\n#n.`
        message += '\r\n\r\nYay they\'re done! Here is your masterpiece!\r\n';
        for (const [result, count] of results) {
            message += `${CraftingResult.StringMap[parseInt(result, 10)]} x ${count}\r\n`;
        }

        this.cm.sendOk(message);
        return ScriptInteractionResult.Dispose;
    }

    // Utility
    goToMainMenu() {
        return this.startingStatus - 1;
    }

    getCraftingFee() {
        let baseFee = this.selectedRecipe.cost;

        const reagentItems = this.getReagentItemIds();

        for (let reagentItemId of reagentItems) {
            const reagentLevel = ((reagentItemId % 10) + 1);
            baseFee += this.MakerItemFactory.getMakerReagentFee(this.selectedRecipe.itemId, reagentLevel);
        }

        if (this.isUsingStimulant && this.availableStimulant) {
            baseFee += this.MakerItemFactory.getMakerStimulantFee(this.selectedRecipe.itemId);
        }

        baseFee = Math.floor(baseFee / 1000) * 1000;

        return baseFee * this.selectedRecipeAmount;
    }

    getReagentItemIds() {
        return this.reagentItems.getSlots().filter(slot => slot !== null)
    }

    getStarForceTargetMaterial() {
        return this.selectedRecipe.materials.filter(material => material.processStarforce)[0];
    }

    findKeyByValue(object, value) {
        return Object.keys(object).find(key => object[key].includes(value));
    }

    getCrystalFromItemId(itemId) {
        return parseInt(this.findKeyByValue(Crystals.ItemMap, itemId));
    }

    printStandardRecipeMaterial(material) {
        let requiredQuantity = material.amount * this.selectedRecipeAmount;

        let playerMaterialQuantity = this.cm.getItemQuantity(material.itemId);

        let isBelowQuantity = playerMaterialQuantity < requiredQuantity;

        let playerQuantityString = isBelowQuantity
            ? `#r${this.cm.numberWithCommas(playerMaterialQuantity)}#k`
            : `${this.cm.numberWithCommas(playerMaterialQuantity)}`

        return `       #i${material.itemId}:# #z${material.itemId}# ( ${playerQuantityString} / ${this.cm.numberWithCommas(requiredQuantity)} )\r\n`;
    }

    printStarForceRecipeMaterial(material) {
        const playerMaterialQuantity = this.cm.getItemQuantity(material.itemId);

        // player must have at least one of the item...
        if (playerMaterialQuantity === 0) {
            return `       #i${material.itemId}:# #z${material.itemId}# #r[No Eligible Items]#k\r\n`;
        }

        let itemString = `#L${CraftingSelectionMenuOption.StarForce}# #i${material.itemId}:# #z${material.itemId}# `;

        if (this.starForceItemTargetSlot) {
            const equip = this.cm.getStarForceEquip(this.starForceItemTargetSlot);
            const statBonus = this.cm.getStarForceEquipCraftingBonus(equip);
            const spellTraceBonus = this.cm.getStarForceEquipSpellTraceBonus(equip);

            itemString += `[Slot: ${this.starForceItemTargetSlot}]#l\r\n\r\n       #eLevel:#n ${equip.getLevel()}\r\n`;
            itemString += '       #eStarForce Bonus#n\r\n';
            itemString += `       WATK: +${statBonus.getWatk()}\r\n`;
            itemString += `       MATK: +${statBonus.getMatk()}\r\n`;
            itemString += `       STR: +${statBonus.getStr()}\r\n`;
            itemString += `       DEX: +${statBonus.getDex()}\r\n`;
            itemString += `       LUK: +${statBonus.getLuk()}\r\n`;
            itemString += `       INT: +${statBonus.getInt()}\r\n`;
            itemString += `       #fUI/UIWindow.img/Shop/trace# +${spellTraceBonus} spell traces\r\n\r\n`;

            return itemString;
        } else {
            return `${itemString}#r[Select]#k#l\r\n\r\n`;
        }
    }

    materialSortComparisonFunction(materialA, materialB) {
        // Sort items that require selecting a target item to the top
        if (materialA.processStarforce < materialB.processStarforce) {
            return 1;
        }

        if (materialA.processStarforce > materialB.processStarforce) {
            return -1;
        }

        // Sort by item id
        if (materialA.itemId < materialB.itemId) {
            return -1;
        }

        if (materialA.itemId > materialB.itemId) {
            return 1;
        }

        return 0;
    }
}

const RemoveOption = 9999997;
const CancelOption = 9999999;


const CraftingSelectionMenuOption = {
    StringMap: {
        1: "Slot 1",
        2: "Slot 2",
        3: "Slot 3",
        10: "Stimulant",
        9999998: "Craft Now",
        9999999: "Cancel",
    },
    Slot1: 1,
    Slot2: 2,
    Slot3: 3,
    Stimulant: 10,
    StarForce: 20,
    CraftNow: 9999998
}

const Crystals= {
    ItemMap: {
        0: [4250800, 4250801, 4250802],
        1: [4251100, 4251101, 4251102],
        2: [4250900, 4250901, 4250902],
        3: [4251000, 4251001, 4251002],
        4: [4250000, 4250001, 4250002],
        5: [4250100, 4250101, 4250102],
        6: [4251300, 4251301, 4251302],
        7: [4251400, 4251401, 4251402],
        8: [4250400, 4250401, 4250402],
        9: [4250200, 4250201, 4250202],
        10: [4250300, 4250301, 4250302],
        11: [4250500, 4250501, 4250502],
        12: [4250600, 4250601, 4250602],
        13: [4250700, 4250701, 4250702]
    },
    PowerCrystal:0,
    DEXCrystal:1,
    WisdomCrystal:2,
    LUKCrystal:3,
    Diamond:4,
    Sapphire:5,
    BlackCrystal:6,
    DarkCrystal:7,
    Amethyst:8,
    Garnet:9,
    Opal:10,
    Aquamarine:11,
    Topaz:12,
    Emerald:13
}