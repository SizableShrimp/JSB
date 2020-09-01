package me.sizableshrimp.jwiki;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

public class WikiUtil {
    private WikiUtil() {}

    //    public static boolean move(Wiki wiki, String fromTitle, String toTile, @Nullable String reason,
    //                               boolean leaveRedirect) {
    //        wiki.basicPOST("move", FL.pMap("from", fromTitle, "to", toTile, "reason", reason == null ? "" : reason,
    //                "noredirect", Boolean.toString(!leaveRedirect)));
    //    }

    public static JsonElement parse(Wiki wiki, String contentmodel, String parse) {
        String cm = contentmodel == null ? "wikitext" : contentmodel;
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
}
