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
    void keepsExistingConfigsChatOnly() throws IOException {
        writeConfig("""
                messages:
                  sent-to-limbo: "Waiting"
                  reconnecting: "Returning"
                  connection-blocked: "Offline"
                """);

        FailoverConfig config = ConfigLoader.load(directory);

        assertEquals("Waiting", PLAIN.serialize(config.messages().sentToLimbo().chat()));
        assertTrue(config.messages().sentToLimbo().title().isEmpty());
        assertTrue(config.messages().reconnecting().title().isEmpty());
        assertTrue(config.messages().connectionBlocked().title().isEmpty());
    }

    @Test
    void parsesTitlesAndTheirTimings() throws IOException {
        writeConfig("""
                titles:
                  fade-in-ms: 150
                  stay-ms: 1800
                  fade-out-ms: 350
                  sent-to-limbo:
                    title: "<red>Unavailable"
                    subtitle: "Please wait"
                """);

        FailoverConfig config = ConfigLoader.load(directory);
        Title title = config.messages().sentToLimbo().title().orElseThrow();
        Title.Times times = title.times();

        assertEquals("Unavailable", PLAIN.serialize(title.title()));
        assertEquals("Please wait", PLAIN.serialize(title.subtitle()));
        assertNotNull(times);
        assertEquals(Duration.ofMillis(150), times.fadeIn());
        assertEquals(Duration.ofMillis(1800), times.stay());
        assertEquals(Duration.ofMillis(350), times.fadeOut());
        assertTrue(config.messages().reconnecting().title().isEmpty());
    }

    private void writeConfig(String yaml) throws IOException {
        Files.writeString(directory.resolve("config.yml"), yaml);
    }
}
