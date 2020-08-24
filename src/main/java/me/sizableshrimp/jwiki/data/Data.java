package me.sizableshrimp.jwiki.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.sizableshrimp.jwiki.WikiUtil;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.GSONP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Data {
    private Data() {}

    public static String getWikitextFromModule(Wiki wiki, String module, String function, String... args) {
        JsonObject json = WikiUtil.useModule(wiki, module, function, args);
        if (json == null)
            return null;
        String result = GSONP.getStr(json, "wikitext");
        //System.out.println(json);
        if (result != null && result.trim().isEmpty())
            return null;
        return result;
    }

    public static JsonArray getArrayFromModule(Wiki wiki, String module, String function, String... args) {
        String result = getWikitextFromModule(wiki, module, function, args);
        if (result == null)
            return new JsonArray();
        JsonElement json = JsonParser.parseString(result);
        return json.isJsonArray() ? json.getAsJsonArray() : new JsonArray();
    }

    public static JsonObject getObjectFromModule(Wiki wiki, String module, String function, String... args) {
        String result = getWikitextFromModule(wiki, module, function, args);
        if (result == null)
            return new JsonObject();
        JsonElement json = JsonParser.parseString(result);
        return json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
    }

    public static List<String> getListFromModule(Wiki wiki, String module, String function, String... args) {
        return GSONP.jaOfStrToAL(getArrayFromModule(wiki, module, function, args));
    }

    public static Map<String, String> getMapFromModule(Wiki wiki, String module, String function, String... args) {
        Map<String, String> map = new HashMap<>();
        JsonObject json = getObjectFromModule(wiki, module, function, args);

        for (var entry : json.entrySet()) {
            if (entry.getValue().isJsonPrimitive())
                map.put(entry.getKey(), entry.getValue().getAsString());
        }

        return map;
    }
}
