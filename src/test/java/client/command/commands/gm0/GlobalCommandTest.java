package client.command.commands.gm0;

import client.Character;
import client.Client;
import io.netty.buffer.Unpooled;
import net.packet.ByteBufInPacket;
import net.packet.Packet;
import net.server.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import server.ChatLogger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalCommandTest {
    @Mock
    private Client client;
    @Mock
    private Character player;
    @Mock
    private World world;

    @Test
    void broadcastsOriginalMessageToTheWholeWorld() {
        when(client.getPlayer()).thenReturn(player);
        when(client.getWorldServer()).thenReturn(world);
        when(player.getName()).thenReturn("RustPocChar");
        when(player.getLastCommandMessage()).thenReturn("Heading to Henesys");

        try (MockedStatic<ChatLogger> loggerStatic = mockStatic(ChatLogger.class)) {
            new GlobalCommand().execute(client, new String[]{"heading", "to", "henesys"});

            var packetCaptor = org.mockito.ArgumentCaptor.forClass(Packet.class);
            verify(world).broadcastPacket(packetCaptor.capture());
            var packet = new ByteBufInPacket(
                    Unpooled.wrappedBuffer(packetCaptor.getValue().getBytes()));
            assertEquals(0x0044, packet.readShort());
            assertEquals(6, packet.readUnsignedByte());
            assertEquals("[Global] RustPocChar: Heading to Henesys", packet.readString());
            assertEquals(0, packet.readInt());
            assertEquals(0, packet.available());
            loggerStatic.verify(() -> ChatLogger.log(client, "Global", "Heading to Henesys"));
        }
    }

    @Test
    void rejectsBlankMessagesWithoutBroadcastingOrLogging() {
        when(client.getPlayer()).thenReturn(player);
        when(player.getLastCommandMessage()).thenReturn("   ");

        try (MockedStatic<ChatLogger> loggerStatic = mockStatic(ChatLogger.class)) {
            new GlobalCommand().execute(client, new String[]{});

            verify(player).yellowMessage("Usage: @global <message>");
            verify(world, never()).broadcastPacket(any(Packet.class));
            loggerStatic.verifyNoInteractions();
        }
    }
}
