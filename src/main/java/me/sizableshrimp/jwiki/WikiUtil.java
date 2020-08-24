package me.sizableshrimp.jwiki;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.fastily.jwiki.core.Wiki;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class WikiUtil {
    private WikiUtil() {}

    //    public static boolean move(Wiki wiki, String fromTitle, String toTile, @Nullable String reason,
    //                               boolean leaveRedirect) {
    //        wiki.basicPOST("move", FL.pMap("from", fromTitle, "to", toTile, "reason", reason == null ? "" : reason,
    //                "noredirect", Boolean.toString(!leaveRedirect)));
    //    }

    public static JsonElement parse(Wiki wiki, String title, String parse) {
        return jsonFromResponse(wiki.basicGET("parse", "wrapoutputclass", "", "disablelimitreport", "true", "title", title == null ? "Test" : title, "text", parse));
    }

    /**
     * Convert a {@link Response} into a {@link JsonElement} by parsing the {@link ResponseBody}.
     * Returns an empty {@link JsonObject} if the response is null, body is null, or {@link ResponseBody#string()} throws an exception.
     *
     * @param response A {@link Response} from a wiki API call.
     * @return A {@link JsonElement} parsed from a {@link ResponseBody}, or an empty {@link JsonObject} on error.
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

    public static JsonObject useModule(Wiki wiki, String module, String function, String... args) {
        String data = "local p = {} function p.run(args) local module = require([[Module:]]..args[1]) if args[2] == nil then return mw.text.jsonEncode(module) else local value = module[args[2]] if " +
                "type(value) == 'function' then return value(unpack(args, 3)) else if type(value) == 'table' then return mw.text.jsonEncode(value) else return value";
        function = function == null ? "" : "|" + function;
        String argsCombined = args == null || args.length == 0 || function.isEmpty()
                ? ""
                : "|" + String.join("|", args);
        String text = String.format("{{:User:SizableShrimp/Workaround|%s%s%s}}", module, function, argsCombined)
                .replace("|", "{{!}}")
                .replace("=", "{{=}}");
        Response response = wiki.basicGET("expandtemplates", "prop", "wikitext", "text", text);

        if (response != null) {
            try {
                ResponseBody body = response.body();
                if (body == null)
                    return null;
                return JsonParser.parseString(body.string()).getAsJsonObject().getAsJsonObject("expandtemplates");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
