package pl.blixy.velocityFailover.handler;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import pl.blixy.velocityFailover.config.FailoverConfig;
import pl.blixy.velocityFailover.reconnect.WaitingPlayers;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ServerDownHandler {

    private static final long REDIRECT_SETTLE_MS = 500;

    private final Object plugin;
    private final ProxyServer proxy;
    private final Logger logger;
    private final FailoverConfig config;
    private final WaitingPlayers pendingRegistry;

    public ServerDownHandler(Object plugin, ProxyServer proxy, Logger logger, FailoverConfig config, WaitingPlayers pendingRegistry) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.logger = logger;
        this.config = config;
        this.pendingRegistry = pendingRegistry;
    }

    public void handle(String serverName) {
        proxy.getScheduler().buildTask(plugin, () -> sweepStragglers(serverName))
                .delay(REDIRECT_SETTLE_MS, TimeUnit.MILLISECONDS)
                .schedule();
    }

    private void sweepStragglers(String serverName) {
        Optional<RegisteredServer> serverOpt = proxy.getServer(serverName);
        if (serverOpt.isEmpty()) return;

        Optional<RegisteredServer> limboOpt = proxy.getServer(config.limbo());
        if (limboOpt.isEmpty()) {
            logger.error("[Failover] CRITICAL: Limbo server '{}' not found! Cannot redirect players.", config.limbo());
            return;
        }

        RegisteredServer limbo = limboOpt.get();
        RegisteredServer server = serverOpt.get();

        for (Player player : server.getPlayersConnected()) {
            if (!pendingRegistry.add(player.getUniqueId(), serverName)) {
                continue;
            }

            player.sendMessage(config.messages().sentToLimbo());
            player.createConnectionRequest(limbo).connect()
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logger.warn("[Failover] Failed to move player {} to limbo: {}", player.getUsername(), error.getMessage());
                        }
                    });
        }
    }
}
