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

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.Mod;
import me.sizableshrimp.jsb.util.WikiUtil;
import reactor.core.publisher.Mono;

public class TilesheetRequestCommand extends AbstractCommand {
    private static final String REQUESTS_PAGE = "Feed The Beast Wiki:Tilesheet requests";
    private static final Pattern PATTERN = Pattern.compile("\\s*<!--Do not edit below this line\\.-->\\n\\{\\{Navbox portals}}\\s*$");
    private static final String FOOTER = "\n\n\n<!--Do not edit below this line.-->\n{{Navbox portals}}";

    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <mod info> <link>", """
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
    public List<String> getRequiredRoles() {
        return List.of("Editor");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 2) {
            return incorrectUsage(event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String modInput = args.getArgRange(0, args.getLength() - 1);
            String link = args.getArg(args.getLength() - 1);
            Mod mod = Mod.getByInfo(context.getWiki(), modInput);

            if (mod == null)
                return sendMessage(String.format("The mod specified (**%s**) does not exist.", modInput), channel);

            String pageText = context.getWiki().getPageText(REQUESTS_PAGE);
            Matcher matcher = PATTERN.matcher(pageText);
            if (!matcher.find())
                return sendMessage("**Error:** Could not detect footer on page. Please fix it -> <"
                        + WikiUtil.getBaseArticleUrl(context.getWiki()) + REQUESTS_PAGE + '>', channel);

            String header = "== " + mod.getName() + " ==";
            String message = String.format("Tilesheet Request for <code>%s</code>. %s ~~~~", mod.getAbbrv(), link);

            String newText = pageText.replace(matcher.group(), "\n\n" + header + '\n' + message + '\n' + FOOTER);
            boolean success = context.getWiki().edit(REQUESTS_PAGE, newText, "Added tilesheet request for " + mod.getAbbrv());

            if (success) {
                return sendMessage("Added tilesheet request for `" + mod.getAbbrv() + "`.", channel);
            } else {
                return sendMessage("An error occurred when uploading the text.", channel);
            }
        });
    }
}
