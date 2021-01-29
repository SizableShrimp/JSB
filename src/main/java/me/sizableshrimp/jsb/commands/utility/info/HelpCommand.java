/*
 * Copyright (c) 2021 SizableShrimp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.sizableshrimp.jsb.commands.utility.info;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.commands.AbstractCommand;
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

    // @Override
    // public boolean isCommand(Message message) {
    //     return message.getUserMentionIds().contains(message.getClient().getSelfId());
    // }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args == null || args.getLength() != 1) {
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
        String commandName = command.getClass().getSimpleName()
                .replace("Command", "")
                .replaceAll("([^A-Z])([A-Z][^A-Z])", "$1 $2")
                .trim();
        CommandInfo commandInfo = command.getInfo();

        String usage = Bot.getConfig().getPrefix() + commandInfo.getUsage(inputCmd);
        String description = commandInfo.getDescription(Bot.getConfig().getPrefix());
        String title = commandName + " Command";

        return embed -> {
            embed.setColor(Color.of(255, 175, 175));
            embed.setAuthor(title, null, null);
            embed.addField("Usage", "`" + usage + "`", false);
            embed.addField("Description", description, false);
            if (!commandInfo.getAliases().isEmpty()) {
                embed.addField("Aliases", String.join(", ", commandInfo.getAllNames()), false);
            }
            if (!commandInfo.getRequiredRoles().isEmpty()) {
                embed.addField("Required Roles", String.join(", ", commandInfo.getRequiredRoles()), false);
            }
        };
    }

    private Mono<Message> displayHelp(CommandContext context, MessageCreateEvent event) {
        List<String> names = context.getCommandManager().getCommands().stream()
                .map(Command::getName)
                .sorted()
                .collect(Collectors.toList());

        Consumer<EmbedCreateSpec> spec = display("help", this)
                .andThen(embed -> embed.addField("Commands", String.join(", ", names), false));
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(spec, c));
    }
}
