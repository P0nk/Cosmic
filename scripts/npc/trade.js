var UntradableTradeManager = Java.type('server.trade.UntradableTradeManager');
var InventoryType = Java.type('client.inventory.InventoryType');
var ItemInformationProvider = Java.type('server.ItemInformationProvider');
var InventoryManipulator = Java.type('client.inventory.manipulator.InventoryManipulator');
var status = 0;
var mode = 0; // 1 = Send, 2 = Claim Incoming, 3 = Claim Refund
var tradeManager;
var ii;

// variables for mode 1
var recipientName = "";
var targetId = -1;
var eligibleItems = [];
var selectedIt = null;
var calculatedFee = 0;

// variables for mode 2/3
var pendingTrades = [];
var selectedTrade = null;

function start() {
    status = -1;
    tradeManager = UntradableTradeManager.getInstance();
    tradeManager.purgeExpired();
    ii = ItemInformationProvider.getInstance();
    action(1, 0, 0);
}

function action(mode_act, type, selection) {
    if (mode_act == -1 || mode_act == 0) {
        if (status == 2 && mode == 2 && mode_act == 0) {
            // The receiver chose 'No' in YesNo for accepting incoming trade
            var actualTrade = tradeManager.getRequest(selectedTrade.getTradeId());
            if (actualTrade != null) {
                tradeManager.markRefunding(selectedTrade.getTradeId());
                cm.sendOk("The item has been successfully refused and returned to the sender.");
            }
        }
        cm.dispose();
        return;
    }
    if (mode_act == 1) {
        status++;
    }

    if (status == 0) {
        var inCount = tradeManager.getRequestsForReceiver(cm.getPlayer().getId()).size();
        var refCount = tradeManager.getRefundsForSender(cm.getPlayer().getId()).size();
        var sendsLeft = Math.max(0, 5 - tradeManager.getDailySends(cm.getPlayer().getId()));

        var inCountStr = inCount > 0 ? ("#r(" + inCount + ")#b") : ("(" + inCount + ")");

        var text = "Welcome to Package delivery System 2.0\r\n";
        text += "There is a limit of 5 package sendable per day. Items not claimed by the end of 2 days will be forfeited.\r\n\r\n";
        text += "#b#L1#Send an equipment to another player (" + sendsLeft + " left)#l\r\n";
        text += "#L2#Check my pending incoming trades " + inCountStr + "#l\r\n";
        text += "#L3#Claim returned/cancelled trades (" + refCount + ")#l";
        cm.sendSimple(text);
    } else if (status == 1) {
        mode = selection;
        if (mode == 1) {
            if (tradeManager.getDailySends(cm.getPlayer().getId()) >= 5) {
                cm.sendOk("You have reached your daily limit of 5 untradable equip deliveries.");
                cm.dispose();
                return;
            }
            cm.sendGetText("Please enter the exact name of the player you wish to trade with. They must be currently online.");
        } else if (mode == 2) {
            var list = tradeManager.getRequestsForReceiver(cm.getPlayer().getId());
            pendingTrades = [];
            for (var i = 0; i < list.size(); i++) {
                pendingTrades.push(list.get(i));
            }
            if (pendingTrades.length == 0) {
                cm.sendOk("You have no pending incoming trades.");
                cm.dispose();
                return;
            }
            var text = "You have incoming trades from the following players:\r\n";
            for (var i = 0; i < pendingTrades.length; i++) {
                var req = pendingTrades[i];
                text += "#b#L" + i + "#From " + req.getSenderName() + ": #t" + req.getItem().getItemId() + "##l\r\n";
            }
            cm.sendSimple(text);
        } else if (mode == 3) {
            var list = tradeManager.getRefundsForSender(cm.getPlayer().getId());
            pendingTrades = [];
            for (var i = 0; i < list.size(); i++) {
                pendingTrades.push(list.get(i));
            }
            if (pendingTrades.length == 0) {
                cm.sendOk("You have no returned items.");
                cm.dispose();
                return;
            }
            var text = "The following items were denied or cancelled and are waiting to be claimed:\r\n";
            for (var i = 0; i < pendingTrades.length; i++) {
                var req = pendingTrades[i];
                text += "#b#L" + i + "##t" + req.getItem().getItemId() + "##l\r\n";
            }
            cm.sendSimple(text);
        }
    } else if (status == 2) {
        if (mode == 1) {
            recipientName = cm.getText();
            var target = cm.getClient().getChannelServer().getPlayerStorage().getCharacterByName(recipientName);
            if (target == null) {
                var channel = cm.getClient().getWorldServer().find(recipientName);
                if (channel > -1) {
                    var rcserv = cm.getClient().getWorldServer().getChannel(channel);
                    if (rcserv != null) {
                        target = rcserv.getPlayerStorage().getCharacterByName(recipientName);
                    }
                }
            }
            if (target == null) {
                cm.sendOk("The player '" + recipientName + "' could not be found or is not online.");
                cm.dispose();
                return;
            }
            if (target.getId() == cm.getPlayer().getId()) {
                cm.sendOk("You cannot trade with yourself.");
                cm.dispose();
                return;
            }
            targetId = target.getId();

            var inv = cm.getPlayer().getInventory(InventoryType.EQUIP);
            var iter = inv.iterator();
            eligibleItems = [];

            var text = "Please select the untradable equipment you wish to send to #b" + recipientName + "#k:\r\n\r\n";
            while (iter.hasNext()) {
                var itemObj = iter.next();
                if (itemObj.isUntradeable() && !ii.isUnmerchable(itemObj.getItemId())) {
                    if (itemObj.getUniqueId() > 0 || ii.isPickupRestricted(itemObj.getItemId())) {
                        eligibleItems.push(itemObj);
                    }
                }
            }

            if (eligibleItems.length == 0) {
                cm.sendOk("You do not have any eligible untradable items (Cash Items or One-of-a-Kind) in your equipment inventory.");
                cm.dispose();
                return;
            }

            for (var i = 0; i < eligibleItems.length; i++) {
                var itemObj = eligibleItems[i];
                text += "#L" + i + "##v" + itemObj.getItemId() + "# #t" + itemObj.getItemId() + "##l\r\n";
            }
            cm.sendSimple(text);

        } else if (mode == 2 || mode == 3) {
            selectedTrade = pendingTrades[selection];
            if (selectedTrade == null) {
                cm.dispose();
                return;
            }
            if (mode == 2) {
                cm.sendYesNo("You have received #v" + selectedTrade.getItem().getItemId() + "# #t" + selectedTrade.getItem().getItemId() + "# from #b" + selectedTrade.getSenderName() + "#k.\r\n\r\nWould you like to #bAccept#k it? If you select No, the item will be returned to the sender.");
            } else if (mode == 3) {
                cm.sendYesNo("Would you like to claim your returned #v" + selectedTrade.getItem().getItemId() + "# #t" + selectedTrade.getItem().getItemId() + "#?");
            }
        }
    } else if (status == 3) {
        if (mode == 1) {
            selectedIt = eligibleItems[selection];
            if (selectedIt == null) {
                cm.dispose();
                return;
            }

            calculatedFee = tradeManager.calculateFee(selectedIt);
            var baseFee = 10000000;
            var statFee = calculatedFee - baseFee;

            var rLevel = selectedIt.getHands(); // Rebirth Level
            var reqMatsText = "";
            var missingMats = false;

            if (rLevel >= 1) {
                reqMatsText += "\r\n#e[Rebirth Courier Surcharge]#n\r\nYour item has been rebirthed! The courier requires the following materials to transport its raw power:\r\n";
                if (rLevel >= 1) reqMatsText += "- 5x #v4032133# Zakum Diamond\r\n";
                if (rLevel >= 2) reqMatsText += "- 5x #v4001094# Horntail Egg\r\n";
                if (rLevel >= 3) reqMatsText += "- 1x #v4021010# Rock of Time\r\n";
                if (rLevel >= 4) reqMatsText += "- 1x #v4001693# Von Leon Stamp\r\n";

                if (rLevel >= 1 && !cm.haveItem(4032133, 5)) missingMats = true;
                if (rLevel >= 2 && !cm.haveItem(4001094, 5)) missingMats = true;
                if (rLevel >= 3 && !cm.haveItem(4021010, 1)) missingMats = true;
                if (rLevel >= 4 && !cm.haveItem(4001693, 1)) missingMats = true;
            }

            var confirmText = "It will cost you #r" + (calculatedFee.toLocaleString ? calculatedFee.toLocaleString() : calculatedFee) + " mesos#k to send #v" + selectedIt.getItemId() + "# #b#t" + selectedIt.getItemId() + "##k to " + recipientName + ".\r\n\r\n";
            confirmText += "Fee Breakdown:\r\n"
            confirmText += "- Base Courier Fee: 10,000,000 mesos\r\n";
            confirmText += "- Equipment Stat Surcharge: " + (statFee.toLocaleString ? statFee.toLocaleString() : statFee) + " mesos";

            var totalStats = ((statFee / 10000) > 0) ? (statFee / 10000) : 0;
            confirmText += "\r\n  #e(#r" + totalStats + " total stats * 10,000 mesos#k#e)#n\r\n";

            confirmText += reqMatsText;

            if (missingMats) {
                confirmText += "\r\n#rYou do not have the required materials in your inventory to send this rebirthed item.#k";
                cm.sendOk(confirmText);
                cm.dispose();
                return;
            }

            confirmText += "\r\nDo you want to proceed?";

            cm.sendYesNo(confirmText);
        } else if (mode == 2) {
            var actualTrade = tradeManager.getRequest(selectedTrade.getTradeId());
            if (actualTrade == null || actualTrade.isRefunding()) {
                cm.sendOk("This trade request is no longer valid.");
                cm.dispose();
                return;
            }

            if (!cm.canHold(actualTrade.getItem().getItemId(), 1)) {
                cm.sendOk("Please make some space in your Equip inventory first.");
                cm.dispose();
                return;
            }

            // Manual insertion bypasses drop restriction checks natively found in addFromDrop
            var inv = cm.getPlayer().getInventory(InventoryType.EQUIP);
            actualTrade.getItem().setPosition(inv.getNextFreeSlot());
            inv.addItem(actualTrade.getItem());
            cm.getClient().sendPacket(Packages.tools.PacketCreator.modifyInventory(true, java.util.Collections.singletonList(new Packages.client.inventory.ModifyInventory(0, actualTrade.getItem())), cm.getPlayer()));

            tradeManager.removeRequest(actualTrade.getTradeId());
            cm.sendOk("You have successfully received the item!");
            cm.dispose();
        } else if (mode == 3) {
            var actualTrade = tradeManager.getRequest(selectedTrade.getTradeId());
            if (actualTrade == null || !actualTrade.isRefunding()) {
                cm.sendOk("This trade request is no longer valid.");
                cm.dispose();
                return;
            }

            if (!cm.canHold(actualTrade.getItem().getItemId(), 1)) {
                cm.sendOk("Please make some space in your Equip inventory first.");
                cm.dispose();
                return;
            }

            var inv = cm.getPlayer().getInventory(InventoryType.EQUIP);
            actualTrade.getItem().setPosition(inv.getNextFreeSlot());
            inv.addItem(actualTrade.getItem());
            cm.getClient().sendPacket(Packages.tools.PacketCreator.modifyInventory(true, java.util.Collections.singletonList(new Packages.client.inventory.ModifyInventory(0, actualTrade.getItem())), cm.getPlayer()));

            tradeManager.removeRequest(actualTrade.getTradeId());
            cm.sendOk("You have successfully reclaimed your returned item!");
            cm.dispose();
        }
    } else if (status == 4) {
        if (mode == 1) {
            if (cm.getPlayer().getMeso() < calculatedFee) {
                cm.sendOk("You do not have enough mesos to cover the fee.");
                cm.dispose();
                return;
            }

            var inv = cm.getPlayer().getInventory(InventoryType.EQUIP);
            var currentItem = inv.getItem(selectedIt.getPosition());
            if (currentItem == null || currentItem.getItemId() != selectedIt.getItemId()) {
                cm.sendOk("The item could not be found. Please try again.");
                cm.dispose();
                return;
            }

            // Deduct fee and remove item
            cm.gainMeso(-calculatedFee);
            InventoryManipulator.removeFromSlot(cm.getClient(), InventoryType.EQUIP, selectedIt.getPosition(), 1, true);

            var rLevel = selectedIt.getHands();
            if (rLevel >= 1) cm.gainItem(4032133, -5);
            if (rLevel >= 2) cm.gainItem(4001094, -5);
            if (rLevel >= 3) cm.gainItem(4021010, -1);
            if (rLevel >= 4) cm.gainItem(4001693, -1);

            // Register trade request
            tradeManager.addTradeRequest(cm.getPlayer().getId(), cm.getPlayer().getName(), targetId, recipientName, selectedIt.copy(), calculatedFee);
            tradeManager.incrementDailySends(cm.getPlayer().getId());

            // Tell receiver
            var channel = cm.getClient().getWorldServer().find(recipientName);
            if (channel > -1) {
                var rcserv = cm.getClient().getWorldServer().getChannel(channel);
                if (rcserv != null) {
                    var target = rcserv.getPlayerStorage().getCharacterByName(recipientName);
                    if (target != null) {
                        target.dropMessage(5, "[Trade Escrow] " + cm.getPlayer().getName() + " has sent you an untradable item. Type @trade to review it.");

                        // Show popup NPC request to receiver
                        target.getClient().sendPacket(Packages.tools.PacketCreator.serverNotice(5, "[Trade Escrow] You have a new pending untradable item delivery from " + cm.getPlayer().getName() + " ! Use @trade to accept."));
                    }
                }
            }

            cm.getPlayer().dropMessage(5, "[Trade Escrow] Successfully sent " + eligibleItems[selection].getItemId() + " to " + recipientName + ".");
            cm.sendOk("The item has been sent securely!");
            cm.dispose();
        }
    }
}
