package me.sizableshrimp.jwiki.data;

import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import me.sizableshrimp.jwiki.WikiUtil;
import org.fastily.jwiki.core.Wiki;

/**
 * An immutable object that stores wiki data about Languages from
 * <a href="https://ftb.gamepedia.com/Module:Language/Names">Module:Language/Names</a>
 * including language code, language name in English, and localized name.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Language {
    String code;
    String localized;
    String english;

    public boolean hasEnglishTranslation() {
        return !localized.equals(english);
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
        String localized = getLanguageInfo(wiki, code);
        if (localized == null || localized.equals(code))
            return null;
        String english = getLanguageInfo(wiki, code + "|en");

        return new Language(code, localized, english);
    }

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

    private static String getLanguageInfo(Wiki wiki, String data) {
        JsonObject parse = WikiUtil.parse(wiki, null, "{{#language:" + data + "}}").getAsJsonObject().getAsJsonObject("parse");
        //System.out.println(parse);
        if (parse == null)
            return null;
        JsonObject text = parse.getAsJsonObject("text");
        if (text == null)
            return null;

        String output = text.get("*").getAsString();
        // Data is normal wrapped like so "<p>data\n<\p>"
        return output.substring(3, output.length() - 5);
    }
}
