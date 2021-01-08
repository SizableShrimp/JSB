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

package me.sizableshrimp.jsb.commands.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.jsb.api.AbstractCommand;
import me.sizableshrimp.jsb.api.CommandContext;
import me.sizableshrimp.jsb.api.CommandInfo;
import me.sizableshrimp.jsb.args.Args;
import me.sizableshrimp.jsb.data.Scribunto;
import org.fastily.jwiki.util.GSONP;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

public class ScribuntoCommand extends AbstractCommand {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public CommandInfo getInfo() {
        return new CommandInfo(this, "%cmdname% <module name> <code>", """
                Allows executing custom code from Scribunto modules.
                The module name should be the name of the module, with or without the `Module:` namespace.
                If the module name has a space in it, the name should be wrapped in quotes.
                The code should not be wrapped in a code block and should be code that could run in a module's console space.
                Editor only.
                """);
    }

    @Override
    public String getName() {
        return "scribunto";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("scrib", "lua");
    }

    @Override
    public List<String> getRequiredRoles() {
        return List.of("Editor");
    }

    @Override
    public Mono<Message> run(CommandContext context, MessageCreateEvent event, Args args) {
        if (args.getLength() < 2) {
            return incorrectUsage(event);
        }

        return event.getMessage().getChannel().flatMap(channel -> {
            String module = args.getArg(0);
            String codeToExecute = event.getMessage().getContent();
            codeToExecute = codeToExecute.substring(codeToExecute.indexOf(' ', codeToExecute.indexOf(module) + module.length()) + 1).trim();
            Scribunto scrib = Scribunto.runScribuntoCode(context.getWiki(), module, codeToExecute);
            JsonObject json = scrib.getResponseJson();
            if (json == null) {
                return sendMessage("`" + module + "` does not exist", channel);
            }

            String type = GSONP.getStr(json, "type");
            if ("normal".equals(type)) {
                String print = GSONP.getStr(json, "print");
                String ret = GSONP.getStr(json, "return");

                StringBuilder builder = new StringBuilder();
                if (print != null && !print.isEmpty()) {
                    // Print values returned from the API should already have \n appended.
                    builder.append("Logs:\n").append(print);
                }
                if (ret != null && !ret.isEmpty()) {
                    builder.append("Returned value: ").append(ret);
                } else {
                    builder.append("No value was returned from the module.");
                }
                return sendMessage("```lua\n" + builder.toString() + "```", channel);
            } else if ("error".equals(type)) {
                return sendMessage("An error occurred: " + printJson(json), channel);
            } else if (type == null) {
                return sendMessage("Type was null: " + printJson(json), channel);
            } else {
                return sendMessage("Unknown type value \"" + type + "\" for JSON: " + printJson(json), channel);
            }
        });
    }

    private String printJson(JsonObject json) {
        json = json.deepCopy();
        json.entrySet().removeIf(entry -> entry.getKey().startsWith("session"));
        return "```json\n" + GSON.toJson(json) + "```";
    }
}
