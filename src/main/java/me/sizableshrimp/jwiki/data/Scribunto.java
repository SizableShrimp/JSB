package me.sizableshrimp.jwiki.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import me.sizableshrimp.jwiki.WikiUtil;
import okhttp3.Response;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;
import org.fastily.jwiki.util.GSONP;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Scribunto {
    String print;
    @Getter(AccessLevel.NONE)
    String ret;
    JsonObject responseJson;

    private Scribunto(JsonObject json) {
        this(GSONP.getStr(json, "print"), GSONP.getStr(json, "return"), json);
    }

    private Scribunto() {
        this(null, null, null);
    }

    /**
     * Runs the given module and returns back the result that has been JSON-encoded using {@code mw.text.jsonEncode}.
     * If {@code code} is set to null, it defaults to "p", or "package."
     *
     * @param wiki A {@link Wiki} object to use for requesting the data.
     * @param module The name of a module on the wiki, with or without the "Module:" prefix, e.g. "Language" or "Module:Language".
     * @param code The code to parse from the module, or "p" if null. Some examples are "p" or "p.functionName(parameters)"
     * @return A {@link Scribunto} response object containing the response JSON, what was printed to the Scribunto console with {@code mw.log}, and the returned JSON-encoded string.
     * This returns a blank object if the module does not exist or the response is not in the form of a JSON object.
     */
    public static Scribunto runScribuntoCode(Wiki wiki, String module, String code) {
        if (code == null)
            code = "p";
        module = prefixModule(module);
        if (!wiki.exists(module))
            return new Scribunto();
        Response response = wiki.basicPOST("scribunto-console", FL.pMap("title", module, "question", "=mw.text.jsonEncode(" + code + ")", "content", wiki.getPageText(module)));
        JsonElement json = WikiUtil.jsonFromResponse(response);
        if (!json.isJsonObject())
            return new Scribunto();

        return new Scribunto(json.getAsJsonObject());
    }

    private static String prefixModule(String title) {
        return title.startsWith("Module:") ? title : "Module:" + title;
    }

    public String getReturn() {
        return ret;
    }

    /**
     * Parses and returns the JSON-encoded string provided by the Scribunto console output,.
     * If the Scribunto console did not return a proper response, this function prints a message to stderr and returns null.
     *
     * @return The JSON from the Scribunto console, or null on error.
     * @see Scribunto#runScribuntoCode(Wiki, String, String)
     */
    public JsonElement getReturnJson() {
        if (this.responseJson.getAsJsonPrimitive("type").getAsString().equals("normal")) {
            return JsonParser.parseString(this.responseJson.getAsJsonPrimitive("return").getAsString());
        } else {
            System.err.println("Scribunto Console API returned a non-normal result:\n" + GSONP.gsonPP.toJson(responseJson));
            return null;
        }
    }
}
