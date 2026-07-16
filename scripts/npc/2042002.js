/**
 -- Version Info -----------------------------------------------------------------------------------
 1.0 - First Version by Drago (MapleStorySA)
 2.0 - Second Version by Ronan (HeavenMS)
 3.0 - Third Version by Jayd - translated CPQ contents to English and added Pirate items
 4.0 - Fourth Version by ForgeMasterVold - removed CPQ all together and set up NPC for ForgeMS esthetic updates.
 ---------------------------------------------------------------------------------------------------
 **/

/*
 * Vidar — ForgeMS Boss Warper
 * Temporary NPC: Spiegelmann
 * NPC ID: 2042002
 *
 * Purpose:
 * - Sends players directly to selected non-FM boss maps
 * - No level restrictions
 * - No party restrictions
 * - No quest checks
 *
 * Important:
 * - Pink Bean is intentionally excluded
 * - FM-spawned meta bosses should not be added here
 */

var status = -1;
var selectedCategory = -1;
var selectedBoss = -1;

/*
 * Early / Regional Bosses
 */
var earlyBosses = [
 { name: "Mushmom", map: 100000005 },
 { name: "Blue Mushmom", map: 101030404 },
 { name: "Zombie Mushmom", map: 105040306 },
 { name: "Deo", map: 221040301 },
 { name: "Tae Roon", map: 250020300 },
 { name: "Eliza", map: 300000000 }
];

/*
 * Major Bosses
 *
 * These IDs must match the working command destinations.
 */
var majorBosses = [
 { name: "Pianus", map: 230040420 },
 { name: "Papulatus", map: 220080000 },
 { name: "Zakum", map: 211042300 },
 { name: "Horntail", map: 240050400 }
];

/*
 * Distant Bosses
 */
var distantBosses = [
 { name: "Scarlion", map: 551030200 },
 { name: "Targa", map: 551030100 },
 { name: "Krexel", map: 610030000 },
 { name: "Crimsonwood Keep", map: 610030000 },
 { name: "Balrog", map: 105100300 },
 { name: "Crimson Balrog", map: 105100400 }
];

function start() {
 action(1, 0, 0);
}

function action(mode, type, selection) {
 if (mode === -1) {
  cm.dispose();
  return;
 }

 /*
  * Cancel / Back handling
  */
 if (mode === 0) {
  if (status <= 0) {
   cm.dispose();
   return;
  }

  if (status === 1) {
   status = -1;
   selectedCategory = -1;
   selectedBoss = -1;
   action(1, 0, 0);
   return;
  }

  if (status === 2) {
   status = 0;
   selectedBoss = -1;
   showCategoryMenu(selectedCategory);
   return;
  }

  cm.dispose();
  return;
 }

 status++;

 /*
  * Main menu
  */
 if (status === 0) {
  showMainMenu();
  return;
 }

 /*
  * Category selected
  */
 if (status === 1) {
  selectedCategory = selection;
  selectedBoss = -1;

  showCategoryMenu(selectedCategory);
  return;
 }

 /*
  * Boss selected
  */
 if (status === 2) {
  selectedBoss = selection;

  var destination = getSelectedDestination();

  if (destination === null) {
   cm.sendOk(
       "That trail is not marked on my map."
   );
   cm.dispose();
   return;
  }

  cm.sendYesNo(
      "#e" + destination.name + "#n\r\n\r\n" +
      "I can guide you directly to this hunting ground.\r\n\r\n" +
      "Shall we depart?"
  );

  return;
 }

 /*
  * Confirmed warp
  */
 if (status === 3) {
  var confirmedDestination = getSelectedDestination();

  if (confirmedDestination === null) {
   cm.sendOk(
       "I seem to have lost the trail. Speak with me again."
   );
   cm.dispose();
   return;
  }

  cm.warp(confirmedDestination.map, 0);
  cm.dispose();
 }
}

function showMainMenu() {
 selectedCategory = -1;
 selectedBoss = -1;

 cm.sendSimple(
     "#eThe Coalition keeps watch over every dangerous trail.#n\r\n\r\n" +
     "I do not summon beasts. I know where they dwell.\r\n" +
     "Choose the hunt, and I will guide you there.\r\n\r\n" +

     "#L0##bEarly and Regional Hunts#k#l\r\n" +
     "Mushmoms and lesser regional threats.\r\n\r\n" +

     "#L1##rMajor Bosses#k#l\r\n" +
     "Pianus, Papulatus, Zakum, and Horntail.\r\n\r\n" +

     "#L2##dDistant Expeditions#k#l\r\n" +
     "Malaysia, Masteria, and other remote threats."
 );
}

function showCategoryMenu(category) {
 var bosses = getCategoryBosses(category);

 if (bosses === null) {
  cm.sendOk(
      "That route is not currently available."
  );
  cm.dispose();
  return;
 }

 var heading = "";

 if (category === 0) {
  heading =
      "#eEarly and Regional Hunts#n\r\n\r\n" +
      "These beasts roam the older roads of Maple World.\r\n\r\n";
 } else if (category === 1) {
  heading =
      "#eMajor Bosses#n\r\n\r\n" +
      "These enemies demand preparation and a steady weapon.\r\n\r\n";
 } else if (category === 2) {
  heading =
      "#eDistant Expeditions#n\r\n\r\n" +
      "These trails reach far beyond the Coalition's common roads.\r\n\r\n";
 }

 var menu = heading;

 for (var i = 0; i < bosses.length; i++) {
  menu +=
      "#L" + i + "##b" +
      bosses[i].name +
      "#k#l\r\n";
 }

 cm.sendSimple(menu);
}

function getCategoryBosses(category) {
 if (category === 0) {
  return earlyBosses;
 }

 if (category === 1) {
  return majorBosses;
 }

 if (category === 2) {
  return distantBosses;
 }

 return null;
}

function getSelectedDestination() {
 var bosses = getCategoryBosses(selectedCategory);

 if (bosses === null) {
  return null;
 }

 if (selectedBoss < 0 || selectedBoss >= bosses.length) {
  return null;
 }

 return bosses[selectedBoss];
}