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
    public static final String NO_BOOLEAN_MESSAGE = "The **%s** parameter should be one of `true`, `t`, or `1` for **true** or one of `false`, `f`, or `0` for **false**.";
    private final String name;
    private final String[] args;
    private final String joinedArgs;

    Args(String name, String[] args) {
        this.name = name;
        this.args = args;
        this.joinedArgs = String.join(" ", args);
    }

    public String getArg(int index) {
        return this.args[index];
    }

    public String getArgNullable(int index) {
        if (index < 0 || index >= getLength())
            return null;
        return this.args[index];
    }

    public boolean isArgValidBoolean(int index) {
        String arg = getArgNullable(index);
        if (arg == null)
            return false;
        return isTrue(arg) || isFalse(arg);
    }

    public boolean getArgAsBoolean(int index) {
        return isTrue(getArg(index));
    }

    public boolean getNullableArgAsBoolean(int index) {
        return isTrue(getArgNullable(index));
    }

    private static boolean isTrue(String arg) {
        return "true".equalsIgnoreCase(arg) || "t".equalsIgnoreCase(arg) || "1".equalsIgnoreCase(arg);
    }

    private static boolean isFalse(String arg) {
        return "false".equalsIgnoreCase(arg) || "f".equalsIgnoreCase(arg) || "0".equalsIgnoreCase(arg);
    }

    public Integer getArgAsInteger(int index) {
        try {
            return Integer.parseInt(getArg(index));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getNullableArgAsInteger(int index) {
        String arg = getArgNullable(index);
        if (arg == null)
            return null;
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    /**
     * Gets all arguments in a range joined by a space from {@code startInclusive} to the end of the array.
     *
     * @param startInclusive A 0-based array index. Must be smaller than the length of the arguments.
     * @return
     */
    public String getArgRange(int startInclusive) {
        return getArgRange(startInclusive, this.args.length);
    }

    /**
     * Gets all arguments in a range joined by a space using 0-based indices.
     *
     * @param startInclusive A 0-based array index. Must be smaller than {@code endExclusive} but at least 0.
     * @param endExclusive A 0-based array index. Must be larger or equal to {@code startInclusive} but at least 0.
     * @return All arguments in this range joined by a space.
     */
    public String getArgRange(int startInclusive, int endExclusive) {
        return String.join(" ", Arrays.copyOfRange(this.args, startInclusive, endExclusive));
    }

    /**
     * Uses {@link #getJoinedArgs()} as a base string. Starting at the start of the
     * string, skips {@code spaces} number of spaces and then returns the remaining
     * substring from the index to the end of the string.
     *
     * @param spaces A number 0 or greater where 0 means the whole string.
     * @return a substring starting from the index after {@code spaces} number of
     * spaces has been passed from index 0 and continuing to the end of the
     * string.
     */
    public String getAfterSpace(int spaces) {
        int result = 0;

        for (int i = 0; i < spaces; i++) {
            result = this.joinedArgs.indexOf(' ', result);
            if (result == -1)
                throw new IllegalStateException("Too many spaces");
            result += 1;
        }

        return this.joinedArgs.substring(result);
    }

    /**
     * Uses {@link #getJoinedArgs()} as a base string. Starting at the end of the
     * string, skips {@code spaces} number of spaces and then returns the remaining
     * substring from the index to the end of the string.
     *
     * @param spaces A number 0 or greater where 0 means an empty string.
     * @return a substring starting from the index after {@code spaces} number of
     * spaces has been passed from the right end of the string and
     * continuing to the end of the string.
     */
    public String getBeforeSpace(int spaces) {
        int result = this.joinedArgs.length();

        for (int i = 0; i < spaces; i++) {
            result = this.joinedArgs.lastIndexOf(' ', result - 1);
            if (result == -1)
                throw new IllegalStateException("Too many spaces");
        }

        return this.joinedArgs.substring(result);
    }

    /**
     * Get the length of the arguments, excluding the {@code name}.
     *
     * @return The length of the arguments, excluding the {@code name}.
     */
    public int getLength() {
        return this.args.length;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Gets all arguments joined by a space, excluding the {@code name}.
     *
     * @return all arguments joined by a space, excluding the {@code name}.
     */
    public String getJoinedArgs() {
        return this.joinedArgs;
    }
}
