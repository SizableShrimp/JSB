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

package me.sizableshrimp.jsb.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.EventListener;
import me.sizableshrimp.jsb.commands.utility.mod.AddModCommand;
import me.sizableshrimp.jsb.commands.utility.mod.RemoveModCommand;
import me.sizableshrimp.jsb.data.Mod;
import me.sizableshrimp.jsb.util.MessageUtil;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ConfirmationListener extends EventListener<ReactionAddEvent> {
    private static final ReactionEmoji CHECKMARK = ReactionEmoji.unicode("✅");
    private static final ReactionEmoji X = ReactionEmoji.unicode("❌");
    private static final ReactionEmoji WASTEBASKET = ReactionEmoji.unicode("\uD83D\uDDD1");

    public ConfirmationListener(GatewayDiscordClient client, Wiki wiki) {
        super(ReactionAddEvent.class, client, wiki);
    }

    @Override
    protected Mono<Void> execute(Flux<ReactionAddEvent> onEvent) {
        return onEvent.flatMap(e -> {
            if (AddModCommand.awaitingConfirmation.containsKey(e.getMessageId()))
                return addModConfirmation(e);
            if (RemoveModCommand.awaitingConfirmation.containsKey(e.getMessageId()))
                return removeModConfirmation(e);
            return Mono.empty();
        }).then();
    }

    private Mono<Message> addModConfirmation(ReactionAddEvent event) {
        AddModCommand.Confirmation confirmation = AddModCommand.awaitingConfirmation.get(event.getMessageId());
        if (!confirmation.author.equals(event.getUserId()) || (!event.getEmoji().equals(CHECKMARK) && !event.getEmoji().equals(X)))
            return Mono.empty();

        AddModCommand.awaitingConfirmation.remove(event.getMessageId());
        Mod newMod = confirmation.newMod;
        if (event.getEmoji().equals(X)) {
            return event.getMessage().flatMap(Message::delete)
                    .then(event.getChannel().flatMap(channel -> MessageUtil.sendMessage(String.format("Cancelling addition of mod named **%s** with abbreviation `%s`...",
                            newMod.getName(), newMod.getAbbrv()), channel)));
        }

        // Otherwise, it has to be a checkmark
        try {
            boolean success = newMod.add();
            String message;
            if (success) {
                message = String.format("Added **%s** (abbreviated as `%s`) to the mods list. Please mark for translation.", newMod.getName(), newMod.getAbbrv());
                Bot.LOGGER.info("Added mod {} to mods list.", this);
            } else {
                message = "An error occurred when adding this mod";
                Bot.LOGGER.error("Error when adding mod {}", this);
            }
            return event.getChannel().flatMap(channel -> MessageUtil.sendMessage(message, channel));
        } catch (IllegalStateException e) {
            return event.getChannel().flatMap(channel -> MessageUtil.sendMessage(e.getMessage(), channel));
        }
    }

    private Mono<Message> removeModConfirmation(ReactionAddEvent event) {
        RemoveModCommand.Confirmation confirmation = RemoveModCommand.awaitingConfirmation.get(event.getMessageId());
        if (!confirmation.author.equals(event.getUserId()) || (!event.getEmoji().equals(X) && !event.getEmoji().equals(WASTEBASKET)))
            return Mono.empty();

        RemoveModCommand.awaitingConfirmation.remove(event.getMessageId());
        Mod toDelete = confirmation.toDelete;

        if (event.getEmoji().equals(WASTEBASKET)) {
            return event.getMessage().flatMap(Message::delete)
                    .then(event.getChannel().flatMap(channel -> MessageUtil.sendMessage(String.format("Cancelling deletion of mod named **%s** with abbreviation `%s`...",
                            toDelete.getName(), toDelete.getAbbrv()), channel)));
        }

        // Otherwise, it has to be an x
        boolean success = toDelete.remove();
        return event.getChannel().flatMap(channel -> {
            String message;
            if (success) {
                message = String.format(
                        "Removed **%s** (abbreviated as `%s`) from the mods list. Please mark for translation.",
                        toDelete.getName(), toDelete.getAbbrv());
                Bot.LOGGER.info("Removed mod {} from mods list.", this);
            } else {
                message = "An error occurred when deleting this mod.";
                Bot.LOGGER.error("Error when deleting mod {}", this);
            }
            return MessageUtil.sendMessage(message, channel);
        });
    }
}
