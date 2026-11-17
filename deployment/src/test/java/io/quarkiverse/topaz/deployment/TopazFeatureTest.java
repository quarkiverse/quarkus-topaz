package io.quarkiverse.topaz.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TopazFeatureTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses())
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .setLogRecordPredicate(r -> r.getMessage().contains("Installed features"))
            .assertLogRecords(records -> assertThat(records).anyMatch(r -> r.getMessage() != null
                    && String.format(r.getMessage(), r.getParameters()).contains("topaz")));

    @Test
    void featureInstalled() {
    }
}
