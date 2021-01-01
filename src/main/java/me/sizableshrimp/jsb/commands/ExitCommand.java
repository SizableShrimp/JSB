package me.sizableshrimp.jsb.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import reactor.core.publisher.Mono;

import java.util.Set;

public class ExitCommand extends AbstractCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname%", "Exit the program");
    }

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("stop");
    }

    @Override
    public Mono<Void> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (event.getMessage().getAuthor().get().getId().asLong() != Bot.getConfig().getOwnerId())
            return event.getMessage().getChannel().flatMap(channel -> sendMessage("You are not the owner! Nice try.", channel)).then();

        Bot.LOGGER.info("Exiting program...");
        return event.getMessage().getChannel().flatMap(channel -> sendMessage("Exiting program", channel)).then(event.getClient().logout());
    }
}
