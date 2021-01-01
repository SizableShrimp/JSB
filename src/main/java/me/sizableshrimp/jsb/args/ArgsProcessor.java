package me.sizableshrimp.jsb.args;

import me.sizableshrimp.jsb.Bot;

import java.util.ArrayList;
import java.util.List;

public class ArgsProcessor {
    private ArgsProcessor() {}

    public static Args processWithPrefix(String data) {
        String prefix = Bot.getConfig().getPrefix();
        if (data == null || data.isBlank() || !data.startsWith(prefix))
            return null;

        data = data.substring(prefix.length());
        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean quote = false;
        boolean escaped = false;
        char[] chars = data.toCharArray();
        for (char c : chars) {
            boolean temp = false;
            if (c == '"' && !escaped) {
                quote = !quote;
            } else if (c == '\\') {
                if (escaped) {
                    current.append(c);
                } else {
                    temp = true;
                }
            } else if (c != ' ' || quote) {
                // Append if not a space or still append space if inside a quote
                current.append(c);
            } else {
                if (current.length() != 0)
                    list.add(current.toString());
                current = new StringBuilder();
            }

            escaped = temp;
        }
        if (current.length() != 0)
            list.add(current.toString()); // Add last arg

        return new Args(list.remove(0).toLowerCase(), list.toArray(String[]::new));
    }
}
