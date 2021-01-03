package me.sizableshrimp.jsb.api;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.Event;
import org.fastily.jwiki.core.Wiki;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class EventListener<T extends Event> {
    protected final Class<T> eventType;
    protected final GatewayDiscordClient client;
    protected final Wiki wiki;

    protected EventListener(Class<T> eventType, GatewayDiscordClient client, Wiki wiki) {
        this.eventType = eventType;
        this.client = client;
        this.wiki = wiki;
    }

    protected abstract Mono<?> execute(Flux<T> onEvent);

    public final void register(EventDispatcher dispatcher) {
        execute(dispatcher.on(eventType)).subscribe();
    }
}
