package me.sizableshrimp.jsb.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.Mod;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RemoveModCommand extends AbstractCommand {
    public static final Map<Snowflake, Confirmation> awaitingConfirmation = new HashMap<>();
    public static final String REACT_WITH = "Please react with ❌ to delete or \uD83D\uDDD1 to cancel.";

    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <mod name|mod abbreviation>",
                """
                        Removes a mod from the mod list which takes either the mod abbreviation or unlocalized mod name.
                        """);
    }

    @Override
    public String getName() {
        return "removemod";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("delmod", "delabbrv", "removeabbrv");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 1) {
            return incorrectUsage(event);
        }

        return requireRole(event, "Editor")
                .flatMap(e -> e.getMessage().getChannel())
                .flatMap(channel -> {
                    Mod toDelete = Mod.getMod(context.getWiki(), args.getArgRange(0));
                    if (toDelete == null) {
                        return sendMessage("That mod doesn't exist!", channel);
                    }

                    Mono<Message> confirm;
                    if (toDelete.hasDistinctLink()) {
                        confirm = sendMessage(String.format("Do you want to delete the mod named **%s** with abbreviation `%s` and link `%s` from the list? " + REACT_WITH,
                                toDelete.getName(), toDelete.getAbbrv(), toDelete.getUrlLink()), channel);
                    } else {
                        confirm = sendMessage(String.format("Do you want to delete the mod named **%s** with abbreviation `%s` from the list? " + REACT_WITH,
                                toDelete.getName(), toDelete.getAbbrv()), channel);
                    }
                    return confirm.doOnNext(m -> awaitingConfirmation.put(m.getId(), new Confirmation(event.getMessage().getAuthor().get().getId(), m.getId(), toDelete)))
                            .flatMap(m -> m.addReaction(ReactionEmoji.unicode("❌")).thenReturn(m))
                            .flatMap(m -> m.addReaction(ReactionEmoji.unicode("\uD83D\uDDD1")).thenReturn(m));
                }).switchIfEmpty(event.getMessage().getChannel().flatMap(channel -> sendMessage("You must be an editor to execute this command!", channel)));
    }

    public static final class Confirmation {
        public final Snowflake author;
        public final Snowflake message;
        public final Mod toDelete;

        public Confirmation(Snowflake author, Snowflake message, Mod toDelete) {
            this.author = author;
            this.message = message;
            this.toDelete = toDelete;
        }
    }
}
