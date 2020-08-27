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

    public static Scribunto runScribuntoCode(Wiki wiki, String module, String code) {
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

    public JsonElement getReturnJson() {
        if (this.responseJson.getAsJsonPrimitive("type").getAsString().equals("normal")) {
            return JsonParser.parseString(this.responseJson.getAsJsonPrimitive("return").getAsString());
        } else {
            System.err.println("Scribunto Console API returned a non-normal result:\n" + GSONP.gsonPP.toJson(responseJson));
            return null;
        }
    }
}
