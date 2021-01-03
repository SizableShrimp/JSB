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
