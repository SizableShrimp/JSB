package me.sizableshrimp.jsb.api;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import org.fastily.jwiki.core.Wiki;

import java.util.Set;

public class EventHandler {
    protected final GatewayDiscordClient client;
    protected final Set<EventListener> listeners;

    public EventHandler(GatewayDiscordClient client, Wiki wiki) {
        this.client = client;
        this.listeners = CommandLoader.loadClasses(EventListener.class, new Class[]{GatewayDiscordClient.class, Wiki.class}, new Object[]{client, wiki});
    }

    public void register() {
        listeners.forEach(listener -> listener.register(client.getEventDispatcher()));
    }
}
