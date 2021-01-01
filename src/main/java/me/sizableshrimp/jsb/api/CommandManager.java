package me.sizableshrimp.jsb.api;

import discord4j.core.event.domain.message.MessageCreateEvent;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.args.ArgsProcessor;
import org.fastily.jwiki.core.Wiki;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommandManager extends CommandLoader<Command> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);
    protected final Wiki wiki;
    protected final Map<String, Command> commandMap = new HashMap<>();
    protected Set<Command> commands;

    public CommandManager(Wiki wiki) {
        super(Command.class);
        this.wiki = wiki;
        loadCommands();
    }

    public Mono<?> executeCommand(MessageCreateEvent event) {
        Args args = ArgsProcessor.processWithPrefix(event.getMessage().getContent());
        if (args == null)
            return Mono.empty();

        Command command = commandMap.get(args.getName());

        // Try getting a command from another condition
        if (command == null) {
            command = commands.stream()
                    .filter(cmd -> cmd.isCommand(event.getMessage()))
                    .findAny().orElse(null);
        }

        if (command != null) {
            return command.run(new CommandContext(this, wiki), event, args);
        }

        return Mono.empty();
    }

    public void loadCommands() {
        this.commands = this.loadClasses(new Class[0], new Object[0]);

        commandMap.clear();
        for (Command command : commands) {
            for (String alias : command.getAliases()) {
                addCommand(alias, command);
            }
            addCommand(command.getName(), command);
        }
    }

    private void addCommand(String name, Command command) {
        if (commandMap.put(name, command) != null) {
            LOGGER.error("Encountered a duplicate alias \"{}\"", name);
            commandMap.remove(name);
        }
    }

    public Map<String, Command> getCommandMap() {
        return this.commandMap;
    }

    public Set<Command> getCommands() {
        return this.commands;
    }
}
