package controller;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Queue;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import view.CompilerFrame;
import view.ConsoleView;
import view.GUIView;
import view.View;
import view.checkboxtree.MyCheckTreeManager;

public abstract class Compiler extends AbstractCompiler {

    static final String RESOURCE_DIR = "resources"; 

    private final File
            XML_DIR = new File(RESOURCE_DIR, "xml"),
            XML_FILE = new File(XML_DIR, "RUL2_IID_structure.xml"),
            XML_FILE2 = new File(XML_DIR, "RUL2_IID_structure_b.xml"),
            DATA_FILE = new File(RESOURCE_DIR, "NAMControllerCompilerData.txt");
    private final CompilerSettingsManager settingsManager ;

    private File inputDir, outputDir;
    private File[] rulDirs;
    private boolean isLHD;
    
    private boolean firstXMLisActive;

    private JTree tree;
//    private MyCheckTreeManager checkTreeManager;
    private Queue<Pattern> patterns;
    private CollectRULsTask collectRULsTask;
    
    private File outputFile;
    
    public Compiler(Mode mode, View view) {
        super(mode, view);
        this.settingsManager = new CompilerSettingsManager(DATA_FILE);
    }

    @Override
    public boolean checkXMLExists() {
        if (!XML_FILE.exists() && !XML_FILE2.exists()) {
            view.publishIssue("XML file \"{0}\" is missing", XML_FILE.toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean checkInputFilesExist() {
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            view.publishIssue("Input directory \"{0}\" does not exist", inputDir);
            return false;
        } else {
            rulDirs = new File[3];
            for (int i = 0; i < rulDirs.length; i++) {
                rulDirs[i] = new File(inputDir, "RUL" + i);
                if (!rulDirs[i].exists()) {
                    view.publishIssue("Input directory \"{0}\" does not exist", rulDirs[i]);
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public boolean readXML() {
        String message = null;
        Exception previousException = null;
        if (XML_FILE.exists()) {
            try {
                tree = XMLParsing.buildJTreeFromXML(mode, XML_FILE);
                new MyCheckTreeManager(tree);
                firstXMLisActive = true;
                return true;
            } catch (PatternSyntaxException e) {
                message = "Syntax exception for Regular Expression in XML file";
                previousException = e;
            } catch (ParserConfigurationException e) {
                message = e.getLocalizedMessage();
                previousException = e;
            } catch (SAXException e) {
                message = e.getLocalizedMessage();
                previousException = e;
            } catch (IOException e) {
                message = e.getLocalizedMessage();
                previousException = e;
            }
            if (!XML_FILE2.exists()) {
                view.publishException(message, previousException);
            }
        }
        if (XML_FILE2.exists()) {
            try {
                tree = XMLParsing.buildJTreeFromXML(mode, XML_FILE2);
                new MyCheckTreeManager(tree);
                firstXMLisActive = false;
                return true;
            } catch (PatternSyntaxException e) {
                if (previousException != null) {
                    view.publishException(message, previousException);
                } else {
                    view.publishException("Syntax exception for Regular Expression in XML file 2", e);
                }
            } catch (ParserConfigurationException e) {
                if (previousException != null) {
                    view.publishException(message, previousException);
                } else {
                    view.publishException(e.getLocalizedMessage(), e);
                }
            } catch (SAXException e) {
                if (previousException != null) {
                    view.publishException(message, previousException);
                } else {
                    view.publishException(e.getLocalizedMessage(), e);
                }
            } catch (IOException e) {
                if (previousException != null) {
                    view.publishException(message, previousException);
                } else {
                    view.publishException(e.getLocalizedMessage(), e);
                }
            }
        }
        return false;
    }

    @Override
    public boolean collectPatterns() {
        patterns = ((AbstractNode) tree.getModel().getRoot()).getAllSelectedPatterns();
        return true;
    }
    
    @Override
    public boolean collectRULInputFiles() {
        collectRULsTask = new CollectRULsTask(rulDirs);
        collectRULsTask.execute();
        return true;
    }

    @Override
    public boolean checkOutputFilesExist() {
        if (!this.outputDir.exists()) {
            if (!view.publishConfirmOption("The directory \"{0}\" does not exist. Do you wish to create it?", outputDir.toString())) {
                LOGGER.log(Level.INFO, "Creating directory \"{0}\" denied", outputDir.toString());
                return false;
            } else {
                this.outputDir.mkdirs();
                LOGGER.info("Created output directory: " + outputDir);
            }
        }
        
//        outputFile = new File(outputDir, String.format(
//                "NetworkAddonMod_Controller_%s_HAND_VERSION.dat", isLHD ? "LEFT" : "RIGHT"));
        outputFile = new File(outputDir, "NetworkAddonMod_Controller.dat");

        if (outputFile.exists()) {
            if (!view.publishConfirmOption("The file \"{0}\" already exists. Do you wish to overwrite it?", outputFile.toString())) {
                LOGGER.log(Level.INFO, "Overwriting file \"{0}\" denied", outputFile.toString());
                return false;
            } else {
                LOGGER.log(Level.INFO, "Overwriting existing file \"{0}\"", outputFile.toString());
            }
        }
        return true;
    }

    @Override
    public boolean writeSettings() {
        try {
            settingsManager.writeSettings(inputDir, outputDir, isLHD);
            if (XML_FILE.exists() && firstXMLisActive) {
                XML_FILE2.delete();
                boolean success = XML_FILE.renameTo(XML_FILE2);
                if (!success) {
                    LOGGER.fine("First attempt to rename XML file failed");
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    success = XML_FILE.renameTo(XML_FILE2);
                }
                if (!success) {
                    LOGGER.severe("Renaming of XML file failed");
                }
            }
            XMLParsing.writeXMLfromJTree(tree, XML_FILE);
            return true;
        } catch (FileNotFoundException e) {
            view.publishException("Could not write settings", e);
        } catch (ParserConfigurationException e) {
            view.publishException("Could not write XML file", e);
        } catch (TransformerException e) {
            view.publishException("Could not write XML file", e);
        }
        return false;
    }
    
    public static class GUICompiler extends Compiler {
        
        public GUICompiler(Mode mode) {
            super(mode, new GUIView());
            if (!mode.isInteractive()) {
                throw new RuntimeException("GUICompiler must be executed in interactive mode.");
            }
        }
        
        @Override
        public boolean readSettings() {
            if (super.DATA_FILE.exists()) {
                try {
                    super.settingsManager.readSettings();
                    super.inputDir = new File(super.settingsManager.getInput());
                    super.outputDir = new File(super.settingsManager.getOutput());
                    super.isLHD = super.settingsManager.getLhdFlag();
                } catch (FileNotFoundException e) {
                    // cannot occur
                    view.publishException(e.getLocalizedMessage(), e);
                    return false;
                }
                return true;
            } else if (!this.mode.isDetailed()) {
                // TODO
                view.publishIssue("The settings file does not exist. It is necessary to reinstall the compiler.");
                return false;
            } else {
                return true;
            }
        }
        
        @Override
        public void writeControllerFile() {
            WriteControllerTask writeTask = new WriteControllerTask(super.collectRULsTask, super.isLHD, super.patterns, super.inputDir.toURI(), super.outputFile, view);
            writeTask.execute();
            // result will be handled by done-method in writeTask
        }

        @Override
        public void execute() {
            runBefore.run();
            showGUI();
        }
        
        private void showGUI() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final CompilerFrame frame = new CompilerFrame(mode.isDetailed(),
                            GUICompiler.super.settingsManager.getInput(), GUICompiler.super.settingsManager.getOutput(),
                            GUICompiler.super.settingsManager.getLhdFlag(), GUICompiler.super.tree);
                    ((GUIView) view).setFrame(frame);
                    frame.addStartButtonListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            if (mode.isDetailed()) {
                                GUICompiler.super.inputDir = new File(frame.getInputPath());
                                GUICompiler.super.outputDir = new File(frame.getOutputPath());
                            }
                            GUICompiler.super.isLHD = frame.isLHD();

                            runAfter.run();
                        }
                    });
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                }
            });
        }
    }
    
    public static class CommandLineCompiler extends Compiler {
        
        public CommandLineCompiler(String inputPath, String outputPath, boolean isLHD) {
            super(Mode.COMMAND_LINE, new ConsoleView());
            super.inputDir = new File(inputPath);
            super.outputDir = new File(outputPath);
            super.isLHD = isLHD;
        }
        
        @Override
        public boolean readSettings() {
            return true;
        }
        
        @Override
        public void writeControllerFile() {
            WriteControllerTask writeTask = new WriteControllerTask(super.collectRULsTask, super.isLHD, super.patterns, super.inputDir.toURI(), super.outputFile, view) {
                @Override
                protected void done() {
                    /*
                     * Dirty hack so as to be able to call determineResult()
                     * in the same Thread because SwingWorker is daemon!
                     */
                };
            };
            writeTask.execute();
            writeTask.determineResult();
        }

        @Override
        public void execute() {
            runBefore.run();
            runAfter.run();
        }
    }
}
