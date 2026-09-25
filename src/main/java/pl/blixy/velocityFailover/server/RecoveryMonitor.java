package pl.blixy.velocityFailover.server;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import pl.blixy.velocityFailover.config.FailoverConfig;
import pl.blixy.velocityFailover.reconnect.Failover;

import java.util.concurrent.TimeUnit;

/** Pings only the servers that are down; the run of answers that puts one into recovery starts the transfer. */
public final class RecoveryMonitor implements Runnable {

    private final ProxyServer proxy;
    private final FailoverConfig config;
    private final ServerStates states;
    private final Failover failover;

    public RecoveryMonitor(ProxyServer proxy, FailoverConfig config, ServerStates states, Failover failover) {
        this.proxy = proxy;
        this.config = config;
        this.states = states;
        this.failover = failover;
    }

    @Override
    public void run() {
        for (String name : states.offline()) {
            RegisteredServer server = proxy.getServer(name).orElse(null);
            if (server == null) {
                states.recordPing(name, false);
                continue;
            }

            server.ping()
                    .orTimeout(config.recovery().pingTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .whenComplete((_, error) -> {
                        if (states.recordPing(name, error == null)) {
                            failover.serverRecovering(name);
                        }
                    });
        }
    }
}
