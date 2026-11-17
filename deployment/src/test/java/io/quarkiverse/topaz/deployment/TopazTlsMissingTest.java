package io.quarkiverse.topaz.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TopazTlsMissingTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.topaz.tls-configuration-name", "nope")
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .assertException(t -> {
                Throwable c = t;
                StringBuilder all = new StringBuilder();
                while (c != null) {
                    all.append(c).append('\n');
                    c = c.getCause();
                }
                assertThat(all.toString()).contains("TLS configuration 'nope'");
            });

    @Test
    void failsFast() {
    }
}
