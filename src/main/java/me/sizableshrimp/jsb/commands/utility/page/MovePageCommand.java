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
import me.sizableshrimp.jsb.util.WikiUtil;
import org.fastily.jwiki.core.AReply;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MovePageCommand extends ConfirmationCommand<MovePageCommand.ConfirmationContext> {
    public MovePageCommand() {
        super(Map.of(
                Reactions.CHECKMARK, (confirmation, event) -> event.getChannel().flatMap(channel -> {
                    String invalidMessage = getInvalidMessage(confirmation.wiki(), confirmation.originalPage(), confirmation.destinationPage());
                    if (invalidMessage != null)
                        return sendMessage(invalidMessage, channel);

                    return event.getClient().getUserById(confirmation.authorId()).flatMap(user -> {
                        if (confirmation.wiki().exists(confirmation.destinationPage())) {
                            confirmation.wiki().delete(confirmation.destinationPage(), String.format("Deleted to make way for move from \"[[%s]]\"", confirmation.wiki().normalizeTitle(confirmation.originalPage())));
                        }
                        AReply reply = confirmation.wiki().move(confirmation.originalPage(), confirmation.destinationPage(), true, true,
                                !confirmation.leaveRedirect(), confirmation.reason() + " by " + MessageUtil.getUsernameDiscriminator(user));

                        String message = MessageUtil.getMessageFromReply(reply,
                                r -> "Moved " + confirmation.fullMessage(),
                                r -> "moving page " + confirmation.originalPage());
                        return sendMessage(message, channel);
                    });
                }), Reactions.X, (confirmation, event) -> event.getMessage().flatMap(Message::delete)
                        .then(event.getChannel().flatMap(channel -> sendMessage(String.format("Cancelling page move from **%s** to <%s>...",
                                confirmation.originalPage(), WikiUtil.getBaseWikiPageUrl(confirmation.wiki(), confirmation.destinationPage())), channel)))
        ), List.of(Reactions.CHECKMARK, Reactions.X));
    }

    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname% <original page> <destination> [leave redirect <true|false>] [reason]", """
                Moves a page from the original location to its destination, optionally leaving a redirect or not.
                If the redirect parameter is omitted, it will default to `true`.
                A reason can be optionally specified. If a reason is specified, leaving a redirect MUST be specified.

                If any parameters have a space in them, **wrap in quotes**.
                """);
    }

    @Override
    public String getName() {
        return "movepage";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("move");
    }

    @Override
    public List<String> getRequiredRoles() {
        return List.of("Editor");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 2) {
            return incorrectUsage(context, event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String originalPage = context.wiki().normalizeTitle(args.getArg(0));
            String destinationPage = context.wiki().normalizeTitle(args.getArg(1));
            if (args.getLength() >= 3 && !args.isArgValidBoolean(2))
                return incorrectUsage(String.format(Args.NO_BOOLEAN_MESSAGE, "leave redirect"), context, event);
            // If this was omitted (less than 3 args), always true.
            boolean leaveRedirect = args.getLength() < 3 || args.getArgAsBoolean(2);
            String reason = args.getLength() >= 4 ? args.getArgRange(3) : null;
            String parsedReason = reason == null ? "Move requested" : '"' + reason + '"';

            String invalidMessage = getInvalidMessage(context.wiki(), originalPage, destinationPage);
            if (invalidMessage != null)
                return sendMessage(invalidMessage, channel);

            String destinationUrl = WikiUtil.getBaseWikiPageUrl(context.wiki(), destinationPage);
            String preface = context.wiki().exists(destinationPage) && !originalPage.equalsIgnoreCase(context.wiki().resolveRedirect(destinationPage))
                    ? String.format("**%s** already exists. Do you want to delete it and move", destinationPage)
                    : "Do you want to move";
            String redirectMessage = leaveRedirect ? "while keeping a redirect" : "__without__ leaving a redirect";
            String reasonMessage = reason == null ? "" : " with reason \"" + reason + "\"";
            String fullMessage = "**%s** to <%s> %s%s".formatted(originalPage, destinationUrl, redirectMessage, reasonMessage);
            return sendMessage("%s **%s** to %s %s%s?".formatted(preface, originalPage, destinationUrl, redirectMessage, reasonMessage), channel)
                    .flatMap(m -> addReactions(m, new ConfirmationContext(context.wiki(), event.getMessage(), m, originalPage, destinationPage, leaveRedirect, parsedReason, fullMessage)));
        });
    }

    private static String getInvalidMessage(Wiki wiki, String originalPage, String destinationPage) {
        if (!wiki.exists(originalPage))
            return String.format("**%s** does not exist!", originalPage);

        return null;
    }

    public record ConfirmationContext(Wiki wiki, Message original, Message response, String originalPage, String destinationPage, boolean leaveRedirect, String reason, String fullMessage) implements BaseConfirmationContext {}
}
