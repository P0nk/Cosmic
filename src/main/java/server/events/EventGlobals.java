package server.events;

import server.life.LifeFactory;
import client.inventory.manipulator.InventoryManipulator;
import tools.PacketCreator;
// Add other common imports here

public class EventGlobals {
    // We expose static classes directly to the engine
    public static Class<?> getLifeFactory() { return LifeFactory.class; }
    public static Class<?> getMapleLifeFactory() { return LifeFactory.class; }
    public static Class<?> getInventoryHandler() { return InventoryManipulator.class; }
    public static Class<?> getPacketCreator() { return PacketCreator.class; }

    // You can add helper methods here that simplify complex Java calls
    // public static void spawnMob(int id, int map) { ... }
}