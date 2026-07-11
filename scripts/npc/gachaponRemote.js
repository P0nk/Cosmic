/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
               Matthias Butz <matze@odinms.de>
               Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
/**
 *9201142 - Witch Malady - Haunted House
 *@author BubblesDev v75 (Moogra)
 *@author DstroyerDev v83 (Revan)
 */

 function start() {
    cm.sendNext("Nice to meet you!");
    cm.dispose();
}

/*
 * creditz: nico, kane, jorn
 */
var status = 0;
var items_10   = [
3010175, // scroll for claw attack 50%
3010174, // scroll for crossbow for attack 50%
3010173, // scroll for bow for attack 50%
3010172, // scroll for polearm for attack 50%
3010171, // scroll for spear for attack 50%
3010170, // scroll for two handed blunt weapon for attack 50%
3010169, // scroll for two handed axe for attack 50%
3010168, // scroll for two handed sword for attack 50%
3010161, // scroll for staff for magic attack 50%
3010155, // scroll for wand for magic attack 50%
3010152, // scroll for dagger for attack 50%
3010139, // scroll for one handed blunt weapon for attack 50%
3010137, // scroll for one handed axe for attack 50%
3010133, // scroll for one handed sword for attack 50%
3010132, // scroll for knuckler for attack 50%
3010131, // scroll for gun for attack 50%
3010117, // black crystal blade
3010115, // 10 Gacha Tickets
3010114, // 3 Onyx Apple
3010110, // 2 Naricain's Demon Elexir
3010109, // 1 Balanced Furies
3010108, // Eternal Bullet
3010107, // black belt
3010097, // witch's crimson belt
3010096, // witch's ocean blue belt
3010095, // witch's deep purple belt
3010027, // cheetos
3010037, // pig big mouth
3010048, // xmas tree
3010050, // jap window
3010077, //  owl cage
3010086, // loli on a sofa
3010087, // ray charles
3010090, // ringo star
3010112, // locker
3010121, // destiny door
3010122, //  famine dragon
3010142, // fishtank
3010144, //  bamboo stars
3010145, // maple faries
3010148, // blue monsta
3010150, // 5th anniversary chair
3010200, // evan chair
3012012, // flower chair
3013001, // fire chair
1000030, // Sachiel Wig (M)
1000031, // Veamoth Wig (M)
1000032, // Janus Wig (M
1102097, // Janus Wings
1102148, // Tania Cloak
1102149, // Mercury Cloak
5010069,
5010075,
1702118, //Janus Sword
1702119, // Sachiel Sword
1702120, // Veamoth Sword
1072281, // Sachiel Shoes
1072282, // Veamoth Shoes
1072283, // Janus Shoes
5010076, // Yellow 
5010077,  // White
5010078, // White godly
5010079, // Black Godly
5010080, // Red Godly
3018180, //Honey Jar Chair
3018183, //Sakura Tree field chair
3018184, //Moonlight chair
3018188, //Space Yeti chair
3018190, //Nether Power chair
3018195, //Cosmic Swan chair
3018197, //Fancy Halloween chair
3018198, //Sakura Window chair
3018205, //Maple Tree field chair
3018207, //Onigiriman chair
3018209, //Mulung Dojo chair
3018218, //Outerspace Empanada chair
3018222, //PicNic chair
3018223, //Wooden Toyhouse chair
3018224, //Window chair
3018225, //Yeti Holmes chair
3018227, //Bergcream chair
3018229, //Snowman Candy chair
3018232, //Dark Chariot chair
3018233, //Simple Tent chair
3018236, //Magic Ball o Ween chair
3018237, //Login chair
3018243, //The Halloween chair
3018246, //Chill Penthouse chair
3018248, //Wolffy Houser chair
3018249, //Umbrella Cat chair
3018252, //Grilled Shroom chair
3018255, //Ancient Memory chair
3018256, //Second Place chair
3018257, //First Place chair
3018261, //Coffe Cup chair
3018263, //Jins Glasses chair
3018273, //Le Paint chair
3018275, //Dressupwall chair
3018276, //Rich Gang chair
3018278, //Bath Time chair
3018279, //Fancy Lady chair
3018280, //Fancy Man chair
3019001, //Huge Dragon chair
3019003, //New Year chair
3019999, //Small PB chair
3014019, //Neon Sign chair
3014020, //Platinum Trophy chair
3014021, //Sakura Frame chair
3014022, //Steampunk Frame chair
3014023, //Waterwheel Frame chair
3014024, //Stary Frame chair
3015006, //Xmas Presents chair
3015010, //Attack on Titan chair
3015046, //Chinese Puppet chair
3015058, //Carnival chair
3015091, //Chocoginger chair
3015106, //Telescope chair
3015155, //Alchemist chair
3015183, //Slime Pool chair
3015195, //YingYang Wolf chair
3015215, //Sharkando chair
3015225, //Slefiephant chair
3015227, //Happydays chair
3015228, //Targa chair
3015229, //Scarlion chair
3015245, //Thousand Swords chair
3015261, //Maple Chariot chair
3015275, //Hungry Sun chair
3015277, //Slayer chair
3015279, //Spiky Night chair
3015373, //Mountains Globe chair
3015670, //Dumb Yeti chair
3015680, //Bottle and Balls chair
3015696, //Frozen Slime chair
3015763, //Rocket Launch chair
3015767, //Celeb chair
3015798, //Maple Actor chair
3015898, //Wizard chair
3015906, //Forgotten Hero chair
3015907, //Arnah chair
3015991, //Night Bath chair
];

var items_30   = [
3010075, // scroll for claw attack 50%
3010068, // scroll for crossbow for attack 50%
3010058, // scroll for bow for attack 50%
3010055, // scroll for polearm for attack 50%
3010052, // scroll for spear for attack 50%
3010049, // scroll for two handed blunt weapon for attack 50%
3010036, // scroll for two handed axe for attack 50%
3010021, // scroll for two handed sword for attack 50%
3010000, // The Relaxer - Catch your breath and relax by sitting on this chair to recover 50 HP every 10 seconds.\n#cCannot be traded or dropped.#
3010001,  //Sky-blue Wooden Chair - A specially-made sky-blue wooden chair that's only available in Lith Harbor. Recover 35 HP every 10 seconds.
3010002, //Green Chair - A comfortable, plush green chair, complete with arm-rests. Recovers 50 HP every 10 seconds.
3010003, // Red Chair - A comfortable, plush red chair, complete with arm-rests.. Recovers 50 HP every 10 seconds.
3010004, // The Yellow Relaxer - Catch your breath and relax by sitting on this chair to recover 50 HP every 10 seconds. Perfect for a quick break from training.
3010005, // The Red Relaxer - Catch your breath and relax by sitting on this chair to recover 50 HP every 10 seconds.  Perfect for a quick break from training.
3010006, // Yellow Chair - A comfortable, plush yellow chair, complete with arm-rests. Recovers 50 HP every 10 seconds.
3010007, // Pink Seal Cushion - An adorable pink cushion that resembles a seal.  Recovers HP 60 every 10 seconds.
3010008, // Blue Seal Cushion - An adorable blue cushion that resembles a seal.  Recovers HP 60 every 10 seconds.
3010009, // Red Round Chair - Rumored to be crafted in Amoria, this special chair is also known as the Love Seat. Recover 20 HP and 20 MP every 10 seconds.
3010010, // White Seal Cushion - An adorable white cushion that resembles a seal.  Recovers HP 50 every 10 seconds.
3010011, // Amorian Relaxer - A chair crafted by Jacob. Recovers 75 HP every 10 seconds.\n#cCannot be traded or dropped.#
3010012, // Warrior Throne - A powerful chair used often on the battlefield. Recovers 60 HP every 10 seconds.\n#cCannot be traded or dropped.#
3010013, // Beach Chair - A chair straight from the relaxation experts in Florina Beach. Recover 20 HP every 10 seconds.
3010014, // Moon Star Chair - A light, stylish chair that seems to be sent from the heavens. Recover 30 HP and 30 MP every 10 seconds.
3010015, // The Red Relaxer - A chair with magical properties crafted in Ellinia. Restores 35 MP per 10 Seconds while sitting.
3010016, // Grey Seal Cushion - An adorable grey cushion that resembles a seal.  Recovers HP 60 every 10 seconds.
3010017, // Gold Seal Cushion - An adorable gold cushion that resembles a seal.  Recovers MP 60 every 10 seconds.
3010018, // Palm Tree Beach Chair - A beach chair placed in the shades under the palm tree at Ariant. Sit on it to recover HP 40 and MP 20 every 10 seconds.
3010019, // Kadomatsu - A specially-made chair that's only available in Mushroom Shrine. Recover 60 MP every 10 seconds.
3010025, // Under the Maple Tree... - A white chair commemorating the 4th anniversary of MapleStory. Sit on it to recover HP 35 and MP 10 every 10 seconds.
3011000, // Fishing Chair - The perfect chair for fishing.
];


var items_50   = [
3010072, // Miwok Chief's Chair - The chair in which the chiefs of the Miwok tribe sat. If you sit in this chair, you can receive the strength of the Miwok ancestors and recover 65MP in 10 seconds.
3010058, // WorldEnd - You will recover 50 HP every 10 seconds. Perhaps, as you recline, you will find the answer to many of life's questions.
3010057, // BloodyRose - You will recover 50 HP every 10 seconds. You will experience the might of a conqueror after recovery.
3010060, // Noblesse Chair - A chair makes you feel like you're sitting in the lap of luxury. Also recovers 50 HP every 10 seconds.
3010061, // Underneath the Maple Tree� - A white chair created to celebrate Maple Story's 6th Anniversary. Sit on it to restore 35 HP and 10 MP every 10 seconds.
3010062, // Bamboo Chair  - A chair that restores HP every 10 seconds when used. It's very strong since it was made from bamboo grown on Rien.
3010063, // Moon and Star Cushion - A pretty cushion shaped like a moon. Recovers 60 HP every 10 seconds.
3010064, // Male Desert Rabbit Cushion   - 60 HP is restored every 10 seconds if you lean back on this cute Male Desert Rabbit Cushion.
3010065, // Pink Beach Parasol - A pink beach chair that makes you want to go to the beach. Restores 60 HP every 10 seconds.
3010066, // Navy Velvet Sofa   - A luxurious velvet sofa dyed with a beautiful shade of navy. Restores 60 HP every 10 seconds.
3010067, // Red Designer Chair   - A designer chair that glows with a passionate red. Restores 60 HP every 10 seconds.
3010043, // Halloween Broomstick Chair - When you sit on the Halloween Broomstick Chair, 50 MP is restored every 10 seconds
3010071, // Mini Shinsoo Chair - When you rest on the Shinsoo, 50 HP and 50 MP are restored every 10 seconds
3010085, // Olivia's Chair - An eerie looking chair that resembles Olivia. Recovers 40 HP and 35 MP every 10 seconds
3010098, // TV Recliner - A new chair to recline and relax in throughout the Thanksgiving holiday.\nRecovers 60 HP and 30 MP every 10 seconds.
3010116, // The Spirit of Rock Chair - A new chair that makes you feel like a Rock Star.\nRecovers 60 HP and 30 MP every 10 seconds.
3010101, // Christmas Gift Box - A huge X-mas Gift Box big enough to fit a grownup. It has a message that reads, "I am here for you." Sitting in it will recover 50 HP and MP every 10 seconds.
3010073, // Giant Pink Bean Cushion - A cushion that resembles Pink Bean, the underling of the Black Mage. Leaning against the cushion and resting will recover 50 HP and 30 MP every 10 seconds. 
3010099, // Cuddly Polar Bear - Cuddling with the Polar Bear for some cozy, comfy rest will recover 50 HP and 50 MP every 10 seconds.
3010044, // Winter Red Chair  - A chair with a big umbrella. Recovers 30 HP and 30 MP every 10 seconds.
3010106, // Ryko Chair - Snuggle up with Aran's loyal mount, Ryko, to recover 50 HP and MP every 10 seconds.
3010111, // Tiger Skin Chair - Lean back on this imposing Tiger Skin Chair to restore 50 HP and 30 MP every 10 seconds.
3010080, // Swing on the Persimmon Tree - There is a swing on the Persimmon Tree with ripe persimmons.
3010081, // ??? ?? ?? - ??? ??? ?? ??? ? ??? ??? ??? ? ? ??.
3010082, // ??? ?? ?? - ??? ??? ?? ??? ? ??? ??? ??? ? ? ??.
3010083, // ??? ?? ?? - ??? ??? ??? ?? ???? ?? ??? ??? ? ? ??.
3010084, // ??? ?? ?? - ??? ??? ??? ?? ?? ???? ??? ? ? ??.
3010092, // Witch's Broomstick - Hold onto the Broomstick so you don't fall off.
3012010, // Half-Heart Chocolate Cake Chair - Sink yourself into this heavenly cake chair next to someone else who owns it as well, and watch it create a scrumptious effect! Mmm, this chair is so delicious you'll recover 50 HP every 10 seconds.
3012011, // Chocolate Fondue Chair - Yummy! Use this chair next to someone else who's also using it, and a mouth-watering chocolate fondue appears. Smack your lips and dream about treats as you recover 50 HP every 10 seconds. 
3010069, // Yellow Robot Chair - Perch yourself on the hand of this powerful yellow robot to recover 50 HP and 30 MP every 10 seconds.

];


var items      = new Array(items_10,items_30,items_50);

function start() {
    status = 0;
    action (1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1 || (mode == 0 && status == 0)) {
        cm.dispose();
    } else {
        if (mode == 1) {
            status++;
        } else {
            status--;
        }
        if (status == 1) {
            cm.sendYesNo("If you give me 1 donor point you will get:\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n\r\n#fUI/UIWindow.img/QuestIcon/5/0# #bx 1 Mysterious NX or Chair#k\r\n\r\n");
        } else if (status == 2) {
            if (cm.haveItem(5220000, 1)) {
                chance = Math.ceil(Math.random() * 100);
                var type_ = 2;
                if (chance <= 10) {
                    type_ = 0;
                } else if (chance <= 30){
                    type_ = 1;
                }
                random = Math.floor(Math.random() * items[type_].length);
                itemid = items[type_][random];
                if (cm.canHold(itemid)) { // Check if player can hold the item
                    cm.gainItem(5220000, -1);
                    cm.gainItem(itemid, 1); // Ensure the quantity is set to 1
                    cm.sendOk("Thank you for the Donation!\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n\r\n#v" + itemid + "# #b#z" + itemid + "#\r\n\r\n");
                    cm.dispose();
                } else {
                    cm.sendOk("Make some space in your inventory; where am I going to put the chair?");
                    cm.dispose();
                }
            } else {
                cm.sendOk("I'm sorry #h #, in order to receive:\r\n\r\n#fUI/UIWindow.img/QuestIcon/5/0# #bx 1 Mysterious Chair#k you would need to have at least 1 donor point\r\n\r\n");
                cm.dispose();
            }
        }
    }
}

