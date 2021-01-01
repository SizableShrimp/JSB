package me.sizableshrimp.jsb.api;

import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.Event;
import reactor.core.publisher.Mono;

public abstract class EventListener<T extends Event> {
    protected Class<T> type;

    protected EventListener(Class<T> type) {
        this.type = type;
    }

    protected abstract Mono<Void> execute(T event);

    public void register(EventDispatcher dispatcher) {
        dispatcher.on(type)
                .flatMap(this::execute)
                .subscribe();
    }
}
