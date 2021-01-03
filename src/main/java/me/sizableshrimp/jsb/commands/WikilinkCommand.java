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
import me.sizableshrimp.jsb.util.WikiUtil;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Mono;

import java.util.Set;

public class WikilinkCommand extends AbstractCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <page>", "Provides a link to the page and states whether it exists.");
    }

    @Override
    public String getName() {
        return "wikilink";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("wl");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 1) {
            return incorrectUsage(event);
        }

        String link = args.getArgRange(0);
        return Mono.fromSupplier(() -> genWikilink(new StringBuilder(), context.getWiki(), link))
                .zipWith(event.getMessage().getChannel())
                .flatMap(tuple -> {
                    String message = tuple.getT1().toString();
                    return message.isBlank() ? Mono.empty() : sendMessage(message, tuple.getT2());
                });
    }

    public static StringBuilder genWikilink(StringBuilder builder, Wiki wiki, String link) {
        String baseUrl = WikiUtil.getBaseArticleUrl(wiki);
        String url = WikiUtil.getWikiPageUrl(wiki, link);
        if (url == null) {
            return builder;
        }

        if (url.startsWith(baseUrl) && !wiki.exists(link)) {
            return builder.append("The page **").append(link).append("** does not exist. Create it here: <")
                    .append(WikiUtil.getWikiPageUrl(wiki, link)).append(">\n");
        }

        return builder.append(WikiUtil.getWikiPageUrl(wiki, link)).append('\n');
    }
}
