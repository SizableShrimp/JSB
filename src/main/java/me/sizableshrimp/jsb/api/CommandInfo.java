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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CommandInfo {
    private final String name;
    private final Set<String> aliases;
    private final List<String> requiredRoles;
    private final String usage;
    private final String description;

    public CommandInfo(Command command, String usage, String description) {
        this.name = command.getName();
        this.aliases = command.getAliases();
        this.requiredRoles = command.getRequiredRoles();
        this.usage = usage;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public Set<String> getAllNames() {
        Set<String> set = new HashSet<>(aliases);
        set.add(name);
        return Collections.unmodifiableSet(set);
    }

    public List<String> getRequiredRoles() {
        return requiredRoles;
    }

    public String getUsage(String commandName) {
        return usage.replace("%cmdname%", commandName);
    }

    public String getDescription(String prefix) {
        return description.replace("%prefix%", prefix);
    }
}
