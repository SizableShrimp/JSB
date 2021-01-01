package me.sizableshrimp.jsb;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new Gson();
    private String useragent;
    private String username;
    private String password;
    private boolean login;
    private String baseapi;
    private String botToken;
    private String prefix;
    private long ownerId;

    private Config() {}

    public static Config getConfig(Path path) throws IOException {
        return GSON.fromJson(Files.newBufferedReader(path), Config.class);
    }

    public String getUserAgent() {
        return this.useragent;
    }

    public String getApi() {
        return this.baseapi;
    }

    public boolean doLogin() {
        return this.login;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getBotToken() {
        return this.botToken;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public long getOwnerId() {
        return ownerId;
    }
}
