package me.sizableshrimp.jsb.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.Language;
import me.sizableshrimp.jsb.data.Mod;
import okhttp3.HttpUrl;
import reactor.core.publisher.Mono;

import java.util.Set;

public class GetModCommand extends AbstractCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <mod name|mod abbreviation> [language code]",
                "Get a mod using the unlocalized mod name or abbreviation. Optionally display the localized name in the language.");
    }

    @Override
    public String getName() {
        return "getmod";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("modinfo", "mod", "getabbrv");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 1 || args.getLength() > 2) {
            return incorrectUsage(event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String modInput = args.getArg(0);
            Mod mod = Mod.getMod(context.getWiki(), modInput);
            if (mod == null)
                return sendMessage(String.format("The mod specified (%s) does not exist.", modInput), channel);

            String langInput = args.getArgNullable(1);
            Language language = langInput == null ? null : Language.getByCode(context.getWiki(), langInput);
            if (langInput != null && language == null)
                return sendMessage(String.format("The language specified (%s) does not exist.", langInput), channel);

            String link = "<" + HttpUrl.parse(Bot.getConfig().getApi()).newBuilder().encodedPath("/").addPathSegment(mod.getLink().replace(' ', '_')).toString() + ">";

            String localized = language == null ? "" : String.format("%nThe localized name in %s is %s.", language.getEnglish(), mod.getLocalized(language));
            String formatted = String.format("**%s** (abbreviated as **%s**) can be found at %s.%s", mod.getName(), mod.getAbbrv(), link, localized);

            return sendMessage(formatted, channel);
        });
    }
}
