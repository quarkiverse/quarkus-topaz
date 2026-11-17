package io.quarkiverse.topaz.security.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class ResumeRoutes {

    static final String KEY = "resume-test-marker";

    // runs before the HTTP permission checks, on the request's duplicated context
    @RouteFilter(Integer.MAX_VALUE)
    void mark(RoutingContext rc) {
        // the request's context itself, compared by identity below (Vert.x 5 dropped string-keyed context locals)
        rc.put(KEY, Vertx.currentContext());
        rc.next();
    }

    // non-blocking: reports where the request continues once the policy has permitted it
    @Route(path = "/resume/where", methods = Route.HttpMethod.GET)
    Uni<String> where(RoutingContext rc) {
        Context context = Vertx.currentContext();
        return Uni.createFrom().item(Context.isOnEventLoopThread() + "|"
                + (context != null && context == rc.get(KEY) ? "set" : null));
    }
}
