package io.quarkiverse.topaz.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;

class VertxGrpcTest {

    private static boolean present(String name) {
        try {
            Class.forName(name, false, VertxGrpcTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // Quarkus 3 has the Vert.x 4 channel, Quarkus 4 (Vert.x 5) does not; the detected API follows it
    @Test
    void detectsTheVertxOnTheClasspath() {
        assertThat(VertxGrpc.get().vertx5()).isEqualTo(!present(VertxGrpc.V4_CHANNEL));
    }

    @Test
    void neitherApiFailsWithBothNames() {
        ClassLoader empty = new ClassLoader(null) {
        };
        assertThatThrownBy(() -> VertxGrpc.detect(empty))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(VertxGrpc.IO_CHANNEL)
                .hasMessageContaining(VertxGrpc.V4_CHANNEL);
    }

    @Test
    void createsAClientAndChannel() {
        Vertx vertx = Vertx.vertx();
        try {
            GrpcClient client = VertxGrpc.get().client(vertx, new HttpClientOptions());
            assertThat(VertxGrpc.get().channel(client, SocketAddress.inetSocketAddress(8282, "localhost")))
                    .isNotNull();
            client.close();
        } finally {
            vertx.close();
        }
    }
}
