package me.sizableshrimp.jsb.api;

import org.fastily.jwiki.core.Wiki;

public final class CommandContext {
    private final CommandManager commandManager;
    private final Wiki wiki;

    public CommandContext(CommandManager commandManager, Wiki wiki) {
        this.commandManager = commandManager;
        this.wiki = wiki;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public Wiki getWiki() {
        return wiki;
    }
}
