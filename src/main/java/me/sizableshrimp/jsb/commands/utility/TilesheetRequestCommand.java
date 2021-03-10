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
import discord4j.core.object.entity.User;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.commands.AbstractCommand;
import me.sizableshrimp.jsb.data.Mod;
import me.sizableshrimp.jsb.util.MessageUtil;
import me.sizableshrimp.jsb.util.WikiUtil;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.AReply;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TilesheetRequestCommand extends AbstractCommand {
    public static final String REQUESTS_PAGE = "Feed The Beast Wiki:Tilesheet requests";
    private static final Pattern PATTERN = Pattern.compile("\\s*<!--Do not edit below this line\\.-->\\n\\{\\{Navbox portals}}\\s*$");
    private static final String FOOTER = "\n\n\n<!--Do not edit below this line.-->\n{{Navbox portals}}";

    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname% <mod name|mod abbreviation> <file url>", """
                Adds a tilesheet request to [FTBW:Tilesheet requests](https://ftb.gamepedia.com/FTBW:Tilesheet_requests).
                Takes a mod abbreviation or unlocalized mod name and the link to the zip file.
                """);
    }

    @Override
    public String getName() {
        return "tilesheetrequest";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("tilerequest", "tr");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 2) {
            return incorrectUsage(context, event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String modInput = args.getArgRange(0, args.getLength() - 1);
            String link = args.getArg(args.getLength() - 1);
            HttpUrl url = HttpUrl.parse(link);
            if (url == null)
                return sendMessage("Please provide a valid file URL!", channel);
            Mod mod = Mod.getByInfo(context.wiki(), modInput);

            if (mod == null)
                return sendMessage(String.format("The mod specified (**%s**) does not exist.", modInput), channel);

            String pageText = context.wiki().getPageText(REQUESTS_PAGE);
            Matcher matcher = PATTERN.matcher(pageText);
            if (!matcher.find())
                return sendMessage("**Error:** Could not detect footer on page. Please fix it -> <"
                        + WikiUtil.getBaseWikiPageUrl(context.wiki(), REQUESTS_PAGE) + '>', channel);

            String header = "== " + mod.name() + " ==";
            User user = event.getMessage().getAuthor().get();
            String talkMessage = String.format("Tilesheet Request for <code>%s</code> requested by %s. %s ~~~~",
                    mod.abbrv(), MessageUtil.getUsernameDiscriminator(user), link);

            String newText = pageText.replace(matcher.group(), "\n\n" + header + '\n' + talkMessage + '\n' + FOOTER);
            AReply reply = context.wiki().edit(REQUESTS_PAGE, newText, "Added tilesheet request for " + mod.name());

            String message = MessageUtil.getMessageFromReply(reply, r -> {
                Bot.LOGGER.info("Added tilesheet request for {}", mod.name());
                return "Added tilesheet request for **" + mod.name() + "**.";
            }, r -> "adding tilesheet request for " + mod.name());

            return sendMessage(message, channel);
        });
    }
}
