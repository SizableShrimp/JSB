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
    private final String name;
    private final String[] args;
    private final String joinedArgs;

    Args(String name, String[] args) {
        this.name = name;
        this.args = args;
        this.joinedArgs = String.join(" ", args);
    }

    public String getArg(int index) {
        return args[index];
    }

    public String getArgNullable(int index) {
        if (index < 0 || index >= getLength())
            return null;
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

    /**
     * Uses {@link #getJoinedArgs()} as a base string. Starting at the start of the
     * string, skips {@code spaces} number of spaces and then returns the remaining
     * substring from the index to the end of the string.
     *
     * @param spaces A number 0 or greater where 0 means the whole string.
     * @return a substring starting from the index after {@code spaces} number of
     *         spaces has been passed from index 0 and continuing to the end of the
     *         string.
     */
    public String getAfterSpace(int spaces) {
        int result = 0;

        for (int i = 0; i < spaces; i++) {
            result = joinedArgs.indexOf(' ', result);
            if (result == -1)
                throw new IllegalStateException("Too many spaces");
            result += 1;
        }

        return joinedArgs.substring(result);
    }

    /**
     * Uses {@link #getJoinedArgs()} as a base string. Starting at the end of the
     * string, skips {@code spaces} number of spaces and then returns the remaining
     * substring from the index to the end of the string.
     *
     * @param spaces A number 0 or greater where 0 means an empty string.
     * @return a substring starting from the index after {@code spaces} number of
     *         spaces has been passed from the right end of the string and
     *         continuing to the end of the string.
     */
    public String getBeforeSpace(int spaces) {
        int result = joinedArgs.length();

        for (int i = 0; i < spaces; i++) {
            result = joinedArgs.lastIndexOf(' ', result - 1);
            if (result == -1)
                throw new IllegalStateException("Too many spaces");
        }

        return joinedArgs.substring(result);
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

    /**
     * Gets all arguments joined by a space, excluding the {@code name}.
     *
     * @return all arguments joined by a space, excluding the {@code name}.
     */
    public String getJoinedArgs() {
        return joinedArgs;
    }
}
