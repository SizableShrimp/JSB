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

package me.sizableshrimp.jsb.commands.utility;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.commands.AbstractCommand;
import me.sizableshrimp.jsb.util.WikiUtil;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.regex.Pattern;

public class WikilinkCommand extends AbstractCommand {
    private static final Pattern SPECIAL_PAGE = Pattern.compile("^:?Special:");

    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <page>", """
                Provides a link to the page and states whether it exists.
                Provide the page name without brackets.
                """);
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
        if (args.getLength() < 1 || args.getJoinedArgs().indexOf('[') != -1 || args.getJoinedArgs().indexOf(']') != -1) {
            return incorrectUsage(event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String link = args.getJoinedArgs();
            String message = genWikilink(context.getWiki(), link);
            return sendMessage(message, channel);
        });
    }

    public static String genWikilink(Wiki wiki, String link) {
        return genWikilink(new StringBuilder(), wiki, link).toString();
    }

    public static StringBuilder genWikilink(StringBuilder builder, Wiki wiki, String link) {
        if (SPECIAL_PAGE.matcher(link).find())
            return builder.append('<').append(WikiUtil.getBaseWikiPageUrl(wiki, link)).append(">\n");

        String baseUrl = WikiUtil.getBaseArticleUrl(wiki);
        String url = WikiUtil.getWikiPageUrl(wiki, link);
        if (url == null) {
            return builder;
        }

        int sectionLink = link.indexOf('#');
        link = sectionLink == -1 ? link : link.substring(0, sectionLink);
        if (url.startsWith(baseUrl) && !wiki.exists(link)) {
            return builder.append("The page **").append(link).append("** does not exist. Create it here: <")
                    .append(url).append(">\n");
        }

        return builder.append(url).append('\n');
    }
}
