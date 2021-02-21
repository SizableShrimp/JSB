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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.sizableshrimp.jsb.api.DiscordConfiguration;
import me.sizableshrimp.jsb.api.EventHandler;
import me.sizableshrimp.jsb.data.Config;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.Wiki;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Path;

@SpringBootApplication
public class Bot {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
    public static final boolean IN_DEBUG_MODE = System.getenv("DEBUG") != null;
    private static Config config;
    private static long firstOnline;

    // Examples - https://github.com/fastily/jwiki/wiki/Examples
    public static void main(String[] args) {
        SpringApplication.run(Bot.class, args);
        // Loads the wiki instance only if the config instance was loaded
        if (!loadConfig())
            return;
        Wiki wiki = loadWiki();
        if (wiki == null)
            return;

        DiscordConfiguration.login(config.getPrefix(), config.getBotToken(), IN_DEBUG_MODE, client -> {
            EventHandler handler = new EventHandler(client, wiki);
            handler.register();
        }).block();
    }

    private static Wiki loadWiki() {
        Wiki wiki = setupWiki();
        if (wiki == null) {
            LOGGER.error("Failed to load wiki instance");
            return null;
        }
        if (LOGGER.isInfoEnabled() && wiki.whoami() != null)
            LOGGER.info("Logged into MediaWiki as user \"{}\"", wiki.whoami());
        return wiki;
    }

    private static boolean loadConfig() {
        try {
            boolean heroku = System.getenv("HEROKU") != null;
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
     * Setup the wiki instance using a {@link Config} instance.
     *
     * @return The created {@link Wiki} instance, or null if an error occurred.
     */
    private static Wiki setupWiki() {
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
                .withPrefixLogging(false)
                .build();
    }

    public static Config getConfig() {
        return config;
    }

    public static long getFirstOnline() {
        return firstOnline;
    }

    public static void setFirstOnline(long millis) {
        if (firstOnline != 0)
            throw new IllegalStateException("firstOnline already set");

        firstOnline = millis;
    }
}
