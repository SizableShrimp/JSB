package me.sizableshrimp.jsb.args;

public final class Args {
    private final String name;
    private final String[] args;

    public Args(String name, String[] args) {
        this.name = name;
        this.args = args;
    }

    public String getArg(int index) {
        return args[index];
    }

    public String getArgNullable(int index) {
        if (index < 0 || index >= getLength())
            return null;
        return args[index];
    }

    /**
     * Get the length of the arguments, excluding the {@code name}.
     *
     * @return The length of the arguments, excluding the {@code name}.
     */
    public int getLength() {
        return args.length;
    }

    public String getName() {
        return name;
    }
}
