package me.sizableshrimp.jwiki.commands;

import me.sizableshrimp.jwiki.Main;
import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import me.sizableshrimp.jwiki.data.Language;
import me.sizableshrimp.jwiki.data.Mod;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.Wiki;

import java.util.List;

public class GetModCommand extends Command {
    public GetModCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "getmod";
    }

    @Override
    public List<String> getAliases() {
        return List.of("modinfo", "mod");
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Get a mod using the unlocalized mod name or abbreviation.");
    }

    @Override
    public void run(Args args) {
        if (args.getArgs().length != 2) {
            return;
        }
        Language language = Language.getByCode(wiki, args.getArgs()[1]);
        Mod mod = Mod.getMod(wiki, args.getArgs()[0]);
        String link = "<" + HttpUrl.parse(Main.CONFIG.getApi()).host() + "/" + mod.getLink() + ">";

        String formatted = String.format("**%s** (abbreviated as **%s**) can be found at %s.", mod.getName(), mod.getAbbrv(), link);
        if (language != null) {
            formatted += String.format("%nThe localized name in %s is %s.", language.getEnglish(), mod.getLocalized(language.getCode()));
        }

        System.out.println(formatted);
    }
}
