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

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.Mod;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class AddModCommand extends AbstractCommand {
    private static final Pattern CHARACTERS = Pattern.compile("^([a-zA-Z0-9]+)$");
    public static final Map<Snowflake, Confirmation> awaitingConfirmation = new HashMap<>();

    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <mod abbreviation> <mod name> [mod page]",
                """
                        Adds a mod to the mod list which takes a mod abbreviation and the unlocalized mod name.
                        Optionally takes a mod page link if it differs from the unlocalized mod name.
                        Wrap any arguments with spaces in quotes.
                        """);
    }

    @Override
    public String getName() {
        return "addmod";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("addabbrv");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 2 || args.getLength() > 3) {
            return incorrectUsage(event);
        }

        return requireRole(event, "Editor")
                .flatMap(e -> e.getMessage().getChannel())
                .flatMap(channel -> {
                    String modAbbrv = args.getArg(0).toUpperCase();
                    String modName = args.getArg(1);
                    String modPage = args.getArgNullable(2);
                    if (!CHARACTERS.matcher(modAbbrv).matches()) {
                        return sendMessage("Abbreviations that are not purely alphanumeric are not supported for technical reasons.", channel);
                    }
                    if (Character.isDigit(modAbbrv.charAt(0))) {
                        return sendMessage("Mod abbreviation cannot start with a number!", channel);
                    }

                    Mod newMod = new Mod(context.getWiki(), modAbbrv, modName, modPage);
                    Mod conflict = newMod.getConflict();
                    if (conflict != null) {
                        return sendMessage(String.format("This mod abbreviation already exists as **%s** with abbreviation `%s`!", conflict.getName(), conflict.getAbbrv()), channel);
                    } else {
                        Mono<Message> confirm;
                        if (newMod.hasDistinctLink()) {
                            confirm = sendMessage(String.format("Do you want to add a new mod to the list named **%s** with abbreviation `%s` and link `%s`? Please react with ✅ for yes or ❌ for no.",
                                    newMod.getName(), newMod.getAbbrv(), newMod.getUrlLink()), channel);
                        } else {
                            confirm = sendMessage(String.format("Do you want to add a new mod to the list named **%s** with abbreviation `%s`? Please react with ✅ for yes or ❌ for no.",
                                    newMod.getName(), newMod.getAbbrv()), channel);
                        }
                        return confirm.doOnNext(m -> awaitingConfirmation.put(m.getId(), new Confirmation(event.getMessage().getAuthor().get().getId(), m.getId(), newMod)))
                                .flatMap(m -> m.addReaction(ReactionEmoji.unicode("✅")).thenReturn(m))
                                .flatMap(m -> m.addReaction(ReactionEmoji.unicode("❌")).thenReturn(m));
                    }
                }).switchIfEmpty(event.getMessage().getChannel().flatMap(channel -> sendMessage("You must be an editor to execute this command!", channel)));
    }

    public static final class Confirmation {
        public final Snowflake author;
        public final Snowflake message;
        public final Mod newMod;

        public Confirmation(Snowflake author, Snowflake message, Mod newMod) {
            this.author = author;
            this.message = message;
            this.newMod = newMod;
        }
    }
}
