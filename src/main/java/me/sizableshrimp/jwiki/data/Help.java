package me.sizableshrimp.jwiki.data;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import me.sizableshrimp.jwiki.commands.Command;

import java.util.List;

@Value
@AllArgsConstructor
public class Help {
    String name;
    @NonNull
    List<String> aliases;
    String desc;

    public Help(Command command, String desc) {
        this(command.getName(), command.getAliases(), desc);
    }
}
