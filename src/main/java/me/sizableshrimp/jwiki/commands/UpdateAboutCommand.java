package me.sizableshrimp.jwiki.commands;

import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import org.fastily.jwiki.core.Wiki;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@DisabledCommand
public class UpdateAboutCommand extends Command {
    public UpdateAboutCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "updateabout";
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Update all pages that transclude {{About}}");
    }

    @Override
    public void run(Args args) {
        ArrayList<String> titles = wiki.whatTranscludesHere("Template:About");
        for (String title : titles) {
            String pageText = wiki.getPageText(title);
            int firstIndex = pageText.indexOf('\n');
            if (firstIndex == -1)
                firstIndex = pageText.length();
            String firstLine = pageText.substring(0, firstIndex);

            String updated = firstLine.replaceFirst("Vanilla", "vanilla Minecraft") + pageText.substring(firstIndex);
            if (!pageText.equals(updated)) {
                if (!firstLine.toLowerCase().contains("about")) {
                    String resolve = URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8);
                    System.err.println("{{About}} is not in the first line, see " + wiki.getConfig().getBaseURL().resolve("/" + resolve));
                    continue;
                }
                boolean result = wiki.edit(title, updated, "Updated reference to vanilla Minecraft");
                if (result) {
                    System.out.println("Updated " + title + ".");
                } else {
                    System.err.println("An error occurred.");
                }
            }
        }
    }
}
