package controller;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import controller.Compiler.Mode;

/**
 * Main class of NAMControllerCompiler.
 * @author memo
 */
public class NAMControllerCompilerMain {
    
    public static final Logger LOGGER = Logger.getLogger("NAMControllerCompiler");
    
	/**
	 * Arguments are optional. If no arguments are passed, the GUI is started.
	 * @param args
	 */
	public static void main(String[] args) {
        LOGGER.setUseParentHandlers(false);
	    Handler handler = new ConsoleHandler();
	    handler.setLevel(Level.INFO);
	    LOGGER.addHandler(handler);
	    LOGGER.setLevel(Level.ALL);
	    
		if (args.length == 0) {
		    // default user mode
		    handler.setLevel(Level.SEVERE);
		    new Compiler(Mode.DEFAULT);
		} else if (args.length == 1 && args[0].equals("dev")) {
		    // developer mode
		    new Compiler(Mode.DEVELOPER);
		} else if (args.length == 1 && args[0].equals("debug")) {
		    // debug mode
		    handler.setLevel(Level.ALL);
		    new Compiler(Mode.DEBUG);
		} else if (args.length == 3) { // TODO number of arguments 
		    // command line mode
		    handler.setLevel(Level.ALL);
		    new Compiler(Mode.COMMAND_LINE, args[0], args[1], args[2].equals("lhd"));
		} else {
		    LOGGER.severe("Wrong number of arguments passed.");
		    System.exit(-1);
		    return;
		}
	}
}
