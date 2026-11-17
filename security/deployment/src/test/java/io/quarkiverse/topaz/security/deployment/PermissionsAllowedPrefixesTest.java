package io.quarkiverse.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;

/** {@code permissions.prefixes} alone, with no policy root, makes the bridge own names under it. */
public class PermissionsAllowedPrefixesTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(PermissionsResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.permissions.enabled", "true")
            // the surrounding spaces must be trimmed
            .overrideConfigKey("quarkus.topaz.enforcement.permissions.prefixes", " todoApp ");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void trimmedPrefixOwnsNames() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/perm/list").then().statusCode(200).body(equalTo("list"));
        given().auth().preemptive().basic("summer@the-citadel.com", "summer")
                .get("/perm/list").then().statusCode(403);
    }

    @Test
    void relationWithOnlyPrefixes() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/perm/todos/todo-1").then().statusCode(200);
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/perm/todos/todo-1").then().statusCode(403);
    }
}
