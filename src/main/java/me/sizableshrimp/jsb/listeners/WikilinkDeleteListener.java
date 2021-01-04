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
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WikilinkDeleteListener extends EventListener<ReactionAddEvent> {
    private static final ReactionEmoji WASTEBASKET = ReactionEmoji.unicode("🗑️");

    public WikilinkDeleteListener(GatewayDiscordClient client, Wiki wiki) {
        super(ReactionAddEvent.class, client, wiki);
    }

    @Override
    protected Mono<Void> execute(Flux<ReactionAddEvent> onEvent) {
        return onEvent
                .filter(e -> e.getEmoji().equals(WASTEBASKET))
                .filter(e -> !e.getUserId().equals(e.getClient().getSelfId()))
                .filter(e -> WikilinkListener.wikilinkMessages.contains(e.getMessageId()))
                .flatMap(ReactionAddEvent::getMessage)
                .flatMap(Message::delete)
                .then();
    }
}
