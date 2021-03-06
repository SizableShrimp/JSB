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

package me.sizableshrimp.jsb.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.jsb.api.Command;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.commands.utility.info.HelpCommand;
import me.sizableshrimp.jsb.util.MessageUtil;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public abstract class AbstractCommand implements Command {
    // Helper functions
    protected final Mono<Message> incorrectUsage(CommandContext context, MessageCreateEvent event) {
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(HelpCommand.display(event, this, context), c));
    }

    protected final Mono<Message> incorrectUsage(String message, CommandContext context, MessageCreateEvent event) {
        return event.getMessage().getChannel().flatMap(c -> sendMessage(message, HelpCommand.display(event, this, context), c));
    }

    protected static Mono<Message> sendMessage(String message, MessageChannel channel) {
        return MessageUtil.sendMessage(message, channel);
    }

    protected static Mono<Message> sendEmbed(Consumer<? super EmbedCreateSpec> spec, MessageChannel channel) {
        return MessageUtil.sendEmbed(spec, channel);
    }

    protected static Mono<Message> sendMessage(String message, Consumer<? super EmbedCreateSpec> spec, MessageChannel channel) {
        return MessageUtil.sendEmbed(message, spec, channel);
    }

    protected static Consumer<EmbedCreateSpec> createRetrievalEmbed(Consumer<EmbedCreateSpec> spec) {
        return spec.andThen(embed -> embed.setFooter("Retrieved by JSB with love ❤", null));
    }
}
