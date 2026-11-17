package io.quarkiverse.topaz.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import io.grpc.Channel;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientOptions;

/**
 * The Vert.x gRPC client API of the running Quarkus version.
 * <p>
 * Quarkus 3 ships Vert.x 4, where {@code io.vertx.grpc.client.GrpcClientChannel} turns a {@link GrpcClient} into a
 * grpc-java {@link Channel}. Quarkus 4 ships Vert.x 5, which moved that bridge to {@code io.vertx.grpcio.client}. The
 * extension compiles against neither class, so one build runs on both. It picks the API once, at class init.
 */
public final class VertxGrpc {

    /** The Vert.x 5 client that bridges to grpc-java. */
    static final String IO_CLIENT = "io.vertx.grpcio.client.GrpcIoClient";
    /** The Vert.x 5 channel. */
    static final String IO_CHANNEL = "io.vertx.grpcio.client.GrpcIoClientChannel";
    /** The Vert.x 4 channel. */
    static final String V4_CHANNEL = "io.vertx.grpc.client.GrpcClientChannel";
    /** Every class this API may call reflectively; the deployment registers the present ones for native. */
    public static final List<String> REFLECTIVE_CLASSES = List.of(IO_CLIENT, IO_CHANNEL, V4_CHANNEL,
            "io.vertx.grpc.client.GrpcClient", "io.vertx.grpc.client.GrpcClientOptions");

    private static final VertxGrpc INSTANCE = detect(VertxGrpc.class.getClassLoader());

    private final MethodHandle client;
    private final MethodHandle channel;
    private final boolean vertx5;

    private VertxGrpc(MethodHandle client, MethodHandle channel, boolean vertx5) {
        this.client = client;
        this.channel = channel;
        this.vertx5 = vertx5;
    }

    /**
     * Get the API of the running Vert.x.
     *
     * @return the API
     */
    static VertxGrpc get() {
        return INSTANCE;
    }

    /**
     * Find the API in a class loader.
     * <p>
     * The Vert.x 4 API is tried first. It is the one Quarkus 3 has always used, and Vert.x 5 removed it. A partial
     * {@code io.vertx.grpcio} module can sit next to it on Quarkus 3, so the presence of a Vert.x 5 class alone does
     * not decide.
     *
     * @param loader the class loader to look in
     * @return the API
     * @throws IllegalStateException when neither API is present
     */
    static VertxGrpc detect(ClassLoader loader) {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodType channelType = MethodType.methodType(void.class, GrpcClient.class, SocketAddress.class);
        ReflectiveOperationException v4Failure;
        try {
            Class<?> v4Channel = Class.forName(V4_CHANNEL, false, loader);
            MethodHandle transport = lookup.findVirtual(GrpcClientOptions.class, "setTransportOptions",
                    MethodType.methodType(GrpcClientOptions.class, HttpClientOptions.class));
            MethodHandle create = lookup.findStatic(GrpcClient.class, "client",
                    MethodType.methodType(GrpcClient.class, Vertx.class, GrpcClientOptions.class));
            // (vertx, options, http) -> GrpcClient.client(vertx, options.setTransportOptions(http)), the Vert.x 5 shape
            MethodHandle withTransport = MethodHandles.collectArguments(create, 1, transport);
            return new VertxGrpc(withTransport, lookup.findConstructor(v4Channel, channelType), false);
        } catch (ReflectiveOperationException e) {
            v4Failure = e;
        }
        try {
            Class<?> ioClient = Class.forName(IO_CLIENT, false, loader);
            Class<?> ioChannel = Class.forName(IO_CHANNEL, false, loader);
            return new VertxGrpc(
                    lookup.findStatic(ioClient, "client",
                            MethodType.methodType(ioClient, Vertx.class, GrpcClientOptions.class, HttpClientOptions.class)),
                    lookup.findConstructor(ioChannel, channelType), true);
        } catch (ReflectiveOperationException e) {
            IllegalStateException failure = new IllegalStateException("No supported Vert.x gRPC client API: found"
                    + " neither " + V4_CHANNEL + " (Vert.x 4) nor " + IO_CHANNEL + " (Vert.x 5)", e);
            failure.addSuppressed(v4Failure);
            throw failure;
        }
    }

    /**
     * Tell whether this is the Vert.x 5 API.
     *
     * @return {@code true} for Vert.x 5
     */
    boolean vertx5() {
        return vertx5;
    }

    /**
     * Create a gRPC client.
     *
     * @param vertx the Vert.x instance
     * @param http the HTTP/2 transport options
     * @return the client
     */
    GrpcClient client(Vertx vertx, HttpClientOptions http) {
        try {
            return (GrpcClient) client.invoke(vertx, new GrpcClientOptions(), http);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("Cannot create the Vert.x gRPC client", e);
        }
    }

    /**
     * Create a grpc-java channel over a client.
     *
     * @param client the client
     * @param address the server address
     * @return the channel
     */
    Channel channel(GrpcClient client, SocketAddress address) {
        try {
            return (Channel) channel.invoke(client, address);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("Cannot create the Vert.x gRPC channel", e);
        }
    }
}
