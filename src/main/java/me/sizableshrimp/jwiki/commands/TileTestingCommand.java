package me.sizableshrimp.jwiki.commands;

import com.google.gson.JsonObject;
import me.sizableshrimp.jwiki.AReply;
import me.sizableshrimp.jwiki.WikiUtil;
import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.data.Help;
import me.sizableshrimp.jwiki.data.Mod;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.core.WQuery.QTemplate;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;
import org.fastily.jwiki.util.GSONP;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TileTestingCommand extends Command {
    private static final QTemplate OREDICTSEARCH = new QTemplate(FL.pMap("list", "oredictsearch"),
            "odlimit", "oredictsearch");
    private static final QTemplate LISTTILES = new QTemplate(FL.pMap("list", "tiles"),
            "tslimit", "tiles");
    private static final QTemplate TILEUSAGES = new QTemplate(FL.pMap("list", "tileusages", "tsnamespace", "*"),
            "tslimit", "tileusages");

    public TileTestingCommand(Wiki wiki) {
        super(wiki);
    }

    @Override
    public String getName() {
        return "tiletesting";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public Help getHelp() {
        return new Help(this, "tile testing");
    }

    @Override
    public void run(Args args) throws IOException {
        Mod mod = Mod.getMod(wiki, "PEX");
        if (mod == null) {
            System.out.println("PEX mod is invalid.");
            return;
        }
        System.out.println(mod);

        System.out.println(getTileUsages(338654));
        // List<JsonObject> tiles = alts.stream()
        //         .map(this::getModTiles)
        //         .flatMap(List::stream)
        //         .collect(Collectors.toList());
        // System.out.println(tiles.size());
        // final int finalSize = tiles.size();
        // Set<String> usages = new ForkJoinPool(20)
        //         .submit(() -> tiles.stream()
        //                 .parallel()
        //                 .map(this::getTileUsages)
        //                 .flatMap(Set::stream)
        //                 .collect(Collectors.toSet()))
        //         .join();
        // System.out.println(usages.size());

        // for (String usage : usages) {
        //     if (!wiki.exists(usage))
        //         return;
        //     String title = WikiUtil.resolveRedirects(wiki, usage);
        //     String originalPageText = wiki.getPageText(title);
        //     System.out.println("Do you want to update references on page " + title + " of GT6-[BIOT] to GT6? Y/N");
        //     if (isYes(ArgsProcessor.input())) {
        //         String replacedPageText = originalPageText.replaceAll("GT6-[BIOT]", "GT6");
        //         if (!originalPageText.equals(replacedPageText))
        //             wiki.edit(title, replacedPageText, "Updating " + mod.getName() + " tiles");
        //     }
        // }
    }

    private boolean isYes(Args args) {
        return args != null && args.getName().equalsIgnoreCase("y");
    }

    private AReply editTile(int tileId, String newName) {
        return AReply.processAction(wiki, "edittile", "tstoken", wiki.getConfig().getToken(),
                "tsid", Integer.toString(tileId), "tstoname", newName);
    }

    private List<JsonObject> getModTiles(Mod mod) {
        WQuery queryListTiles = new WQuery(wiki, -1, LISTTILES);
        queryListTiles.set("tsmod", mod.getAbbrv());
        return WikiUtil.getQueryRepliesAsList(queryListTiles, "tiles");
    }

    private Set<String> getTileUsages(JsonObject tile) {
        // System.out.println(tile.getAsJsonPrimitive("name").getAsString());
        // counter.incrementAndGet();
        return getTileUsages(tile.getAsJsonPrimitive("id").getAsInt());
    }

    private Set<String> getTileUsages(int tileId) {
        WQuery queryTileUsages = new WQuery(wiki, -1, TILEUSAGES);
        queryTileUsages.set("tstile", Integer.toString(tileId));
        return WikiUtil.getQueryRepliesAsList(queryTileUsages, "tileusages")
                .stream()
                .map(json -> GSONP.getStr(json, "title"))
                .collect(Collectors.toSet());
    }
}
