package pl.blixy.velocityFailover.config;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @TempDir
    Path directory;

    @Test
    void addsDefaultTitlesToExistingConfigs() throws IOException {
        writeConfig("""
                messages:
                  sent-to-limbo: "Waiting"
                  reconnecting: "Returning"
                  connection-blocked: "Offline"
                """);

        FailoverConfig config = ConfigLoader.load(directory);

        assertEquals("Waiting", PLAIN.serialize(config.messages().sentToLimbo()));
        assertEquals(3, config.titleAnimation().waitingFrames().size());
        assertEquals(3, config.titleAnimation().connectingFrames().size());
        assertEquals("Server unavailable.", PLAIN.serialize(config.titleAnimation().waitingFrames().getFirst().title()));
        assertEquals("Reconnecting.", PLAIN.serialize(config.titleAnimation().connectingFrames().getFirst().title()));
        assertEquals("Server unavailable", PLAIN.serialize(config.messages().connectionBlocked().title().orElseThrow().title()));
    }

    @Test
    void allowsDisablingADefaultTitle() throws IOException {
        writeConfig("""
                titles:
                  sent-to-limbo:
                    title: ""
                    subtitle: ""
                """);

        FailoverConfig config = ConfigLoader.load(directory);

        assertTrue(config.titleAnimation().waitingFrames().isEmpty());
        assertEquals(3, config.titleAnimation().connectingFrames().size());
    }

    @Test
    void parsesTitlesAndTheirTimings() throws IOException {
        writeConfig("""
                titles:
                  interval-ms: 750
                  animation-stay-ms: 10000
                  fade-in-ms: 150
                  stay-ms: 1800
                  fade-out-ms: 350
                  waiting:
                    - title: "<red>Unavailable."
                      subtitle: "Please wait"
                    - title: "<red>Unavailable.."
                      subtitle: "Please wait"
                  connecting:
                    - title: "<green>Connecting"
                      subtitle: "Almost ready"
                  connection-blocked:
                    title: "<red>Blocked"
                    subtitle: "Try later"
                """);

        FailoverConfig config = ConfigLoader.load(directory);
        Title waitingTitle = config.titleAnimation().waitingFrames().getFirst();
        Title.Times animationTimes = waitingTitle.times();
        Title blockedTitle = config.messages().connectionBlocked().title().orElseThrow();
        Title.Times notificationTimes = blockedTitle.times();

        assertEquals(Duration.ofMillis(750), config.titleAnimation().interval());
        assertEquals(2, config.titleAnimation().waitingFrames().size());
        assertEquals(1, config.titleAnimation().connectingFrames().size());
        assertEquals("Unavailable.", PLAIN.serialize(waitingTitle.title()));
        assertEquals("Please wait", PLAIN.serialize(waitingTitle.subtitle()));
        assertNotNull(animationTimes);
        assertEquals(Duration.ZERO, animationTimes.fadeIn());
        assertEquals(Duration.ofMillis(10000), animationTimes.stay());
        assertEquals(Duration.ZERO, animationTimes.fadeOut());
        assertNotNull(notificationTimes);
        assertEquals(Duration.ofMillis(150), notificationTimes.fadeIn());
        assertEquals(Duration.ofMillis(1800), notificationTimes.stay());
        assertEquals(Duration.ofMillis(350), notificationTimes.fadeOut());
    }

    private void writeConfig(String yaml) throws IOException {
        Files.writeString(directory.resolve("config.yml"), yaml);
    }
}
