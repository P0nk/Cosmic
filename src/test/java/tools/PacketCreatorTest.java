package tools;

import net.packet.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PacketCreatorTest {

    @Test
    void serverMessage_withValidMessage_returnsPacket() {
        String message = "Test server message";
        Packet packet = PacketCreator.serverMessage(message);
        assertNotNull(packet);
    }

    @Test
    void serverMessage_withNullMessage_returnsPacket() {
        String message = null;
        Packet packet = PacketCreator.serverMessage(message);
        assertNotNull(packet);
    }
}