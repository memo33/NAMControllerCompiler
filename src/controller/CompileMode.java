package controller;

public enum CompileMode {
    DEBUG,
    DEVELOPER,
    DEFAULT,
    COMMAND_LINE;
    
    public boolean isInteractive() {
        return this != CompileMode.COMMAND_LINE;
    }
    
    public boolean isDetailed() {
        return this == DEVELOPER || this == DEBUG;
    }
}