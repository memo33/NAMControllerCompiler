package controller;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Queue;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import view.CompilerFrame;
import view.ConsoleView;
import view.GUIView;
import view.View;
import view.checkboxtree.CheckTreeManager;
import controller.XMLParsing.MyNode;

public abstract class Compiler extends AbstractCompiler {

    private final File
            RESOURCE_DIR = new File("resources"),
            XML_DIR = new File(RESOURCE_DIR, "xml"),
            XML_FILE = new File(XML_DIR, "RUL2_IID_structure.xml"),
            DATA_FILE = new File(RESOURCE_DIR, "NAMControllerCompilerData.txt");
    private final CompilerSettingsManager settingsManager ;

    private File inputDir, outputDir;
    private File[] rulDirs;
    private boolean isLHD;

    private JTree tree;
    private CheckTreeManager checkTreeManager;
    private Queue<Pattern> patterns;
    private CollectRULsTask collectRULsTask;
    
    private File outputFile;
    
    public Compiler(Mode mode, View view) {
        super(mode, view);
        this.settingsManager = new CompilerSettingsManager(DATA_FILE);
    }

    @Override
    public boolean checkXMLExists() {
        if (!XML_FILE.exists()) {
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
        try {
            tree = XMLParsing.buildJTreeFromXML(XML_FILE);
            checkTreeManager = new CheckTreeManager(tree);
            return true;
        } catch (PatternSyntaxException e) {
            view.publishException("Syntax exception for Regular Expression in XML file", e);
            return false;
        } catch (ParserConfigurationException e) {
            view.publishException(e.getLocalizedMessage(), e);
            return false;
        } catch (SAXException e) {
            view.publishException(e.getLocalizedMessage(), e);
            return false;
        } catch (IOException e) {
            view.publishException(e.getLocalizedMessage(), e);
            return false;
        }
    }

    @Override
    public boolean collectPatterns() {
        patterns = new ArrayDeque<Pattern>();
        TreePath[] checkedPaths = checkTreeManager.getSelectionModel().getSelectionPaths();
        if (checkedPaths != null) {
            String newline = System.getProperty("line.separator");
            StringBuilder sb = new StringBuilder("Selected Nodes:");
            for (int i = 0; i < checkedPaths.length; i++) {
                sb.append(newline + checkedPaths[i].toString());
                MyNode node = (MyNode) checkedPaths[i].getLastPathComponent();
                collectPatterns(patterns, node);
            }
            LOGGER.config(sb.toString());
        }
        return true;
    }
    
    /**
     * Recursive collecting.
     * @param patterns an existing queue into which the patterns are to be inserted.
     * @param node of the sub-tree.
     */
    private void collectPatterns(Collection<Pattern> patterns, MyNode node) {
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
        
        outputFile = new File(outputDir, String.format(
                "NetworkAddonMod_Controller_%s_HAND_VERSION.dat", isLHD ? "LEFT" : "RIGHT"));

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
            return true;
        } catch (FileNotFoundException e) {
            view.publishException("Could not write settings", e);
            return false;
        }
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
            }
            return true;
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
