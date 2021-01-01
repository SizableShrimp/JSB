package me.sizableshrimp.jsb.api;

import me.sizableshrimp.jsb.Bot;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Auto-detects and loads commands that implement a generic type T. Can be used to dynamically load up all commands
 * without explicitly instantiating them. Injects needed dependencies into the instances individually.
 *
 * @param <T> The superclass whose subclasses should be loaded.
 */
public class CommandLoader<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLoader.class);
    private static final Reflections REFLECTIONS = new Reflections(Bot.class.getPackageName());
    private final Class<T> token;

    /**
     * Creates a {@link CommandLoader} that takes in the surrounding dependencies and a token that represents the
     * classes to load.
     *
     * @param token A reflection {@link Class} that represents the superclass to load all subclasses from.
     */
    public CommandLoader(Class<T> token) {
        this.token = token;
    }

    /**
     * Dynamically instantiates all classes that inherit from type T,
     * ignoring classes that are annotated with {@link DisabledCommand}.
     *
     * @param parameterTypes The array of parameter types used when finding the constructor.
     * @param initArgs The array of parameter types used when instantiating the class.
     * @return An unmodifiable set of instantiated objects derived from the superclass {@code T}.
     */
    public Set<T> loadClasses(Class<?>[] parameterTypes, Object[] initArgs) {
        Set<Class<? extends T>> commands = REFLECTIONS.getSubTypesOf(token);
        Set<T> instances = new HashSet<>();

        for (Class<? extends T> clazz : commands) {
            if (Modifier.isAbstract(clazz.getModifiers()) || clazz.isAnnotationPresent(DisabledCommand.class)) {
                continue;
            }
            try {
                T instance = clazz.getDeclaredConstructor(parameterTypes).newInstance(initArgs);
                instances.add(instance);
            } catch (Exception e) {
                LOGGER.error("Could not load {}", clazz.getName(), e);
            }
        }

        return Collections.unmodifiableSet(instances);
    }
}
