package controller;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

class CommandLineArguments implements Iterable<String> {

    private final List<String> argsList;

    private CommandLineArguments(String[] args) {
        this.argsList = Arrays.asList(args);
    }

    @Override
    public Iterator<String> iterator() {
        return argsList.iterator();
    }

    public String getArgument(ArgumentID argumentID) {
        return argsList.get(argumentID.index);
    }

    public void setArgument(ArgumentID argumentID, String argumentValue) {
        this.argsList.set(argumentID.index, argumentValue);
    }

    public void setArgument(int index, String argumentValue) {
        this.argsList.set(index, argumentValue);
    }

    public static CommandLineArguments getInstance(String[] args) {
        if (args.length != getExpectedArgumentCount()) {
            return null;
        } else {
            return new CommandLineArguments(args);
        }
    }

    public static CommandLineArguments getInstance() {
        String[] args = new String[getExpectedArgumentCount()];
        for (int i = 0; i < args.length; i++) {
            args[i] = ArgumentID.values()[i].defaultValue;
        }
        return getInstance(args);
    }

    public static int getExpectedArgumentCount() {
        return ArgumentID.values().length;
    }

    private static final String defaultWord = "0xFFFF";
    public enum ArgumentID {

        INPUT_DIR(0, ""),
        OUTPUT_DIR(1, ""),
        RHD_FLAG(2, "1"),  // 1=RHD, 0=LHD
        LTEXT(3, "");

        private final int index;
        private final String defaultValue;

        ArgumentID(int index, String defaultValue) {
            this.index = index;
            this.defaultValue = defaultValue;
        }
    }
}
