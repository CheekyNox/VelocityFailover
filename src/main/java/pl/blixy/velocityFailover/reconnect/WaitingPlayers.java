package pl.blixy.velocityFailover.reconnect;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Who is parked on limbo and which server they are waiting to get back to. */
public final class WaitingPlayers {

    private final Map<UUID, String> waiting = new ConcurrentHashMap<>();

    /** False when the player is already waiting, so the same crash never queues them twice. */
    public boolean add(UUID player, String server) {
        return waiting.putIfAbsent(player, server) == null;
    }

    public void remove(UUID player) {
        waiting.remove(player);
    }

    public boolean contains(UUID player) {
        return waiting.containsKey(player);
    }

    public Optional<String> serverOf(UUID player) {
        return Optional.ofNullable(waiting.get(player));
    }

    public List<UUID> waitingFor(String server) {
        return waiting.entrySet().stream().filter(entry -> entry.getValue().equals(server)).map(Map.Entry::getKey).toList();
    }

    public Set<UUID> all() {
        return Set.copyOf(waiting.keySet());
    }
}
