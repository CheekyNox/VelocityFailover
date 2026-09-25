package pl.blixy.velocityFailover.config;

import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/** Everything read from config.yml, with the messages already parsed so no handler parses MiniMessage per player. */
public record FailoverConfig(String limbo, Set<String> servers, Recovery recovery, List<String> shutdownKeywords,
                             Messages messages, ActionBar actionBar) {

    public record Recovery(Duration pingInterval, int pingsToReady, Duration gracePeriod, Duration transferInterval,
                           Duration pingTimeout) {}

    public record Messages(Component sentToLimbo, Component reconnecting, Component connectionBlocked) {}

    /** One rendered action bar per spinner frame, cycled every {@code interval}. */
    public record ActionBar(Duration interval, List<Component> frames) {}
}
