package me.sizableshrimp.jsb.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Permission;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.EventListener;
import me.sizableshrimp.jsb.commands.WikilinkCommand;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikilinkListener extends EventListener<MessageCreateEvent> {
    private static final String LEGAL_TITLE_CHARS = "[ %!\"$&'()*,\\-./0-9:;=?@A-Z\\\\^_`a-z~\\x80-\\xFF+]";
    private static final Pattern WIKILINK = Pattern.compile("\\[\\[(" + LEGAL_TITLE_CHARS + "+)(?:\\|" + LEGAL_TITLE_CHARS + "+)?]]");

    public WikilinkListener(GatewayDiscordClient client, Wiki wiki) {
        super(MessageCreateEvent.class, client, wiki);
    }

    private Mono<Message> genMessage(MessageCreateEvent event) {
        return Mono.just(event.getMessage().getContent())
                .map(WIKILINK::matcher)
                .filter(Matcher::find)
                .flatMap(m -> event.getMessage().getChannel().flatMap(MessageChannel::type).thenReturn(m))
                .flatMapMany(matcher -> {
                    Set<MatchResult> matches = new HashSet<>();
                    do {
                        matches.add(matcher.toMatchResult());
                    } while (matcher.find());
                    return Flux.fromIterable(matches);
                }).reduce(new StringBuilder(), (builder, m) -> {
                    String link = m.group(1);
                    // String display = m.group(2);

                    return WikilinkCommand.genWikilink(builder, wiki, link);
                }).zipWith(event.getMessage().getChannel())
                .flatMap(tuple -> {
                    String message = tuple.getT1().toString();
                    return message.isBlank() ? Mono.empty() : tuple.getT2().createMessage(message);
                });
    }

    @Override
    protected Mono<Void> execute(Flux<MessageCreateEvent> onEvent) {
        return onEvent
                .filterWhen(e -> e.getMessage().getChannel().map(c -> c instanceof GuildMessageChannel))
                .filterWhen(e -> canSendMessages(e.getMessage()))
                .filter(e -> e.getMessage().getAuthor().map(u -> !u.isBot()).orElse(false))
                .filter(e -> !e.getMessage().getContent().isEmpty())
                .flatMap(this::genMessage)
                .onErrorContinue((error, event) -> Bot.LOGGER.error("Wikilink listener had an uncaught exception!", error))
                .then();
    }

    private Mono<Boolean> canSendMessages(Message message) {
        Snowflake id = message.getClient().getSelfId();
        return message.getChannel()
                .cast(GuildMessageChannel.class)
                .flatMap(c -> c.getEffectivePermissions(id))
                .map(set -> set.asEnumSet().contains(Permission.SEND_MESSAGES));
    }
}
