package me.sizableshrimp.jwiki;

import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.args.ArgsProcessor;
import me.sizableshrimp.jwiki.commands.Command;
import me.sizableshrimp.jwiki.commands.manager.CommandManager;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    private static final WQuery.QTemplate SITEINFO = new WQuery.QTemplate(FL.pMap("action", "query", "meta", "siteinfo", "siprop", "general"), "query");
    public static final Scanner SCANNER = new Scanner(System.in);
    public static final Config CONFIG = createConfig(Path.of("config.json"));
    private static final Wiki WIKI = setup(CONFIG, false);
    public static final CommandManager COMMAND_MANAGER = new CommandManager(WIKI);

    // Examples - https://github.com/fastily/jwiki/wiki/Examples
    public static void main(String[] args) throws MalformedURLException, InterruptedException {
        if (WIKI == null || WIKI.whoami() == null) {
            System.out.println("Failed to login to MediaWiki");
            return;
        }
        System.out.println("Logged into MediaWiki as user \"" + WIKI.whoami() + "\"");
        System.out.println();

        //System.out.println(WikiUtil.parse(WIKI, null, "[https://google.com oof], stuff here more stuff [https://github.com git]."));
        //System.out.println(WIKI.basicGET("query", "prop", "info", "titles", "UserProfile:SizableShrimp").body().string());

        //List<String> titles = WIKI.prefixIndex("Template:Doc/Start/");
        //String pageText = WIKI.getPageText("Template:Doc/End/en");
        //for (String title : titles) {
        //    title = "Template:Doc/End/" + title.substring(title.lastIndexOf('/') + 1);
        //    System.out.println(title);
        //    if (WIKI.exists(title))
        //        continue;
        //    System.out.println(WIKI.edit(title, pageText, "Created page with \"Template:Doc/End\""));
        //}
        runConsole();
    }

    /**
     * Defaults to manual mode.
     */
    private static void runConsole(String... arr) {
        runConsole(false, arr);
    }

    /**
     * Run a console for help with executing commands (objects extending {@link Command}). There are a few different modes.
     * <br />
     * If {@code arr} is null, enter manual mode. This allows the user to enter commands using {@link System#in}, including the "help" command to list commands.
     * This will continue asking for input until the user uses "exit".
     * <br />
     * Otherwise if {@code arr} has entries, enter automatic mode. This continually parses and repeats the commands input from {@code arr}.
     * How this is repeated depends on the value of {@code infinite}. If infinite is true, repeat automatically with a breakpoint on the continue line.
     * Otherwise, ask the user if they would like to repeat the commands again.
     *
     * @param infinite A value that determines how to repeat when in automatic mode.
     * @param arr Used for automatic mode and parsing commands, determines what mode the method runs in.
     */
    private static void runConsole(boolean infinite, String... arr) {
        if (arr == null || arr.length == 0) {
            // Manual Mode
            while (true) {
                COMMAND_MANAGER.loadCommands(); // Reload instances each time for use in breakpoints
                System.out.print("Enter command: ");
                Args args = ArgsProcessor.input();
                COMMAND_MANAGER.executeCmd(args);
            }
        } else {
            // Automatic Mode
            boolean again = infinite;
            do {
                COMMAND_MANAGER.loadCommands(); // Reload instances each time for use in breakpoints
                Arrays.stream(arr)
                        .map(ArgsProcessor::process)
                        .forEach(args -> {
                            System.out.println("Running command \"" + args.getName() + "\"");
                            COMMAND_MANAGER.executeCmd(args);
                        });
                if (infinite)
                    continue;
                System.out.println("Again? [Y/n]");
                again = "y".equalsIgnoreCase(SCANNER.nextLine());
            } while (again);
        }
    }

    private static Config createConfig(Path configPath) {
        try {
            return Config.getConfig(configPath);
        } catch (IOException e) {
            System.err.println("Couldn't load the config.");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Setup the wiki instance using a {@link Config} instance and whether to enable jwiki's logger.
     * Note that this logger can be helpful for debugging, but does not integrate with ANY logging APIs.
     * Returns null if the config isn't loaded or the api endpoint is invalid.
     *
     * @param config The {@link Config} instance used to login and configure everything.
     * @param defaultLogger Whether to enable the default logger used by jwiki.
     * @return The created {@link Wiki} instance, or null.
     */
    private static Wiki setup(Config config, boolean defaultLogger) {
        if (config == null || config.getApi() == null) {
            System.err.println("API endpoint is null");
            return null;
        }

        HttpUrl apiEndpoint = HttpUrl.parse(config.getApi());
        if (apiEndpoint == null) {
            System.err.println("Invalid API endpoint: " + config.getApi());
            return null;
        }

        Wiki.Builder builder = new Wiki.Builder();
        if (config.doLogin())
            builder.withLogin(config.getUsername(), config.getPassword());
        return builder
                .withApiEndpoint(apiEndpoint)
                .withUserAgent(config.getUserAgent())
                .withDefaultLogger(defaultLogger)
                .build();
    }
}
