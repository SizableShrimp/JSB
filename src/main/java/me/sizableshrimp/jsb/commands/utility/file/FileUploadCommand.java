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

package me.sizableshrimp.jsb.commands.utility.file;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.api.ConfirmationCommand;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.BaseConfirmationContext;
import me.sizableshrimp.jsb.util.MessageUtil;
import me.sizableshrimp.jsb.util.Reactions;
import me.sizableshrimp.jsb.util.WikiUtil;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.AReply;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileUploadCommand extends ConfirmationCommand<FileUploadCommand.ConfirmationContext> {
    public FileUploadCommand() {
        super(Map.of(
                Reactions.CHECKMARK, (confirmation, event) ->
                        event.getMessage()
                                .flatMap(Message::getChannel)
                                .zipWith(event.getClient().getUserById(confirmation.authorId))
                                .flatMap(tuple -> {
                                    MessageChannel channel = tuple.getT1();
                                    User user = tuple.getT2();

                                    if (confirmation.wiki.exists(confirmation.fullDestination)) {
                                        return MessageUtil.sendMessage(String.format("That file already exists at <%s>!", getFileLink(confirmation)), channel);
                                    }

                                    Bot.LOGGER.info("Uploading file URL {} to '{}'", confirmation.url, confirmation.fullDestination);

                                    AReply reply = confirmation.wiki.uploadByUrl(confirmation.url, confirmation.destination, confirmation.pageText,
                                            "Uploaded file requested by " + MessageUtil.getUsernameDiscriminator(user));

                                    String message = MessageUtil.getMessageFromReply(reply,
                                            r -> String.format("Uploaded file to <%s>.", reply.getSuccessJson().get("descriptionurl").getAsString()),
                                            r -> "uploading file to " + getFileLink(confirmation));

                                    return MessageUtil.sendMessage(message, channel);
                                }),
                Reactions.X, (confirmation, event) -> event.getMessage().flatMap(Message::delete)
                        .then(event.getChannel().flatMap(channel -> MessageUtil.sendMessage(String.format("Cancelling file upload to <%s>...",
                                getFileLink(confirmation)), channel)))
        ), List.of(Reactions.CHECKMARK, Reactions.X));
    }

    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <file url> <destination> [summary]", """
                Uploads a file to the wiki.
                Provide the destination without the `File:` prefix but include the file extension. For example, "Image.png".
                Provide an optional summary to be placed on the file page.

                If the destination page has spaces in it and you have added a summary, **wrap in quotes**.
                """);
    }

    @Override
    public String getName() {
        return "fileupload";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("upload", "uploadfile", "uploadimage", "imageupload");
    }

    @Override
    public List<String> getRequiredRoles() {
        return List.of("Editor");
    }

    @Override
    public Mono<?> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 2) {
            return incorrectUsage(event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String file = args.getArg(0);
            String pageText = args.getLength() >= 3 ? args.getArgRange(2) : "";

            HttpUrl url = file.startsWith("<") && file.endsWith(">") ? HttpUrl.parse(file.substring(1, file.length() - 1)) : HttpUrl.parse(file);
            if (url == null) {
                return sendMessage("The file URL provided is invalid!", channel);
            }

            String destination = args.getArg(1).replaceAll("^File:", "");
            String fullDestination = "File:" + destination;
            if (context.getWiki().exists(fullDestination)) {
                String fileLink = WikiUtil.getBaseWikiPageUrl(context.getWiki(), fullDestination);
                return MessageUtil.sendMessage(String.format("That file already exists at <%s>!", fileLink), channel);
            }

            String message = String.format("Do you want to add a new file to the wiki at **%s**? Please react with ✅ for yes or ❌ for no.", fullDestination);

            return sendMessage(message, channel)
                    .flatMap(m -> addReactions(m, new ConfirmationContext(context.getWiki(), event.getMessage(), m, url, destination, fullDestination, pageText)));
        });
    }

    private static String getFileLink(ConfirmationContext confirmation) {
        return WikiUtil.getBaseWikiPageUrl(confirmation.wiki, confirmation.fullDestination);
    }

    public static final class ConfirmationContext extends BaseConfirmationContext {
        public final HttpUrl url;
        public final String destination;
        public final String fullDestination;
        public final String pageText;

        public ConfirmationContext(Wiki wiki, Message original, Message response, HttpUrl url, String destination, String fullDestination, String pageText) {
            super(wiki, original, response);
            this.url = url;
            this.destination = destination;
            this.fullDestination = fullDestination;
            this.pageText = pageText;
        }
    }
}
