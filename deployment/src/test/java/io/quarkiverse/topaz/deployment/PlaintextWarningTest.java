package io.quarkiverse.topaz.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.topaz.TopazClient;
import io.quarkus.test.QuarkusExtensionTest;

/** Implicit plaintext to a remote host logs one WARN per service when the client builds its channels. */
public class PlaintextWarningTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "topaz.example.com")
            .overrideConfigKey("quarkus.topaz.directory.host", "topaz.example.com")
            .setLogRecordPredicate(r -> r.getLevel().intValue() == Level.WARNING.intValue()
                    && r.getLoggerName().endsWith("TopazChannels"))
            .assertLogRecords(records -> assertThat(records)
                    .filteredOn(r -> r.getMessage().contains("uses a plaintext channel"))
                    .hasSize(2));

    @Inject
    TopazClient topaz;

    @Test
    void channelsBuiltWithWarning() {
        assertThat(topaz.authorizer()).isNotNull();
    }
}
