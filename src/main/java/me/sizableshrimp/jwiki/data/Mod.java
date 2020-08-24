package me.sizableshrimp.jwiki.data;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.fastily.jwiki.core.Wiki;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * An immutable object that stores wiki data about Mods from
 * <a href="https://ftb.gamepedia.com/Module:Mods/list">Module:Mods/list</a>
 * including mod abbreviation, mod name in English, localized name, and link.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Mod {
    String abbrv;
    String name;
    String localized;
    String link;

    /**
     * Get a {@link Mod} by its mod abbreviation.
     *
     * @param wiki A {@link Wiki} object to use for requesting the data.
     * @param abbrv The mod abbreviation in all caps (ex. "GT6", "BTC", "IB"). Calls {@link String#toUpperCase()}.
     * @return A {@link Mod} object.
     */
    public static Mod getByAbbrv(Wiki wiki, String abbrv) {
        return getMod(wiki, "byAbbrv", abbrv.toUpperCase());
    }

    /**
     * Get a {@link Mod} by its mod name.
     *
     * @param wiki A {@link Wiki} object to use for requesting the data.
     * @param name The mod name, case sensitive.
     * @return A {@link Mod} object.
     */
    public static Mod getByName(Wiki wiki, String name) {
        return getMod(wiki, "byName", name);
    }

    private static Mod getMod(Wiki wiki, String function, String... args) {
        Map<String, String> data = Data.getMapFromModule(wiki, "Mods/list", function, args);
        if (data.isEmpty())
            return null;
        String abbrv = data.get("abbr");
        String name = data.get("name");
        String localized = data.get("localized"); //TODO fix UK spelling
        String link = data.get("link");
        return new Mod(abbrv, name, localized, link);
    }
}
