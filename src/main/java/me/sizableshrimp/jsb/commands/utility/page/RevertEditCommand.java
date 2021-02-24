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

package me.sizableshrimp.jsb.commands.utility.page;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.api.ConfirmationCommand;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.BaseConfirmationContext;
import me.sizableshrimp.jsb.util.MessageUtil;
import me.sizableshrimp.jsb.util.Reactions;
import org.fastily.jwiki.core.AReply;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.Revision;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RevertEditCommand extends ConfirmationCommand<RevertEditCommand.ConfirmationContext> {
    public RevertEditCommand() {
        super(Map.of(
                Reactions.CHECKMARK, (confirmation, event) -> event.getChannel().flatMap(channel -> {
                    String invalidMessage = getInvalidMessage(confirmation.wiki(), confirmation.page());
                    if (invalidMessage != null)
                        return sendMessage(invalidMessage, channel);

                    Revision latest = getLatestRev(confirmation.wiki(), confirmation.page());
                    if (!confirmation.latest().equals(latest))
                        return sendMessage("The page has had new edits since you requested to revert it! Please try again.", channel);

                    return event.getClient().getUserById(confirmation.authorId()).flatMap(user -> {
                        String reason = "Reverted edits by [[Special:Contribs/%s|%<s]] ([[User talk:%<s|talk]])%s, requested by %s."
                                .formatted(confirmation.user(), confirmation.reason(), MessageUtil.getUsernameDiscriminator(user));
                        AReply reply = confirmation.wiki().edit(confirmation.page(), confirmation.revertHere().text, reason);

                        String addS = confirmation.count() == 1 ? "" : "s";
                        String message = MessageUtil.getMessageFromReply(reply,
                                r -> "Reverted **%d** consecutive edit%s by `%s` on page **%s**.".formatted(confirmation.count(), addS, confirmation.user(), confirmation.page()),
                                r -> "reverting edits on page **%s**".formatted(confirmation.page()));
                        return sendMessage(message, channel);
                    });
                }), Reactions.X, (confirmation, event) -> event.getMessage().flatMap(Message::delete)
                        .then(event.getChannel().flatMap(channel -> sendMessage("Cancelling revert of consecutive edits by `%s` on page **%s**..."
                                .formatted(confirmation.user(), confirmation.page()), channel)))
        ), List.of(Reactions.CHECKMARK, Reactions.X));
    }

    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname% <page> [reason] [number of edits]", """
                Reverts the latest consecutive edits made by the same user on a page.
                Optionally specify a reason and number of edits to revert from the same user.
                If no number of edits is supplied, edits from the latest user will be reverted up to the point that a different user has edited the page.

                If any parameters have a space in them, **wrap in quotes**.
                """);
    }

    @Override
    public String getName() {
        return "revert";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("revertedit", "revertedits");
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
            String page = context.wiki().normalizeTitle(args.getArg(0));
            String reason = args.getArgNullable(1);
            String parsedReason = reason == null ? "" : " for \"" + reason + '"';
            Integer edits = args.getNullableArgAsInteger(2);
            String invalidMessage = getInvalidMessage(context.wiki(), page);
            if (invalidMessage != null)
                return sendMessage(invalidMessage, channel);

            String lastEditor = context.wiki().getLastEditor(page);
            if (lastEditor == null)
                return sendMessage("An error occurred when trying to get the last editor of **%s**.".formatted(page), channel);

            List<Revision> nonUserRevs = context.wiki().getRevisions(page, 1, false, null, null, null, lastEditor);
            if (nonUserRevs.isEmpty())
                return sendMessage("There has only been one editor of **%s**. Please delete the page instead.".formatted(page), channel);

            Revision revertHere = nonUserRevs.get(0);
            Revision latest = getLatestRev(context.wiki(), page);
            List<Revision> allRevs = context.wiki().getRevisions(page, -1, false, null, null, null, null);
            int count = 0;
            for (Revision rev : allRevs) {
                if (edits != null && edits == count && count != 0) {
                    revertHere = rev;
                    break;
                }
                if (rev.equals(revertHere))
                    break;
                count++;
            }
            String addS = count == 1 ? "" : "s";
            String withReason = reason == null ? "" : " with reason \"%s\"".formatted(reason);
            int finalCount = count;
            Revision finalRevertHere = revertHere;
            return sendMessage("Do you want to revert **%d** consecutive edit%s by `%s` on page **%s**%s?".formatted(count, addS, lastEditor, page, withReason), channel)
                    .flatMap(m -> addReactions(m, new ConfirmationContext(context.wiki(), event.getMessage(), m, page, parsedReason, lastEditor, finalRevertHere, latest, finalCount)));
        });
    }

    private static Revision getLatestRev(Wiki wiki, String page) {
        return wiki.getRevisions(page, 1, false, null, null, null, null).get(0);
    }

    private static String getInvalidMessage(Wiki wiki, String page) {
        if (!wiki.exists(page))
            return String.format("**%s** does not exist!", page);

        return null;
    }

    public record ConfirmationContext(Wiki wiki, Message original, Message response, String page, String reason, String user, Revision revertHere, Revision latest, int count) implements BaseConfirmationContext {}
}
