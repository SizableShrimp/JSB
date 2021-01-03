package me.sizableshrimp.jsb.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import reactor.core.publisher.Mono;

public class FuckCommand extends AbstractCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname%", "Use it and find out");
    }

    @Override
    public String getName() {
        return "fuck";
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        return event.getMessage().getChannel().flatMap(channel -> sendMessage("You", channel));
    }
}
