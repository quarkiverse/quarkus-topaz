package io.quarkiverse.topaz.security.deployment;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.topaz.security.TopazAuthorize;
import io.quarkiverse.topaz.security.TopazCheck;
import io.quarkiverse.topaz.security.TopazObjectId;
import io.quarkiverse.topaz.security.TopazResource;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@ApplicationScoped
public class SecuredService {

    public record TodoRef(String id, String note) {
    }

    public record OwnedTodo(@TopazObjectId String id, String note) {
    }

    @TopazAuthorize(path = "todoApp.DELETE.todos.__id")
    public Uni<String> deleteReactive(@TopazResource("id") String id) {
        return Uni.createFrom().item("deleted " + id);
    }

    @TopazAuthorize(path = "todoApp.DELETE.todos.__id")
    public String deleteBlocking(@TopazResource("id") String id) {
        return "deleted " + id;
    }

    @TopazAuthorize(path = "todoApp.DELETE.todos.ref")
    public String deleteRef(@TopazResource("todo") TodoRef ref) {
        return "deleted " + ref.id();
    }

    @TopazAuthorize(path = "todoApp.GET.todos", decision = "allowed")
    public String list() {
        return "ok";
    }

    @TopazCheck(objectType = "todo", relation = "owner")
    public Uni<String> deleteOwned(@TopazObjectId String id) {
        return Uni.createFrom().item("deleted " + id);
    }

    @TopazCheck(objectType = "todo", relation = "owner")
    public String deleteOwnedDto(OwnedTodo todo) {
        return "deleted " + todo.id();
    }

    @TopazCheck(objectType = "todo", relation = "owner", objectId = "todo-1")
    public String deleteFixed() {
        return "deleted todo-1";
    }

    private final AtomicInteger streamEntered = new AtomicInteger();

    @TopazAuthorize(path = "todoApp.GET.todos")
    public Multi<String> streamTodos() {
        streamEntered.incrementAndGet();
        return Multi.createFrom().items("todo-1", "todo-2");
    }

    public int streamEntered() {
        return streamEntered.get();
    }

    // todoApp.GET.visibleonly sets visible=true and allowed=false for rick
    @TopazAuthorize(path = "todoApp.GET.visibleonly", decision = "visible")
    public String peekVisible() {
        return "visible";
    }

    @TopazAuthorize(path = "todoApp.GET.visibleonly", decision = "allowed")
    public String peekAllowed() {
        return "allowed";
    }

    @Inject
    SecurityIdentity identity;

    // where the body runs and who it sees, so tests can tell the event loop from a worker
    private String where() {
        return (Context.isOnEventLoopThread() ? "event-loop" : "worker") + " as " + identity.getPrincipal().getName();
    }

    @TopazAuthorize(path = "todoApp.DELETE.todos.__id")
    public CompletionStage<String> deleteStage(@TopazResource("id") String id) {
        return CompletableFuture.completedFuture("deleted " + id + " on " + where());
    }

    @TopazCheck(objectType = "todo", relation = "owner")
    public CompletableFuture<String> deleteOwnedFuture(@TopazObjectId String id) {
        return CompletableFuture.completedFuture("deleted " + id + " on " + where());
    }

    @TopazCheck(objectType = "todo", relation = "owner")
    public CompletionStage<String> deleteOwnedThrowing(@TopazObjectId String id) {
        throw new IllegalStateException("boom");
    }
}
