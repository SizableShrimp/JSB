package me.sizableshrimp.jwiki.commands;

import me.sizableshrimp.jwiki.Main;
import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import org.fastily.jwiki.core.Wiki;

import java.util.List;

public class HelpCommand extends Command {
    public HelpCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public List<String> getAliases() {
        return List.of("?");
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Display this command");
    }

    @Override
    public void run(Args args) {
        for (Command command : Main.COMMAND_MANAGER.getCommands()) {
            Help help = command.getHelp();
            String aliases = String.join(", ", help.getAliases());

            System.out.printf("%s - %s", help.getName(), help.getDesc());
            System.out.println(aliases.isEmpty() ? "" : " - Aliases: " + aliases);
        }
    }
}
