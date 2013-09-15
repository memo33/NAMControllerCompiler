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

    private static final String def = "0xffff";
    public enum ArgumentID {
        
        INPUT_DIR(0, ""),
        OUTPUT_DIR(1, ""),
        RHD_FLAG(2, "1"),
        PS_FLAG(3, "0"),
        NWM_FLAGS(4, def),
        RHW_L0_FLAGS(5, def),
        RHW_L1_FLAGS(6, def),
        RHW_L2_FLAGS(7, def),
        RHW_L3_FLAGS(8, def),
        RHW_L4_FLAGS(9, def),
        RHW_ADDITIONAL_FLAGS(10, def),
        SAM_FLAGS(11, def);

        private final int index;
        private final String defaultValue;
        
        ArgumentID(int index, String defaultValue) {
            this.index = index;
            this.defaultValue = defaultValue;
        }
    }
}
