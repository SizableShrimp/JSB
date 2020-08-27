package me.sizableshrimp.jwiki.commands;

import com.google.gson.JsonObject;
import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Scribunto;
import me.sizableshrimp.jwiki.data.Help;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.GSONP;

import java.util.List;

public class ScribuntoCommand extends Command {
    public ScribuntoCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "scribunto";
    }

    @Override
    public List<String> getAliases() {
        return List.of("scrib", "lua");
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Allows executing custom code from Scribunto modules.");
    }

    @Override
    public void run(Args args) {
        String module = args.getArgs()[0];
        Scribunto scrib = Scribunto.runScribuntoCode(wiki, module, args.getArgs()[1]);
        if (scrib.getResponseJson() == null) {
            System.out.println(module + " does not exist.");
            return;
        }
        JsonObject json = scrib.getResponseJson();

        String type = GSONP.getStr(json, "type");
        if ("normal".equals(type)) {
            String print = GSONP.getStr(json, "print");
            String ret = GSONP.getStr(json, "return");
            if (print != null && !print.isEmpty()) {
                // Print values returned from the API should have \n appended.
                System.out.print("Logs:\n" + print);
            }
            if (ret != null && !ret.isEmpty()) {
                System.out.println("Returned value: " + ret);
            } else {
                System.out.println("No value was returned from the module.");
            }
        } else if ("error".equals(type)) {
            System.err.println("An error occurred: " + GSONP.gsonPP.toJson(json));
        } else if (type == null) {
            System.err.println("Type was null: " + GSONP.gsonPP.toJson(json));
        } else {
            throw new IllegalStateException("Unknown type value \"" + type + "\" for JSON: " + GSONP.gsonPP.toJson(json));
        }
    }
}
