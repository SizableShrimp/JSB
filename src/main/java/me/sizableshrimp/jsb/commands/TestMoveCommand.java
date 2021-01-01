// package me.sizableshrimp.jsb.commands;
//
// import me.sizableshrimp.jsb.api.Command;
// import me.sizableshrimp.jsb.api.DisabledCommand;
// import me.sizableshrimp.jsb.args.Args;
// import me.sizableshrimp.jsb.data.Help;
// import org.fastily.jwiki.core.Wiki;
//
// @DisabledCommand
// public class TestMoveCommand extends Command {
//     public TestMoveCommand(Wiki wiki) {
//         super(wiki);
//     }
//
//     @Override
//     public String getName() {
//         return "testmove";
//     }
//
//     @Override
//     public Help getHelp() {
//         return new Help(this, "Test a move");
//     }
//
//     @Override
//     public void run(Args args) {
//         if (args.getArgs().length != 2) {
//             System.out.println("Invalid Usage: testmove \"Namespace:Page/Test One\" Namespace:Page/Two");
//         }
//         //ArrayList<String> redirects = wiki.allPages("Tilesheet", true, false, -1, NS.FILE);
//         //wiki.prefixIndex(NS.FILE, "Tilesheet").stream()
//         //        .filter(title -> !title.endsWith(" 0.png"))
//         //        .filter(title -> !redirects.contains(title))
//         //        .filter(title -> title.matches("File:Tilesheet [\\w\\d-]+ \\d+\\.png"))
//         //        .forEach(title -> wiki.basicPOST("move", FL.pMap("")));
//     }
// }
