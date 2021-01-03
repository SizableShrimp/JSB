package me.sizableshrimp.jsb.args;

import me.sizableshrimp.jsb.Bot;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgsProcessor {
    private ArgsProcessor() {}

    public static Args processWithPrefix(String data) {
        return processWithPrefix(Bot.getConfig().getPrefix(), data);
    }

    public static Args processWithPrefix(String prefix, String data) {
        if (data == null || data.isBlank() || !data.startsWith(prefix))
            return null;

        return process(data.substring(prefix.length()));
    }

    public static Args processWithPrefixRegex(Pattern prefixRegex, String data) {
        if (data == null || data.isBlank())
            return null;
        Matcher matcher = prefixRegex.matcher(data);
        if (!matcher.find() || matcher.start() != 0)
            return null;

        return process(data.substring(matcher.end()));
    }

    public static Args process(String data) {
        if (data == null || data.isBlank())
            return null;

        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean quote = false;
        boolean escaped = false;
        char[] chars = data.toCharArray();
        for (char c : chars) {
            if (c == '"' && !escaped) {
                quote = !quote;
            } else if (c == '\\') {
                if (escaped) {
                    current.append(c);
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else if (c != ' ' || quote) {
                // Append if not a space or still append space if inside a quote
                current.append(c);
            } else {
                if (current.length() != 0)
                    list.add(current.toString());
                current = new StringBuilder();
            }
        }
        if (current.length() != 0)
            list.add(current.toString()); // Add last arg

        return new Args(data, list.remove(0).toLowerCase(), list.toArray(String[]::new));
    }
}
