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

package me.sizableshrimp.jsb.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NoPermissionException extends Exception {
    private static final Set<Character> VOWELS = Set.of('a', 'e', 'i', 'o', 'u');
    private final List<String> roles;

    public NoPermissionException(List<String> roles) {
        super(format(roles));
        this.roles = roles;
    }

    public List<String> getRoles() {
        return roles;
    }

    private static String format(List<String> roles) {
        List<String> formatted = new ArrayList<>();

        for (int i = 0; i < roles.size(); i++) {
            String role = roles.get(i);
            String article = VOWELS.contains(Character.toLowerCase(role.charAt(0))) ? "an" : "a";
            if (i == roles.size() - 1 && i > 0) {
                article = "and " + article;
            }
            formatted.add(article + " `" + role + "`");
        }

        String joined = formatted.size() == 2
                ? formatted.get(0) + " " + formatted.get(1)
                : String.join(", ", formatted);
        return String.format("You must be %s to execute this command!", joined);
    }
}
