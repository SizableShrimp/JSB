/*
 * Copyright (c) 2021 SizableShrimp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
 * Auto-detects and loads commands that implement a generic token type T. Can be used to dynamically load up all commands
 * without explicitly instantiating them. Injects needed dependencies into the instances individually.
 *
 */
public final class CommandLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLoader.class);
    private static final Reflections REFLECTIONS = new Reflections(Bot.class.getPackageName());

    private CommandLoader() {}

    /**
     * Dynamically instantiates all classes that inherit from the {@code token} type T,
     * ignoring classes that are annotated with {@link DisabledCommand}.
     *
     * @param token The token type T used to load subclasses.
     * @param parameterTypes The array of parameter types used when finding the constructor.
     * @param initArgs The array of parameter types used when instantiating the class.
     * @return An unmodifiable set of instantiated objects derived from the superclass {@code T}.
     */
    public static <T> Set<T> loadClasses(Class<T> token, Class<?>[] parameterTypes, Object[] initArgs) {
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
