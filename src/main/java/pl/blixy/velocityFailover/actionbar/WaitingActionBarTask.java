package pl.blixy.velocityFailover.actionbar;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import pl.blixy.velocityFailover.config.FailoverConfig;
import pl.blixy.velocityFailover.reconnect.PendingReconnectRegistry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WaitingActionBarTask implements Runnable {

    private final ProxyServer proxy;
    private final FailoverConfig config;
    private final PendingReconnectRegistry pendingRegistry;
    private int frameIndex = 0;

    public WaitingActionBarTask(ProxyServer proxy, FailoverConfig config, PendingReconnectRegistry pendingRegistry) {
        this.proxy = proxy;
        this.config = config;
        this.pendingRegistry = pendingRegistry;
    }

    @Override
    public void run() {
        List<String> frames = config.getSpinnerFrames();
        if (frames.isEmpty()) return;

        String frame = frames.get(frameIndex % frames.size());
        frameIndex = (frameIndex + 1) % frames.size();

        String rendered = config.getWaitingActionBarMessage().replace("{spinner}", frame);
        Component message = MiniMessage.miniMessage().deserialize(rendered);
        String limboName = config.getLimboServer();

        for (UUID uuid : pendingRegistry.snapshotPlayers()) {
            Optional<Player> playerOpt = proxy.getPlayer(uuid);
            if (playerOpt.isEmpty()) continue;

            Player player = playerOpt.get();
            boolean onLimbo = player.getCurrentServer()
                    .map(conn -> conn.getServerInfo().getName().equals(limboName))
                    .orElse(false);
            if (!onLimbo) continue;

            player.sendActionBar(message);
        }
    }
}
