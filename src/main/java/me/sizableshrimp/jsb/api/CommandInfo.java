package me.sizableshrimp.jsb.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class CommandInfo {
    private final String name;
    private final Set<String> aliases;
    private final String usage;
    private final String description;

    public CommandInfo(Command command, String usage, String description) {
        this.name = command.getName();
        this.aliases = command.getAliases();
        this.usage = usage;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public Set<String> getAllNames() {
        Set<String> set = new HashSet<>(aliases);
        set.add(name);
        return Collections.unmodifiableSet(set);
    }

    public String getUsage(String commandName) {
        return usage.replace("%cmdname%", commandName);
    }

    public String getDescription(String prefix) {
        return description.replace("%prefix%", prefix);
    }
}
