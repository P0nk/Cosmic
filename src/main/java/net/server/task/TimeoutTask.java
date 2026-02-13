package net.server.task;

import client.Character;
import config.YamlConfig;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.TransitionService;

import java.util.Collection;

/**
 * @author Shavit
 */
public class TimeoutTask extends BaseTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(TimeoutTask.class);
    private final TransitionService transitionService;

    public TimeoutTask(World world, TransitionService transitionService) {
        super(world);
        this.transitionService = transitionService;
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        Collection<Character> chars = wserv.getPlayerStorage().getAllCharacters();
        for (Character chr : chars) {
            if (time - chr.getClient().getLastPacket() > YamlConfig.config.server.TIMEOUT_DURATION) {
                log.info("Chr {} auto-disconnected due to inactivity", chr.getName());
                transitionService.disconnect(chr.getClient(), true);
            }
        }
    }
}
