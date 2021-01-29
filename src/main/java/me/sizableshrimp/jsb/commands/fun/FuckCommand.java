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

package me.sizableshrimp.jsb.commands.fun;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.commands.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import reactor.core.publisher.Mono;

public class FuckCommand extends AbstractCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname%", "Use it and find out");
    }

    @Override
    public String getName() {
        return "fuck";
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        return event.getMessage().getChannel().flatMap(channel -> sendMessage("You", channel));
    }
}
