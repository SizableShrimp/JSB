package me.sizableshrimp.jwiki.commands;

import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import me.sizableshrimp.jwiki.data.Language;
import org.fastily.jwiki.core.Wiki;

import java.util.ArrayList;

public class DisListCommand extends Command {
    public DisListCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "dislist";
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Generate a list of disambiguations and their updated link, stored in dislist.txt");
    }

    @Override
    public void run(Args args) {
        ArrayList<String> oneParen = wiki.search("intitle:/\\(.+\\)/", 5);
        ArrayList<String> twoParen = wiki.search("intitle:/\\(.+\\) \\(.+\\)/", 5);
        System.out.println(oneParen);
        System.out.println(twoParen);
    }

    //private void filter(ArrayList<String> list) {
    //    list.removeIf(s -> s.startsWith("Translations:"));
    //    //list.removeIf(s -> )
    //}
    //
    //private boolean languageCode(String title) {
    //    if (!subpage(title))
    //        return false;
    //    String code = title.substring(title.lastIndexOf('/') + 1);
    //    Language lang = Language.getByCode(wiki, code);
    //    return lang != null;
    //}
    //
    //private boolean subpage(String title) {
    //    return title.lastIndexOf('/') != -1;
    //}
}
