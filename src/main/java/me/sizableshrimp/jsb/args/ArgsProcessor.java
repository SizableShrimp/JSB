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

package me.sizableshrimp.jsb.args;

import me.sizableshrimp.jsb.Bot;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgsProcessor {
    private static final Pattern SPACES = Pattern.compile("\\s+");

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
        data = data.trim();
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
        char[] chars = SPACES.matcher(data.trim()).replaceAll(" ").toCharArray();
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
            } else if (c == '\u200B') {
                // Don't add zero-width spaces; they may have come from the user copying messages from us.
            } else if (c != ' ' || quote) {
                // Append if not a space or still append space if inside a quote
                current.append(c);
            } else {
                list.add(current.toString().trim());
                current = new StringBuilder();
            }
        }
        list.add(current.toString().trim()); // Add last arg

        return new Args(list.remove(0).toLowerCase(), list.toArray(String[]::new));
    }
}
