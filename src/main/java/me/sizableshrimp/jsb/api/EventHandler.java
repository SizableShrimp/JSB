package me.sizableshrimp.jsb.api;

import discord4j.core.event.EventDispatcher;
import org.fastily.jwiki.core.Wiki;

import java.util.Set;

public class EventHandler {
    protected final Set<EventListener> listeners;

    public EventHandler(Wiki wiki) {
        this.listeners = new CommandLoader<>(EventListener.class).loadClasses(new Class[]{Wiki.class}, new Object[]{wiki});
    }

    public void register(EventDispatcher dispatcher) {
        listeners.forEach(listener -> listener.register(dispatcher));
    }
}
