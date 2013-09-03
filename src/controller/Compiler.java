package controller;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Enumeration;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import view.CompilerFrame;
import view.checkboxtree.CheckTreeManager;
import controller.XMLParsing.MyNode;

/**
 * Main class of NAMControllerCompiler.
 * @author memo
 */
public class Compiler {
    
    private final File
            RESOURCE_DIR = new File("resources"),
            XML_DIR = new File(RESOURCE_DIR, "xml"),
            XML_FILE = new File(XML_DIR, "RUL2_IID_structure.xml"),
            DATA_FILE = new File(RESOURCE_DIR, "NAMControllerCompilerData.txt");

    private File inputDir, outputDir;
    private File[] rulDirs;              // rul0, rul1, rul2
    private boolean isLHD;
    private CollectRULsTask collectRULsTask;
    
    private final Mode mode;
    private final ErrorHandler errorHandler;
    private final CompilerSettingsManager settingsManager ;
    
    public Compiler(Mode mode) {
        this(mode, null, null, false);
    }
    public Compiler(Mode mode, String inputPath, String outputPath, boolean lhd) {
        this.mode = mode;
        if (inputPath != null) {
            this.inputDir = new File(inputPath);
        }
        if (outputPath != null) {
            this.outputDir = new File(outputPath);
        }
        this.isLHD = lhd;
        this.errorHandler = new ErrorHandler();
        this.settingsManager = new CompilerSettingsManager(DATA_FILE);
        /*
         * read settings
         */
        if (this.mode.isInteractive()) {
            // TODO
            if (DATA_FILE.exists()) {
                try {
                    settingsManager.readSettings();
                    this.inputDir = new File(settingsManager.getInput());
                    this.outputDir = new File(settingsManager.getOutput());
                    this.isLHD = settingsManager.getLhdFlag();
                } catch (FileNotFoundException e) {
                    // cannot occur
                    LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        }
        /*
         * check if resource files exist
         */
        if (!XML_FILE.exists()) {
            errorHandler.handleMissingResourceFile("Resource file", XML_FILE);
            System.exit(-1);
        }
        if (!this.mode.isDetailed()) {
            this.checkInputFilesExist(true);
        }
        try {
            /*
             * read XML file
             */
            JTree tree = XMLParsing.buildJTreeFromXML(XML_FILE);
            final CheckTreeManager checkTreeManager = new CheckTreeManager(tree);
            /*
             * write controller file
             */
            if (this.mode.isInteractive()) {
                this.showGUI(tree, checkTreeManager); // calls writeControllerFile upon button action
            } else {
                this.writeControllerFile(checkTreeManager, null);
            }
        } catch (PatternSyntaxException e) {
            errorHandler.handleException(e.getDescription(), e);
            System.exit(-1);
        } catch (ParserConfigurationException e) {
            errorHandler.handleException(e.getLocalizedMessage(), e);
            System.exit(-1);
        } catch (SAXException e) {
            errorHandler.handleException(e.getLocalizedMessage(), e);
            System.exit(-1);
        } catch (IOException e) {
            errorHandler.handleException(e.getLocalizedMessage(), e);
            System.exit(-1);
        }
    }
    
    private void writeControllerFile(CheckTreeManager checkTreeManager, JFrame parentFrame) {
        Queue<Pattern> patterns = collectPatterns(checkTreeManager);

        /*
         * final configuration before writing the controller
         */
        if (this.mode.isDetailed()) {
            boolean exist = this.checkInputFilesExist(false);
            if (!exist) {
                LOGGER.info("Writing cancelled");
                return;
            }
        }
        
        collectRULsTask = new CollectRULsTask(rulDirs);
        collectRULsTask.execute();
        
        if (!this.outputDir.exists()) {
            if (this.mode.isInteractive()) {
                String message = "The output directory " + outputDir + " does not exist.\n" +
                		"Do you wish to create it?";
                int result = JOptionPane.showConfirmDialog(null, message, null, JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    collectRULsTask.cancel(true);
                    LOGGER.info("Writing cancelled");
                    return;
                }
            }
            this.outputDir.mkdirs();
            LOGGER.info("Created output directory: " + this.outputDir);
        }

        File outputFile = new File(outputDir, String.format(
                "NetworkAddonMod_Controller_%s_HAND_VERSION.dat", isLHD ? "LEFT" : "RIGHT"));

        if (outputFile.exists() && this.mode.isInteractive()) {
            String message = "The file " + outputFile.getName() + " already exists. Do you wish to overwrite it?";
            int result = JOptionPane.showConfirmDialog(null, message, null, JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                LOGGER.info("Writing cancelled");
                return;
            }
        }
        
        try {
            settingsManager.writeSettings(inputDir, outputDir, isLHD);
        } catch (FileNotFoundException e) {
            errorHandler.handleException("Could not write settings", e);
        }

        WriteControllerTask writeTask = new WriteControllerTask(this.mode, collectRULsTask, parentFrame, isLHD, patterns, outputFile);
        if (this.mode.isInteractive()) {
            writeTask.execute();
        } else {
            // run command line mode in the same thread
            try {
                boolean result = writeTask.doInBackground();
                if (result) {
                    LOGGER.log(Level.INFO,
                            "Writing of Controller completed. Total time consumed: " +
                            "{0} milliseconds.", writeTask.getTimeConsumed());
                    System.exit(0);
                } else {
                    LOGGER.severe("Compiler finished with errors.");
                    System.exit(-1);
                }
            } catch (FileNotFoundException e) {
                errorHandler.handleException(e.getLocalizedMessage(), e);
                System.exit(-1);
            } catch (IOException e) {
                errorHandler.handleException(e.getLocalizedMessage(), e);
                System.exit(-1);
            } catch (InterruptedException e) {
                errorHandler.handleException(e.getLocalizedMessage(), e);
                System.exit(-1);
            } catch (ExecutionException e) {
                errorHandler.handleException(e.getLocalizedMessage(), e);
                System.exit(-1);
            }
        }
    }

    /**
     * Gets data from dataFile and displays Frame.
     */
    private void showGUI(final JTree tree, final CheckTreeManager checkTreeManager) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final CompilerFrame frame = new CompilerFrame(Compiler.this.mode.isDetailed(),
                        settingsManager.getInput(), settingsManager.getOutput(),
                        settingsManager.getLhdFlag(), tree);
                frame.addStartButtonListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        if (Compiler.this.mode.isDetailed()) {
                            Compiler.this.inputDir = new File(frame.getInputPath());
                            Compiler.this.outputDir = new File(frame.getOutputPath());
                        }
                        Compiler.this.isLHD = frame.isLHD();
                        
                        writeControllerFile(checkTreeManager, frame);
                    }
                });
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
    
//    /**
//     * @throws FileNotFoundException if specified directories do not exist or specified
//     * directory is not a directory.
//     */
//    private void testIfFilesExist() throws FileNotFoundException {
//        List<File> files = new ArrayList<File>();
//        files.add(inputDir);
//        files.add(outputDir);
//        for (File file : rulDirs) {
//            files.add(file);
//        }
//        for (File file : files) {
//            if (!file.exists())
//                throw new FileNotFoundException("Directory does not exist: " + file.getPath());
//            if (!file.isDirectory())
//                throw new FileNotFoundException("File is not a directory: " + file.getPath());
//        }
//    }
    
    private boolean checkInputFilesExist(boolean exitOnError) {
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            errorHandler.handleMissingResourceFile("Input directory", inputDir);
            if (exitOnError) {
                System.exit(-1);
            }
            return false;
        } else {
            rulDirs = new File[3];
            for (int i = 0; i < rulDirs.length; i++) {
                rulDirs[i] = new File(inputDir, "RUL" + i);
                if (!rulDirs[i].exists()) {
                    errorHandler.handleMissingResourceFile("Input directory", rulDirs[i]);
                    if (exitOnError) {
                        System.exit(-1);
                    }
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Collects the Regex-patterns from the selected nodes, specified by the checkTreeManager.
     * @param checkTreeManager from the checkBoxTree.
     * @return a queue containing the patterns.
     */
    private Queue<Pattern> collectPatterns(CheckTreeManager checkTreeManager){
        Queue<Pattern> patterns = new ArrayDeque<Pattern>();
        TreePath[] checkedPaths = checkTreeManager.getSelectionModel().getSelectionPaths();
        if (checkedPaths != null) {
            for (int i = 0; i < checkedPaths.length; i++) {
                NAMControllerCompilerMain.LOGGER.config("Selected Node: " + checkedPaths[i].toString());
                MyNode node = (MyNode) checkedPaths[i].getLastPathComponent();
                collectPatterns(patterns, node);
            }
        }
        return patterns;
    }
    
    /**
     * Recursive collecting.
     * @param patterns an existing queue into which the patterns are to be inserted.
     * @param node of the sub-tree.
     */
    private void collectPatterns(Queue<Pattern> patterns, MyNode node) {
        if (node.hasPatterns()) {
            for (Pattern p : node) {
                patterns.add(p);
            }
        } else {
            @SuppressWarnings("rawtypes")
            Enumeration children = node.children();
            while (children.hasMoreElements()) {
                MyNode child = (MyNode) children.nextElement();
                collectPatterns(patterns, child);
            }   
        }
    }
    
    enum Mode {
        DEBUG,
        DEVELOPER,
        DEFAULT,
        COMMAND_LINE;
        
        public boolean isInteractive() {
            return this != Mode.COMMAND_LINE;
        }
        
        public boolean isDetailed() {
            return this == DEVELOPER || this == DEBUG;
        }
    }
    
    private class ErrorHandler {
        
        private void handleException(String message, Throwable t) {
            LOGGER.log(Level.SEVERE, message, t);
            if (Compiler.this.mode.isInteractive()) {
                JOptionPane.showMessageDialog(null, t.getMessage(), null, JOptionPane.ERROR_MESSAGE);
            }
        }
        
        private void handleMissingResourceFile(String name, File file) {
            String message = name + " is missing: " + file.toString();
            LOGGER.severe(message);
            if (Compiler.this.mode.isInteractive()) {
                message += "\nCannot compile controller. Check your installation!";
                JOptionPane.showMessageDialog(null, message, null, JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
