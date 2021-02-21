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
                Reactions.CHECKMARK, (confirmation, event) -> event.getChannel().flatMap(channel -> {
                    String invalidMessage = getInvalidMessage(confirmation.wiki(), confirmation.destination());
                    if (invalidMessage != null)
                        return sendMessage(invalidMessage, channel);

                    return event.getClient().getUserById(confirmation.authorId()).flatMap(user -> {
                        AReply reply = confirmation.wiki().uploadByUrl(confirmation.url(), confirmation.destination(), confirmation.pageText(),
                                "Uploaded file requested by " + MessageUtil.getUsernameDiscriminator(user));

                        String message = MessageUtil.getMessageFromReply(reply,
                                r -> String.format("Uploaded file to <%s>.", reply.getSuccessJson().getAsJsonObject("imageinfo").get("descriptionurl").getAsString()),
                                r -> "uploading file to " + getFileLink(confirmation));

                        return sendMessage(message, channel);
                    });
                }),
                Reactions.X, (confirmation, event) -> event.getMessage().flatMap(Message::delete)
                        .then(event.getChannel().flatMap(channel -> sendMessage(String.format("Cancelling file upload to <%s>...",
                                getFileLink(confirmation)), channel)))
        ), List.of(Reactions.CHECKMARK, Reactions.X));
    }

    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname% <destination> <file url> [page summary]", """
                Uploads a file to the wiki.
                Provide the destination and include the file extension. For example, `Image.png`.
                Provide an optional summary to be placed on the file page.

                If any parameters have a space in them, **wrap in quotes**.
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
            return incorrectUsage(context, event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String file = args.getArg(1);

            HttpUrl url = file.startsWith("<") && file.endsWith(">") ? HttpUrl.parse(file.substring(1, file.length() - 1)) : HttpUrl.parse(file);
            if (url == null) {
                return sendMessage("The file URL provided is invalid!", channel);
            }

            String destination = context.wiki().normalizeTitle(args.getArg(0).startsWith("File:") ? args.getArg(0) : "File:" + args.getArg(0));
            if (context.wiki().exists(destination)) {
                String fileLink = WikiUtil.getBaseWikiPageUrl(context.wiki(), destination);
                return sendMessage(String.format("That file already exists at <%s>!", fileLink), channel);
            }

            String pageText = args.getLength() > 2 ? args.getArgRange(2) : "";
            String message = String.format("Do you want to add a new file to the wiki at **%s**?", destination);

            return sendMessage(message, channel)
                    .flatMap(m -> addReactions(m, new ConfirmationContext(context.wiki(), event.getMessage(), m, url, destination, pageText)));
        });
    }

    private static String getFileLink(ConfirmationContext confirmation) {
        return WikiUtil.getBaseWikiPageUrl(confirmation.wiki, confirmation.destination);
    }

    private static String getInvalidMessage(Wiki wiki, String destination) {
        if (wiki.exists(destination))
            return String.format("That file already exists at <%s>!", WikiUtil.getBaseWikiPageUrl(wiki, destination));

        return null;
    }

    public record ConfirmationContext(Wiki wiki, Message original, Message response, HttpUrl url, String destination, String pageText) implements BaseConfirmationContext {}
}
