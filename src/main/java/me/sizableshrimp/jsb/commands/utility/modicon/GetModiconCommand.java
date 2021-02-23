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

package me.sizableshrimp.jsb.commands.utility.modicon;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.commands.AbstractCommand;
import me.sizableshrimp.jsb.commands.utility.mod.GetModCommand;
import me.sizableshrimp.jsb.data.Mod;
import me.sizableshrimp.jsb.util.WikiUtil;
import org.fastily.jwiki.core.QTemplate;
import org.fastily.jwiki.util.FL;
import reactor.core.publisher.Mono;

import java.util.Set;

public class GetModiconCommand extends AbstractCommand {
    private static final QTemplate IMAGE_INFO = new QTemplate(FL.pMap("iiprop", "url"), "iilimit", "pages");

    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname% <mod name|mod abbreviation>",
                "Get a mod's modicon file and displays it in an embed.");
    }

    @Override
    public String getName() {
        return "getmodicon";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("modicon");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 1) {
            return incorrectUsage(context, event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String modInput = args.getJoinedArgs();
            Mod mod = Mod.getByInfo(context.wiki(), modInput);

            if (mod == null) {
                return GetModCommand.formatModDoesntExistMessage(channel, modInput);
            }

            String file = String.format("File:Modicon %s.png", mod.getName());
            String fileUrl = WikiUtil.getLatestFileUrl(context.wiki(), file);
            String link = WikiUtil.getBaseWikiPageUrl(context.wiki(), file);

            if (file == null) // It is missing
                return sendMessage(String.format("A modicon for **%s** does not exist at <%s>.", mod.getName(), link), channel);

            return sendEmbed(createRetrievalEmbed(embed -> embed.setImage(fileUrl)
                    .setTitle(file)
                    .setUrl(link)), channel);
        });
    }
}
