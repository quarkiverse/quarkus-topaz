package io.quarkiverse.topaz.security.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;

/** A blank entry in the prefixes fails startup. */
public class PermissionsAllowedBlankPrefixTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(PermissionsResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.enforcement.permissions.enabled", "true")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.topaz.enforcement.permissions.prefixes[0]", " ")
            .assertException(t -> assertThat(t).isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("permissions.prefixes must not contain a blank entry"));

    @Test
    void startupFails() {
        fail("should not start");
    }
}
