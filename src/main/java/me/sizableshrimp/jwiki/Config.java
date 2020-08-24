package me.sizableshrimp.jwiki;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new Gson();
    @Expose
    private String useragent;
    @Expose
    private String baseapi;
    @Expose
    @Getter
    private String username;
    @Expose
    @Getter
    private String password;

    private Config() {}

    public static Config getConfig(Path path) throws IOException {
        return GSON.fromJson(Files.newBufferedReader(path), Config.class);
    }

    public String getUserAgent() {
        return useragent;
    }

    public String getApi() {
        return baseapi;
    }
}
