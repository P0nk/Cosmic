/**
 * ADMIN NPC
 * Purin, triggers search query
 * @author Chronos
 */let status = 0;
 let object;
 let type;
 
 function start() {
     if (cm.getPlayer().getDataSearch() !== null) {
         cm.sendSimple(cm.getPlayer().getDataSearch());
     } else {
         cm.sendOk("Hello.");
         cm.dispose();
     }
 }
 
 function action(m, t, s) {
     if (m !== 1) {
         cm.dispose();
         return;
     }
     status++;
     if (status === 1) {
         object = cm.getPlayer().getDataSearchArr().get(s);
         type = cm.getPlayer().getDataSearchType();
         if (type === "item" || type === "mob") {
             cm.sendGetNumber("How many " +type+"s would you like?", 1, 1, 1000);
         } else if (type === "npc") {
             cm.makeNpc(object);
             cm.dispose();
         } else if (type === "map") {
             cm.sendSimple("Warping to #e#m" + object + "##n\r\n#b#L1#Warp #eyourself#l\r\n#n#r#L2#Warp #eall players#n in #ecurrent map#l");
         } else if (type === "quest") {
             cm.sendSimple("What would you like to do with the quest #e" + object + "#n?\r\n#e#b#L1#Start quest#l\r\n#r#L2#Reset quest#l\r\n#g#L3#Finish quest#l");
         }
     } else if (status === 2) {
         if (type === "item") {
             cm.gainItem(object, s);
             cm.sendOk("You got #r" + s + "x#k #b#t" + object + "#");
         } else if (type === "mob") {
             cm.summonMob(object, s);
         } else if (type === "map") {
             if (s === 1) {
                 cm.warp(object);
             } else if (s === 2) {
                 cm.warpMap(object);
             }
         } else if (type === "quest") {
             if (s===1) {
                 cm.forceStartQuest(object);
             } else if (s===2) {
                 cm.resetQuest(object);
                 cm.sendOk("Quest " + object + " was reset.");
             } else if (s===3) {
                 cm.forceCompleteQuest(object);
             }
         }
         cm.dispose();
     }
 }