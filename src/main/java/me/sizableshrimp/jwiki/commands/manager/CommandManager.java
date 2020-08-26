package me.sizableshrimp.jwiki.commands.manager;

import lombok.Getter;
import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.commands.Command;
import me.sizableshrimp.jwiki.commands.DisabledCommand;
import org.fastily.jwiki.core.Wiki;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CommandManager {
    private static final Reflections REFLECTIONS = new Reflections(Command.class.getPackage().getName());
    private final Wiki wiki;
    private final Set<Constructor<Command>> constructors;
    @Getter
    private final Map<String, Command> commandMap = new HashMap<>();
    @Getter
    private Set<Command> commands;

    public CommandManager(Wiki wiki) {
        this.wiki = wiki;
        this.constructors = loadConstructors();
    }

    public void executeCmd(Args args) {
        Command command = commandMap.get(args.getName());
        if (command != null)
            command.run(args);
    }

    public void loadCommands() {
        this.commands = constructors.stream()
                .map(constructor -> {
                    try {
                        return constructor.newInstance(wiki);
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Command::getName))));

        commandMap.clear();
        for (Command command : commands) {
            commandMap.put(command.getName(), command);
            command.getAliases().forEach(alias -> commandMap.put(alias, command));
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Constructor<Command>> loadConstructors() {
        return REFLECTIONS.getSubTypesOf(Command.class).stream()
                .filter(clazz -> !clazz.isAnnotationPresent(DisabledCommand.class))
                .map(clazz -> {
                    try {
                        return (Constructor<Command>) clazz.getConstructor(Wiki.class);
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
