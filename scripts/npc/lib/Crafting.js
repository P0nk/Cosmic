class SlotManager {
    constructor(maxSlots = 3) {
        this.maxSlots = maxSlots;
        this.slots = Array(maxSlots).fill(null);
    }

    setSlot(slotNumber, item) {
        if (slotNumber < 1 || slotNumber > this.maxSlots) {
            return SlotManagerResult.InvalidSlot;
        }

        this.slots[slotNumber - 1] = item;
        return SlotManagerResult.Success;
    }

    clearSlot(slotNumber) {
        if (slotNumber < 1 || slotNumber > this.maxSlots) {
            return SlotManagerResult.InvalidSlot;
        }

        if (this.slots[slotNumber - 1] === null) {
            return SlotManagerResult.SlotEmpty;
        }

        this.slots[slotNumber - 1] = null;
        return SlotManagerResult.Success;
    }

    getSlot(slotNumber) {
        if (slotNumber < 1 || slotNumber > this.maxSlots) {
            return SlotManagerResult.InvalidSlot;
        }

        return this.slots[slotNumber - 1];
    }

    getSlots() {
        return this.slots;
    }
}

class MissingMaterialRequirement {
    constructor(itemId, requiredAmount, actualAmount) {
        this.itemId = itemId;
        this.requiredAmount = requiredAmount;
        this.actualAmount = actualAmount;
    }
}

class Material {
    constructor(itemId, amount, processStarforce) {
        this.itemId = itemId;
        this.amount = amount;
        this.processStarforce = processStarforce ?? false;
    }

    static fromJson(json) {
        return new Material(json.itemId, json.amount, json.processStarforce);
    }
}

class CraftingRecipe {
    constructor(name, itemId, quantity, materials, cost, stimulant, slots, requiredLevel, requiredMakerLevel, processStarforce) {
        this.name = name;
        this.itemId = itemId;
        this.quantity = quantity;
        this.materials = materials.map(Material.fromJson);
        this.cost = cost;
        this.stimulant = stimulant ?? null;
        this.slots = slots ?? 0;
        this.requiredLevel = requiredLevel ?? 1;
        this.requiredMakerLevel = requiredMakerLevel ?? 1;
    }

    static fromJson(json) {
        return new CraftingRecipe(
            json.name,
            json.itemId,
            json.quantity,
            json.materials,
            json.cost,
            json.stimulant,
            json.slots,
            json.requiredLevel,
            json.requiredMakerLevel
        );
    }
}

class ReagentBuff {
    constructor(stat, value) {
        this.stat = stat;
        this.value = value;
    }

    static fromJson(json) {
        return new ReagentBuff(json.stat, json.value);
    }
}

const SlotManagerResult = {
    StringMap: {
        0: "Operation successful.",
        1: "Invalid slot number.",
        2: "Slot is empty."
    },
    Success: 0,
    InvalidSlot: 1,
    SlotEmpty: 2
};

const CraftingResult = {
    StringMap: {
        0: "Item crafted and enhanced successfully.",
        1: "Item crafted but failed enhancement.",
        2: "You can only use WATK and MATK Strengthening Gems on weapon items.",
        3: "Item crafting failed."
    },
    Success: 0,
    SuccessWithoutEnhancement: 1,
    OddItemFault: 2,
    Failed: 3
}