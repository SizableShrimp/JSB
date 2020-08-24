package me.sizableshrimp.jwiki.commands;

import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import org.fastily.jwiki.core.Wiki;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocStartLCommand extends Command {
    public DocStartLCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "fixdocstart";
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Adds {{L}} to all locations where {{Doc/Start}} is used and the page is marked for translation.");
    }

    @Override
    public void run(Args args) {
        Pattern pattern = Pattern.compile("\\{\\{Doc/Start(\\{\\{L}})?(.*?)}}");
        List<String> transclusions = wiki.whatTranscludesHere("Template:Doc/Start");
        System.out.println(transclusions.size() + " transclusions");
        for (String title : transclusions) {
            // If the /en subpage doesn't exist, the page isn't translatable.
            if (!wiki.exists(title + "/en"))
                continue;

            String original = wiki.getPageText(title);
            String text = original.replace("{{Doc/Start}}", "{{Doc/Start{{L}}}}");
            if (text.contains("baddoc") || text.contains("nodoc")) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    System.err.println("Found group, check manually - " + matcher.group() + " - " + title);
                } else {
                    System.err.println("Found nothing but should check - " + title);
                }
                continue;
            }

            // If the text was changed
            if (!original.equals(text)) {
                wiki.edit(title, text, "Updating {{Doc/Start}} to use {{L}} in translatable pages.");
                System.out.println("Updated " + title);
            }
        }
    }
}
