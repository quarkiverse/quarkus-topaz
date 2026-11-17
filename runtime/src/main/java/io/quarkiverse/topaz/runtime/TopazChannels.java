package io.quarkiverse.topaz.runtime;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;

/**
 * Factory of Vert.x gRPC channels for Topaz services.
 */
public final class TopazChannels {

    private static final Logger LOG = Logger.getLogger(TopazChannels.class);

    private static final Pattern LOOPBACK_V4 = Pattern.compile("127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    private TopazChannels() {
    }

    /**
     * Create a channel.
     * {@code plainText=true} always wins.
     * It produces a plaintext channel and ignores {@code tls}.
     * A named TLS configuration that was not resolved fails instead of silently downgrading.
     * If TLS is forced with {@code plainText=false} and no TLS configuration was resolved, the channel uses TLS with
     * the JVM default trust.
     * The channel never falls back to plaintext when TLS is required.
     *
     * @param vertx the Vert.x instance
     * @param s the settings of the service
     * @param tls the resolved TLS configuration, if any
     * @param interceptors the interceptors to apply to the channel
     * @return a holder of the channel and its client
     */
    public static ManagedChannelHolder create(Vertx vertx, ResolvedServiceSettings s, Optional<TlsConfiguration> tls,
            List<ClientInterceptor> interceptors) {
        HttpClientOptions http = transportOptions(s, tls);
        VertxGrpc api = VertxGrpc.get();
        GrpcClient client = api.client(vertx, http);
        Channel channel = api.channel(client, SocketAddress.inetSocketAddress(s.port(), s.host()));
        if (!interceptors.isEmpty()) {
            channel = ClientInterceptors.intercept(channel, interceptors);
        }
        return new ManagedChannelHolder(channel, client);
    }

    /**
     * Compute the HTTP/2 transport options for {@link #create}.
     * Tests use it.
     *
     * @param s the settings of the service
     * @param tls the resolved TLS configuration, if any
     * @return the transport options
     */
    static HttpClientOptions transportOptions(ResolvedServiceSettings s, Optional<TlsConfiguration> tls) {
        HttpClientOptions http = new HttpClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
        if (s.plainText().orElse(false)) {
            return http.setSsl(false).setHttp2ClearTextUpgrade(false);
        }
        if (tls.isEmpty() && s.tlsConfigurationName().isPresent()) {
            throw new IllegalStateException("TLS configuration '" + s.tlsConfigurationName().get()
                    + "' for Topaz service " + s.host() + ":" + s.port() + " was not found");
        }
        if (tls.isPresent()) {
            http.setSsl(true).setUseAlpn(true);
            TlsConfigUtils.configure(http, tls.get());
        } else if (s.plainText().isPresent()) {
            // plain-text=false without a usable TLS configuration uses TLS with the JVM default trust.
            http.setSsl(true).setUseAlpn(true);
        } else {
            implicitPlaintextWarning(s, tls).ifPresent(LOG::warn);
            http.setSsl(false).setHttp2ClearTextUpgrade(false);
        }
        return http;
    }

    /**
     * Get the warning for a plaintext channel to a remote host that nobody asked for.
     * <p>
     * It applies when {@code plain-text} is unset, no TLS configuration is named or resolved, and the host is not
     * loopback. API keys and tokens would then cross the network in clear.
     *
     * @param s the settings of the service
     * @param tls the resolved TLS configuration, if any
     * @return the warning text, or empty when the channel is not implicit plaintext to a remote host
     */
    static Optional<String> implicitPlaintextWarning(ResolvedServiceSettings s, Optional<TlsConfiguration> tls) {
        if (s.plainText().isPresent() || tls.isPresent() || s.tlsConfigurationName().isPresent()
                || isLoopback(s.host())) {
            return Optional.empty();
        }
        return Optional.of("Topaz service " + s.host() + ":" + s.port() + " uses a plaintext channel because neither"
                + " quarkus.topaz.plain-text nor a TLS configuration is set. Set quarkus.topaz.plain-text=true to"
                + " confirm plaintext, or configure TLS.");
    }

    /**
     * Tell whether a host names the loopback interface.
     * <p>
     * It reads the string only and never resolves the name.
     *
     * @param host the host
     * @return {@code true} for {@code localhost}, a {@code 127.x.x.x} address and {@code ::1}
     */
    static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || LOOPBACK_V4.matcher(host).matches() || "::1".equals(host)
                || "[::1]".equals(host);
    }

    /**
     * Holder of a channel and the gRPC client that owns it.
     *
     * @param channel the channel
     * @param client the gRPC client behind the channel
     * @param closed whether the holder is closed
     */
    public record ManagedChannelHolder(Channel channel, GrpcClient client, AtomicBoolean closed)
            implements
                AutoCloseable {

        /**
         * Create an open holder.
         *
         * @param channel the channel
         * @param client the gRPC client behind the channel
         */
        public ManagedChannelHolder(Channel channel, GrpcClient client) {
            this(channel, client, new AtomicBoolean());
        }

        /** Close the gRPC client and mark the holder closed. */
        @Override
        public void close() {
            closed.set(true);
            client.close();
        }

        /**
         * Check whether {@link #close()} has been called.
         *
         * @return {@code true} once the holder is closed
         */
        public boolean isClosed() {
            return closed.get();
        }
    }
}
