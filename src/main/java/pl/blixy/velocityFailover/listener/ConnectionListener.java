package pl.blixy.velocityFailover.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import pl.blixy.velocityFailover.config.FailoverConfig;
import pl.blixy.velocityFailover.reconnect.WaitingPlayers;
import pl.blixy.velocityFailover.server.ServerStates;

public class ConnectionListener {

    private final FailoverConfig config;
    private final ServerStates stateRegistry;
    private final WaitingPlayers pendingRegistry;

    public ConnectionListener(FailoverConfig config, ServerStates stateRegistry, WaitingPlayers pendingRegistry) {
        this.config = config;
        this.stateRegistry = stateRegistry;
        this.pendingRegistry = pendingRegistry;
    }

    @Subscribe(priority = 100)
    public void onConnect(ServerPreConnectEvent event) {
        if (event.getResult().getServer().isEmpty()) return;

        String serverName = event.getResult().getServer().get().getServerInfo().getName();

        if (!stateRegistry.isMonitored(serverName)) return;

        if (stateRegistry.isAvailable(serverName)) return;

        if (pendingRegistry.serverOf(event.getPlayer().getUniqueId()).filter(serverName::equals).isPresent()) return;

        event.setResult(ServerPreConnectEvent.ServerResult.denied());

        event.getPlayer().sendMessage(config.messages().connectionBlocked());
    }
}
