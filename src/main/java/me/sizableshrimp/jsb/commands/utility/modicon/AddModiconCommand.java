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

package me.sizableshrimp.jsb.commands.utility.modicon;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.api.ConfirmationCommand;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.BaseConfirmationContext;
import me.sizableshrimp.jsb.data.Mod;
import me.sizableshrimp.jsb.util.MessageUtil;
import me.sizableshrimp.jsb.util.Reactions;
import me.sizableshrimp.jsb.util.WikiUtil;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.AReply;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddModiconCommand extends ConfirmationCommand<AddModiconCommand.ConfirmationContext> {
    public AddModiconCommand() {
        super(Map.of(
                Reactions.CHECKMARK, (confirmation, event) -> event.getChannel().flatMap(channel -> {
                    String invalidMessage = getInvalidMessage(confirmation.wiki(), confirmation.modName());
                    if (invalidMessage != null)
                        return sendMessage(invalidMessage, channel);

                    return event.getClient().getUserById(confirmation.authorId()).flatMap(user -> {
                        AReply reply = confirmation.wiki().uploadByUrl(confirmation.url(), confirmation.destination(), "{{Modicon}}",
                                "Uploaded modicon requested by " + MessageUtil.getUsernameDiscriminator(user));

                        String message = MessageUtil.getMessageFromReply(reply,
                                r -> String.format("Uploaded modicon to <%s>.", reply.getSuccessJson().getAsJsonObject("imageinfo").get("descriptionurl").getAsString()),
                                r -> "uploading modicon to for " + confirmation.modName());

                        return sendMessage(message, channel);
                    });
                }),
                Reactions.X, (confirmation, event) -> event.getMessage().flatMap(Message::delete)
                        .then(event.getChannel().flatMap(channel -> sendMessage(String.format("Cancelling modicon upload for **%s**..",
                                confirmation.modName()), channel)))
        ), List.of(Reactions.CHECKMARK, Reactions.X));
    }

    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname% <mod name|mod abbreviation> <file url>", """
                Uploads a modicon to the wiki in the form `File:Modicon (mod name).png`.
                """);
    }

    @Override
    public String getName() {
        return "addmodicon";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("uploadmodicon");
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
            String modInput = args.getArgRange(0, args.getLength() - 1);
            String link = args.getArg(args.getLength() - 1);
            HttpUrl url = HttpUrl.parse(link);
            if (url == null)
                return sendMessage("Please provide a valid file URL!", channel);
            Mod mod = Mod.getByInfo(context.wiki(), modInput);
            String modName = mod == null ? modInput : mod.name();

            String invalidMessage = getInvalidMessage(context.wiki(), modName);
            if (invalidMessage != null)
                return sendMessage(invalidMessage, channel);

            String destination = context.wiki().normalizeTitle("File:Modicon %s".formatted(modName)); // The file extension will be added by MediaWiki.
            String message = mod == null
                    ? "A mod with the name **%s** does not exist in the mods list, would you like to still add a new modicon to the wiki for it?"
                    : "Do you want to add a new modicon to the wiki for **%s**?";

            return sendMessage(message.formatted(modName), channel)
                    .flatMap(m -> addReactions(m, new ConfirmationContext(context.wiki(), event.getMessage(), m, modName, url, destination)));
        });
    }

    private static String getInvalidMessage(Wiki wiki, String modName) {
        List<String> prefixIndex = wiki.allPages("Modicon %s.".formatted(modName), false, false, 1, NS.FILE);
        if (!prefixIndex.isEmpty()) {
            String fileLink = WikiUtil.getBaseWikiPageUrl(wiki, prefixIndex.get(0));
            return "That modicon already exists at <%s>!".formatted(fileLink);
        }

        return null;
    }

    public record ConfirmationContext(Wiki wiki, Message original, Message response, String modName, HttpUrl url, String destination) implements BaseConfirmationContext {}
}
