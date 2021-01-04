/*
 * Copyright (c) 2021 SizableShrimp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.sizableshrimp.jsb.api;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.rest.util.Image;
import discord4j.rest.util.Image.Format;
import me.sizableshrimp.jsb.Bot;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.Consumer;

public class DiscordConfiguration {
    private DiscordConfiguration() {}

    /**
     * Returns a {@link Mono} that signals completion when all shards have
     * disconnected.
     *
     * @param token    The token of the discord bot.
     * @param consumer A consumer to use the joined {@link GatewayDiscordClient}
     *                 containing all shards immediately after login.
     * @return A {@link Mono} that signals completion when all shards have
     *         disconnected.
     */
    public static Mono<Void> login(String token, Consumer<GatewayDiscordClient> consumer) {
        byte[] imageBytes = null;
        try {
            imageBytes = DiscordConfiguration.class.getResourceAsStream("/JSB.png").readAllBytes();
        } catch (IOException e) {
            Bot.LOGGER.error("Could not read avatar image file. Falling back to current avatar.", e);
        }
        Image image = imageBytes == null ? null : Image.ofRaw(imageBytes, Format.PNG);

        return DiscordClient.create(token).gateway()
                .setInitialStatus(
                        client -> Presence.online(Activity.watching(Bot.getConfig().getPrefix() + "help for help")))
                .withGateway(client -> {
                    if (image != null)
                        client.edit(u -> u.setAvatar(image));
                    consumer.accept(client);

                    return client.onDisconnect();
                });
    }
}
