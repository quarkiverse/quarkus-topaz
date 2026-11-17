package io.quarkiverse.topaz.deployment;

import static org.hamcrest.Matchers.empty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class TopazHealthDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.health.enabled", "false");

    @Test
    void disabledWhenConfigured() {
        RestAssured.get("/q/health/ready").then()
                .body("checks.findAll { it.name == 'Topaz' }", empty());
    }
}
