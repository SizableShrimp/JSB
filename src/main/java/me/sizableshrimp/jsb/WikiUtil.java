package me.sizableshrimp.jsb;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.tools.javac.Main;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.core.WQuery.QReply;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;
import org.jetbrains.annotations.Nullable;
import reactor.util.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WikiUtil {
    private WikiUtil() {}

    //    public static boolean move(Wiki wiki, String fromTitle, String toTile, @Nullable String reason,
    //                               boolean leaveRedirect) {
    //        wiki.basicPOST("move", FL.pMap("from", fromTitle, "to", toTile, "reason", reason == null ? "" : reason,
    //                "noredirect", Boolean.toString(!leaveRedirect)));
    //    }

    /**
     * Run input parse text through {@code action=parse} with the specified {@code contentmodel}.
     * If a content model is not specified, defaults to wikitext.
     *
     * @param wiki A {@link Wiki} object used to perform API calls.
     * @param contentModel The content model of the input text, defaults to "wikitext" if null.
     * Some possible options are "wikitext", "GadgetDefinition", "Scribunto", "javascript", "json", "css", and "text".
     * Note that these are <i>case-sensitive</i>.
     * @param parse The input text to parse with the given {@code wiki} and {@code contentmodel}.
     * @return The JSON data returned from the API.
     */
    public static JsonElement parse(Wiki wiki, String contentModel, String parse) {
        String cm = contentModel == null ? "wikitext" : contentModel;
        return jsonFromResponse(wiki.basicGET("parse", "wrapoutputclass", "", "disablelimitreport", "true", "contentmodel", cm, "text", parse));
    }

    /**
     * Convert a {@link Response} into a {@link JsonElement} by parsing the {@link ResponseBody}.
     * Returns an empty {@link JsonObject} if the response is null, body is null, or {@link ResponseBody#string()} throws an exception.
     *
     * @param response A {@link Response} from a wiki API call.
     * @return A {@link JsonElement} parsed from a {@link ResponseBody}, or an empty {@link JsonObject} on invalid {@link Response}.
     */
    @NonNull
    public static JsonElement jsonFromResponse(@Nullable Response response) {
        if (response == null)
            return new JsonObject();

        try {
            ResponseBody body = response.body();
            if (body != null)
                return JsonParser.parseString(body.string());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new JsonObject();
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

    public static AReply move(Wiki wiki, String from, String to, String reason, boolean moveTalk, boolean moveSubPages, boolean noRedirect) {
        Map<String, String> formData = FL.pMap("from", from, "to", to, "token", wiki.getConfig().getToken(), "watchlist", "nochange");
        if (reason != null)
            formData.put("reason", reason);
        if (moveTalk)
            formData.put("movetalk", "true");
        if (moveSubPages)
            formData.put("movesubpages", "true");
        if (noRedirect)
            formData.put("noredirect", "true");
        return AReply.processAction(wiki, "move", formData);
    }

    public static List<QReply> getQueryReplies(WQuery query) {
        List<QReply> list = new ArrayList<>();

        while (query.has()) {
            QReply reply = query.next();
            if (reply == null) {
                Bot.LOGGER.error("Error when parsing QReply at index " + (list.size() - 1));
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
}
