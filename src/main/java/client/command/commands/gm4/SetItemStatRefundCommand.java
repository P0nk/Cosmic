package client.command.commands.gm4;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.Equip;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import constants.inventory.ItemConstants;

public class SetItemStatRefundCommand extends Command {
    {
        setDescription("Set stats of all equips in inventory.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !setitemstat <str> <dex> <int> <luk> <watk> <matk> <hands> <wdef> <mdef> <speed> <jump> <avoid> <acc>");
            return;
        }

        short newStr = (short) Math.max(0, Integer.parseInt(params[0]));
        short newDex = (short) Math.max(0, Integer.parseInt(params[1]));
        short newInt = (short) Math.max(0, Integer.parseInt(params[2]));
        short newLuk = (short) Math.max(0, Integer.parseInt(params[3]));
        short newWatk = (short) Math.max(0, Integer.parseInt(params[4]));
        short newMatk = (short) Math.max(0, Integer.parseInt(params[5]));
        short newHands = (short) Math.max(0, Integer.parseInt(params[6]));
        short newWdef = (short) Math.max(0, Integer.parseInt(params[7]));
        short newMdef = (short) Math.max(0, Integer.parseInt(params[8]));
        short newSpeed = (short) Math.max(0, Integer.parseInt(params[9]));
        short newJump = (short) Math.max(0, Integer.parseInt(params[10]));
        short newAvoid = (short) Math.max(0, Integer.parseInt(params[11]));
        short newAccuracy = (short) Math.max(0, Integer.parseInt(params[12]));
        Inventory equip = player.getInventory(InventoryType.EQUIP);

        for (byte i = 1; i <= equip.getSlotLimit(); i++) {
            //        byte i = 1;
            try {
                Equip eq = (Equip) equip.getItem(i);
                if (eq == null) {
                    continue;
                }

                eq.setMatk(newMatk);
                eq.setWatk(newWatk);
                eq.setDex(newDex);
                eq.setInt(newInt);
                eq.setStr(newStr);
                eq.setLuk(newLuk);
                eq.setHands(newHands);
                eq.setMdef(newMdef);
                eq.setWdef(newWdef);
                eq.setSpeed(newSpeed);
                eq.setJump(newJump);
                eq.setAvoid(newAvoid);
                eq.setAcc(newAccuracy);

                short flag = eq.getFlag();
               // flag |= ItemConstants.UNTRADEABLE;
                eq.setFlag(flag);

                player.forceUpdateItem(eq);
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}