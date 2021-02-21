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

import discord4j.common.GitProperties;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.commands.AbstractCommand;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InfoCommand extends AbstractCommand {
    @Override
    public CommandInfo getInfo(CommandContext context) {
        return new CommandInfo(this, "%cmdname%", "Displays information about the bot including uptime, author, and how the bot was made.");
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        return event.getClient().getUserById(Snowflake.of(Bot.getConfig().getOwnerId()))
                .map(User::getMention)
                .map(InfoCommand::getEmbed)
                .zipWith(event.getMessage().getChannel())
                .flatMap(tuple -> sendMessage("To learn my commands, use `" + Bot.getConfig().getPrefix() + "help`", tuple.getT1(), tuple.getT2()));
    }

    private static Consumer<EmbedCreateSpec> getEmbed(String owner) {
        String description = """
                **Java Shrimp Bot** (or JSB) was built with [Discord4J](https://github.com/Discord4J/Discord4J) and programmed in Java with love.
                """;

        return embed -> {
            embed.addField("Author", owner, true);
            embed.setAuthor("Information", null, null);
            embed.setDescription(description);
            embed.addField("Discord4J Version", GitProperties.getProperties().getProperty(GitProperties.APPLICATION_VERSION), true);
            embed.addField("Source", "[https://github.com/SizableShrimp/JSB](SizableShrimp/JSB on Github)", false);
            embed.addField("Prefix", '`' + Bot.getConfig().getPrefix() + '`', false);
            embed.addField("Uptime", getUptime(), false);
        };
    }

    private static String getUptime() {
        Duration duration = Duration.ofMillis(System.currentTimeMillis() - Bot.getFirstOnline());
        List<String> list = new ArrayList<>();

        if (duration.toDays() > 0) {
            list.add(duration.toDays() == 1 ? duration.toDays() + " day" : duration.toDays() + " days");
        }
        duration = duration.minusDays(duration.toDays());

        if (duration.toHours() > 0) {
            list.add(duration.toHours() == 1 ? duration.toHours() + " hour" : duration.toHours() + " hours");
        }
        duration = duration.minusHours(duration.toHours());

        if (duration.toMinutes() > 0) {
            list.add(duration.toMinutes() == 1 ? duration.toMinutes() + " minute" : duration.toMinutes() + " minutes");
        }
        duration = duration.minusMinutes(duration.toMinutes());

        if (duration.getSeconds() > 0) {
            list.add(duration.getSeconds() == 1 ? duration.getSeconds() + " second" : duration.getSeconds() + " seconds");
        }

        if (list.isEmpty()) {
            return "Less than a second";
        }

        return String.join(", ", list);
    }
}
