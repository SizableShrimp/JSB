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

import java.util.Arrays;

public final class Args {
    private final String raw;
    private final String name;
    private final String[] args;

    Args(String raw, String name, String[] args) {
        this.raw = raw;
        this.name = name;
        this.args = args;
    }

    public String getArg(int index) {
        return args[index];
    }

    public String getArgRange(int startInclusive) {
        return getArgRange(startInclusive, args.length);
    }

    /**
     * Gets the arguments in a range joined by a space.
     *
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public String getArgRange(int startInclusive, int endExclusive) {
        return String.join(" ", Arrays.copyOfRange(args, startInclusive, endExclusive));
    }

    public String getArgNullable(int index) {
        if (index < 0 || index >= getLength())
            return null;
        return args[index];
    }

    /**
     * Returns the index right after the space after skipping the supplied amount of spaces.
     *
     * @param spaces A number 1 or greater.
     * @return A substring starting from index right after the number of spaces has been passed and going until the end of the string.
     */
    public String getAfterSpace(int spaces) {
        int result = 0;

        for (int i = 0; i < spaces; i++) {
            result = raw.indexOf(' ', result);
            if (result == -1)
                throw new IllegalStateException("Too many spaces");
            result += 1;
        }

        return raw.substring(result);
    }

    public String getBeforeSpace(int spaces) {
        int result = raw.length() - 1;

        for (int i = 0; i < spaces; i++) {
            result = raw.lastIndexOf(' ', result);
            if (result == -1)
                throw new IllegalStateException("Too many spaces");
            result -= 1;
        }

        return raw.substring(result + 2);
    }

    /**
     * Get the length of the arguments, excluding the {@code name}.
     *
     * @return The length of the arguments, excluding the {@code name}.
     */
    public int getLength() {
        return args.length;
    }

    public String getName() {
        return name;
    }

    public String getRaw() {
        return raw;
    }
}
