package me.sizableshrimp.jwiki.commands;

import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import org.fastily.jwiki.core.Wiki;

public class ExitCommand extends Command {
    public ExitCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Exit the program");
    }

    @Override
    public void run(Args args) {
        System.out.println("Exiting program...");
        System.exit(0);
    }
}
