package io.quarkiverse.topaz.security.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.topaz.security.TopazCheck;
import io.quarkiverse.topaz.security.TopazObjectId;

// The binding sits on the class only: its business methods carry none of their own.
@ApplicationScoped
@TopazCheck(objectType = "todo", relation = "owner")
public class CheckBoundService {

    public String delete(@TopazObjectId String id) {
        return "deleted " + id;
    }
}
