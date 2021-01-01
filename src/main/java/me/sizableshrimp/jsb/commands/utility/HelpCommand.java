package me.sizableshrimp.jsb.commands.utility;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.Command;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.args.ArgsProcessor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HelpCommand extends AbstractCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% [command]", "Use `%prefix%help [command]` to find out more information about each command.");
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public boolean isCommand(Message message) {
        return message.getUserMentionIds().contains(message.getClient().getSelfId());
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() != 1) {
            return displayHelp(context, event);
        }

        String inputCmd = args.getArg(0).toLowerCase();
        Command selected = context.getCommandManager().getCommandMap().get(inputCmd);
        if (selected == null) {
            return displayHelp(context, event);
        }
        return event.getMessage().getChannel().flatMap(channel -> sendEmbed(display(inputCmd, selected), channel));
    }

    public static Consumer<EmbedCreateSpec> display(MessageCreateEvent event, Command command) {
        return display(ArgsProcessor.processWithPrefix(event.getMessage().getContent()).getName(), command);
    }

    public static Consumer<EmbedCreateSpec> display(String inputCmd, Command command) {
        String commandName = command.getClass().getSimpleName().replace("Command", "");
        CommandInfo commandInfo = command.getInfo();

        String usage = Bot.getConfig().getPrefix() + commandInfo.getUsage(inputCmd);
        String description = commandInfo.getDescription(Bot.getConfig().getPrefix());
        String title = commandName + " Command";
        String aliases = String.join(", ", commandInfo.getAllNames());

        return embed -> {
            embed.setColor(Color.of(255, 175, 175));
            embed.setAuthor(title, null, null);
            embed.addField("Usage", "`" + usage + "`", false);
            embed.addField("Description", description, false);
            if (!commandInfo.getAliases().isEmpty()) {
                embed.addField("Aliases", aliases, false);
            }
        };
    }

    private Mono<Message> displayHelp(CommandContext context, MessageCreateEvent event) {
        List<String> names = context.getCommandManager().getCommands().stream()
                .map(Command::getName)
                .collect(Collectors.toList());

        Consumer<EmbedCreateSpec> spec = display("help", this)
                .andThen(embed -> embed.addField("Commands", String.join(", ", names), false));
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(spec, c));
    }
}
