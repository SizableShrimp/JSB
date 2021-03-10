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

package me.sizableshrimp.jsb.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.sizableshrimp.jsb.Bot;
import me.sizableshrimp.jsb.util.CachedMap;
import me.sizableshrimp.jsb.util.WikiUtil;
import org.fastily.jwiki.core.AReply;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.GSONP;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * An immutable object that stores wiki data about Mods from
 * <a href="https://ftb.gamepedia.com/Module:Mods/list">Module:Mods/list</a>
 * including mod abbreviation, unlocalized mod name in English, and link text.
 * <i>Note:</i> Each instance is tied to a specific {@link Wiki} object.
 */
public final record Mod(Wiki wiki, String abbrv, String name, String link) {
    /**
     * The page name of the mods list.
     */
    public static final String MODS_LIST = "Module:Mods/list";
    private static final Pattern NORMAL_ABBREVIATION = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");
    private static final CachedMap<Wiki, ModData> CACHED_MOD_DATA = new CachedMap<>();
    private static final Function<Wiki, ModData> RETRIEVE_FUNCTION = w -> {
        // Order abbreviations with TreeMap for addition and removal purposes
        TreeMap<String, Mod> byAbbrv = new TreeMap<>(getTableAsMap(w));

        Map<String, Mod> joinedMods = new HashMap<>();
        for (Map.Entry<String, Mod> entry : byAbbrv.entrySet()) {
            Mod mod = entry.getValue();
            logConflict(mod, joinedMods.put(mod.name().toLowerCase(), mod)); // Case insensitive mod name
            logConflict(mod, joinedMods.put(entry.getKey().toLowerCase(), mod)); // Add abbreviation but lowercase
        }

        return new ModData(joinedMods, byAbbrv);
    };

    public Mod(Wiki wiki, String abbrv, String name, String link) {
        this.wiki = wiki;
        this.abbrv = abbrv;
        this.name = name;
        this.link = link == null ? name : link;
    }

    /**
     * Get the localized name of the mod in the specified language.
     *
     * @param lang A {@link Language} object to use, or English if null.
     * @return The localized mod name in the specified language, or null if it does not exist.
     */
    public String getLocalized(Language lang) {
        String langCode = lang == null ? "en" : lang.getCode();
        JsonElement returnJson = Scribunto.runScribuntoCode(this.wiki, MODS_LIST + '/' + langCode, "p.byAbbrv['" + this.abbrv + "']").getReturnJson();
        return returnJson == null ? null : GSONP.getStr(returnJson.getAsJsonObject(), "localized");
    }

    /**
     * Returns another {@link Mod} instance if there is a conflicting field, or null if there is no conflict.
     *
     * @return another {@link Mod} instance if there is a conflicting field, or null if there is no conflict.
     */
    public Mod getConflict() {
        // Reload the mods just in case it was already added recently
        ModData modData = loadMods(this.wiki, true);
        Map<String, Mod> joinedMods = modData.joinedMods();

        Mod existing = joinedMods.get(this.name.toLowerCase());
        if (existing != null)
            return existing;

        return joinedMods.get(this.abbrv.toLowerCase());
    }

    /**
     * Adds this {@link Mod} instance to the mods list, if it does not already exist.
     *
     * @return the {@link AReply} corresponding to the response from the server.
     * @throws IllegalStateException if the mod already exists in the mods list.
     */
    public AReply add() {
        Mod conflict = getConflict();
        if (conflict != null)
            throw new IllegalStateException(this.toString() + " already exists in mods list.");

        // Mod#getConflict already forcefully retrieves the mod data
        ModData modData = loadMods(wiki, false);
        Map.Entry<String, Mod> lowerEntry = modData.byAbbrv().lowerEntry(this.abbrv);
        Map.Entry<String, Mod> higherEntry = modData.byAbbrv().higherEntry(this.abbrv);

        String rawText = this.wiki.getPageText(MODS_LIST);
        String[] lines = rawText.split("\n");
        int selected = -1;

        Mod lower = lowerEntry == null ? new Mod(null, null, null, null) : lowerEntry.getValue();
        Mod higher = higherEntry == null ? new Mod(null, null, null, null) : higherEntry.getValue();
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i];
            int end = line.indexOf('=');
            if (end == -1)
                continue;
            String id = line.substring(0, end).trim();
            if (id.startsWith("[")) {
                id = id.substring(2, id.length() - 2);
            }
            if (id.equals(lower.abbrv)) {
                selected = i + 1;
                break;
            }
            if (id.equals(higher.abbrv)) {
                selected = i;
                break;
            }
        }
        if (selected == -1)
            throw new IllegalStateException("Could not find an insertion point for " + this);

        String expandedAbbrv = NORMAL_ABBREVIATION.matcher(this.abbrv).matches() ? this.abbrv : "[\"" + this.abbrv.replace("\"", "\\\"") + "\"]";
        String newLine;
        if (this.hasDistinctLink()) {
            newLine = String.format("    %s = {'%s', '%s', [=[<translate>%s</translate>]=]},", expandedAbbrv, this.link.replace("'", "\\'"), this.name.replace("'", "\\'"), this.name);
        } else {
            newLine = String.format("    %s = {'%s', [=[<translate>%s</translate>]=]},", expandedAbbrv, this.name.replace("'", "\\'"), this.name);
        }
        String insertLine = lines[selected];
        String newText = rawText.replace(insertLine, newLine + '\n' + insertLine);

        AReply reply = this.wiki.edit(MODS_LIST, newText, "Added " + this.abbrv + '=' + this.name);

        if (reply.isSuccess()) {
            modData.joinedMods().put(this.name.toLowerCase(), this);
            modData.joinedMods().put(this.abbrv.toLowerCase(), this);
            modData.byAbbrv().put(this.abbrv.toUpperCase(), this);
        }

        return reply;
    }

    /**
     * Removes this {@link Mod} instance from the mods list, if it exists.
     *
     * @return the {@link AReply} corresponding to the response from the server.
     * @throws IllegalStateException if the mod does not exist in the mods list.
     */
    public AReply remove() {
        // Reload the mods just in case it was added recently
        ModData modData = loadMods(this.wiki, true);
        if (modData.byAbbrv().get(this.abbrv) == null)
            throw new IllegalStateException(this + " does not exist in mods list."); // Doesn't exist

        String rawText = this.wiki.getPageText(MODS_LIST);
        String[] lines = rawText.split("\n");
        int selected = -1;

        for (int i = 2; i < lines.length; i++) {
            String line = lines[i];
            int end = line.indexOf('=');
            if (end == -1)
                continue;
            String id = line.substring(0, end).trim();
            if (id.startsWith("[")) {
                id = id.substring(2, id.length() - 2);
            }
            if (id.equals(this.abbrv)) {
                selected = i;
                break;
            }
        }
        if (selected == -1)
            throw new IllegalStateException("Could not find " + this.toString() + " in mods list.");

        String thisLine = lines[selected];
        String newText = rawText.replace('\n' + thisLine, "");

        AReply reply = this.wiki.edit(MODS_LIST, newText, "Removed " + this.abbrv + '=' + this.name);

        if (reply.isSuccess()) {
            modData.joinedMods().remove(this.name.toLowerCase());
            modData.joinedMods().remove(this.abbrv.toLowerCase());
            modData.byAbbrv().remove(this.abbrv.toUpperCase());
        }

        return reply;
    }

    /**
     * Get a {@link Mod} by its unlocalized mod name or abbreviation, e.g. "Crop Dusting" or "CD". This method also converts all names to lowercase since no names should conflict anyways. This method
     * will return null if the JSON from Scribunto Console API is null OR if the mod does not exist.
     *
     * @param wiki A {@link Wiki} object to use for requesting the data.
     * @param modInfo The unlocalized mod name or abbreviation, case insensitive.
     * @return A {@link Mod} object, or null.
     */
    public static Mod getByInfo(Wiki wiki, String modInfo) {
        ModData modData = loadMods(wiki, false);
        return modData.joinedMods().get(modInfo.toLowerCase());
    }

    /**
     * Get a {@link Mod} by its abbreviation, e.g. "CD". This method also converts all abbreviations to uppercase. This method will return null if the JSON from Scribunto Console API is null OR if the
     * mod does not exist.
     *
     * @param wiki A {@link Wiki} object to use for requesting the data.
     * @param modAbbrv The mod abbreviation, case insensitive.
     * @return A {@link Mod} object, or null.
     */
    public static Mod getByAbbreviation(Wiki wiki, String modAbbrv) {
        ModData modData = loadMods(wiki, false);
        return modData.byAbbrv().get(modAbbrv.toUpperCase());
    }

    /**
     * Load the mods.
     *
     * @param wiki The {@link Wiki} object to use for requesting the data.
     * @param retrieve If true, always retrieve the data. Otherwise, use cached data if it exists.
     * @return The mod data.
     */
    private static ModData loadMods(Wiki wiki, boolean retrieve) {
        if (retrieve) {
            return CACHED_MOD_DATA.retrieve(wiki, RETRIEVE_FUNCTION);
        } else {
            return CACHED_MOD_DATA.getOrRetrieve(wiki, RETRIEVE_FUNCTION);
        }
    }

    private static void logConflict(Mod mod, Mod prev) {
        if (prev != null && mod != prev) {
            Bot.LOGGER.warn("A mod conflict was found between {} and {}.", prev, mod);
        }
    }

    // Puts all mod abbreviations in the map in UPPERCASE form
    private static Map<String, Mod> getTableAsMap(Wiki wiki) {
        JsonElement json = Scribunto.runScribuntoCode(wiki, Mod.MODS_LIST, "p.byAbbrv").getReturnJson();
        if (json == null)
            return Map.of();

        Map<String, Mod> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
            JsonObject modJson = entry.getValue().getAsJsonObject();

            String abbrv = GSONP.getStr(modJson, "abbr");
            String name = GSONP.getStr(modJson, "name");
            String link = GSONP.getStr(modJson, "link");
            Mod mod = new Mod(wiki, abbrv, name, link);
            map.put(entry.getKey().toUpperCase(), mod); // Get all abbreviations as UPPERCASE
        }

        return map;
    }

    /**
     * Get the page name of the mod. In almost all cases, this is equal to the unlocalized name.
     *
     * @return The page name of the mod, named "link" in the mods list.
     */
    public String link() {
        return this.link;
    }

    /**
     * Returns true if this mod's page link is different from its unlocalized name. In almost all cases, this is false.
     *
     * @return true if this mod's page link is different from its unlocalized name.
     */
    public boolean hasDistinctLink() {
        return !this.name.equals(this.link);
    }

    /**
     * Returns the full path to the mod's page based on the current {@link Wiki}.
     *
     * @return the full path to the mod's page based on the current {@link Wiki}.
     */
    public String getUrlLink() {
        return WikiUtil.getBaseWikiPageUrl(this.wiki, this.link);
    }

    @Override
    public String toString() {
        return "Mod{" +
                "abbrv='" + abbrv + '\'' +
                ", name='" + name + '\'' +
                ", link='" + link + '\'' +
                '}';
    }

    private record ModData(Map<String, Mod> joinedMods, TreeMap<String, Mod> byAbbrv) {}
}
