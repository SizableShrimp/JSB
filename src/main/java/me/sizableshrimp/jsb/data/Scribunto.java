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
import com.google.gson.JsonParser;
import me.sizableshrimp.jsb.util.WikiUtil;
import okhttp3.Response;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;
import org.fastily.jwiki.util.GSONP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Scribunto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Scribunto.class);
    private final String print;
    private final String ret;
    private final JsonObject responseJson;

    private Scribunto(JsonObject json) {
        this(GSONP.getStr(json, "print"), GSONP.getStr(json, "return"), json);
    }

    private Scribunto() {
        this(null, null, null);
    }

    private Scribunto(String print, String ret, JsonObject responseJson) {
        this.print = print;
        this.ret = ret;
        this.responseJson = responseJson;
    }

    /**
     * Runs the given module and returns back the result that has been JSON-encoded
     * using {@code mw.text.jsonEncode}. If {@code code} is set to null, it defaults
     * to "p", or "package."
     *
     * @param wiki   A {@link Wiki} object to use for requesting the data.
     * @param module The name of a module on the wiki, with or without the "Module:"
     *               prefix, e.g. "Language" or "Module:Language".
     * @param code   The code to parse from the module, or "p" if null. Some
     *               examples are "p" or "p.functionName(parameters)"
     * @return A {@link Scribunto} response object containing the response JSON,
     *         what was printed to the Scribunto console with {@code mw.log}, and
     *         the returned JSON-encoded string. This returns a blank object if the
     *         module does not exist or the response is not in the form of a JSON
     *         object.
     */
    public static Scribunto runScribuntoCode(Wiki wiki, String module, String code) {
        if (code == null)
            code = "p";
        module = prefixModule(module);
        if (!wiki.exists(module))
            return new Scribunto();
        Response response = wiki.basicPOST("scribunto-console", FL.pMap("title", module, "question",
                "=mw.text.jsonEncode(" + code + ")", "content", wiki.getPageText(module)));
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
     * Parses and returns the JSON-encoded string provided by the Scribunto console
     * output,. If the Scribunto console did not return a proper response, this
     * function logs an error message and returns null.
     *
     * @return The JSON from the Scribunto console, or null on error.
     * @see Scribunto#runScribuntoCode(Wiki, String, String)
     */
    public JsonElement getReturnJson() {
        if (this.responseJson == null)
            return null;
        if (this.responseJson.getAsJsonPrimitive("type").getAsString().equals("normal")) {
            return JsonParser.parseString(this.responseJson.getAsJsonPrimitive("return").getAsString());
        } else {
            LOGGER.error("Scribunto Console API returned a non-normal result:\n{}", GSONP.gsonPP.toJson(responseJson));
            return null;
        }
    }

    public String getPrint() {
        return this.print;
    }

    public JsonObject getResponseJson() {
        return this.responseJson;
    }
}
