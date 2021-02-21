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
import me.sizableshrimp.jsb.util.CachedData;
import me.sizableshrimp.jsb.util.MessageUtil;
import me.sizableshrimp.jsb.util.Reactions;
import me.sizableshrimp.jsb.util.WikiUtil;
import org.fastily.jwiki.core.AReply;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeletePageCommand extends ConfirmationCommand<DeletePageCommand.ConfirmationContext> {
    public static final String DELETE_REASONS_PAGE = "MediaWiki:Filedelete-reason-dropdown";
    private final CachedData<String> deleteReasons = new CachedData<>();

    public DeletePageCommand() {
        super(Map.of(
                Reactions.CHECKMARK, (confirmation, event) -> event.getChannel().flatMap(channel -> {
                    String invalidMessage = getInvalidMessage(confirmation.wiki(), confirmation.page());
                    if (invalidMessage != null)
                        return sendMessage(invalidMessage, channel);

                    return event.getClient().getUserById(confirmation.authorId()).flatMap(user -> {
                        AReply reply = confirmation.wiki().delete(confirmation.page(), confirmation.reason() + " by " + MessageUtil.getUsernameDiscriminator(user));

                        String message = MessageUtil.getMessageFromReply(reply,
                                r -> "Deleted " + confirmation.fullMessage(),
                                r -> "deleting page " + confirmation.page());
                        return sendMessage(message, channel);
                    });
                }), Reactions.X, (confirmation, event) -> event.getMessage().flatMap(Message::delete)
                        .then(event.getChannel().flatMap(channel -> sendMessage("Cancelling deletion of **%s**...".formatted(confirmation.page()), channel)))
        ), List.of(Reactions.CHECKMARK, Reactions.X));
    }

    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname% <page> [reason]", """
                Deletes a page.
                A reason can be optionally specified.
                
                ```asciidoc
                %s
                ```
                """.formatted(this.deleteReasons.getOrRetrieve(() -> context.wiki().getPageText(DELETE_REASONS_PAGE))));
    }

    @Override
    public String getName() {
        return "deletepage";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("delete", "delpage", "del");
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
            String reason = args.getLength() > 1 ? args.getArgRange(1) : null;
            String parsedReason = reason == null ? "Delete requested" : '"' + reason + '"';

            String invalidMessage = getInvalidMessage(context.wiki(), page);
            if (invalidMessage != null)
                return sendMessage(invalidMessage, channel);

            String url = WikiUtil.getBaseWikiPageUrl(context.wiki(), page);
            String reasonMessage = reason == null ? "" : "with reason \"" + reason + "\"";
            String fullMessage = "**%s** at page <%s> %s".formatted(page, url, reasonMessage);
            return sendMessage("Do you want to delete **%s** at page %s %s?".formatted(page, url, reasonMessage), channel)
                    .flatMap(m -> addReactions(m, new ConfirmationContext(context.wiki(), event.getMessage(), m, page, parsedReason, fullMessage)));
        });
    }

    private static String getInvalidMessage(Wiki wiki, String page) {
        if (!wiki.exists(page))
            return String.format("**%s** does not exist!", page);

        return null;
    }

    public record ConfirmationContext(Wiki wiki, Message original, Message response, String page, String reason, String fullMessage) implements BaseConfirmationContext {}
}
