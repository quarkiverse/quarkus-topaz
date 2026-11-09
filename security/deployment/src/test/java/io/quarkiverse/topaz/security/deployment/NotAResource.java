package io.quarkiverse.topaz.security.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.topaz.security.TopazAuthorize;

@ApplicationScoped
public class NotAResource {

    @TopazAuthorize
    public String secured() {
        return "secured";
    }
}
