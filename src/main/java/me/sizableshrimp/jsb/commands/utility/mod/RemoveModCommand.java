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
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.api.ConfirmationCommand;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.BaseConfirmationContext;
import me.sizableshrimp.jsb.data.Mod;
import me.sizableshrimp.jsb.util.MessageUtil;
import me.sizableshrimp.jsb.util.Reactions;
import org.fastily.jwiki.core.AReply;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoveModCommand extends ConfirmationCommand<RemoveModCommand.ConfirmationContext> {
    public static final String REACT_WITH = "Please react with ❌ to delete or \uD83D\uDDD1 to cancel.";

    public RemoveModCommand() {
        super(Map.of(
                Reactions.X, (confirmation, event) -> {
                    Mod toDelete = confirmation.toDelete();

                    try {
                        AReply reply = toDelete.remove();
                        String message = MessageUtil.getMessageFromReply(reply, r -> {
                            Bot.LOGGER.info("Removed mod {} from mods list.", toDelete);
                            return String.format("Removed **%s** (abbreviated as `%s`) from the mods list. Please mark for translation.",
                                    toDelete.name(), toDelete.abbrv());
                        }, r -> "deleting mod " + toDelete.abbrv());

                        return event.getChannel().flatMap(channel -> sendMessage(message, channel));
                    } catch (IllegalStateException e) {
                        return event.getChannel().flatMap(channel -> sendMessage(e.getMessage(), channel));
                    }
                }, Reactions.WASTEBASKET, (confirmation, event) -> event.getMessage().flatMap(Message::delete)
                        .then(event.getChannel().flatMap(channel -> sendMessage(String.format("Cancelling deletion of mod named **%s** with abbreviation `%s`...",
                                confirmation.toDelete().name(), confirmation.toDelete().abbrv()), channel)))
        ), List.of(Reactions.X, Reactions.WASTEBASKET));
    }

    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname% <mod name|mod abbreviation>", """
                Removes a mod from the mod list which takes either the mod abbreviation or unlocalized mod name.
                """);
    }

    @Override
    public String getName() {
        return "removemod";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("delmod", "delabbrv", "removeabbrv");
    }

    @Override
    public List<String> getRequiredRoles() {
        return List.of("Editor");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 1) {
            return incorrectUsage(context, event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            Mod toDelete = Mod.getByInfo(context.wiki(), args.getJoinedArgs());
            if (toDelete == null) {
                return sendMessage("That mod doesn't exist!", channel);
            }

            Mono<Message> confirm;
            if (toDelete.hasDistinctLink()) {
                confirm = sendMessage(String.format(
                        "Do you want to delete the mod named **%s** with abbreviation `%s` and link `%s` from the list? "
                                + REACT_WITH,
                        toDelete.name(), toDelete.abbrv(), toDelete.getUrlLink()), channel);
            } else {
                confirm = sendMessage(String
                                .format("Do you want to delete the mod named **%s** with abbreviation `%s` from the list? "
                                        + REACT_WITH, toDelete.name(), toDelete.abbrv()),
                        channel);
            }
            return confirm.flatMap(m -> addReactions(m, new ConfirmationContext(context.wiki(), event.getMessage(), m, toDelete)));
        });
    }

    public record ConfirmationContext(Wiki wiki, Message original, Message response, Mod toDelete) implements BaseConfirmationContext {}
}
