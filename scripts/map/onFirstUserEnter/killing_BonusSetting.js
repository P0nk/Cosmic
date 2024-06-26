function start(ms) {
    const PacketCreator = Java.type('tools.PacketCreator');
    ms.getPlayer().resetEnteredScript();
    ms.getPlayer().sendPacket(PacketCreator.getClock(60));
    ms.getPlayer().sendPacket(PacketCreator.mapEffect("killing/bonus/bonus"));
    ms.getPlayer().sendPacket(PacketCreator.mapEffect("killing/bonus/stage"));
}