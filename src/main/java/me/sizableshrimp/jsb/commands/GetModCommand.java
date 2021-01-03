package me.sizableshrimp.jsb.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.Language;
import me.sizableshrimp.jsb.data.Mod;
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
        if (args.getLength() < 1) {
            return incorrectUsage(event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String langInput = args.getLength() == 1 ? "" : args.getBeforeSpace(1);
            Language language = Language.getByCode(context.getWiki(), langInput);

            String modInput = args.getLength() == 1 ? args.getArg(0) : args.getArgRange(0, args.getLength() - 1);
            Mod mod = Mod.getMod(context.getWiki(), modInput);

            // This code allows users to get a mod with spaces so long as there is no language.
            if (language == null && mod == null && !langInput.isBlank()) {
                modInput = modInput + ' ' + langInput;
                mod = Mod.getMod(context.getWiki(), modInput);
            } else if (!langInput.isBlank() && language == null) {
                // If we already found the mod and langInput is not empty and the language is null, it is an invalid language.
                return sendMessage(String.format("The language specified (%s) does not exist.", langInput), channel);
            }
            if (mod == null)
                return sendMessage(String.format("The mod specified (**%s**) does not exist.", modInput), channel);

            String link = '<' + mod.getUrlLink() + '>';

            String localized = language == null ? "" : String.format("%nThe localized name in %s is %s.", language.getEnglish(), mod.getLocalized(language));
            String formatted = String.format("**%s** (abbreviated as `%s`) can be found at %s.%s", mod.getName(), mod.getAbbrv(), link, localized);

            return sendMessage(formatted, channel);
        });
    }
}
