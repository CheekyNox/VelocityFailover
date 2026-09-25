package pl.blixy.velocityFailover.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import pl.blixy.velocityFailover.config.FailoverConfig;
import pl.blixy.velocityFailover.reconnect.WaitingPlayers;

import java.util.UUID;

public class DisconnectListener {

    private final FailoverConfig config;
    private final WaitingPlayers pendingRegistry;

    public DisconnectListener(FailoverConfig config, WaitingPlayers pendingRegistry) {
        this.config = config;
        this.pendingRegistry = pendingRegistry;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        pendingRegistry.remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        if (!pendingRegistry.contains(playerId)) return;

        String connectedServer = event.getServer().getServerInfo().getName();
        String pendingServer = pendingRegistry.serverOf(playerId).orElse(null);

        if (!connectedServer.equals(config.limbo()) && !connectedServer.equals(pendingServer)) {
            pendingRegistry.remove(playerId);
        }
    }
}
