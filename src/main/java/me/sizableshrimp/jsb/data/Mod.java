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
import me.sizableshrimp.jsb.Config;
import me.sizableshrimp.jsb.util.WikiUtil;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.GSONP;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * An immutable object that stores wiki data about Mods from
 * <a href="https://ftb.gamepedia.com/Module:Mods/list">Module:Mods/list</a>
 * including mod abbreviation, unlocalized mod name in English, and link text.
 * <i>Note:</i> Each instance is tied to a specific {@link Wiki} object.
 */
public final class Mod {
    /**
     * The page name of the mods list.
     */
    public static final String MODS_LIST = "Module:Mods/list";
    private static final Map<Wiki, Map<String, Mod>> joinedData = new HashMap<>();
    private static final Map<Wiki, TreeMap<String, Mod>> abbrvData = new HashMap<>();
    private final Wiki wiki;
    private final String abbrv;
    private final String name;
    private final String link;

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
        return GSONP.getStr(Scribunto.runScribuntoCode(wiki, Mod.MODS_LIST + langCode, "p.byAbbrv['" + abbrv + "']").getReturnJson().getAsJsonObject(), "localized");
    }

    /**
     * Returns another {@link Mod} instance if there is a conflicting field, or null if there is no conflict.
     *
     * @return another {@link Mod} instance if there is a conflicting field, or null if there is no conflict.
     */
    public Mod getConflict() {
        // Reload the mods just in case it was already added recently
        loadMods(this.wiki);
        Map<String, Mod> joined = joinedData.get(this.wiki);

        Mod existing;
        if ((existing = joined.get(this.name.toLowerCase())) != null || (existing = joined.get(this.abbrv.toLowerCase())) != null) {
            return existing;
        }

        return null;
    }

    /**
     * Adds this {@link Mod} instance to the mods list, if it does not already exist.
     *
     * @return {@code null} if this {@link Mod} instance was successfully added,
     * otherwise the instance of a conflicting mod that already existed.
     */
    public Mod add() {
        Mod conflict = getConflict();
        if (conflict != null)
            return conflict;

        TreeMap<String, Mod> byAbbrv = abbrvData.get(this.wiki);
        Map.Entry<String, Mod> lowerEntry = byAbbrv.lowerEntry(this.abbrv);
        Map.Entry<String, Mod> higherEntry = byAbbrv.higherEntry(this.abbrv);

        String rawText = this.wiki.getPageText(MODS_LIST);
        String[] lines = rawText.split("\n");
        int selected = -1;

        Mod lower = lowerEntry == null ? new Mod(null, null, null, null) : lowerEntry.getValue();
        Mod higher = higherEntry == null ? new Mod(null, null, null, null) : higherEntry.getValue();
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i];
            if (line.indexOf('=') == -1)
                continue;
            String id = line.substring(0, line.indexOf('=')).trim();
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
        String newLine;
        if (this.hasDistinctLink()) {
            newLine = String.format("    %s = {'%s', '%s', [=[<translate>%s</translate>]=]},", this.abbrv, this.link.replace("'", "\\'"), this.name.replace("'", "\\'"), this.name);
        } else {
            newLine = String.format("    %s = {'%s', [=[<translate>%s</translate>]=]},", this.abbrv, this.name.replace("'", "\\'"), this.name);
        }
        String insertLine = lines[selected];
        String newText = rawText.replace(insertLine, newLine + '\n' + insertLine);

        this.wiki.edit(MODS_LIST, newText, "Added " + this.abbrv);
        joinedData.get(this.wiki).put(this.name.toLowerCase(), this);
        joinedData.get(this.wiki).put(this.abbrv.toLowerCase(), this);
        abbrvData.get(this.wiki).put(this.abbrv.toUpperCase(), this);

        return null; // No conflict
    }

    /**
     * Removes this {@link Mod} instance from the mods list, if it exists.
     *
     * @return {@code true} if this {@link Mod} instance was removed, {@code false} if it did not exist.
     */
    public boolean remove() {
        // Reload the mods just in case it was added recently
        loadMods(this.wiki);
        if (abbrvData.get(this.wiki).get(this.abbrv) == null)
            return false; // Doesn't exist

        String rawText = this.wiki.getPageText(MODS_LIST);
        String[] lines = rawText.split("\n");
        int selected = -1;

        for (int i = 2; i < lines.length; i++) {
            String line = lines[i];
            if (line.indexOf('=') == -1)
                continue;
            String id = line.substring(0, line.indexOf('=')).trim();
            if (id.startsWith("[")) {
                id = id.substring(2, id.length() - 2);
            }
            if (id.equals(this.abbrv)) {
                selected = i;
                break;
            }
        }
        String thisLine = lines[selected];
        String newText = rawText.replace('\n' + thisLine, "");

        this.wiki.edit(MODS_LIST, newText, "Removed " + this.abbrv);
        joinedData.get(this.wiki).remove(this.name.toLowerCase());
        joinedData.get(this.wiki).remove(this.abbrv.toLowerCase());
        abbrvData.get(this.wiki).remove(this.abbrv.toUpperCase());

        return true;
    }

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
        Map<String, Mod> joinedMods = joinedData.get(wiki);
        if (joinedMods == null)
            joinedMods = loadMods(wiki);

        return joinedMods.get(modInfo.toLowerCase());
    }

    private static Map<String, Mod> loadMods(Wiki wiki) {
        Map<String, Mod> byAbbrv = getTableAsMap(wiki);

        // Order abbreviations for addition and removal purposes
        abbrvData.put(wiki, new TreeMap<>(byAbbrv));

        Map<String, Mod> updated = new HashMap<>();
        for (Map.Entry<String, Mod> entry : byAbbrv.entrySet()) {
            Mod mod = entry.getValue();
            updated.put(mod.getName().toLowerCase(), mod); // Case insensitive mod name
            updated.put(entry.getKey().toLowerCase(), mod); // Add abbreviation but lowercase
        }

        joinedData.put(wiki, updated);
        return updated;
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

    public Wiki getWiki() {
        return this.wiki;
    }

    public String getAbbrv() {
        return this.abbrv;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Get the page name of the mod. In almost all cases, this is equal to the unlocalized name.
     *
     * @return The page name of the mod, named "link" in the mods list.
     */
    public String getLink() {
        return this.link;
    }

    /**
     * Returns true if this mod's page link is different from its unlocalized name.
     * In almost all cases, this is false.
     *
     * @return true if this mod's page link is different from its unlocalized name.
     */
    public boolean hasDistinctLink() {
        return !this.name.equals(this.link);
    }

    /**
     * Returns the full path to the mod's page based on the current {@link Config#getApi()}.
     *
     * @return the full path to the mod's page based on the current {@link Config#getApi()}.
     */
    public String getUrlLink() {
        return WikiUtil.getBaseArticleUrl(this.wiki) + this.link.replace(' ', '_');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Mod mod = (Mod) o;

        if (!wiki.equals(mod.wiki))
            return false;
        if (!abbrv.equals(mod.abbrv))
            return false;
        if (!name.equals(mod.name))
            return false;
        return link.equals(mod.link);
    }

    @Override
    public int hashCode() {
        int result = wiki.hashCode();
        result = 31 * result + abbrv.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + link.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Mod{" +
                "wiki=" + wiki +
                ", abbrv='" + abbrv + '\'' +
                ", name='" + name + '\'' +
                ", link='" + link + '\'' +
                '}';
    }
}
