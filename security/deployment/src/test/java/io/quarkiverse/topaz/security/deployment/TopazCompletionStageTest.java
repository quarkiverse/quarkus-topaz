package io.quarkiverse.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;

/**
 * {@code @TopazAuthorize} and {@code @TopazCheck} on methods returning {@code CompletionStage}: checked without blocking
 * on the event loop, with the body on the caller's context; unchanged on a worker thread.
 */
public class TopazCompletionStageTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ServiceResource.class, SecuredService.class, TestIdentities.class,
                            TestIdentityProvider.class, TestIdentityController.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void authorizePermittedOnEventLoop() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/stage/todo-1").then().statusCode(200)
                .body(equalTo("deleted todo-1 on event-loop as rick@the-citadel.com"));
    }

    @Test
    void authorizeDenied() {
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/service/stage/todo-1").then().statusCode(403);
    }

    @Test
    void checkPermittedOnEventLoop() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/owned-future/todo-1").then().statusCode(200)
                .body(equalTo("deleted todo-1 on event-loop as rick@the-citadel.com"));
    }

    @Test
    void checkDenied() {
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/service/owned-future/todo-1").then().statusCode(403);
    }

    @Test
    void anonymousUnauthorized() {
        given().delete("/service/stage/todo-1").then().statusCode(401);
    }

    // the body throws; a permitted caller gets the failure, a denied one never reaches the body
    @Test
    void throwingBodyFails() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/owned-throwing/todo-1").then().statusCode(500);
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/service/owned-throwing/todo-1").then().statusCode(403);
    }

    // an Error from the body must fail the request, not leave it hanging
    @Test
    void errorFromBodyFails() {
        given().config(RestAssured.config().httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.socket.timeout", 10_000)))
                .auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/owned-error/todo-1").then().statusCode(500);
    }

    @Test
    void blockingEndpointUnchanged() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .delete("/service/stage-blocking/todo-1").then().statusCode(200)
                .body(equalTo("deleted todo-1 on worker as rick@the-citadel.com"));
        given().auth().preemptive().basic("morty@the-citadel.com", "morty")
                .delete("/service/stage-blocking/todo-1").then().statusCode(403);
    }
}
