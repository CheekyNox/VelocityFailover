package pl.blixy.velocityFailover.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import pl.blixy.velocityFailover.config.FailoverConfig;
import pl.blixy.velocityFailover.reconnect.PendingReconnectRegistry;
import pl.blixy.velocityFailover.handler.ServerDownHandler;
import pl.blixy.velocityFailover.server.ServerStates;

import java.util.Optional;

public class KickListener {

    private final ProxyServer proxy;
    private final FailoverConfig config;
    private final ServerStates stateRegistry;
    private final PendingReconnectRegistry pendingRegistry;
    private final ServerDownHandler downHandler;

    public KickListener(ProxyServer proxy, FailoverConfig config, ServerStates stateRegistry, PendingReconnectRegistry pendingRegistry, ServerDownHandler downHandler) {
        this.proxy = proxy;
        this.config = config;
        this.stateRegistry = stateRegistry;
        this.pendingRegistry = pendingRegistry;
        this.downHandler = downHandler;
    }

    @Subscribe(priority = 100)
    public void onKick(KickedFromServerEvent event) {
        String serverName = event.getServer().getServerInfo().getName();

        if (!stateRegistry.isMonitored(serverName)) return;

        if (!isShutdownKick(event)) return;

        if (stateRegistry.markOffline(serverName)) {
            downHandler.handle(serverName);
        }

        Optional<RegisteredServer> limboOpt = proxy.getServer(config.limbo());
        if (limboOpt.isEmpty()) return;

        pendingRegistry.register(event.getPlayer().getUniqueId(), serverName);
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(limboOpt.get(), config.messages().sentToLimbo()));
    }

    private boolean isShutdownKick(KickedFromServerEvent event) {
        Optional<Component> reasonOpt = event.getServerKickReason();
        if (reasonOpt.isEmpty()) {
            return false;
        }

        String plainReason = PlainTextComponentSerializer.plainText().serialize(reasonOpt.get());
        for (String keyword : config.shutdownKeywords()) {
            if (plainReason.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
