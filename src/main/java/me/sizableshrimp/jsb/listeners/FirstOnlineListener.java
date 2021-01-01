package me.sizableshrimp.jsb.listeners;

import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.api.EventListener;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Mono;

public class FirstOnlineListener extends EventListener<ReadyEvent> {
    public FirstOnlineListener(Wiki wiki) {
        super(ReadyEvent.class);
    }

    @Override
    protected Mono<Void> execute(ReadyEvent event) {
        return Mono.empty();
    }

    @Override
    public void register(EventDispatcher dispatcher) {
        dispatcher.on(type)
                .next()
                // Sets firstOnline to the time when the first ready event is received
                .doOnNext(ignored -> Bot.setFirstOnline(System.currentTimeMillis()))
                .subscribe();
    }
}
