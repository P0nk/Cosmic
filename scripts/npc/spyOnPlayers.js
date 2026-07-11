/***

 Spy player NPC - Limbs

 Author - @Limbs

 ***/
var status = 0;
var sel;
var playerName = "";

function start() {
  status = -1;
  action(1, 0, 0);
}

function action(mode, type, selection) {
  if (mode == 1) {
    status++;
  } else {
    cm.dispose();
    return;
  }

  const longLine = "#fUI/UIWindow.img/UserList/Party/party3#";
  const selectItem = "";

  const GameConstants = Java.type("constants.game.GameConstants");
  const prepaidBalance = cm.getNx(4);
  const nxCreditBalance = cm.getNx(1);

  const optionList = ["Check Equipment", "Check Inventory", "Check Info"];
  if (status == 0) {
    cm.sendGetText("Welcome to the Spy Hub.\r\n"
        + "Who would you like to spy on?\r\n\r\n"
        + "Type their name below, the player name is \'cAsE SeNsiTivE\'"
    );
  } else if (status == 1) {
    playerName = cm.getText();
    const player = cm.getClient().getWorldServer().getPlayerStorage().getCharacterByName(playerName);
    if (player == null) {
      cm.getPlayer().dropMessage(1, "The player you are trying to search is either offline or does not exist.");
      cm.dispose();
      return;
    }

    let choiceList = "";
    for (let i = 0; i < optionList.length; i++) {
      choiceList += "#L" + i + "##b" + optionList[i] + "#k#l\r\n";
    }
    cm.sendSimple("You are currently spying on player: #e" + playerName + "#n\r\n\r\n"
        + "What would you like to spy on?\r\n\r\n"
        + selectItem + "\r\n#e"
        + choiceList + "#n");

  } else if (status == 2) {
    switch (selection) {
      case 0: //spy equips
        sel = 0;
        cm.sendNextPrev(cm.spyOnPlayer("equip", playerName));
        break;

      case 1: //spy inventory
        sel = 1;
        cm.sendNextPrev(cm.spyOnPlayer("inventory", playerName));
        break;

      case 2: //spy info
        sel = 2;
        cm.sendNextPrev(cm.spyOnPlayer("info", playerName));
        break;

      default:
        cm.dispose();
    }
    cm.dispose();
  }
}