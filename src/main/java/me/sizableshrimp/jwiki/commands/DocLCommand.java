package me.sizableshrimp.jwiki.commands;

import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import org.fastily.jwiki.core.Wiki;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DocLCommand extends Command {
    public DocLCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "fixdoc";
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Adds {{L}} to {{Doc/Start}} and {{Doc/End}} on documentation pages marked for translation.");
    }

    @Override
    public void run(Args args) {
        String start = genRegex("Start");
        String end = genRegex("End");
        List<String> transclusions = wiki.whatTranscludesHere("Template:Doc/Start");
        System.out.println(transclusions.size() + " transclusions");

        for (String title : transclusions) {
            String original = wiki.getPageText(title);
            String text = original;
            if (original.contains("{{Doc/Start{{L}}}}")) {
                text = text.replace(end, "{{Doc/End{{L}}$2}}");
            }
            // If the /en subpage doesn't exist, the page isn't translatable.
            if (!wiki.exists(title + "/en")) {
                checkUpdate(title, original, text);
                continue;
            }

            text = text.replaceAll(start, "{{Doc/Start{{L}}$2}}");

            checkUpdate(title, original, text);
        }
    }

    private void checkUpdate(String title, String original, String text) {
        // If the text was changed
        if (!original.equals(text)) {
            wiki.edit(title, text, "Updating {{Doc/Start}} to use {{L}} in translatable pages.");
            System.out.println("Updated " + title);
        }
    }

    @NotNull
    private String genRegex(String type) {
        return "\\{\\{Doc/" + type + "(\\{\\{L}})?(.*?)}}";
    }
}
