// package me.sizableshrimp.jsb.commands;
//
// import com.google.gson.JsonObject;
// import me.sizableshrimp.jsb.WikiUtil;
// import me.sizableshrimp.jsb.api.Command;
// import me.sizableshrimp.jsb.args.Args;
// import me.sizableshrimp.jsb.args.ArgsProcessor;
// import me.sizableshrimp.jsb.data.Help;
// import me.sizableshrimp.jsb.data.Language;
// import me.sizableshrimp.jsb.data.Mod;
// import org.fastily.jwiki.core.WQuery;
// import org.fastily.jwiki.core.WQuery.QTemplate;
// import org.fastily.jwiki.core.Wiki;
// import org.fastily.jwiki.util.FL;
// import org.fastily.jwiki.util.GSONP;
// import org.fastily.jwiki.util.Tuple;
//
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.util.List;
// import java.util.Objects;
// import java.util.Set;
// import java.util.concurrent.ForkJoinPool;
// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.stream.Collectors;
//
// public class GT6FixCommand extends Command {
//     private static final QTemplate OREDICTSEARCH = new QTemplate(FL.pMap("list", "oredictsearch"),
//             "odlimit", "oredictsearch");
//     private static final QTemplate LISTTILES = new QTemplate(FL.pMap("list", "tiles"),
//             "tslimit", "tiles");
//     private static final QTemplate TILEUSAGES = new QTemplate(FL.pMap("list", "tileusages", "tsnamespace", "*"),
//             "tslimit", "tileusages");
//     private static final QTemplate SEARCH = new QTemplate(FL.pMap("list", "search"),
//             "srlimit", "search");
//
//     public GT6FixCommand(Wiki wiki) {
//         super(wiki);
//     }
//
//     @Override
//     public String getName() {
//         return "gt6fix";
//     }
//
//     @Override
//     public List<String> getAliases() {
//         return List.of();
//     }
//
//     @Override
//     public Help getHelp() {
//         return new Help(this, "Update all pages that use the tiles in GT6's alternate tilesheets to use the main one.");
//     }
//
//     @Override
//     public void run(Args args) throws IOException {
//         Mod mod = Mod.getMod(wiki, "GT6");
//         if (mod == null) {
//             System.out.println("GT6 mod is invalid.");
//             return;
//         }
//         Set<Mod> alts = Set.of(Mod.getMod(wiki, "GT6-B"), Mod.getMod(wiki, "GT6-I"),
//                 Mod.getMod(wiki, "GT6-O"), Mod.getMod(wiki, "GT6-T"));
//         System.out.println(mod);
//         System.out.println(alts);
//         // List<JsonObject> tiles = alts.stream()
//         //         .map(this::getModTiles)
//         //         .flatMap(List::stream)
//         //         .collect(Collectors.toList());
//         // System.out.println(tiles.size());
//         Set<String> usages = alts.stream()
//                 .map(m -> new WQuery(wiki, SEARCH).set("srsearch", m.getAbbrv()))
//                 .flatMap(query -> WikiUtil.getQueryRepliesAsList(query, "search").stream())
//                 .map(json -> json.getAsJsonPrimitive("title").getAsString())
//                 .map(title -> Language.flattenTranslationPage(wiki, title))
//                 .collect(Collectors.toSet());
//         System.out.println(usages.size());
//
//         AtomicInteger counter = new AtomicInteger(1);
//         Set<Tuple<String, Tuple<String, String>>> context = new ForkJoinPool(26)
//                 .submit(() -> usages.stream()
//                         .parallel()
//                         .map(title -> {
//                             System.out.println(counter.getAndIncrement());
//                             String originalPageText = "";
//                             boolean valid;
//                             do {
//                                 try {
//                                     originalPageText = wiki.getPageText(title);
//                                     valid = true;
//                                 } catch (Exception e) {
//                                     e.printStackTrace();
//                                     valid = false;
//                                 }
//                             } while (!valid);
//                             String replacedPageText = originalPageText.replaceAll("GT6-[BIOT]", "GT6");
//
//                             if (!originalPageText.equals(replacedPageText)) {
//                                 return new Tuple<>(title, new Tuple<>(originalPageText, replacedPageText));
//                             }
//
//                             return null;
//                         }).filter(Objects::nonNull)
//                         .collect(Collectors.toSet()))
//                 .join();
//
//         System.out.println(context.size());
//
//         for (var tuple : context) {
//             String title = tuple.x;
//             String originalPageText = tuple.y.x;
//             String replacedPageText = tuple.y.y;
//             int diff = replacedPageText.length() - originalPageText.length();
//             System.out.println("Do you want to update references on page " + title + " of GT6-[BIOT] to GT6? (" + diff + ") Y/N");
//             loadDiff(originalPageText, replacedPageText);
//             if (isYes(ArgsProcessor.input())) {
//                 boolean success = wiki.edit(title, replacedPageText, "Updating " + mod.getName() + " tiles");
//                 if (success) {
//                     System.out.println("Page edited successfully");
//                 } else {
//                     System.out.println("Something went wrong.");
//                 }
//             }
//         }
//
//         System.out.println("done");
//     }
//
//     private void loadDiff(String left, String right) throws IOException {
//         Path folder = Path.of("test");
//         if (!Files.exists(folder))
//             Files.createDirectory(folder);
//         Path leftPath = folder.resolve("left.txt");
//         Path rightPath = folder.resolve("right.txt");
//         Files.writeString(leftPath, left);
//         Files.writeString(rightPath, right);
//
//         Runtime.getRuntime().exec("C:\\Program Files (x86)\\WinMerge\\WinMergeU.exe test/left.txt test/right.txt");
//     }
//
//     private boolean isYes(Args args) {
//         return args != null && args.getFirst().equalsIgnoreCase("y");
//     }
//
//     //private void editTile(int tileId, String newName) throws IOException {
//     //    System.out.println(wiki.basicPOST("edittile", FL.pMap("tstoken", wiki.getConfig().getToken(),
//     //            "tsid", Integer.toString(tileId), "tstoname", newName)).body().string());
//     //}
//
//     private List<JsonObject> getModTiles(Mod mod) {
//         WQuery queryListTiles = new WQuery(wiki, -1, LISTTILES);
//         queryListTiles.set("tsmod", mod.getAbbrv());
//         return WikiUtil.getQueryRepliesAsList(queryListTiles, "tiles");
//     }
//
//     private Set<String> getTileUsages(JsonObject tile) {
//         // System.out.println(tile.getAsJsonPrimitive("name").getAsString());
//         return getTileUsages(tile.getAsJsonPrimitive("id").getAsInt());
//     }
//
//     private Set<String> getTileUsages(int tileId) {
//         WQuery queryTileUsages = new WQuery(wiki, -1, TILEUSAGES);
//         queryTileUsages.set("tstile", Integer.toString(tileId));
//         return WikiUtil.getQueryRepliesAsList(queryTileUsages, "tileusages")
//                 .stream()
//                 .map(json -> GSONP.getStr(json, "title"))
//                 .collect(Collectors.toSet());
//     }
// }
