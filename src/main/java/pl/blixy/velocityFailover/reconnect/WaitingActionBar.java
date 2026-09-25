package pl.blixy.velocityFailover.reconnect;

import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import pl.blixy.velocityFailover.config.FailoverConfig;

import java.util.List;
import java.util.UUID;

/** Shows the next spinner frame to everyone waiting on limbo; the frames are rendered once, at config load. */
public final class WaitingActionBar implements Runnable {

    private final ProxyServer proxy;
    private final FailoverConfig config;
    private final WaitingPlayers waiting;
    private int frame;

    public WaitingActionBar(ProxyServer proxy, FailoverConfig config, WaitingPlayers waiting) {
        this.proxy = proxy;
        this.config = config;
        this.waiting = waiting;
    }

    @Override
    public void run() {
        List<Component> frames = config.actionBar().frames();
        if (frames.isEmpty()) {
            return;
        }

        Component message = frames.get(frame);
        frame = (frame + 1) % frames.size();
        for (UUID uuid : waiting.all()) {
            proxy.getPlayer(uuid).filter(config::isLimbo).ifPresent(player -> player.sendActionBar(message));
        }
    }
}
