package me.sizableshrimp.jwiki.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.sizableshrimp.jwiki.args.Args;
import org.fastily.jwiki.core.Wiki;

@AllArgsConstructor
public abstract class Command {
    @Getter
    @Setter
    protected Wiki wiki;

    public abstract String getName();

    public abstract String getHelp();

    public abstract void run(Args args);
}
