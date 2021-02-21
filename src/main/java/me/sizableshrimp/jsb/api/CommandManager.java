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

package me.sizableshrimp.jsb.api;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.args.ArgsProcessor;
import me.sizableshrimp.jsb.util.MessageUtil;
import org.fastily.jwiki.core.Wiki;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandManager {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);
    protected final GatewayDiscordClient client;
    protected final Pattern mentionPrefix;
    protected final Wiki wiki;
    protected final Map<String, Command> commandMap = new HashMap<>();
    protected Set<Command> commands;

    public CommandManager(GatewayDiscordClient client, Wiki wiki) {
        this.client = client;
        this.mentionPrefix = Pattern.compile("^<@!?" + client.getSelfId().asLong() + ">\\s*");
        this.wiki = wiki;
        loadCommands();
    }

    public Mono<Void> executeCommand(MessageCreateEvent event) {
        Args args = ArgsProcessor.processWithPrefix(event.getMessage().getContent());
        if (args == null) {
            args = ArgsProcessor.processWithPrefixRegex(this.mentionPrefix, event.getMessage().getContent());
        }
        if (args == null) {
            return Mono.empty();
        }
        Args finalArgs = args;

        Command command = this.commandMap.get(args.getName());

        if (command == null) {
            return Mono.empty();
        }

        return event.getMessage().getChannel().flatMap(MessageChannel::type)
                .then(requireRoles(event, command.getRequiredRoles()))
                .flatMap(b -> command.run(new CommandContext(this, this.wiki), event, finalArgs)).then()
                .onErrorResume(NoPermissionException.class, noperms -> event.getMessage().getChannel()
                        .flatMap(channel -> MessageUtil.sendMessage(noperms.getMessage(), channel)).then());
    }

    public void loadCommands() {
        this.commands = CommandLoader.loadClasses(Command.class, null, null);

        this.commandMap.clear();
        for (Command command : this.commands) {
            for (String alias : command.getAliases()) {
                addCommand(alias, command);
            }
            addCommand(command.getName(), command);
        }
    }

    private void addCommand(String name, Command command) {
        Command prev = this.commandMap.put(name, command);
        if (prev != null) {
            LOGGER.error("Encountered a duplicate alias \"{}\" for {} and {}", name, command.getClass(), prev.getClass());
            this.commandMap.remove(name);
        }
    }

    public Map<String, Command> getCommandMap() {
        return this.commandMap;
    }

    public Set<Command> getCommands() {
        return this.commands;
    }

    private static Mono<Boolean> requireRoles(MessageCreateEvent event, List<String> requiredRoles) {
        if (requiredRoles.isEmpty())
            return Mono.just(true);

        return Mono.justOrEmpty(event.getMember())
                .flatMap(m -> m.getRoles().map(Role::getName).collect(Collectors.toSet())
                        .map(roles -> roles.containsAll(requiredRoles)))
                .filter(b -> b).switchIfEmpty(Mono.error(() -> new NoPermissionException(requiredRoles)));
    }
}
