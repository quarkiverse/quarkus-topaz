package io.quarkiverse.topaz.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.KeyStoreOptions;

class TopazChannelsTest {

    private static ResolvedServiceSettings settings(Optional<Boolean> plainText, Optional<String> tlsName) {
        return new ResolvedServiceSettings("localhost", 8282, Optional.empty(), tlsName, plainText, Optional.empty());
    }

    private static ResolvedServiceSettings settings(String host, Optional<Boolean> plainText) {
        return new ResolvedServiceSettings(host, 8282, Optional.empty(), Optional.empty(), plainText, Optional.empty());
    }

    @Test
    void loopbackHostsAreSilent() {
        for (String host : List.of("localhost", "LOCALHOST", "127.0.0.1", "127.10.0.1", "::1", "[::1]")) {
            assertThat(TopazChannels.isLoopback(host)).as(host).isTrue();
        }
        for (String host : List.of("topaz.example.com", "10.0.0.5", "128.0.0.1", "localhost.example.com",
                "127.0.0.1.example.com")) {
            assertThat(TopazChannels.isLoopback(host)).as(host).isFalse();
        }
    }

    @Test
    void remotePlaintextWarns() {
        assertThat(TopazChannels.implicitPlaintextWarning(settings("topaz.example.com", Optional.empty()),
                Optional.empty()))
                .hasValueSatisfying(text -> assertThat(text).contains("topaz.example.com:8282")
                        .contains("quarkus.topaz.plain-text=true").contains("TLS"));
    }

    @Test
    void explicitPlaintextIsSilent() {
        assertThat(TopazChannels.implicitPlaintextWarning(settings("topaz.example.com", Optional.of(true)),
                Optional.empty())).isEmpty();
    }

    @Test
    void tlsResolvedIsSilent() {
        assertThat(TopazChannels.implicitPlaintextWarning(settings("topaz.example.com", Optional.empty()),
                Optional.of(withTrustStore()))).isEmpty();
    }

    @Test
    void localPlaintextIsSilent() {
        assertThat(TopazChannels.implicitPlaintextWarning(settings("localhost", Optional.empty()), Optional.empty()))
                .isEmpty();
    }

    @Test
    void warningOmitsApiKey() {
        ResolvedServiceSettings s = new ResolvedServiceSettings("topaz.example.com", 8282, Optional.of("secret-key"),
                Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(TopazChannels.implicitPlaintextWarning(s, Optional.empty()))
                .hasValueSatisfying(text -> assertThat(text).doesNotContain("secret-key"));
    }

    private static TlsConfiguration withTrustStore() {
        TlsConfiguration c = mock(TlsConfiguration.class);
        when(c.getTrustStoreOptions()).thenReturn(new KeyStoreOptions());
        return c;
    }

    @Test
    void tlsForcedWithoutConfigUsesTlsWithJvmDefaultTrust() {
        HttpClientOptions http = TopazChannels.transportOptions(settings(Optional.of(false), Optional.empty()),
                Optional.empty());
        assertThat(http.isSsl()).isTrue();
        assertThat(http.isUseAlpn()).isTrue();
        assertThat(http.getTrustOptions()).isNull();
        assertThat(http.isTrustAll()).isFalse();
    }

    @Test
    void createWithTlsForcedButNoConfigBuildsTlsChannel() {
        Vertx vertx = Vertx.vertx();
        try (TopazChannels.ManagedChannelHolder h = TopazChannels.create(vertx,
                settings(Optional.of(false), Optional.empty()), Optional.empty(), List.of())) {
            assertThat(h).isNotNull();
        } finally {
            vertx.close();
        }
    }

    @Test
    void unsetPlainTextWithoutConfigIsPlaintext() {
        HttpClientOptions http = TopazChannels.transportOptions(settings(Optional.empty(), Optional.empty()),
                Optional.empty());
        assertThat(http.isSsl()).isFalse();
    }

    @Test
    void createThrowsWhenNamedTlsMissing() {
        assertThatThrownBy(() -> TopazChannels.create(mock(Vertx.class), settings(Optional.empty(), Optional.of("mytls")),
                Optional.empty(), List.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("mytls");
    }

    @Test
    void createPlainTextIgnoresTlsConfig() {
        Vertx vertx = Vertx.vertx();
        try (TopazChannels.ManagedChannelHolder h = TopazChannels.create(vertx, settings(Optional.of(true), Optional.of("n")),
                Optional.of(withTrustStore()), List.of())) {
            assertThat(h).isNotNull();
        } finally {
            vertx.close();
        }
    }
}
