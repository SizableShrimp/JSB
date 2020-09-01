package me.sizableshrimp.jwiki.commands;

import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import me.sizableshrimp.jwiki.data.Language;
import org.fastily.jwiki.core.Wiki;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DisabledCommand
public class BritsCommand extends Command {
    public BritsCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "brits";
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Fix those stinky Brits in the Module namespace");
    }

    @Override
    public void run(Args args) {
        List<String> list = wiki.prefixIndex(wiki.getNS("Module"), "");
        Pattern pattern = Pattern.compile("localised", Pattern.CASE_INSENSITIVE);

        for (String module : list) {
            if (Language.getByTitle(wiki, module) != null)
                continue;
            String text = wiki.getPageText(module);
            StringBuilder updated = new StringBuilder(text);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                updated.replace(matcher.start(), matcher.end(), matcher.group().replace('s', 'z').replace('S', 'Z'));
            }
            if (!text.equals(updated.toString())) {
                if (wiki.edit(module, updated.toString(), "stinky Brits")) {
                    System.out.println("Successfully updated word for " + module);
                } else {
                    System.out.println("Failed to update word for " + module);
                }
            }
        }
    }
}
