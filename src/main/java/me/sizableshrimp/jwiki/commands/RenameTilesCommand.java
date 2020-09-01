package me.sizableshrimp.jwiki.commands;

import com.google.gson.JsonObject;
import me.sizableshrimp.jwiki.AReply;
import me.sizableshrimp.jwiki.WikiUtil;
import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.args.ArgsProcessor;
import me.sizableshrimp.jwiki.data.Help;
import me.sizableshrimp.jwiki.data.Mod;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.core.WQuery.QReply;
import org.fastily.jwiki.core.WQuery.QTemplate;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;
import org.fastily.jwiki.util.GSONP;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RenameTilesCommand extends Command {
    private static final QTemplate OREDICTSEARCH = new QTemplate(FL.pMap("list", "oredictsearch"),
            "odlimit", "oredictsearch");
    private static final QTemplate LISTTILES = new QTemplate(FL.pMap("list", "tiles"),
            "tslimit", "tiles");
    private static final QTemplate TILEUSAGES = new QTemplate(FL.pMap("list", "tileusages", "tsnamespace", "*"),
            "tslimit", "tileusages");

    public RenameTilesCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "renametiles";
    }

    @Override
    public List<String> getAliases() {
        return List.of("tiles");
    }

    @Override
    public Help getHelp() {
        return new Help(this, "Update all pages that use the tiles in <mod abbrv>.txt and oredict entries, where the first arg is mod data to get mod.");
    }

    @Override
    public void run(Args args) throws IOException {
        if (args.getArgsLength() == 0) {
            System.out.println("You must enter a mod abbreviation or unlocalized name, e.g. CPC or Compact Claustrophobia. Case insensitive");
            return;
        }
        Mod mod = Mod.getMod(wiki, args.getArg(0));
        if (mod == null) {
            System.out.println("The mod you entered is invalid.");
            return;
        }
        Path path = Path.of(mod.getAbbrv() + ".txt");
        if (!Files.exists(path)) {
            System.out.println(path.toFile().getName() + " does not exist.");
            return;
        }
        System.out.println("Do you want to update OreDict entries, pages with tile entries, or page names? O/T/P");
        Decision decision = Decision.process(ArgsProcessor.input());
        if (decision == null) {
            System.out.println("Invalid response, one of O, T, or P.");
            return;
        }
        Set<JsonObject> tiles = getModTiles(mod);
        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            if (line.endsWith("="))
                continue;
            int i = line.indexOf('=');
            String before = line.substring(0, i);
            String after = line.substring(i + 1);

            if (decision == Decision.ORE_DICT) {
                updateOreDict(mod, before, after);
            } else if (decision == Decision.TILES) {
                int tileId = getTileId(tiles, after); // Use the updated name to query tile
                if (tileId == -1) {
                    System.err.println("Tile with name \"" + after + "\" does not exist in mod " + mod.getAbbrv() + ".");
                    continue;
                }
                //TODO add tile stuff here
            } else if (decision == Decision.PAGES) {
                updatePage(mod, before, after);
            }
            //editTile(tileId, before);
            //Set<String> tileUsages = getTileUsages(tileId);
            //editTile(tileId, after);
            //System.out.println(after + " - " + tileUsages);
        }
    }

    private void updatePage(Mod mod, String before, String after) {
        if (!wiki.exists(before))
            return;
        String title = WikiUtil.resolveRedirects(wiki, before);
        String originalPageText = wiki.getPageText(title);
        if (originalPageText.replace(after, "").contains(before)) { // Account for cyclic replacement and stop it
            System.out.println("Do you want to update references on page " + title + " of \"" + before + "\" to \"" + after + "\"? Y/N");
            if (isYes(ArgsProcessor.input())) {
                String replacedPageText = originalPageText.replace(before, after);
                if (!originalPageText.equals(replacedPageText))
                    wiki.edit(title, replacedPageText, "Updating " + mod.getName().toLowerCase() + " tiles");
            }
        }
        if (before.equalsIgnoreCase(title)) {
            System.out.println("Do you want to move page \"" + title + "\" to \"" + after + "\"? Y/N");
            if (isYes(ArgsProcessor.input())) {
                String reason = "Updating " + mod.getName().toLowerCase() + " page names";
                AReply reply = WikiUtil.move(wiki, title, after, reason, true, true, false);
                if ("articleexists".equals(reply.getErrorCode())) {
                    System.err.println("The new page name already exists (page conflict).");
                    System.out.println(title + "\n-------------------------------");
                    System.out.println(wiki.getPageText(title));
                    System.out.println("\n\n" + after + "\n-------------------------------");
                    System.out.println(wiki.getPageText(after));
                    System.out.println("Do you wish to redirect to the newer page (R), overwrite the newer page (O), or cancel (N)? R/O/N");
                    String switchDecision = getSwitchDecision(ArgsProcessor.input());
                    if ("r".equalsIgnoreCase(switchDecision)) {
                        boolean result = wiki.edit(title, "#REDIRECT [[" + after + "]]", "Redirect");
                        if (result) {
                            System.out.println("Successfully redirected.");
                        } else {
                            System.out.println("An error occurred.");
                        }
                    } else if ("o".equalsIgnoreCase(switchDecision)) {
                        boolean result = wiki.delete(after, "Deleted to make way for move from \"[[" + title + "]]\"");
                        result = result && WikiUtil.move(wiki, title, after, "Overwrite page", true, true, false).getType() == AReply.Type.SUCCESS;
                        if (result) {
                            System.out.println("Successfully overwritten.");
                        } else {
                            System.out.println("An error occurred.");
                        }
                    }
                } else if (reply.getType() == AReply.Type.SUCCESS) {
                    System.out.println("Successfully moved.");
                } else {
                    System.out.println("An error occurred.");
                }
            }
        }
    }

    private boolean isYes(Args args) {
        return args != null && args.getName().equalsIgnoreCase("y");
    }

    private String getSwitchDecision(Args args) {
        return args == null ? "" : args.getName();
    }

    private void editTile(int tileId, String newName) throws IOException {
        System.out.println(wiki.basicPOST("edittile", FL.pMap("tstoken", wiki.getConfig().getToken(),
                "tsid", Integer.toString(tileId), "tstoname", newName)).body().string());
    }

    private Set<JsonObject> getModTiles(Mod mod) {
        WQuery queryListTiles = new WQuery(wiki, -1, LISTTILES);
        queryListTiles.set("tsmod", mod.getAbbrv());
        return getQueryReplies(queryListTiles).stream()
                .flatMap(reply -> reply.listComp("tiles").stream())
                .collect(Collectors.toSet());
    }

    private Set<String> getTileUsages(int tileId) {
        WQuery queryTileUsages = new WQuery(wiki, -1, TILEUSAGES);
        queryTileUsages.set("tstile", Integer.toString(tileId));
        return getQueryReplies(queryTileUsages).stream()
                .flatMap(reply -> reply.listComp("tileusages").stream())
                .map(json -> GSONP.getStr(json, "title"))
                .collect(Collectors.toSet());
    }

    private int getTileId(Set<JsonObject> tiles, String name) {
        return tiles.stream()
                .filter(json -> name.equals(GSONP.getStr(json, "name")))
                .findFirst()
                .map(json -> json.getAsJsonPrimitive("id").getAsInt())
                .orElse(-1);
    }

    private void updateOreDict(Mod mod, String before, String after) {
        WQuery queryOreDict = new WQuery(wiki, -1, OREDICTSEARCH);
        queryOreDict.set("odmod", mod.getAbbrv());
        queryOreDict.set("odname", before);
        List<QReply> replies = getQueryReplies(queryOreDict);
        for (QReply reply : replies) {
            List<JsonObject> entries = reply.listComp("oredictentries");
            for (JsonObject json : entries) {
                //System.out.println(json);
                int id = json.getAsJsonPrimitive("id").getAsInt();
                String tagName = GSONP.getStr(json, "tag_name");
                String gridParams = GSONP.getStr(json, "grid_params");
                // OreDict extension requires all parameters to be set, otherwise omitted fields are set to be empty. (Santa!!!)
                HashMap<String, String> params = FL.pMap("odtoken", wiki.getConfig().getToken(), "odid", Integer.toString(id),
                        "odtag", tagName, "oditem", after, "odmod", mod.getAbbrv(), "odparams", gridParams);
                AReply actionReply = AReply.processAction(wiki, "editoredict", params);
                if (actionReply.getFullJson().has("edit")) {
                    System.out.println("Updated " + before + " to " + after + ".");
                } else if (actionReply.getType() == AReply.Type.ERROR) {
                    System.err.println(actionReply.getData());
                }
            }
        }
    }

    private List<QReply> getQueryReplies(WQuery query) {
        List<QReply> list = new ArrayList<>();

        while (query.has()) {
            list.add(query.next());
        }

        return list;
    }

    private enum Decision {
        ORE_DICT,
        TILES,
        PAGES;

        private static Decision process(Args args) {
            if (args == null || args.getName() == null)
                return null;

            String str = args.getName();
            if (str.equalsIgnoreCase("o")) {
                return ORE_DICT;
            } else if (str.equalsIgnoreCase("t")) {
                return TILES;
            } else if (str.equalsIgnoreCase("p")) {
                return PAGES;
            }

            return null;
        }
    }
}
