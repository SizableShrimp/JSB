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
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Permission;
import me.sizableshrimp.jsb.api.EventListener;
import me.sizableshrimp.jsb.util.MessageUtil;
import me.sizableshrimp.jsb.util.Reactions;
import org.fastily.jwiki.core.Wiki;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PagerListener extends EventListener<ReactionAddEvent> {
    public static final List<ReactionEmoji> PAGER_LIST = List.of(Reactions.REWIND, Reactions.ARROW_LEFT, Reactions.ARROW_RIGHT, Reactions.FAST_FORWARD);
    private static final Map<Snowflake, PageData> pagedMessages = new HashMap<>();

    public PagerListener(GatewayDiscordClient client, Wiki wiki) {
        super(ReactionAddEvent.class, client, wiki);
    }

    @Override
    protected Mono<Void> execute(Flux<ReactionAddEvent> onEvent) {
        return onEvent
                .filterWhen(e -> e.getChannel().map(c -> c instanceof GuildMessageChannel))
                .filter(e -> pagedMessages.containsKey(e.getMessageId()) && pagedMessages.get(e.getMessageId()).authorId().equals(e.getUserId()))
                .flatMap(e -> {
                    if (!PAGER_LIST.contains(e.getEmoji()))
                        return Mono.empty();

                    PageData data = pagedMessages.get(e.getMessageId());
                    int lastIndex = data.pages().size() - 1;

                    return e.getMessage().flatMap(m -> {
                        if (e.getEmoji().equals(Reactions.REWIND)) {
                            return editPage(m, data, 0);
                        } else if (e.getEmoji().equals(Reactions.ARROW_LEFT)) {
                            return editPage(m, data, data.index() == 0 ? lastIndex : data.index() - 1);
                        } else if (e.getEmoji().equals(Reactions.ARROW_RIGHT)) {
                            return editPage(m, data, data.index() == lastIndex ? 0 : data.index() + 1);
                        } else {
                            return editPage(m, data, lastIndex);
                        }
                    }).filterWhen(m -> m.getChannel().cast(GuildMessageChannel.class)
                            .flatMap(c -> c.getEffectivePermissions(e.getClient().getSelfId()).map(p -> p.contains(Permission.MANAGE_MESSAGES)))
                    ).flatMap(m -> m.removeReaction(e.getEmoji(), e.getUserId()));
                }).then();
    }

    @NotNull
    private Mono<Message> editPage(Message message, PageData data, int index) {
        String description = data.pages().get(index);
        pagedMessages.put(message.getId(), new PageData(index, data.pages(), data.authorId()));
        return message.edit(edit -> {
            Embed embed = message.getEmbeds().get(0);
            edit.setEmbed(embedSpec -> embedSpec.setTitle(embed.getTitle().get())
                    .setUrl(embed.getUrl().get())
                    .setFooter("Page " + (index + 1) + "/" + data.pages().size(), null)
                    .setDescription(description));
        });
    }

    public static Mono<Message> sendInitialPageMessage(MessageChannel channel, PageData data, String specialPage, String url) {
        String description = data.pages().get(0);
        int size = data.pages().size();
        return MessageUtil.sendEmbed(embedSpec -> embedSpec.setTitle(specialPage)
                .setUrl(url)
                .setFooter("Page 1/" + size, null)
                .setDescription(description), channel)
                .filter(m -> size > 1)
                .doOnNext(m -> pagedMessages.put(m.getId(), data))
                .flatMap(m -> MessageUtil.addReactions(m, PAGER_LIST).thenReturn(m));
    }

    public record PageData(int index, List<String> pages, Snowflake authorId) {
        public PageData(List<String> pages, Snowflake authorId) {
            this(0, pages, authorId);
        }
    }
}
