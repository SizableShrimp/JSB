package me.sizableshrimp.jwiki.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import me.sizableshrimp.jwiki.WikiUtil;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;
import org.fastily.jwiki.util.GSONP;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An immutable object that stores wiki data about Languages from
 * <a href="https://ftb.gamepedia.com/Module:Language/Names">Module:Language/Names</a>
 * including language code, language name in English, and localized name.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Language {
    /**
     * The MediaWiki-specific language code which overlaps with some standards.
     */
    @NotNull
    @NonNull
    String code;
    /**
     * The writing direction of the language.
     */
    @Nullable
    Direction writingDirection;
    /**
     * The name of the language in its own language. In some rare instances for untranslated languages, this is blank.
     */
    @NotNull
    @NonNull
    String autonym;
    /**
     * The name of the language in English, if it exists.
     */
    @Nullable
    String english;

    //TODO This query only works in MW 1.34+, use once Gamepedia updates (when?)
    ///**
    // * https://www.mediawiki.org/wiki/API:Languageinfo
    // */
    //private static final WQuery.QTemplate LANGUAGEINFO = new WQuery.QTemplate(FL.pMap("action", "query", "meta", "languageinfo", "liprop", "code|dir|autonym|name",
    //        "uselang", "en"), "query");
    /**
     * https://www.mediawiki.org/wiki/API:Siteinfo
     */
    private static final WQuery.QTemplate LANGUAGEINFO = new WQuery.QTemplate(FL.pMap("action", "query", "meta", "siteinfo", "siprop", "languages"), "query");
    private static Map<String, Language> languages = null;

    /**
     * Get a {@link Language} by a title with a subpage as a language code (ex. "GregTech/de").
     * Returns null if there is no subpage or the subpage is not a language code.
     *
     * @param wiki A {@link Wiki} object to use for requesting the data.
     * @param title A title with the subpage being a language code, ex. "Template:Navbox GregTech 6/de".
     * @return A {@link Language} object.
     * @see Language#getByCode(Wiki, String)
     */
    public static Language getByTitle(Wiki wiki, String title) {
        int index = title.lastIndexOf('/');
        if (index == -1 || title.length() == index + 1)
            return null;

        return getByCode(wiki, title.substring(index + 1));
    }

    /**
     * Get a {@link Language} by its language code (ex. "en").
     * Returns null if the language code is not valid.
     *
     * @param wiki A {@link Wiki} object to use for requesting the data.
     * @param code A short language code, usually 2 letters (ex. "en", "de", "fr").
     * @return A {@link Language} object, or null if invalid.
     */
    public static Language getByCode(Wiki wiki, String code) {
        if (languages == null)
            loadLanguages(wiki);

        return languages.get(code);
    }

    private static void loadLanguages(Wiki wiki) {
        if (languages != null)
            return;
        languages = new HashMap<>();
        if (wiki == null)
            return;
        List<JsonObject> languageinfo = WikiUtil.getQueryRepliesAsList(new WQuery(wiki, LANGUAGEINFO),"languages");
        List<JsonObject> englishNames = WikiUtil.getQueryRepliesAsList(new WQuery(wiki, LANGUAGEINFO).set("siinlanguagecode", "en"), "languages");

        parseLanguageInfo(languageinfo, englishNames);
        parseOverrides(Scribunto.runScribuntoCode(wiki, "Language/Names", null).getReturnJson().getAsJsonObject());
    }

    private static void parseLanguageInfo(List<JsonObject> languageinfo, List<JsonObject> englishNames) {
        for (int i = 0; i < languageinfo.size(); i++) {
            JsonObject json = languageinfo.get(i);

            // The older siprop=languages API does not return writing direction and takes another request for English-localized names
            String code = GSONP.getStr(json, "code");
            String autonym = GSONP.getStr(json, "*");
            String english = GSONP.getStr(englishNames.get(i).getAsJsonObject(), "*");
            languages.put(code, new Language(code, null, autonym, english));
        }
    }

    //private static void parseLanguageInfo(List<JsonObject> languageinfo) {
    //    for (JsonObject json : languageinfo) {
    //        String code = GSONP.getStr(json, "code");
    //        Direction writingDirection = Direction.getDirection(GSONP.getStr(json, "dir"));
    //        String autonym = GSONP.getStr(json, "autonym");
    //        String english = GSONP.getStr(json, "name");
    //        languages.put(code, new Language(code, writingDirection, autonym, english));
    //    }
    //}

    private static void parseOverrides(JsonObject overrides) {
        for (Map.Entry<String, JsonElement> entry : overrides.entrySet()) {
            String code = entry.getKey();
            JsonArray json = entry.getValue().getAsJsonArray();
            languages.put(code, new Language(code, null, json.get(1).getAsString(), json.get(0).getAsString()));
        }
    }

    public static boolean hasLoadedLanguages() {
        return languages != null;
    }

    public boolean hasEnglishTranslation() {
        return english != null && !autonym.equals(english) && !english.isEmpty();
    }

    public static Map<String, Language> getAllLanguages(Wiki wiki) {
        loadLanguages(wiki);
        return languages;
    }

    public static Set<String> flattenTranslationPages(Wiki wiki, Set<String> titles) {
        return titles.stream()
                .map(title -> flattenTranslationPage(wiki, title))
                .collect(Collectors.toSet());
    }

    public static String flattenTranslationPage(Wiki wiki, String title) {
        return Language.getByTitle(wiki, title) == null ? title : title.substring(0, title.lastIndexOf('/'));
    }

    @RequiredArgsConstructor
    public enum Direction {
        LEFT_TO_RIGHT("ltr"),
        RIGHT_TO_LEFT("rtl");

        private final String dir;

        public static Direction getDirection(String dir) {
            for (Direction value : values()) {
                if (value.dir.equals(dir))
                    return value;
            }

            return null;
        }
    }
}
