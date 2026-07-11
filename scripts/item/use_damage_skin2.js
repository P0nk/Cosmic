const DamageSkin = Java.type('client.DamageSkin');

let damageSkinItem;

function start() {
    damageSkinItem = Number(im.getValue());

    const str = 'Do you want to use the #b#z' + damageSkinItem + '##k?\r\n\r\n';
    // str += '#fItem/Consume/0243.img/0' + damageSkinItem + '/info/sample#'; // not all damage skin items have a sample image
    im.sendYesNo(str);
}

function action(mode, type, selection) {
    if (mode === 1 && im.haveItem(damageSkinItem)) {
        const damageSkinId = DamageSkin.getDamageSkinId(damageSkinItem);
        if (damageSkinId == im.getPlayer().getDamageSkin()) {
            im.getPlayer().setDamageSkin(0, true);
            im.sendOk('Your damage skin has been removed.');
        } else {
            im.getPlayer().setDamageSkin(damageSkinId, true);
            im.sendOk('Your damage skin has been changed to #b#z' + damageSkinItem + '##k.');
        }
    }
    im.dispose();
}