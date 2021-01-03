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

package me.sizableshrimp.jsb.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class EightBallCommand extends AbstractCommand {
    private static final List<String> OUTCOMES = List.of("Signs point to yes.",
            "Yeeee.",
            "Reply hazy- I'm too faded for this shit. Try again.",
            "Without a doubt.",
            "My sources say no. <No sources cited>",
            "As I see it, yes.",
            "FUCK YOU BITCH!",
            "You may rely on it, dependent fuck.",
            "Concentrate and ask again.",
            "Outlook not so good. Sucks for you.",
            "It is decidedly so.",
            "Better not tell you now.",
            "Very doubtful.",
            "Yes, definitely.",
            "It is certain.",
            "Cannot predict now. Still too faded.",
            "Most likely.",
            "Ask again later, I'm too faded for this shit.",
            "My reply is no.",
            "Outlook good.",
            "Don't count on it. You should learn to count, though.",
            "Yes, in due time.",
            "Definitely not.",
            "You will have to wait. lol",
            "I have my doubts.",
            "Outlook so-so.",
            "Looks good to me!",
            "Who knows?",
            "Looking good!",
            "Probably.",
            "Are you fucking kidding me right now?",
            "Go for it.",
            "Don't bet on it; you have a gambling problem.",
            "Forget about it, cracker.");

    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname%", "Tells you your fortune!");
    }

    @Override
    public String getName() {
        return "8ball";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("eightball");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        return event.getMessage().getChannel().flatMap(channel -> sendMessage(OUTCOMES.get(ThreadLocalRandom.current().nextInt(OUTCOMES.size())), channel));
    }
}
