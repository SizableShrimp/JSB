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
import me.sizableshrimp.jsb.Bot;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class DiscordConfiguration {
    private static final Reflections REFLECTIONS = new Reflections("images", new ResourcesScanner());

    private DiscordConfiguration() {}

    /**
     * Returns a {@link Mono} that signals completion when all shards have
     * disconnected.
     *
     * @param prefix The prefix to use when setting the presence.
     * @param token The token of the discord bot.
     * @param consumer A consumer to use the joined {@link GatewayDiscordClient}
     * containing all shards immediately after login.
     * @return A {@link Mono} that signals completion when all shards have
     * disconnected.
     */
    public static Mono<Void> login(String prefix, String token, Consumer<GatewayDiscordClient> consumer) {
        return login(prefix, token, false, consumer);
    }

    /**
     * Returns a {@link Mono} that signals completion when all shards have
     * disconnected.
     *
     * @param prefix The prefix to use when setting the presence.
     * @param token The token of the discord bot.
     * @param isDebugMode If true, will not update the avatar as during debugging this may be done too fast and cause errors.
     * @param consumer A consumer to use the joined {@link GatewayDiscordClient}
     * containing all shards immediately after login.
     * @return A {@link Mono} that signals completion when all shards have
     * disconnected.
     */
    public static Mono<Void> login(String prefix, String token, boolean isDebugMode, Consumer<GatewayDiscordClient> consumer) {
        Image image = getImage();

        return DiscordClient.create(token).gateway()
                .setInitialStatus(
                        client -> Presence.online(Activity.watching(prefix + "help for help")))
                .withGateway(client ->
                        Mono.just(client)
                                .flatMap(c -> image == null || isDebugMode ? Mono.just(c) : c.edit(u -> u.setAvatar(image)).thenReturn(c))
                                .doOnNext(consumer)
                                .flatMap(GatewayDiscordClient::onDisconnect));
    }

    private static Image getImage() {
        try {
            List<String> images = new ArrayList<>(REFLECTIONS.getResources(Pattern.compile(".*\\.png")));

            return Image.ofRaw(DiscordConfiguration.class.getResourceAsStream('/' + images.get(ThreadLocalRandom.current().nextInt(images.size()))).readAllBytes(), Image.Format.PNG);
        } catch (Exception e) {
            Bot.LOGGER.error("Could not read avatar image file. Falling back to current avatar.", e);
            return null;
        }
    }
}
