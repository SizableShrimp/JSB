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

package me.sizableshrimp.jsb.util;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import me.sizableshrimp.jsb.Bot;
import org.fastily.jwiki.core.AReply;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A util class containing helper methods related to sending messages to the user.
 */
public final class MessageUtil {
    public static final char ZERO_WIDTH_SPACE = '\u200B';

    private MessageUtil() {}

    public static Mono<Message> sendMessage(String message, MessageChannel channel) {
        return message.isBlank() ? Mono.empty() : channel.createMessage(ZERO_WIDTH_SPACE + message);
    }

    public static Mono<Message> sendEmbed(Consumer<? super EmbedCreateSpec> embed, MessageChannel channel) {
        return channel.createMessage(message -> message.setEmbed(embed));
    }

    public static Mono<Message> sendEmbed(String message, Consumer<? super EmbedCreateSpec> embed, MessageChannel channel) {
        return channel.createMessage(m -> m.setContent(ZERO_WIDTH_SPACE + message).setEmbed(embed));
    }

    public static String getUsernameDiscriminator(User user) {
        return user.getUsername() + '#' + user.getDiscriminator();
    }

    public static Mono<Boolean> canSendMessages(Message message) {
        Snowflake id = message.getClient().getSelfId();
        return message.getChannel()
                .cast(GuildMessageChannel.class)
                .flatMap(c -> c.getEffectivePermissions(id))
                .map(set -> set.contains(Permission.SEND_MESSAGES));
    }

    public static Mono<Boolean> canReply(Message message) {
        return message.getChannel().map(c -> c instanceof GuildMessageChannel)
                .filterWhen(b -> MessageUtil.canSendMessages(message))
                .filter(b -> message.getAuthor().map(u -> !u.isBot()).orElse(false))
                .filter(e -> !message.getContent().isEmpty());
    }

    public static Mono<Boolean> canReply(MessageCreateEvent event) {
        return canReply(event.getMessage());
    }

    /**
     * Return the relevant message to send to the user depending on the state of the {@link AReply}.
     *
     * @param reply The {@link AReply} used to check the state of the response from the server.
     * @param success The {@link Function} to call when the reply is in an {@link AReply.Type#SUCCESS} state.
     * @param errorWhen The {@link Function} to call when the reply is in an {@link AReply.Type#ERROR} or {@link AReply.Type#UNKNOWN} state.
     * This should be a short blurb that is logged and returned as part of the message, e.g. "adding mod".
     * Always used in the form "when XXX".
     * @return the relevant message to send to the user depending on the state of the {@link AReply}.
     * @throws IllegalStateException if the {@code reply} is somehow not in an {@code SUCCESS}, {@code ERROR}, or {@code UNKNOWN} state.
     * @see #getMessageFromReply(AReply, Function, Function, Function)
     */
    public static String getMessageFromReply(AReply reply, Function<AReply, String> success, Function<AReply, String> errorWhen) {
        return getMessageFromReply(reply, success, r -> {
            String when = errorWhen.apply(r);
            if (Bot.LOGGER.isErrorEnabled())
                Bot.LOGGER.error("Error when {} from response {}", when, r.getResponse(), new Exception("Stack trace"));
            return String.format("An error occurred when %s: ```\n%s```", when, r.getError());
        }, r -> {
            String when = errorWhen.apply(r);
            Bot.LOGGER.warn("Unknown format returned when {} with response {}", when, r.getResponse());
            return String.format("An unknown format was returned by the API when %s. Please try again later.", when);
        });
    }

    /**
     * Return the relevant message to send to the user depending on the state of the {@link AReply}.
     *
     * @param reply The {@link AReply} used to check the state of the response from the server.
     * @param success The {@link Function} to call when the reply is in an {@link AReply.Type#SUCCESS} state.
     * @param error The {@link Function} to call when the reply is in an {@link AReply.Type#ERROR} state.
     * @param unknown The {@link Function} to call when the reply is in an {@link AReply.Type#UNKNOWN} state.
     * @return the relevant message to send to the user depending on the state of the {@link AReply}.
     * @throws IllegalStateException if the {@code reply} is somehow not in an {@code SUCCESS}, {@code ERROR}, or {@code UNKNOWN} state.
     * @see #getMessageFromReply(AReply, Function, Function)
     */
    public static String getMessageFromReply(AReply reply, Function<AReply, String> success, Function<AReply, String> error, Function<AReply, String> unknown) {
        if (reply.isSuccess()) {
            return success.apply(reply);
        } else if (reply.isError()) {
            return error.apply(reply);
        } else if (reply.isUnknown()) {
            return unknown.apply(reply);
        }

        throw new IllegalStateException("Reply was not in a valid state");
    }
}
