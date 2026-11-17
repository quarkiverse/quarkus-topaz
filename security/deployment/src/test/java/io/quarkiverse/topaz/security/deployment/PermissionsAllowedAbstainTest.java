package io.quarkiverse.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With Topaz unreachable, permissions outside the policy root are never sent to Topaz: no timeout, no WARN, and the
 * answer comes from the other checkers.
 */
public class PermissionsAllowedAbstainTest {

    private static final ServerSocket SILENT = SilentListener.open();

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(PermissionsResource.class, GrantingAugmentor.class, TestIdentities.class,
                            TestIdentityProvider.class, TestIdentityController.class, SilentListener.class))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.enabled", "false")
            .overrideConfigKey("quarkus.topaz.authorizer.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.directory.host", "127.0.0.1")
            .overrideConfigKey("quarkus.topaz.authorizer.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.directory.port", String.valueOf(SILENT.getLocalPort()))
            .overrideConfigKey("quarkus.topaz.plain-text", "true")
            .overrideConfigKey("quarkus.topaz.enforcement.timeout", "1s")
            .overrideConfigKey("quarkus.topaz.enforcement.permissions.enabled", "true")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .setLogRecordPredicate(r -> r.getLevel().intValue() >= Level.WARNING.intValue()
                    && r.getLoggerName().endsWith("TopazPermissionAugmentor"))
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @AfterAll
    static void close() throws IOException {
        SILENT.close();
    }

    @Test
    void unownedGrantedByOtherChecker() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/perm/billing").then().statusCode(200);
    }

    @Test
    void unownedGrantedByNobodyDenied() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/perm/other").then().statusCode(403);
    }

    @Test
    void lookalikeNameUnowned() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/perm/lookalike").then().statusCode(403);
    }
}
