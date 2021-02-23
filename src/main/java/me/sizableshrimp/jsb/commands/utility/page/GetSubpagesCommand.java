package me.sizableshrimp.jsb.commands.utility.page;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.commands.AbstractCommand;
import me.sizableshrimp.jsb.data.Language;
import me.sizableshrimp.jsb.listeners.PagerListener;
import me.sizableshrimp.jsb.util.WikiUtil;
import org.fastily.jwiki.core.NS;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GetSubpagesCommand extends AbstractCommand {
    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname% <prefix>", """
                Get all pages that are prefixed by the provided value, excluding translated pages.
                """);
    }

    @Override
    public String getName() {
        return "getsubpages";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("subpage", "subpages", "prefixindex");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 1) {
            return incorrectUsage(context, event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String fullPage = context.wiki().normalizeTitle(args.getJoinedArgs());
            int nsIndex = fullPage.indexOf(':');
            NS ns = null;
            String prefix = fullPage;
            if (nsIndex != -1) {
                ns = context.wiki().getNS(fullPage.substring(0, nsIndex));
                prefix = fullPage.substring(nsIndex + 1);
            }
            List<String> temp = context.wiki().allPages(prefix, false, false, -1, ns);
            temp.removeIf(page -> Language.getByTitle(context.wiki(), page) != null);

            if (temp.isEmpty())
                return sendMessage("The returned list of pages was empty.", channel);

            String complete = "- " + temp.stream().map(s -> {
                int index = s.indexOf(':');
                return index == -1 ? s : s.substring(index + 1);
            }).collect(Collectors.joining("\n- "));

            String specialPage = "Special:PrefixIndex/" + fullPage;
            String url = WikiUtil.getBaseWikiPageUrl(context.wiki(), specialPage);
            List<String> pages = new ArrayList<>();

            int index = complete.indexOf('\n');
            int count = 1; // Include the one we just calculated
            while (index >= 0) {
                if (count == 20) {
                    pages.add(complete.substring(0, index + 1));
                    complete = complete.substring(index + 1);
                    count = 0;
                    index = 0;
                }
                index = complete.indexOf('\n', index + 1);
                count++;
            }
            pages.add(complete);

            return PagerListener.sendInitialPageMessage(channel, new PagerListener.PageData(pages, event.getMessage().getAuthor().get().getId()), specialPage, url);
        });
    }
}
