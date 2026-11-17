package io.quarkiverse.topaz.security.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

/**
 * Converts a {@code @TopazResource} argument to plain maps, lists and scalars with the application's Jackson mapper.
 * <p>
 * Quarkus 3 configures a Jackson 2 {@code ObjectMapper}, Quarkus 4 a Jackson 3 one, in another package. The extension
 * compiles against neither, so one build runs on both. It uses the {@code ObjectMapper} bean of whichever Jackson is
 * present, preferring Jackson 3, so the conversion follows the application's Jackson configuration as before.
 */
public final class JacksonConverter {

    /** The Jackson 3 mapper. */
    static final String JACKSON_3 = "tools.jackson.databind.ObjectMapper";
    /** The Jackson 2 mapper. */
    static final String JACKSON_2 = "com.fasterxml.jackson.databind.ObjectMapper";
    /** The mappers in the order they are tried; the deployment keeps their beans and registers them for native. */
    public static final List<String> MAPPERS = List.of(JACKSON_3, JACKSON_2);

    private final MethodHandle convert;

    private JacksonConverter(MethodHandle convert) {
        this.convert = convert;
    }

    /**
     * Find the application's mapper in the CDI container.
     *
     * @return the converter
     * @throws IllegalStateException when no {@code ObjectMapper} bean exists
     */
    static JacksonConverter fromContainer() {
        ClassLoader loader = JacksonConverter.class.getClassLoader();
        for (String name : MAPPERS) {
            Class<?> type;
            try {
                type = Class.forName(name, false, loader);
            } catch (ClassNotFoundException e) {
                continue;
            }
            InstanceHandle<?> mapper = Arc.container().instance(type);
            if (!mapper.isAvailable()) {
                continue;
            }
            try {
                MethodHandle convertValue = MethodHandles.publicLookup().findVirtual(type, "convertValue",
                        MethodType.methodType(Object.class, Object.class, Class.class));
                return new JacksonConverter(convertValue.bindTo(mapper.get()));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unsupported Jackson ObjectMapper API in " + name, e);
            }
        }
        throw new IllegalStateException("@TopazResource needs a Jackson ObjectMapper bean, but none of " + MAPPERS
                + " is available");
    }

    /**
     * Convert a value to plain maps, lists and scalars.
     *
     * @param value the value
     * @return the converted value
     */
    Object toPlain(Object value) {
        try {
            return convert.invoke(value, Object.class);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("Cannot convert a @TopazResource argument", e);
        }
    }
}
