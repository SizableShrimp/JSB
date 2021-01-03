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

package me.sizableshrimp.jsb;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.sizableshrimp.jsb.util.WikiUtil;
import okhttp3.Response;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;
import org.fastily.jwiki.util.GSONP;

import java.util.HashMap;
import java.util.Map;

public final class AReply {
    private final Type type;
    private final String data;
    private final JsonObject fullJson;
    private final JsonElement json;

    public AReply(Type type, String data, JsonObject fullJson, JsonElement json) {
        this.type = type;
        this.data = data;
        this.fullJson = fullJson;
        this.json = json;
    }

    public static AReply processAction(Wiki wiki, String action, String... formData) {
        return processAction(action, wiki.basicPOST(action, FL.pMap(formData)));
    }

    public static AReply processAction(Wiki wiki, String action, Map<String, String> formData) {
        // This lib sucks, requires HashMap
        return processAction(action, wiki.basicPOST(action, new HashMap<>(formData)));
    }

    public static AReply processAction(String action, Response response) {
        JsonObject json = WikiUtil.jsonFromResponse(response).getAsJsonObject();
        return processAction(action, json);
    }

    public static AReply processAction(String action, JsonObject json) {
        if (json.has("error")) {
            JsonObject error = json.getAsJsonObject("error");
            String code = GSONP.getStr(error, "code");
            String info = GSONP.getStr(error, "info");
            return new AReply(Type.ERROR, String.format("%s - %s", code, info), json, error);
        } else if (json.has(action)) {
            JsonElement jsonAction = json.get(action);
            return new AReply(Type.SUCCESS, jsonAction.toString(), json, jsonAction);
        }

        return new AReply(Type.UNKNOWN, json.toString(), json, json);
    }

    public String getErrorCode() {
        if (type != Type.ERROR)
            return null;

        return GSONP.getStr(getJson().getAsJsonObject(), "code");
    }

    public Type getType() {
        return this.type;
    }

    public String getData() {
        return this.data;
    }

    public JsonObject getFullJson() {
        return this.fullJson;
    }

    public JsonElement getJson() {
        return this.json;
    }

    public enum Type {
        SUCCESS,
        ERROR,
        UNKNOWN
    }
}