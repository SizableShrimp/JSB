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

package me.sizableshrimp.jsb.api;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.jsb.commands.utility.HelpCommand;
import me.sizableshrimp.jsb.util.MessageUtil;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public abstract class AbstractCommand implements Command {
    // Helper functions
    protected static Mono<MessageChannel> requireRole(MessageCreateEvent event, String role) {
        return Mono.just(event)
                .filter(e -> e.getMember().isPresent())
                .filterWhen(e -> e.getMember().get().getRoles().any(r -> role.equals(r.getName())))
                .flatMap(e -> e.getMessage().getChannel())
                .switchIfEmpty(Mono.error(() -> new NoPermissionException(role)));
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