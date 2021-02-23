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

package me.sizableshrimp.jsb.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.sizableshrimp.jsb.Bot;
import org.fastily.jwiki.core.AReply;
import org.fastily.jwiki.core.QReply;
import org.fastily.jwiki.core.QTemplate;
import org.fastily.jwiki.core.WAction;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class WikiUtil {
    private static final QTemplate IMAGE_INFO = new QTemplate(FL.pMap("iiprop", "url"), "iilimit", "pages");
    private static final QTemplate SITE_INFO = new QTemplate(FL.pMap("action", "query", "meta", "siteinfo"), "query");
    private static final Map<Wiki, String> baseUrls = new HashMap<>();

    private WikiUtil() {}

    /**
     * Run input parse text through {@code action=parse} with the specified {@code contentmodel}.
     * If a content model is not specified, defaults to wikitext.
     *
     * @param wiki A {@link Wiki} object used to perform API calls.
     * @param contentModel The content model of the input text, defaults to "wikitext" if null.
     * Some possible options are "wikitext", "GadgetDefinition", "Scribunto", "javascript", "json", "css", and "text".
     * Note that these are <i>case-sensitive</i>.
     * @param parse The input text to parse with the given {@code wiki} and {@code contentmodel}.
     * @return The {@link AReply} containing JSON data returned from the API.
     */
    public static AReply parse(Wiki wiki, String contentModel, String parse) {
        String cm = contentModel == null ? "wikitext" : contentModel;
        return WAction.getAction(wiki, "parse", true, "wrapoutputclass", "", "disablelimitreport", "true", "contentmodel", cm, "text", parse);
    }

    public static String getLatestFileUrl(Wiki wiki, String page) {
        String file = page.startsWith("File:") ? page : "File:" + page;
        QReply reply = new WQuery(wiki, 1, IMAGE_INFO)
                .set("titles", file)
                .set("prop", "imageinfo")
                .next();

        Map.Entry<String, JsonElement> missing = reply.propComp("title", "missing").entrySet().iterator().next();
        if (missing.getValue() != null) // It is missing
            return null;

        return reply.propComp("title", "imageinfo").values().iterator().next()
                .getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
    }

    /**
     * Resolve multiple redirects, accounting for double redirects, etc.
     *
     * @param wiki
     * @param title
     * @return
     */
    public static String resolveRedirects(Wiki wiki, String title) {
        String prev;
        String current = title;
        do {
            prev = current;
            current = wiki.resolveRedirect(prev);
        } while (!current.equals(prev));

        return current;
    }

    public static List<QReply> getQueryReplies(WQuery query) {
        List<QReply> list = new ArrayList<>();

        while (query.has()) {
            QReply reply = query.next();
            if (reply == null) {
                Bot.LOGGER.error("Error when parsing QReply at index {}", list.size() - 1);
                break;
            }
            list.add(reply);
        }

        return list;
    }

    public static List<JsonObject> getQueryRepliesAsList(WQuery query, String listComp) {
        return getQueryReplies(query).stream()
                .flatMap(reply -> reply.listComp(listComp).stream())
                .collect(Collectors.toList());
    }

    /**
     * Returns a link to the {@code page} specified, never null.
     * This always returns a link even if the page does not exist.
     * Only supports the current {@link Wiki} domain using {@link #getBaseArticleUrl(Wiki)}.
     *
     * @param wiki The {@link  Wiki} instance.
     * @param page The page to link to, not including square brackets.
     * @return a link to the {@code page} specified, never null.
     */
    public static String getBaseWikiPageUrl(Wiki wiki, String page) {
        return getBaseArticleUrl(wiki) + URLEncoder.encode(page.replace(' ', '_'), StandardCharsets.UTF_8);
    }

    /**
     * Returns a link to the {@code page} specified, or null if it does not exist.
     * Supports interwiki links.
     *
     * @param wiki The {@link  Wiki} instance.
     * @param page The page to link to, not including square brackets.
     * @return a link to the {@code page} specified, or null if it does not exist.
     */
    public static String getWikiPageUrl(Wiki wiki, String page) {
        page = page.startsWith(":") ? page : ":" + page;
        JsonObject json = WikiUtil.parse(wiki, null, "[[" + page + "]]").getSuccessJson();
        JsonArray localLinks = getOrDefault(json, "links");
        JsonArray interwikiLinks = getOrDefault(json, "iwlinks");
        JsonArray images = getOrDefault(json, "images");

        String result = null;

        if (localLinks.size() > 0) {
            JsonObject obj = localLinks.get(0).getAsJsonObject();
            result = getBaseWikiPageUrl(wiki, obj.get("*").getAsString());
        } else if (interwikiLinks.size() > 0) {
            JsonObject obj = interwikiLinks.get(0).getAsJsonObject();
            result = obj.get("url").getAsString();
        } else if (images.size() > 0) {
            String image = images.get(0).getAsString();
            result = getBaseWikiPageUrl(wiki, image);
        }

        if (result != null) {
            // Add section link
            int sectionIndex = page.indexOf('#');
            if (sectionIndex != -1) {
                result = result + page.substring(sectionIndex);
            }
        }

        return result;
    }

    private static JsonArray getOrDefault(JsonObject json, String memberName) {
        JsonElement arr = json.get(memberName);
        return arr == null ? new JsonArray() : arr.getAsJsonArray();
    }

    /**
     * Returns the base article url including a trailing slash.
     *
     * @param wiki The {@link Wiki} instance.
     * @return the base article url including a trailing slash.
     */
    public static String getBaseArticleUrl(Wiki wiki) {
        if (baseUrls.containsKey(wiki))
            return baseUrls.get(wiki);

        String base = SITE_INFO.createQuery(wiki).next().metaComp("general").getAsJsonObject().get("base").getAsString();
        base = base.substring(0, base.lastIndexOf('/') + 1); // Include the trailing slash but remove the main page
        baseUrls.put(wiki, base);
        return base;
    }
}
