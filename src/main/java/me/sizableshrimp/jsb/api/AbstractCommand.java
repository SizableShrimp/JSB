package me.sizableshrimp.jsb.api;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.jsb.commands.utility.HelpCommand;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public abstract class AbstractCommand implements Command {
    // Helper functions
    protected static Mono<MessageCreateEvent> requireRole(MessageCreateEvent event, String role) {
        return Mono.just(event)
                .filter(e -> e.getMember().isPresent())
                .filterWhen(e -> e.getMember().get().getRoles().any(r -> role.equals(r.getName())));
    }

    protected final Mono<Message> incorrectUsage(MessageCreateEvent event) {
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(HelpCommand.display(event, this), c));
    }

    protected static Mono<Message> sendMessage(String message, MessageChannel channel) {
        return MessageUtil.sendMessage(message, channel);
    }

    protected static Mono<Message> sendEmbed(Consumer<? super EmbedCreateSpec> embed, MessageChannel channel) {
        return MessageUtil.sendEmbed(embed, channel);
    }

    protected static Mono<Message> sendMessage(String message, Consumer<? super EmbedCreateSpec> embed, MessageChannel channel) {
        return MessageUtil.sendEmbed(message, embed, channel);
    }
}
