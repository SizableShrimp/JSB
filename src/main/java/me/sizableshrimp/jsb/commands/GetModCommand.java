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
            String langInput = args.getLength() == 1 ? "" : args.getBeforeSpace(1);
            Language language = Language.getByCode(context.getWiki(), langInput);

            String modInput = args.getLength() == 1 ? args.getArg(0) : args.getArgRange(0, args.getLength() - 1);
            Mod mod = Mod.getMod(context.getWiki(), modInput);

            // This code allows users to get a mod with spaces so long as there is no language.
            if (language == null && mod == null && !langInput.isBlank()) {
                modInput = modInput + ' ' + langInput;
                mod = Mod.getMod(context.getWiki(), modInput);
            } else if (!langInput.isBlank() && language == null) {
                // If we already found the mod and langInput is not empty and the language is null, it is an invalid language.
                return sendMessage(String.format("The language specified (%s) does not exist.", langInput), channel);
            }
            if (mod == null)
                return sendMessage(String.format("The mod specified (**%s**) does not exist.", modInput), channel);

            String link = '<' + mod.getUrlLink() + '>';

            String localized = language == null ? "" : String.format("%nThe localized name in %s is %s.", language.getEnglish(), mod.getLocalized(language));
            String formatted = String.format("**%s** (abbreviated as `%s`) can be found at %s.%s", mod.getName(), mod.getAbbrv(), link, localized);

            return sendMessage(formatted, channel);
        });
    }
}
