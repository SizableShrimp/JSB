package me.sizableshrimp.jwiki;

import com.google.gson.JsonElement;
import me.sizableshrimp.jwiki.args.Args;
import me.sizableshrimp.jwiki.args.ArgsProcessor;
import me.sizableshrimp.jwiki.commands.Command;
import me.sizableshrimp.jwiki.data.Language;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.Wiki;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Main {
    public static final Scanner SCANNER = new Scanner(System.in);
    private static final Map<String, Command> COMMANDS = new TreeMap<>();
    private static final Wiki WIKI = setup(Path.of("config.json"), false);
    private static final Reflections REFLECTIONS = new Reflections(Command.class.getPackage().getName());
    private static Set<Constructor<Command>> constructors;

    // Examples - https://github.com/fastily/jwiki/wiki/Examples
    public static void main(String[] a) throws IOException {
        if (WIKI == null || WIKI.whoami() == null) {
            System.out.println("Failed to login to MediaWiki");
            return;
        }
        System.out.println("Logged into MediaWiki as user \"" + WIKI.whoami() + "\"");
        System.out.println();

        //System.out.println(WikiUtil.parse(WIKI, null, "[https://google.com oof], stuff here more stuff [https://github.com git]."));

        //System.out.println(WIKI.basicGET("query", "prop", "info", "titles", "UserProfile:SizableShrimp").body().string());
        runConsole("fixdocstart");
        //runConsole("reportaddedby");
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
        if (constructors == null) {
            loadConstructors();
        }
        if (arr == null) {
            // Manual Mode
            while (true) {
                loadCommands(); // Reload instances each time for use in breakpoints
                System.out.print("Enter command: ");
                Args args = ArgsProcessor.input();
                if ("help".equals(args.getName())) {
                    COMMANDS.values().forEach(cmd -> System.out.printf("%s - %s%n", cmd.getName(), cmd.getHelp()));
                    continue;
                }
                executeCmd(args);
            }
        } else {
            // Automatic Mode
            boolean again = infinite;
            do {
                loadCommands(); // Reload instances each time for use in breakpoints
                Arrays.stream(arr)
                        .map(ArgsProcessor::process)
                        .forEach(args -> {
                            System.out.println("Running command \"" + args.getName() + "\"");
                            executeCmd(args);
                        });
                if (infinite)
                    continue;
                System.out.println("Again? [Y/n]");
                again = "y".equalsIgnoreCase(SCANNER.nextLine());
            } while (again);
        }
    }

    /**
     * Setup the wiki instance using a path to the config file and whether to enable jwiki's logger.
     * Note that this logger can be helpful for debugging, but does not integrate with ANY logging APIs.
     * Returns null if the config isn't loaded or the api endpoint is invalid.
     *
     * @param configPath The {@link Path} to the config file.
     * @param defaultLogger Whether to enable the default logger used by jwiki.
     * @return The created {@link Wiki} instance, or null.
     */
    private static Wiki setup(Path configPath, boolean defaultLogger) {
        Config config;
        try {
            config = Config.getConfig(configPath);
        } catch (IOException e) {
            System.err.println("Couldn't load the config.");
            e.printStackTrace();
            return null;
        }

        if (config.getApi() == null) {
            System.err.println("API endpoint is null");
            return null;
        }

        HttpUrl apiEndpoint = HttpUrl.parse(config.getApi());
        if (apiEndpoint == null) {
            System.err.println("Invalid API endpoint: " + config.getApi());
            return null;
        }

        return new Wiki.Builder()
                .withApiEndpoint(apiEndpoint)
                .withLogin(config.getUsername(), config.getPassword())
                .withUserAgent(config.getUserAgent())
                .withDefaultLogger(defaultLogger)
                .build();
    }

    /**
     * Defaults to manual mode.
     */
    private static void runConsole(String... arr) {
        runConsole(false, arr);
    }

    private static void executeCmd(Args args) {
        Command command = COMMANDS.get(args.getName());
        if (command != null)
            command.run(args);
    }

    private static void loadCommands() {
        constructors.stream()
                .map(constructor -> {
                    try {
                        return constructor.newInstance(WIKI);
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .forEach(cmd -> COMMANDS.put(cmd.getName(), cmd));
    }

    private static void loadConstructors() {
        constructors = REFLECTIONS.getSubTypesOf(Command.class).stream()
                .map(clazz -> (Constructor<Command>[]) clazz.getConstructors())
                .filter(arr -> arr.length != 0)
                .map(arr -> arr[0])
                .collect(Collectors.toSet());
    }
}
