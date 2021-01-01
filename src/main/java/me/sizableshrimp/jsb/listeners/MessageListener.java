package me.sizableshrimp.jsb.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.rest.util.Permission;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.CommandManager;
import me.sizableshrimp.jsb.api.EventListener;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Mono;

public class MessageListener extends EventListener<MessageCreateEvent> {
    private final CommandManager commandManager;

    public MessageListener(Wiki wiki) {
        super(MessageCreateEvent.class);
        this.commandManager = new CommandManager(wiki);
    }

    @Override
    protected Mono<Void> execute(MessageCreateEvent event) {
        return commandManager.executeCommand(event).then();
    }

    @Override
    public void register(EventDispatcher dispatcher) {
        dispatcher.on(this.type)
                .filterWhen(e -> e.getMessage().getChannel().map(c -> c instanceof GuildMessageChannel))
                .filterWhen(e -> canSendMessages(e.getMessage()))
                .filter(e -> e.getMessage().getAuthor().map(u -> !u.isBot()).orElse(false))
                .flatMap(this::execute)
                .onErrorContinue((error, event) -> Bot.LOGGER.error("Event listener had an uncaught exception!", error))
                .subscribe();
    }

    private Mono<Boolean> canSendMessages(Message message) {
        Snowflake id = message.getClient().getSelfId();
        return message.getChannel()
                .cast(GuildMessageChannel.class)
                .flatMap(c -> c.getEffectivePermissions(id))
                .map(set -> set.asEnumSet().contains(Permission.SEND_MESSAGES));
    }
}
