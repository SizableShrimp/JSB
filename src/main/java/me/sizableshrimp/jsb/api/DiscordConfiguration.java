package me.sizableshrimp.jsb.api;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import me.sizableshrimp.jsb.Bot;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class DiscordConfiguration {
    private DiscordConfiguration() {}

    /**
     * Returns a {@link Mono} that signals completion when all shards have disconnected.
     *
     * @param token The token of the discord bot.
     * @param consumer A consumer to use the joined {@link GatewayDiscordClient} containing all shards immediately after login.
     * @return A {@link Mono} that signals completion when all shards have disconnected.
     */
    public static Mono<Void> login(String token, Consumer<GatewayDiscordClient> consumer) {
        return DiscordClient.create(token)
                .gateway()
                .setInitialStatus(client -> Presence.online(Activity.watching(Bot.getConfig().getPrefix() + "help for help")))
                .withGateway(client -> {
                    consumer.accept(client);

                    return client.onDisconnect();
                });
    }
}

