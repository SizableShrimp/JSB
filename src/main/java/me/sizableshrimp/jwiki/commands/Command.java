package me.sizableshrimp.jwiki.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import org.fastily.jwiki.core.Wiki;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
public abstract class Command {
    @Getter
    @Setter
    protected Wiki wiki;

    public abstract String getName();

    public List<String> getAliases() {
        return List.of();
    }

    public abstract Help getHelp();

    public abstract void run(Args args) throws Exception;
}
