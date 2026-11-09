package io.quarkiverse.topaz.zanzibar.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkiverse.topaz.runtime.Internal;

class InternalPackageTest {

    @Test
    void runtimePackageIsMarkedInternal() throws ClassNotFoundException {
        assertThat(Class.forName("io.quarkiverse.topaz.zanzibar.runtime.package-info").getPackage()
                .isAnnotationPresent(Internal.class)).isTrue();
    }
}
