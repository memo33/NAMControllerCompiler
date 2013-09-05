package controller;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
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
	 * @param args
	 */
	public static void main(String[] args) {
	    LOGGER.setLevel(Level.ALL);
	    DBPFUtil.LOGGER.setLevel(Level.ALL);
	    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
	        @Override
	        public void uncaughtException(Thread t, Throwable e) {
	            LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + t, e);
	        }
	    });

	    Handler consoleHandler = new ConsoleHandler();
	    consoleHandler.setLevel(Level.INFO);
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(consoleHandler);
        DBPFUtil.LOGGER.setUseParentHandlers(false);
        DBPFUtil.LOGGER.addHandler(consoleHandler);
	    
	    AbstractCompiler compiler = null;
		if (args.length == 0) {
		    // default user mode
		    consoleHandler.setLevel(Level.SEVERE);
		    compiler = new GUICompiler(Mode.DEFAULT);
		} else if (args.length == 1 && args[0].equals("dev")) {
		    // developer mode
	        consoleHandler.setLevel(Level.INFO);
		    compiler = new GUICompiler(Mode.DEVELOPER);
		} else if (args.length == 1 && args[0].equals("debug")) {
		    // debug mode
		    consoleHandler.setLevel(Level.ALL);
	        try {
	            Handler fileHandler = new FileHandler(Compiler.RESOURCE_DIR + "/log%g.txt", 1024 * 1024, 5);
	            fileHandler.setLevel(Level.ALL);
	            LOGGER.addHandler(fileHandler);
	            DBPFUtil.LOGGER.addHandler(fileHandler);
	        } catch (SecurityException e) {
	            LOGGER.log(Level.SEVERE, "Security exception at instantiation of FileHandler", e);
	        } catch (IOException e) {
	            LOGGER.log(Level.SEVERE, "IOException at instantiation of FileHandler", e);
	        }
		    compiler = new GUICompiler(Mode.DEBUG);
		} else if (args.length >= 3) { // TODO number of arguments 
		    // command line mode
//	        consoleHandler.setLevel(Level.INFO);
	        consoleHandler.setLevel(Level.ALL);
		    compiler = new CommandLineCompiler(args[0], args[1], args[2].equals("lhd"));
		} else {
		    LOGGER.severe("Wrong number of arguments passed.");
		    System.exit(-1);
		    return;
		}
		compiler.execute();
	}
}
