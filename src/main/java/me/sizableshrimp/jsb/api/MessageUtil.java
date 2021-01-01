package me.sizableshrimp.jsb.api;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * A util class used to make sure the bot does not interfere with any other bots by adding a ZWS before each message.
 */
public final class MessageUtil {
    private MessageUtil() {}

    public static Mono<Message> sendMessage(String message, MessageChannel channel) {
        return channel.createMessage("\u200B" + message);
    }

    public static Mono<Message> sendEmbed(Consumer<? super EmbedCreateSpec> embed, MessageChannel channel) {
        return channel.createMessage(message -> message.setEmbed(embed));
    }

    public static Mono<Message> sendEmbed(String message, Consumer<? super EmbedCreateSpec> embed, MessageChannel channel) {
        return channel.createMessage(m -> m.setContent("\u200B" + message).setEmbed(embed));
    }
}
