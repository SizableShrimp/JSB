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

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.EventListener;
import me.sizableshrimp.jsb.util.MessageUtil;
import me.sizableshrimp.jsb.util.Reactions;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

public abstract class TrashableMessageListener extends EventListener<MessageCreateEvent> {
    protected static final Set<Snowflake> messages = new HashSet<>();

    protected TrashableMessageListener(GatewayDiscordClient client, Wiki wiki) {
        super(MessageCreateEvent.class, client, wiki);
    }

    protected abstract Mono<Message> genMessage(MessageCreateEvent event);

    @Override
    protected final Mono<Void> execute(Flux<MessageCreateEvent> onEvent) {
        return onEvent
                .filterWhen(MessageUtil::canReply)
                .flatMap(this::genMessage)
                .flatMap(m -> m.addReaction(Reactions.WASTEBASKET).thenReturn(m))
                .doOnNext(m -> messages.add(m.getId()))
                .onErrorContinue((error, event) -> Bot.LOGGER.error("Event listener had an uncaught exception!", error))
                .then();
    }

    public static final class DeleteListener extends EventListener<ReactionAddEvent> {
        public DeleteListener(GatewayDiscordClient client, Wiki wiki) {
            super(ReactionAddEvent.class, client, wiki);
        }

        @Override
        protected Mono<Void> execute(Flux<ReactionAddEvent> onEvent) {
            return onEvent
                    .filter(e -> e.getEmoji().equals(Reactions.WASTEBASKET)
                            && !e.getUserId().equals(e.getClient().getSelfId())
                            && messages.contains(e.getMessageId()))
                    .flatMap(ReactionAddEvent::getMessage)
                    .flatMap(Message::delete)
                    .then();
        }
    }
}
