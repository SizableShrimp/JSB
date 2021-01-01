package me.sizableshrimp.jsb.api;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.args.Args;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface Command {
    CommandInfo getInfo();

    String getName();

    default Set<String> getAliases() {
        return Set.of();
    }

    /**
     * Used to run a command on conditions other than name, like tagging the bot, etc.
     *
     * @param message The message to use as an input.
     * @return True if the command meets a condition to run other than a matching name. False otherwise. (This should
     * not check if the command name or aliases match.)
     */
    default boolean isCommand(Message message) {
        return false;
    }

    Mono<?> run(CommandContext context, MessageCreateEvent event, Args args);
}
