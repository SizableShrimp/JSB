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

package me.sizableshrimp.jsb;

import me.sizableshrimp.jsb.api.DiscordConfiguration;
import me.sizableshrimp.jsb.api.EventHandler;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.Wiki;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class Bot {
    public static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
    private static Config config;
    private static Wiki wiki;
    private static long firstOnline;
    // private static final WQuery.QTemplate SITEINFO = new WQuery.QTemplate(FL.pMap("action", "query", "meta", "siteinfo", "siprop", "general"), "query");

    // Examples - https://github.com/fastily/jwiki/wiki/Examples
    public static void main(String[] args) {
        SpringApplication.run(Bot.class, args);
        boolean heroku = System.getenv("HEROKU") != null;
        // Loads the wiki instance only if the config instance was loaded
        if (!loadConfig(heroku) || !loadWiki())
            return;

        if (heroku) {
            HttpClient client = HttpClient.newHttpClient();
            URI uri = URI.create(System.getenv("URL"));
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                try {
                    client.send(HttpRequest.newBuilder(uri).build(), info -> HttpResponse.BodySubscribers.discarding());
                    // HttpsURLConnection connection = (HttpsURLConnection) new URL(System.getenv("URL")).openConnection();
                    // connection.setRequestMethod("GET");
                    // connection.connect();
                    // connection.getResponseCode();
                    // connection.disconnect();
                } catch (IOException | InterruptedException e) {
                    LOGGER.error("Error while pinging self website.", e);
                }
            }, 0, 10, TimeUnit.MINUTES);
        }
        DiscordConfiguration.login(config.getBotToken(), client -> {
            EventHandler handler = new EventHandler(client, wiki);
            handler.register();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> client.logout().timeout(Duration.ofSeconds(15)).block()));
        }).subscribe();
    }

    private static boolean loadWiki() {
        wiki = setupWiki(false);
        if (wiki == null) {
            LOGGER.error("Failed to login to MediaWiki");
            return false;
        }
        LOGGER.info("Logged into MediaWiki as user \"{}\"", wiki.whoami());
        return true;
    }

    private static boolean loadConfig(boolean heroku) {
        try {
            if (heroku) {
                config = Config.loadHeroku();
                LOGGER.info("Loaded config from Heroku environment variables.");
            } else {
                String path = System.getenv("CONFIG");
                config = Config.getConfig(Path.of(path));
                LOGGER.info("Loaded config from path \"{}\"", path);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Couldn't load the config.", e);
            return false;
        }
    }

    /**
     * Setup the wiki instance using a {@link Config} instance and whether to enable jwiki's logger.
     * Note that this logger can be helpful for debugging, but does not integrate with ANY logging APIs.
     * Returns null if the config isn't loaded or the api endpoint is invalid.
     *
     * @param defaultLogger Whether to enable the default logger used by jwiki.
     * @return The created {@link Wiki} instance, or null.
     */
    private static Wiki setupWiki(boolean defaultLogger) {
        if (config == null || config.getApi() == null) {
            LOGGER.error("API endpoint is null");
            return null;
        }

        HttpUrl apiEndpoint = HttpUrl.parse(config.getApi());
        if (apiEndpoint == null) {
            LOGGER.error("Invalid API endpoint: {}", config.getApi());
            return null;
        }

        Wiki.Builder builder = new Wiki.Builder();
        if (config.doLogin())
            builder.withLogin(config.getUsername(), config.getPassword());
        return builder
                .withApiEndpoint(apiEndpoint)
                .withUserAgent(config.getUserAgent())
                .withDefaultLogger(defaultLogger)
                .build();
    }

    public static Config getConfig() {
        return config;
    }

    public static long getFirstOnline() {
        return firstOnline;
    }

    public static void setFirstOnline(long millis) {
        Bot.firstOnline = millis;
    }
}
