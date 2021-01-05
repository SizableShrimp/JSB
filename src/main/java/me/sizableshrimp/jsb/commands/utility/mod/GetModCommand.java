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

package me.sizableshrimp.jsb.commands.utility.mod;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.Language;
import me.sizableshrimp.jsb.data.Mod;
import reactor.core.publisher.Mono;

import java.util.Set;

public class GetModCommand extends AbstractCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <mod name|mod abbreviation> [language code]",
                "Get a mod using the unlocalized mod name or abbreviation. Optionally display the localized name in the language.");
    }

    @Override
    public String getName() {
        return "getmod";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("modinfo", "mod", "getabbrv");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 1) {
            return incorrectUsage(event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String modInput = args.getRawArgs();
            Mod mod = Mod.getByInfo(context.getWiki(), modInput);
            if (mod != null) {
                return formatModMessage(channel, mod, null);
            } else if (args.getLength() == 1) {
                return formatModDoesntExistMessage(channel, modInput);
            }

            String langInput = args.getArgRange(args.getLength() - 1);
            Language language = Language.getByCode(context.getWiki(), langInput);

            modInput = args.getArgRange(0, args.getLength() - 1);
            mod = Mod.getByInfo(context.getWiki(), modInput);

            if (mod == null && language == null) {
                return formatModDoesntExistMessage(channel, args.getRawArgs());
            } else if (mod == null) {
                return formatModDoesntExistMessage(channel, modInput);
            } else if (language == null) {
                return sendMessage(String.format("The language specified (%s) does not exist.", langInput), channel);
            }

            return formatModMessage(channel, mod, language);
        });
    }

    private Mono<Message> formatModDoesntExistMessage(MessageChannel channel, String modInput) {
        return sendMessage(String.format("The mod specified (**%s**) does not exist.", modInput), channel);
    }

    private Mono<Message> formatModMessage(MessageChannel channel, Mod mod, Language language) {
        String link = '<' + mod.getUrlLink() + '>';
        String formatted = String.format("**%s** (abbreviated as `%s`) can be found at %s.", mod.getName(), mod.getAbbrv(), link);

        if (language != null) {
            String localized = String.format("The localized name in %s is %s.", language.getEnglish(), mod.getLocalized(language));
            formatted = formatted + '\n' + localized;
        }

        return sendMessage(formatted, channel);
    }
}
