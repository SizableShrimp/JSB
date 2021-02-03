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
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.api.ConfirmationCommand;
import me.sizableshrimp.jsb.api.DisabledCommand;
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

@DisabledCommand
public class MovePageCommand extends ConfirmationCommand<MovePageCommand.ConfirmationContext> {
    public MovePageCommand() {
        super(Map.of(
                Reactions.CHECKMARK, (confirmation, event) -> event.getMessage().flatMap(Message::getChannel).map(channel -> {
                    String invalidMessage = getInvalidMessage(confirmation.wiki, confirmation.originalPage, confirmation.destinationPage);
                    if (invalidMessage != null)
                        return sendMessage(invalidMessage, channel);

                    AReply reply = confirmation.wiki.move(confirmation.originalPage, confirmation.destinationPage, true, true,
                            !confirmation.leaveRedirect, confirmation.reason /*+ " by " + MessageUtil.getUsernameDiscriminator(user)*/);

                    String message = MessageUtil.getMessageFromReply(reply,
                            r -> "Moved " + confirmation.fullMessage,
                            r -> "moving page " + confirmation.originalPage);
                    return MessageUtil.sendMessage(message, channel);
                }), Reactions.X, (confirmation, event) -> event.getMessage().flatMap(Message::delete)
                        .then(event.getChannel().flatMap(channel -> MessageUtil.sendMessage(String.format("Cancelling page move from **%s** to <%s>...",
                                confirmation.originalPage, WikiUtil.getBaseWikiPageUrl(confirmation.wiki, confirmation.destinationPage)), channel)))
        ), List.of(Reactions.CHECKMARK, Reactions.X));
    }

    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <original page> <destination> [leave redirect <true|false>] [reason]", """
                Moves a page from the original location to its destination, optionally leaving a redirect or not.
                If the redirect parameter is omitted, it will default to `true`.
                A reason can be optionally specified. If a reason is specified, leaving a redirect MUST be specified.

                If any parameters have a space in their name, **wrap in quotes**.
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
            return incorrectUsage(event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String originalPage = args.getArg(0);
            String destinationPage = args.getArg(1);
            // If this was omitted (less than 3 args), always true.
            boolean leaveRedirect = args.getLength() < 3 || args.getArgAsBoolean(2);
            String reason = args.getLength() >= 4 ? args.getArgRange(4) : null;
            String parsedReason = reason == null ? "Move requested" : '"' + reason + '"';

            String invalidMessage = getInvalidMessage(context.getWiki(), originalPage, destinationPage);
            if (invalidMessage != null)
                return sendMessage(invalidMessage, channel);

            String destinationUrl = WikiUtil.getBaseWikiPageUrl(context.getWiki(), destinationPage);
            String redirectMessage = leaveRedirect ? "while keeping a redirect" : "__without__ leaving a redirect";
            String reasonMessage = reason == null ? "" : " with reason \"" + reason + "\"";
            String fullMessage = String.format("**%s** to <%s> %s%s", originalPage, destinationUrl, redirectMessage, reasonMessage);
            return sendMessage("Do you want to move " + fullMessage + '?', channel)
                    .flatMap(m -> addReactions(m, new ConfirmationContext(context.getWiki(), event.getMessage(), m, originalPage, destinationPage, leaveRedirect, parsedReason, fullMessage)));
        });
    }

    private static String getInvalidMessage(Wiki wiki, String originalPage, String destinationPage) {
        if (!wiki.exists(originalPage)) {
            return String.format("**%s** does not exist!", originalPage);
        // Allow overwriting redirects if that redirect links to the page we are moving
        } else if (wiki.exists(destinationPage) && originalPage.equalsIgnoreCase(wiki.resolveRedirect(destinationPage))) {
            String link = WikiUtil.getBaseWikiPageUrl(wiki, destinationPage);
            return String.format("**%s** already exists as <%s>.", destinationPage, link);
        }

        return null;
    }

    public static final class ConfirmationContext extends BaseConfirmationContext {
        public final String originalPage;
        public final String destinationPage;
        public final boolean leaveRedirect;
        public final String reason;
        public final String fullMessage;

        public ConfirmationContext(Wiki wiki, Message original, Message response, String originalPage, String destinationPage, boolean leaveRedirect, String reason, String fullMessage) {
            super(wiki, original, response);
            this.originalPage = originalPage;
            this.destinationPage = destinationPage;
            this.leaveRedirect = leaveRedirect;
            this.reason = reason;
            this.fullMessage = fullMessage;
        }
    }
}
