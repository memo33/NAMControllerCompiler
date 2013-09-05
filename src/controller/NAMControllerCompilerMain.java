package controller;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jdpbfx.util.DBPFUtil;

import controller.AbstractCompiler.Mode;
import controller.Compiler.CommandLineCompiler;
import controller.Compiler.GUICompiler;

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
	    LOGGER.setLevel(Level.ALL);
	    Handler handler = new ConsoleHandler();
	    handler.setLevel(Level.INFO);
	    
	    LOGGER.setUseParentHandlers(false);
	    LOGGER.addHandler(handler);
	    DBPFUtil.LOGGER.setUseParentHandlers(false);
	    DBPFUtil.LOGGER.addHandler(handler);
	    
	    AbstractCompiler compiler = null;
		if (args.length == 0) {
		    // default user mode
		    handler.setLevel(Level.SEVERE);
		    compiler = new GUICompiler(Mode.DEFAULT);
		} else if (args.length == 1 && args[0].equals("dev")) {
		    // developer mode
	        handler.setLevel(Level.INFO);
		    compiler = new GUICompiler(Mode.DEVELOPER);
		} else if (args.length == 1 && args[0].equals("debug")) {
		    // debug mode
		    handler.setLevel(Level.ALL);
		    compiler = new GUICompiler(Mode.DEBUG);
		} else if (args.length >= 3) { // TODO number of arguments 
		    // command line mode
//	        handler.setLevel(Level.INFO);
	        handler.setLevel(Level.ALL);
		    compiler = new CommandLineCompiler(args[0], args[1], args[2].equals("lhd"));
		} else {
		    LOGGER.severe("Wrong number of arguments passed.");
		    System.exit(-1);
		    return;
		}
		compiler.execute();
	}
}
