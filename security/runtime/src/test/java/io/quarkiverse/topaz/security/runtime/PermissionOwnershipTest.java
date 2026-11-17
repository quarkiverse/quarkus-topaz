package io.quarkiverse.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class PermissionOwnershipTest {

    @Test
    void rootOwnsItselfAndNamesBelowIt() {
        assertThat(PermissionOwnership.owns("todoApp", Optional.of("todoApp"), List.of())).isTrue();
        assertThat(PermissionOwnership.owns("todoApp.GET.todos", Optional.of("todoApp"), List.of())).isTrue();
    }

    @Test
    void lookalikeIsNotOwned() {
        assertThat(PermissionOwnership.owns("todoAppX.GET.todos", Optional.of("todoApp"), List.of())).isFalse();
    }

    @Test
    void prefixesOwnNamesBelowThem() {
        assertThat(PermissionOwnership.owns("billing.read", Optional.of("todoApp"), List.of("billing"))).isTrue();
        assertThat(PermissionOwnership.owns("billing.read", Optional.empty(), List.of(" billing "))).isTrue();
        assertThat(PermissionOwnership.owns("bill.read", Optional.empty(), List.of("billing"))).isFalse();
    }

    @Test
    void nothingConfiguredOwnsNothing() {
        assertThat(PermissionOwnership.owns("x", Optional.empty(), List.of())).isFalse();
    }
}
