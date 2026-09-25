package pl.blixy.velocityFailover.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Copies the bundled config.yml on first start and reads it into a {@link FailoverConfig}; a missing key keeps its default. */
public final class ConfigLoader {

    private ConfigLoader() {}

    public static FailoverConfig load(Path dataDirectory) throws IOException {
        Path file = dataDirectory.resolve("config.yml");
        if (!Files.exists(file)) {
            Files.createDirectories(dataDirectory);
            try (InputStream bundled = ConfigLoader.class.getClassLoader().getResourceAsStream("config.yml")) {
                if (bundled != null) {
                    Files.copy(bundled, file);
                }
            }
        }

        try (InputStream in = Files.newInputStream(file)) {
            return read(new Section(new Yaml().load(in)));
        }
    }

    private static FailoverConfig read(Section root) {
        Set<String> servers = new HashSet<>();
        for (Object group : root.section("groups").values().values()) {
            servers.addAll(new Section(group).strings("servers", List.of()));
        }

        Section recovery = root.section("recovery");
        Section messages = root.section("messages");
        Section titles = root.section("titles");
        Section actionBar = root.section("action-bar");
        String waiting = messages.string("waiting-action-bar", "<yellow>Connecting to the server <gray>{spinner}");
        List<Component> frames = actionBar.strings("spinner-frames", List.of("[|]", "[/]", "[-]", "[\\]")).stream()
                .map(frame -> MiniMessage.miniMessage().deserialize(waiting.replace("{spinner}", frame)))
                .toList();
        Title.Times titleTimes = Title.Times.times(
                titles.millis("fade-in-ms", 300),
                titles.millis("stay-ms", 2500),
                titles.millis("fade-out-ms", 500));

        return new FailoverConfig(
                root.string("limbo-server", "limbo"),
                Set.copyOf(servers),
                new FailoverConfig.Recovery(
                        recovery.millis("ping-interval-ms", 2000),
                        recovery.integer("pings-to-ready", 3),
                        recovery.millis("grace-period-ms", 5000),
                        recovery.millis("transfer-interval-ms", 50),
                        recovery.millis("ping-timeout-ms", 2000)),
                root.strings("shutdown-keywords", List.of("Server closed", "Server shutting down")),
                new FailoverConfig.Messages(
                        notification(messages, titles, "sent-to-limbo", "<red>The server is temporarily unavailable. You will be moved back automatically when it returns.", titleTimes),
                        notification(messages, titles, "reconnecting", "<green>The server is back online! Reconnecting...", titleTimes),
                        notification(messages, titles, "connection-blocked", "<red>This server is currently unavailable. Please try again in a moment.", titleTimes)),
                new FailoverConfig.ActionBar(actionBar.millis("interval-ms", 400), frames));
    }

    private static FailoverConfig.Notification notification(Section messages, Section titles, String key,
                                                            String fallback, Title.Times times) {
        Section configuredTitle = titles.section(key);
        String heading = configuredTitle.string("title", "");
        String subtitle = configuredTitle.string("subtitle", "");
        Optional<Title> title = heading.isBlank() && subtitle.isBlank()
                ? Optional.empty()
                : Optional.of(Title.title(
                        MiniMessage.miniMessage().deserialize(heading),
                        MiniMessage.miniMessage().deserialize(subtitle),
                        times));
        return new FailoverConfig.Notification(messages.component(key, fallback), title);
    }

    /** One mapping of the YAML tree; anything that is not a mapping reads as empty. */
    private record Section(Map<String, Object> values) {

        @SuppressWarnings("unchecked")
        Section(Object node) {
            this(node instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of());
        }

        Section section(String key) {
            return new Section(values.get(key));
        }

        String string(String key, String fallback) {
            return values.get(key) instanceof String value ? value : fallback;
        }

        @SuppressWarnings("unchecked")
        List<String> strings(String key, List<String> fallback) {
            return values.get(key) instanceof List<?> list ? (List<String>) list : fallback;
        }

        int integer(String key, int fallback) {
            return values.get(key) instanceof Number value ? value.intValue() : fallback;
        }

        Duration millis(String key, long fallback) {
            return Duration.ofMillis(values.get(key) instanceof Number value ? value.longValue() : fallback);
        }

        Component component(String key, String fallback) {
            return MiniMessage.miniMessage().deserialize(string(key, fallback));
        }
    }
}
