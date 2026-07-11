function start(ms) {
    ms.mapEffect("maplemap/enter/910000008");
    var props = ms.getPlayer().getMap().getMapObjectProperties("brambleScramble", "13");
    for (var i = 0; i < props.size(); i++) {
        var prop = props.get(i);
        ms.spawnItemDropOnMap(prop.pos(), prop.getTagInt("itemId"), prop.getTagInt("qty", 1));
    }
}   