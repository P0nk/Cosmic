load('scripts/npc/lib/Crafting.js');
load('scripts/npc/interactions/shared/MakerCrafting.js');

class RefiningMineralsInteraction {
    constructor(cm) {
        this.cm = cm;
        this.handlerRegistry = new InteractionHandlerRegistry();

        this.craftingMenuInteraction = new MakerCraftingMenuSubInteraction(cm);

        this.refiningMaterialData = getJson("refiningMaterials.json");

        this.activeRefiningMaterialList = this.refiningMaterialData.map(CraftingRecipe.fromJson);

        this.selectedRefiningMaterial = null;
        this.selectedRefiningMaterialAmount = null;
        this.activeRefiningMaterialRecipe = null;

        this.initializeHandlers();
    }

    initializeHandlers() {
        // Entrypoint
        this.handlerRegistry.register(1, null, this.showRefiningMaterialSelectionMenu.bind(this));
        this.handlerRegistry.register(2, null, this.getRefiningAmount.bind(this));
        this.handlerRegistry.register(3, null, this.confirmRefiningItemSelection.bind(this));
    }

    action(mode, type, selection, status) {
        try {
            if (status > 2 && status > 3)
            {
                return this.craftingMenuInteraction.action(mode, type, selection, status);
            }

            const handler = this.handlerRegistry.getHandler(status, status);
            return handler.call(this, mode, type, selection, status);
        } catch (error) {
            this.cm.logToConsole(`HandlerError: ${error}`);
            return ScriptInteractionResult.HandlerError;
        }
    }

    // ======================= The Beginning ===========================
    // TODO: Get player inventory and/or ore storage quantities to filter items
    showRefiningMaterialSelectionMenu(mode, type, selection, status) {
        let selStr = "Let me take a look at what minerals you have...#b#e\r\n\r\n";

        for (let index = 0; index < this.activeRefiningMaterialList.length; index++) {
            let refiningMaterial = this.activeRefiningMaterialList[index];
            selStr += `#L${index + 1}# #i${refiningMaterial.itemId}# #z${refiningMaterial.itemId}##l\r\n`;
        }

        this.cm.sendSimple(selStr);
        return status;
    }

    // TODO: show item in this prompt
    getRefiningAmount(mode, type, selection, status) {
        this.selectedRefiningMaterial = this.selectedRefiningMaterial || selection;

        this.activeRefiningMaterialRecipe = this.activeRefiningMaterialRecipe || this.activeRefiningMaterialList[selection - 1];

        this.cm.sendGetNumber("How many would you like to make?", 1, 1, 1000);
        return status;
    }

    confirmRefiningItemSelection(mode, type, selection, status) {
        this.selectedRefiningMaterialAmount = selection;

        this.craftingMenuInteraction.setupInteraction(status);
        this.craftingMenuInteraction.setSelectedRecipe(this.activeRefiningMaterialRecipe, this.selectedRefiningMaterialAmount);

        return this.craftingMenuInteraction.action(mode, type, selection, status);
    }
}