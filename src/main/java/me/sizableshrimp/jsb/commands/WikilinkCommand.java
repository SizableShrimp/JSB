package me.sizableshrimp.jsb.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.WikiUtil;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Mono;

import java.util.Set;

public class WikilinkCommand extends AbstractCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <page>", "Provides a link to the page and states whether it exists.");
    }

    @Override
    public String getName() {
        return "wikilink";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("wl");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 1) {
            return incorrectUsage(event);
        }

        String link = args.getArgRange(0);
        return Mono.fromSupplier(() -> genWikilink(new StringBuilder(), context.getWiki(), link))
                .zipWith(event.getMessage().getChannel())
                .flatMap(tuple -> {
                    String message = tuple.getT1().toString();
                    return message.isBlank() ? Mono.empty() : sendMessage(message, tuple.getT2());
                });
    }

    public static StringBuilder genWikilink(StringBuilder builder, Wiki wiki, String link) {
        String baseUrl = WikiUtil.getBaseArticleUrl(wiki);
        String url = WikiUtil.getWikiPageUrl(wiki, link);
        if (url == null) {
            return builder;
        }

        if (url.startsWith(baseUrl) && !wiki.exists(link)) {
            return builder.append("The page **").append(link).append("** does not exist. Create it here: <")
                    .append(WikiUtil.getWikiPageUrl(wiki, link)).append(">\n");
        }

        return builder.append(WikiUtil.getWikiPageUrl(wiki, link)).append('\n');
    }
}
