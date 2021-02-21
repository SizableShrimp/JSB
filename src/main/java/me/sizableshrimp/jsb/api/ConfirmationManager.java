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

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import me.sizableshrimp.jsb.data.BaseConfirmationContext;
import me.sizableshrimp.jsb.listeners.ConfirmationListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class ConfirmationManager<C extends BaseConfirmationContext> {
    public static final Duration CONFIRMATION_TIMEOUT = Duration.ofMinutes(10);
    private final Map<ReactionEmoji, BiFunction<C, ReactionAddEvent, Mono<Message>>> reactionsMap;
    private final List<ReactionEmoji> reactionsOrder;
    private final Map<Snowflake, C> awaitingConfirmation = new HashMap<>();

    public ConfirmationManager(Map<ReactionEmoji, BiFunction<C, ReactionAddEvent, Mono<Message>>> reactionsMap, List<ReactionEmoji> reactionsOrder) {
        this.reactionsMap = Map.copyOf(reactionsMap);
        this.reactionsOrder = reactionsOrder;

        if (reactionsOrder.size() != reactionsMap.size() || !Set.copyOf(reactionsOrder).equals(reactionsMap.keySet()))
            throw new IllegalStateException("Reactions list does not match reactions map");

        ConfirmationListener.managers.add(this);
    }

    public boolean isValid(ReactionAddEvent event) {
        C confirmation = this.awaitingConfirmation.get(event.getUserId());
        return confirmation != null && confirmation.messageId().equals(event.getMessageId()) && this.reactionsMap.containsKey(event.getEmoji());
    }

    public Mono<Message> execute(ReactionAddEvent event) {
        C confirmation = this.awaitingConfirmation.remove(event.getUserId());
        if (CONFIRMATION_TIMEOUT.compareTo(Duration.between(Instant.now(), event.getMessageId().getTimestamp())) <= 0)
            return Mono.empty(); // Do not allow responses after the confirmation timeout
        return this.reactionsMap.get(event.getEmoji()).apply(confirmation, event);
    }

    public Mono<Void> addReactions(Message response, C confirmation) {
        this.awaitingConfirmation.put(confirmation.authorId(), confirmation);

        return Flux.fromIterable(this.reactionsOrder)
                .flatMap(response::addReaction)
                .then();
    }
}
