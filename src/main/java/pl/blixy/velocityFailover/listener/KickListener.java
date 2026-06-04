package pl.blixy.velocityFailover.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import pl.blixy.velocityFailover.config.FailoverConfig;
import pl.blixy.velocityFailover.reconnect.PendingReconnectRegistry;
import pl.blixy.velocityFailover.server.ServerStateRegistry;

import java.util.Optional;

public class KickListener {

    private final ProxyServer proxy;
    private final FailoverConfig config;
    private final ServerStateRegistry stateRegistry;
    private final PendingReconnectRegistry pendingRegistry;

    public KickListener(ProxyServer proxy, FailoverConfig config, ServerStateRegistry stateRegistry, PendingReconnectRegistry pendingRegistry) {
        this.proxy = proxy;
        this.config = config;
        this.stateRegistry = stateRegistry;
        this.pendingRegistry = pendingRegistry;
    }

    @Subscribe(priority = 100)
    public void onKick(KickedFromServerEvent event) {
        String serverName = event.getServer().getServerInfo().getName();

        if (!stateRegistry.isMonitored(serverName)) return;

        if (!isShutdownKick(event)) return;

        stateRegistry.markOffline(serverName);

        Optional<RegisteredServer> limboOpt = proxy.getServer(config.getLimboServer());
        if (limboOpt.isEmpty()) return;

        pendingRegistry.register(event.getPlayer().getUniqueId(), serverName);
        Component message = MiniMessage.miniMessage().deserialize(config.getSentToLimboMessage());
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(limboOpt.get(), message));
    }

    private boolean isShutdownKick(KickedFromServerEvent event) {
        Optional<Component> reasonOpt = event.getServerKickReason();
        if (reasonOpt.isEmpty()) {
            return false;
        }

        String plainReason = PlainTextComponentSerializer.plainText().serialize(reasonOpt.get());
        for (String keyword : config.getShutdownKeywords()) {
            if (plainReason.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
