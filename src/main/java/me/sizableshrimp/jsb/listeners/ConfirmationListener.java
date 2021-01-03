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
import me.sizableshrimp.jsb.api.EventListener;
import me.sizableshrimp.jsb.commands.AddModCommand;
import me.sizableshrimp.jsb.commands.RemoveModCommand;
import me.sizableshrimp.jsb.data.Mod;
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
                    .then(event.getChannel().flatMap(channel -> channel.createMessage(String.format("Cancelling addition of mod named **%s** with abbreviation `%s`...",
                            newMod.getName(), newMod.getAbbrv()))));
        }

        // Otherwise, it has to be a checkmark
        Mod conflict = newMod.add();
        if (conflict != null) {
            return event.getChannel().flatMap(channel -> channel.createMessage(String.format("This mod abbreviation already exists as %s with abbreviation `%s`!",
                    conflict.getName(), conflict.getAbbrv())));
        } else {
            return event.getChannel().flatMap(channel -> channel.createMessage(String.format("Added **%s** (abbreviated as `%s`) to the mods list. Please mark for translation.",
                    newMod.getName(), newMod.getAbbrv())));
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
                    .then(event.getChannel().flatMap(channel -> channel.createMessage(String.format("Cancelling deletion of mod named **%s** with abbreviation `%s`...",
                            toDelete.getName(), toDelete.getAbbrv()))));
        }

        // Otherwise, it has to be an x
        toDelete.remove();
        return event.getChannel().flatMap(channel -> channel.createMessage(String.format("Removed **%s** (abbreviated as `%s`) from the mods list. Please mark for translation.",
                toDelete.getName(), toDelete.getAbbrv())));
    }
}
