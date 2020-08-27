package me.sizableshrimp.jwiki.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.GSONP;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An immutable object that stores wiki data about Mods from
 * <a href="https://ftb.gamepedia.com/Module:Mods/list">Module:Mods/list</a>
 * including mod abbreviation, unlocalized mod name in English, and link text.
 * <i>Note:</i> Each instance is tied to a specific {@link Wiki} object.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Mod {
    @ToString.Exclude
    Wiki wiki;
    String abbrv;
    String name;
    String link;

    public String getLocalized(String langCode) {
        langCode = langCode == null ? "en" : langCode;
        return GSONP.getStr(Scribunto.runScribuntoCode(wiki, "Mods/list/" + langCode, "p.byAbbrv['" + abbrv + "']").getReturnJson().getAsJsonObject(), "localized");
    }

    private static Map<String, JsonElement> data = null;

    /**
     * Get a {@link Mod} by its unlocalized mod name or abbreviation, e.g. "Crop Dusting" or "CD".
     * This method also converts all names to lowercase since no names should conflict anyways.
     * This method will return null if the JSON from Scribunto Console API is null.
     *
     * @param wiki A {@link Wiki} object to use for requesting the data.
     * @param modInfo The unlocalized mod name or abbreviation, case insensitive.
     * @return A {@link Mod} object.
     */
    public static Mod getMod(Wiki wiki, String modInfo) {
        if (data == null)
            loadMods(wiki);

        JsonObject json = data.get(modInfo.toLowerCase()).getAsJsonObject(); // Case insensitive
        if (json == null)
            return null;

        String abbrv = GSONP.getStr(json, "abbr");
        String name = GSONP.getStr(json, "name");
        String link = GSONP.getStr(json, "link");
        return new Mod(wiki, abbrv, name, link);
    }

    public static void loadMods(Wiki wiki) {
        data = new HashMap<>();
        data.putAll(getTableAsMap(wiki, "byAbbrv"));
        data.putAll(getTableAsMap(wiki, "byName"));
    }

    private static Map<String, JsonElement> getTableAsMap(Wiki wiki, String table) {
        JsonElement json = Scribunto.runScribuntoCode(wiki, "Mods/list", "p." + table).getReturnJson();
        if (json == null)
            return Map.of();
        return json.getAsJsonObject().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue));
    }
}
