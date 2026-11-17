package io.quarkiverse.topaz.deployment.devservices;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkiverse.topaz.TopazClient;
import io.quarkus.test.QuarkusExtensionTest;

public class DevServicesTopazApiKeyTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.topaz.devservices.api-key", "secret")
            .overrideConfigKey("quarkus.topaz.api-key", "wrong");

    @Inject
    TopazClient topaz;

    @Test
    void wrongApiKeyIsUnauthenticated() {
        assertThat(topaz.authorizer().info().await().atMost(ofSeconds(10)).getVersion()).isNotBlank();
        assertThatThrownBy(() -> topaz.directory().getManifest().await().atMost(ofSeconds(10)))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
                .isEqualTo(Status.Code.UNAUTHENTICATED);
    }
}
