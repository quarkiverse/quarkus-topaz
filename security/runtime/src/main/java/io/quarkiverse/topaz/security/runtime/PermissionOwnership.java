package io.quarkiverse.topaz.security.runtime;

import java.util.List;
import java.util.Optional;

/**
 * The rule that decides which permission names the {@code @PermissionsAllowed} bridge evaluates in Topaz.
 */
final class PermissionOwnership {

    private PermissionOwnership() {
    }

    /**
     * Tell whether the bridge owns a permission name.
     *
     * @param name the permission name
     * @param policyRoot the policy root, if set
     * @param prefixes the configured prefixes, trimmed here
     * @return {@code true} when the name equals the root or a prefix, or starts with one of them followed by {@code .}
     */
    static boolean owns(String name, Optional<String> policyRoot, List<String> prefixes) {
        if (policyRoot.isPresent() && under(name, policyRoot.get().trim())) {
            return true;
        }
        for (String prefix : prefixes) {
            if (under(name, prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean under(String name, String prefix) {
        return !prefix.isEmpty() && (name.equals(prefix) || name.startsWith(prefix + "."));
    }
}
