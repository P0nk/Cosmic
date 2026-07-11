class ItemCrafter {
    constructor(cm, selectedRecipe, selectedRecipeAmount, reagentItems, starForceTargetItemSlot, isUsingStimulant) {
        this.cm = cm;
        this.ItemInformationProvider = Java.type('server.ItemInformationProvider');
        this.ii = this.ItemInformationProvider.getInstance();
        this.ItemConstants = Java.type('constants.inventory.ItemConstants');
        this.NpcMakerCraftingService = Java.type('scripting.npc.NpcMakerCraftingService');
        this.InventoryManipulator = Java.type('client.inventory.manipulator.InventoryManipulator');
        this.MakerItemFactory = Java.type('server.MakerItemFactory');

        this.reagentBuffData = getJson("reagentBuffs.json");

        this.selectedRecipe = selectedRecipe;
        this.selectedRecipeAmount = selectedRecipeAmount;
        this.reagentItems = reagentItems;
        this.starForceTargetItemSlot = starForceTargetItemSlot;
        this.isUsingStimulant = isUsingStimulant;
    }

    tryCrafting() {
        let counts = {
            [CraftingResult.Success]: 0,
            [CraftingResult.SuccessWithoutEnhancement]: 0,
            [CraftingResult.OddItemFault]: 0,
            [CraftingResult.Failed]: 0
        };

        for (let index = 0; index < this.selectedRecipeAmount; index++) {
            const result = this.craftItem();
            counts[result]++;
        }

        return Object.entries(counts).filter(([key, value]) => value > 0);
    }

    craftItem() {
        let itemQuantities = new Map();

        for (let reagentId of this.reagentItems) {
            itemQuantities[reagentId] = cm.getItemQuantity(reagentId);
        }

        if (!this.sanitizeReagentQuantities(itemQuantities)) {
            return CraftingResult.OddItemFault;
        }

        return this.applyMakerResult();
    }

    applyMakerResult() {
        const cost = this.getRecipeCraftingFee();

        // automatically take items that don't need starforce processing
        for (let material of this.selectedRecipe.materials.filter(material => !material.processStarforce)) {
            this.cm.gainItem(material.itemId, -(material.amount), false);
        }

        const needsStarForceProcessing = this.selectedRecipe.materials.some(material => material.processStarforce);

        if ((!this.isUsingStimulant || !this.selectedRecipe.stimulant) && (!this.reagentItems || this.reagentItems.length === 0)) {
            if (cost > 0) {
                this.cm.gainMeso(-(cost), false);

                const itemId = this.selectedRecipe.itemId;
                // no disassembly as such, there is only one reward per recipe
                if (itemId - 4250000 < 2000 && itemId > 4250000) {
                    this.cm.getPlayer().setCS(true);
                    const randomNumber = this.getRandomNumber();

                    let luck = 0;

                    if (itemId % 10 === 0) {
                        if (randomNumber === 1) {
                            luck = 2;
                        } else if (randomNumber < 12) {
                            luck = 1;
                        }
                    } else if (itemId % 10 === 1) {
                        if (randomNumber < 6) {
                            luck = 1;
                        }
                    }

                    this.cm.gainItem(itemId + luck, this.selectedRecipe.quantity, false);
                    this.cm.getPlayer().setCS(false);
                } else {
                    this.craftItemWithStarForceProcessing(itemId, this.selectedRecipe.quantity,  needsStarForceProcessing);
                }

                return CraftingResult.Success;
            }
        } else {
            this.cm.gainMeso(-(cost), false);

            if (this.isUsingStimulant && this.selectedRecipe.stimulant) {
                this.cm.gainItem(this.selectedRecipe.stimulant, -1, false);
            }

            if (this.reagentItems && this.reagentItems.length) {
                for (let reagentItemId of this.reagentItems) {
                    this.cm.gainItem(reagentItemId, -1, false);
                }
            }

            switch (this.tryBoostItem()){
                case 1:
                    return CraftingResult.Success;
                case 0:
                    return CraftingResult.SuccessWithoutEnhancement;
                default:
                    return CraftingResult.Failed;
            }
        }

        return CraftingResult.Failed;
    }

    tryBoostItem() {
        const needsStarForceProcessing = this.selectedRecipe.materials.some(material => material.processStarforce);

        if ((this.isUsingStimulant && this.selectedRecipe.stimulant) && !this.ItemInformationProvider.rollSuccessChance(90.0)) {
            this.craftItemWithStarForceProcessing(this.selectedRecipe.itemId, this.selectedRecipe.quantity,  needsStarForceProcessing);

            return 0;
        }

        let item = this.ii.getEquipByIdAsEquip(this.selectedRecipe.itemId);
        if (!item) {
            return -1;
        }

        if (this.reagentItems.length > 0) {
            let statBuffs = new Map();
            let randomStatBuffs = [];
            let randomOptionBuffs = [];

            for (let reagentId of this.reagentItems) {
                const reagentBuff = this.getReagentBuffForId(reagentId);

                if (reagentBuff) {
                    let buffStat = reagentBuff.stat;

                    if (buffStat.substring(0, 4).includes("rand")) {
                        switch (buffStat.substring(4)) {
                            case 'Stat':
                                randomStatBuffs.push(reagentBuff.value);
                                break;
                            case 'Option':
                                randomOptionBuffs.push(reagentBuff.value);
                                break;
                        }
                    } else {
                        let targetStat = buffStat.substring(3);

                        if (targetStat !== "ReqLevel") {
                            switch (targetStat) {
                                case "MaxHP":
                                    targetStat = "MHP";
                                    break;
                                case "MaxMP":
                                    targetStat ="MMP";
                                    break;
                            }
                        }

                        let currentStatValue = statBuffs.get(targetStat) || 0;
                        statBuffs.set(targetStat, currentStatValue + reagentBuff.value);
                    }
                }
            }

            this.ItemInformationProvider.improveEquipStats(item, statBuffs);

            for (let statBuffValue of randomStatBuffs) {
                // reference update
                this.ii.scrollOptionEquipWithChaos(item, statBuffValue, false);
            }

            for (let statBuffValue of randomOptionBuffs) {
                // reference update
                this.ii.scrollOptionEquipWithChaos(item, statBuffValue, true);
            }
        }

        if (this.isUsingStimulant && this.selectedRecipe.stimulant) {
            // reference update, no need for the return
            this.ii.randomizeUpgradeStats(item);
        }

        if (needsStarForceProcessing) {
            // this method will remove the associated item
            this.cm.applyStarForceEquipCraftingBonus(item, this.starForceTargetItemSlot);
        }

        this.InventoryManipulator.add(this.cm.getClient(), item, false, -1);
        return 1;
    }

    craftItemWithStarForceProcessing(itemId, amount, needsStarForceProcessing) {
        if (needsStarForceProcessing) {
            let item = this.ii.getEquipByIdAsEquip(itemId);

            // this method will remove the associated item
            this.cm.applyStarForceEquipCraftingBonus(item, this.starForceTargetItemSlot);

            this.InventoryManipulator.add(this.cm.getClient(), item, false, -1);
        } else {
            this.cm.gainItem(itemId, amount, false);
        }
    }

    sanitizeReagentQuantities(itemQuantities) {
        const reagentTypes = new Map();
        const toRemove = [];

        const isWeapon = this.ItemConstants.isWeapon(this.selectedRecipe.itemId)
            || this.NpcMakerCraftingService.useMakerPermissionAtkUp();

        for (let [key, value] of itemQuantities) {
            const itemId = key;
            const type = itemId / 100;

            if (type < 43502 && !isWeapon) {
                toRemove.push(itemId);
            } else {
                let tableId = null;

                if (!reagentTypes.has(type)) {
                    reagentTypes[type] = itemId;
                    continue;
                } else {
                    tableId = reagentTypes[type];
                }

                if (tableId < itemId) {
                    toRemove.push(tableId);
                    reagentTypes[type] = itemId;
                    continue;
                }

                toRemove.push(itemId);
            }
        }

        for (let itemId of toRemove) {
            itemQuantities.delete(itemId);
        }

        for (const [key, value] of itemQuantities) {
            itemQuantities.set(key, 1);
        }

        return true;
    }

    getReagentBuffForId(reagentId) {
        return ReagentBuff.fromJson(this.reagentBuffData[reagentId]);
    }

    getRandomNumber() {
        return Math.floor(Math.random() * 100) + 1;
    }

    getRecipeCraftingFee() {
        let baseFee = this.selectedRecipe.cost;

        for (let reagentItemId of this.reagentItems) {
            const reagentLevel = ((reagentItemId % 10) + 1)
            baseFee += this.MakerItemFactory.getMakerReagentFee(this.selectedRecipe.itemId, reagentLevel);
        }

        if (this.isUsingStimulant && this.selectedRecipe.stimulant) {
            baseFee += this.MakerItemFactory.getMakerStimulantFee(this.selectedRecipe.itemId);
        }

        baseFee = Math.floor(baseFee / 1000) * 1000;

        return baseFee;
    }
}