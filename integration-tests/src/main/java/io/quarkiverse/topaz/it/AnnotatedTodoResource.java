package io.quarkiverse.topaz.it;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.topaz.security.TopazAuthorize;
import io.quarkiverse.topaz.security.TopazCheck;
import io.quarkiverse.topaz.security.TopazObjectId;
import io.quarkiverse.topaz.security.TopazResource;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;

/**
 * Todo endpoints guarded by the Topaz annotations instead of the route policy.
 * The path {@code /annotated} is outside the policy paths.
 */
@Path("/annotated")
@Produces(MediaType.APPLICATION_JSON)
public class AnnotatedTodoResource {

    /** The installed identity augmentors. */
    @Inject
    @Any
    Instance<SecurityIdentityAugmentor> augmentors;

    /** The installed HTTP security policies. */
    @Inject
    @Any
    Instance<HttpSecurityPolicy> policies;

    /**
     * Create the resource.
     */
    public AnnotatedTodoResource() {
    }

    /**
     * List the todos, allowed by the policy path {@code todoApp.GET.todos}.
     *
     * @return the todo ids
     */
    @GET
    @Path("todos")
    @TopazAuthorize(path = "todoApp.GET.todos")
    public List<String> list() {
        return List.of("todo-1");
    }

    /**
     * Delete a todo, allowed when the caller has the {@code owner} relation on it.
     *
     * @param id the todo id
     */
    @DELETE
    @Path("todos/{id}")
    @TopazCheck(objectType = "todo", relation = "owner")
    public void delete(@TopazObjectId @PathParam("id") String id) {
        // No-op. The test asserts the authorization outcome, not the deletion.
    }

    /**
     * A reference to a todo, sent as the request body.
     *
     * @param id the todo id
     * @param note a free-form note
     */
    public record TodoRef(String id, String note) {
    }

    /**
     * Delete a todo by reference, allowed by the policy path {@code todoApp.DELETE.todos.ref}.
     * <p>
     * The application's Jackson mapper converts the reference into the resource context, which runs in native too.
     *
     * @param ref the todo reference
     * @return the deleted todo id, under the key {@code deleted}
     */
    @POST
    @Path("todos/delete-ref")
    @Consumes(MediaType.APPLICATION_JSON)
    @TopazAuthorize(path = "todoApp.DELETE.todos.ref")
    public Map<String, String> deleteRef(@TopazResource("todo") TodoRef ref) {
        return Map.of("deleted", ref.id());
    }

    /**
     * Report the bean classes of the installed augmentors and HTTP security policies.
     * <p>
     * This endpoint is for tests and needs no authentication.
     * Tests, including the native IT that cannot use Arc, use it to assert which beans are present.
     *
     * @return the sorted class names, under the keys {@code augmentors} and {@code policies}
     */
    @GET
    @Path("augmentors")
    public Map<String, List<String>> augmentors() {
        return Map.of("augmentors", beanClasses(augmentors), "policies", beanClasses(policies));
    }

    /**
     * Get the sorted class names of the beans in an instance.
     *
     * @param beans the instance to read
     * @return the sorted class names
     */
    private static List<String> beanClasses(Instance<?> beans) {
        return beans.handlesStream().map(h -> h.getBean().getBeanClass().getName()).sorted().toList();
    }
}
