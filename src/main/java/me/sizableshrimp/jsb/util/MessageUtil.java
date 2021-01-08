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
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * A util class used to make sure the bot does not interfere with any other bots by adding a ZWS before each message.
 */
public final class MessageUtil {
    private MessageUtil() {}

    public static Mono<Message> sendMessage(String message, MessageChannel channel) {
        return message.isBlank() ? Mono.empty() : channel.createMessage('\u200B' + message); // ZWS
    }

    public static Mono<Message> sendEmbed(Consumer<? super EmbedCreateSpec> embed, MessageChannel channel) {
        return channel.createMessage(message -> message.setEmbed(embed));
    }

    public static Mono<Message> sendEmbed(String message, Consumer<? super EmbedCreateSpec> embed, MessageChannel channel) {
        return channel.createMessage(m -> m.setContent('\u200B' + message).setEmbed(embed)); // ZWS
    }

    public static Mono<Boolean> canSendMessages(Message message) {
        Snowflake id = message.getClient().getSelfId();
        return message.getChannel()
                .cast(GuildMessageChannel.class)
                .flatMap(c -> c.getEffectivePermissions(id))
                .map(set -> set.contains(Permission.SEND_MESSAGES));
    }
}
