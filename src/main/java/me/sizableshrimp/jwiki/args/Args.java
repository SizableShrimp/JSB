package me.sizableshrimp.jwiki.args;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

@Value
public class Args {
    String name;
    String[] args;

    public String getArg(int index) {
        if (index < 0 || index >= getArgsLength())
            return null;
        return args[index];
    }

    public int getArgsLength() {
        return args.length;
    }
}
