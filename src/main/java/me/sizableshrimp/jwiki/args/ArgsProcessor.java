package me.sizableshrimp.jwiki.args;

import me.sizableshrimp.jwiki.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ArgsProcessor {
    private ArgsProcessor() {}

    public static Args input() {
        return input(Main.SCANNER);
    }

    public static Args input(Scanner scanner) {
        return process(scanner.nextLine());
    }

    public static Args process(String line) {
        if (line == null || line.isBlank())
            return null;

        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean quote = false;
        boolean escaped = false;
        char[] chars = line.toCharArray();
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
