package pl.blixy.velocityFailover.server;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import pl.blixy.velocityFailover.config.FailoverConfig;
import pl.blixy.velocityFailover.reconnect.Failover;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RecoveryMonitor implements Runnable {

    private final ProxyServer proxy;
    private final ServerStates stateRegistry;
    private final Failover failover;
    private final long pingTimeoutMs;

    public RecoveryMonitor(ProxyServer proxy, FailoverConfig config, ServerStates stateRegistry, Failover failover) {
        this.proxy = proxy;
        this.stateRegistry = stateRegistry;
        this.failover = failover;
        this.pingTimeoutMs = config.recovery().pingTimeout().toMillis();
    }

    @Override
    public void run() {
        Set<String> offline = stateRegistry.offline();
        if (offline.isEmpty()) return;

        for (String serverName : offline) {
            Optional<RegisteredServer> serverOpt = proxy.getServer(serverName);
            if (serverOpt.isEmpty()) {
                stateRegistry.recordPing(serverName, false);
                continue;
            }

            serverOpt.get().ping()
                    .orTimeout(pingTimeoutMs, TimeUnit.MILLISECONDS)
                    .whenComplete((_, error) -> {
                        if (stateRegistry.recordPing(serverName, error == null)) {
                            failover.serverRecovering(serverName);
                        }
                    });
        }
    }
}
