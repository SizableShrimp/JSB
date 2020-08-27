package me.sizableshrimp.jwiki.commands;

import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;

import java.util.ArrayList;

public class TilesheetMoveCommand extends Command {
    public TilesheetMoveCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "tsmove";
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Move tilesheets to support z axis that have not yet been moved");
    }

    @Override
    public void run(Args args) {
        //ArrayList<String> redirects = wiki.allPages("Tilesheet", true, false, -1, NS.FILE);
        //wiki.prefixIndex(NS.FILE, "Tilesheet").stream()
        //        .filter(title -> !title.endsWith(" 0.png"))
        //        .filter(title -> !redirects.contains(title))
        //        .filter(title -> title.matches("File:Tilesheet [\\w\\d-]+ \\d+\\.png"))
        //        .forEach(title -> wiki.basicPOST("move", FL.pMap("")));
        //TODO fix, add arguments
    }
}
