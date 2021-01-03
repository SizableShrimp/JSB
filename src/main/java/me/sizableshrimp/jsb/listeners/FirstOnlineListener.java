package me.sizableshrimp.jsb.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.EventListener;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FirstOnlineListener extends EventListener<ReadyEvent> {
    public FirstOnlineListener(GatewayDiscordClient client, Wiki wiki) {
        super(ReadyEvent.class, client, wiki);
    }

    @Override
    protected Mono<ReadyEvent> execute(Flux<ReadyEvent> onEvent) {
        return onEvent.next()
                // Sets firstOnline to the time when the first ready event is received
                .doOnNext(ignored -> Bot.setFirstOnline(System.currentTimeMillis()));
    }
}
