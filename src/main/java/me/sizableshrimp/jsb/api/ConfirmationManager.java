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
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class ConfirmationManager<C extends BaseConfirmationContext> {
    private final Map<ReactionEmoji, BiFunction<C, ReactionAddEvent, Mono<?>>> reactionsMap;
    private final List<ReactionEmoji> reactionsOrder;
    private final Map<Snowflake, C> awaitingConfirmation = new HashMap<>();

    public ConfirmationManager(Map<ReactionEmoji, BiFunction<C, ReactionAddEvent, Mono<?>>> reactionsMap, List<ReactionEmoji> reactionsOrder) {
        this.reactionsMap = Map.copyOf(reactionsMap);
        this.reactionsOrder = reactionsOrder;

        if (reactionsOrder.size() != reactionsMap.size() || !Set.copyOf(reactionsOrder).equals(reactionsMap.keySet()))
            throw new IllegalStateException("Reactions list does not match reactions map");

        ConfirmationListener.managers.add(this);
    }

    public boolean isValid(ReactionAddEvent event) {
        C confirmation = awaitingConfirmation.get(event.getMessageId());
        return confirmation != null && confirmation.authorId.equals(event.getUserId()) && reactionsMap.containsKey(event.getEmoji());
    }

    public Mono<?> execute(ReactionAddEvent event) {
        C confirmation = awaitingConfirmation.remove(event.getMessageId());
        if (System.currentTimeMillis() - event.getMessageId().getTimestamp().toEpochMilli() > 10_000)
            return Mono.empty(); // Do not allow responses after 10 minutes
        return reactionsMap.get(event.getEmoji()).apply(confirmation, event);
    }

    public Mono<Message> addReactions(Message message, C confirmation) {
        awaitingConfirmation.put(message.getId(), confirmation);
        Mono<Message> mono = Mono.just(message);

        for (ReactionEmoji emoji : reactionsOrder) {
            mono = mono.flatMap(m -> m.addReaction(emoji).thenReturn(m));
        }

        return mono;
    }
}
