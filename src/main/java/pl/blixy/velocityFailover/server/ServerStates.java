package pl.blixy.velocityFailover.server;

import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Where every monitored server is in the crash-and-recovery cycle. Each server has its own lock, so a
 * kick and a ping answer for the same server never race, and the transitions that matter come back as
 * booleans so the caller, not this class, decides what happens next.
 */
public final class ServerStates {

    private final Map<String, Tracker> servers;
    private final int pingsToReady;
    private final Logger logger;

    public ServerStates(Set<String> names, int pingsToReady, Logger logger) {
        this.servers = names.stream().collect(Collectors.toUnmodifiableMap(Function.identity(), _ -> new Tracker()));
        this.pingsToReady = pingsToReady;
        this.logger = logger;
    }

    public boolean isMonitored(String name) {
        return servers.containsKey(name);
    }

    /** An unmonitored server is always available. */
    public boolean isAvailable(String name) {
        return state(name) == ServerState.ONLINE;
    }

    public ServerState state(String name) {
        Tracker tracker = servers.get(name);
        if (tracker == null) {
            return ServerState.ONLINE;
        }
        synchronized (tracker) {
            return tracker.state;
        }
    }

    public Set<String> offline() {
        Set<String> offline = new HashSet<>();
        for (Map.Entry<String, Tracker> entry : servers.entrySet()) {
            if (state(entry.getKey()) == ServerState.OFFLINE) {
                offline.add(entry.getKey());
            }
        }
        return offline;
    }

    /** True when the server was online until now, so its players still need sweeping; a repeated kick changes nothing. */
    public boolean markOffline(String name) {
        Tracker tracker = servers.get(name);
        if (tracker == null) {
            return false;
        }

        synchronized (tracker) {
            ServerState previous = tracker.state;
            if (previous == ServerState.OFFLINE) {
                return false;
            }

            tracker.state = ServerState.OFFLINE;
            tracker.successfulPings = 0;
            logger.warn("[Failover] Server {} marked OFFLINE (was {})", name, previous);
            return previous == ServerState.ONLINE;
        }
    }

    /** True when this ping completed the run that moves the server into recovery; a failed ping during recovery sends it back offline. */
    public boolean recordPing(String name, boolean success) {
        Tracker tracker = servers.get(name);
        if (tracker == null) {
            return false;
        }

        synchronized (tracker) {
            if (!success) {
                tracker.successfulPings = 0;
                if (tracker.state == ServerState.RECOVERY) {
                    tracker.state = ServerState.OFFLINE;
                    logger.warn("[Failover] Server {} failed ping during RECOVERY, back to OFFLINE", name);
                }
                return false;
            }

            if (tracker.state != ServerState.OFFLINE || ++tracker.successfulPings < pingsToReady) {
                return false;
            }

            tracker.state = ServerState.RECOVERY;
            logger.info("[Failover] Server {} entering RECOVERY ({} successful pings)", name, tracker.successfulPings);
            return true;
        }
    }

    public void markOnline(String name) {
        Tracker tracker = servers.get(name);
        if (tracker == null) {
            return;
        }

        synchronized (tracker) {
            tracker.state = ServerState.ONLINE;
            tracker.successfulPings = 0;
            logger.info("[Failover] Server {} is now ONLINE", name);
        }
    }

    private static final class Tracker {
        private ServerState state = ServerState.ONLINE;
        private int successfulPings;
    }
}
