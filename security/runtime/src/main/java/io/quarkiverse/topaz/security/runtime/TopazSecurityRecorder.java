package io.quarkiverse.topaz.security.runtime;

import java.util.List;
import java.util.regex.Pattern;

import jakarta.enterprise.inject.Instance;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.topaz.security.PolicyPathMapper;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * The recorder that validates the enforcement configuration at startup.
 */
@Recorder
public class TopazSecurityRecorder {

    private static final Pattern PERMISSION_POLICY = Pattern.compile("quarkus\\.http\\.auth\\.permission\\..+\\.policy");

    private final RuntimeValue<TopazEnforcementConfig> config;

    /**
     * Create the recorder.
     *
     * @param config the runtime enforcement configuration
     */
    public TopazSecurityRecorder(RuntimeValue<TopazEnforcementConfig> config) {
        this.config = config;
    }

    /**
     * Fail startup when the policy root is unset but a derived policy path is needed.
     *
     * @param policyRootRequired whether the application uses the {@code topaz} policy through an annotation
     */
    public void validate(boolean policyRootRequired) {
        if (config.getValue().policyRoot().isPresent() || hasCustomPathMapper()) {
            return;
        }
        if (policyRootRequired || permissionUsesTopaz()) {
            throw new ConfigurationException("quarkus.topaz.enforcement.policy-root must be set when the 'topaz' HTTP"
                    + " security policy or a derived @TopazAuthorize path is used");
        }
    }

    /**
     * Fail startup when the {@code @PermissionsAllowed} bridge is on but does not know which permissions are Topaz's.
     * <p>
     * The bridge owns the names under the policy root and under the configured prefixes. It needs at least one of
     * them. No prefix may be blank or start or end with {@code .}.
     */
    public void validatePermissionBridge() {
        TopazEnforcementConfig c = config.getValue();
        List<String> prefixes = c.permissions().prefixes().orElse(List.of());
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isBlank()) {
                throw new ConfigurationException(
                        "quarkus.topaz.enforcement.permissions.prefixes must not contain a blank entry");
            }
            String trimmed = prefix.trim();
            if (trimmed.startsWith(".") || trimmed.endsWith(".")) {
                // such a prefix would own no name at all
                throw new ConfigurationException("quarkus.topaz.enforcement.permissions.prefixes entry '" + trimmed
                        + "' must not start or end with '.'");
            }
        }
        if (c.policyRoot().filter(r -> !r.isBlank()).isEmpty() && prefixes.isEmpty()) {
            throw new ConfigurationException("The @PermissionsAllowed bridge"
                    + " (quarkus.topaz.enforcement.permissions.enabled=true) needs"
                    + " quarkus.topaz.enforcement.policy-root or quarkus.topaz.enforcement.permissions.prefixes to know"
                    + " which permissions Topaz evaluates");
        }
    }

    // The application supplies its own mapper when the resolved bean is not the default one.
    // A custom mapper derives the path itself, so it needs no policy root.
    // Reading the bean class does not create the instance.
    private static boolean hasCustomPathMapper() {
        Instance<PolicyPathMapper> mapper = Arc.container().select(PolicyPathMapper.class);
        return mapper.isResolvable() && !DefaultPolicyPathMapper.class.equals(mapper.getHandle().getBean().getBeanClass());
    }

    private static boolean permissionUsesTopaz() {
        Config c = ConfigProvider.getConfig();
        for (String name : c.getPropertyNames()) {
            if (PERMISSION_POLICY.matcher(name).matches()
                    && c.getOptionalValue(name, String.class).filter(TopazHttpSecurityPolicy.NAME::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
