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

//    static final String RESOURCE_DIR = "resources"; 

    private final File RESOURCE_DIR, XML_DIR, XML_FILE, XML_FILE_TEMP;
    private final File[] DATA_FILES;
    private final CompilerSettingsManager settingsManager ;

    private File inputDir, outputDir;
    private File[] rulDirs;
    private boolean isLHD;
    
    private boolean firstXMLisActive;

    private PatternNode rootNode;
    private JTree tree;
//    private MyCheckTreeManager checkTreeManager;
    private Queue<Pattern> patterns;
    private CollectRULsTask collectRULsTask;
    
    private File outputFile;
    
    public static Compiler getCommandLineCompiler(File resourceDir, File inputPath, File outputPath, boolean isLHD) {
        return new CommandLineCompiler(resourceDir, inputPath, outputPath, isLHD);
    }
    
    public static Compiler getInteractiveCompiler(File resourceDir, Mode mode) {
        if (!mode.isInteractive()) {
            throw new IllegalArgumentException("GUICompiler must be executed in interactive mode.");
        }
        return new GUICompiler(resourceDir, mode);
    }
    
    private Compiler(File resourceDir, Mode mode, View view) {
        super(mode, view);
        this.RESOURCE_DIR = resourceDir;
        this.XML_DIR = new File(RESOURCE_DIR, "xml");
        this.XML_FILE = new File(XML_DIR, "RUL2_IID_structure.xml");
        this.XML_FILE_TEMP = new File(XML_DIR, "RUL2_IID_structure.xml~1");
        this.DATA_FILES = new File[] {
                new File(RESOURCE_DIR, "NAMControllerCompilerSettings.txt"),
                new File(RESOURCE_DIR, "NAMControllerCompilerSettings.txt~1"),
                new File(RESOURCE_DIR, "NAMControllerCompilerSettings.txt~2")};
        this.settingsManager = new CompilerSettingsManager(DATA_FILES);
    }

    @Override
    public boolean checkXMLExists() {
        if (!XML_FILE.exists() && !XML_FILE_TEMP.exists()) {
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
        firstXMLisActive = false;
        boolean success = false;
        if (XML_FILE.exists()) {
            try {
                rootNode = XMLParsing.buildTreeFromXML(mode, XML_FILE);
                firstXMLisActive = true;
                success = true;
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
            if (!success && !XML_FILE_TEMP.exists()) {
                view.publishException(message, previousException);
            }
        }
        if (!success && XML_FILE_TEMP.exists()) {
            try {
                rootNode = XMLParsing.buildTreeFromXML(mode, XML_FILE_TEMP);
                firstXMLisActive = false;
                success = true;
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
        if (!success) {
            return false;
        } else if (!firstXMLisActive) {
            LOGGER.log(Level.SEVERE, "The XML file \"{0}\" is unreadable.", XML_FILE);
            boolean approved = view.publishConfirmOption("The XML file \"{0}\" is unreadable. Use previous configuration?", XML_FILE);
            if (!approved) {
                LOGGER.info("Using previous XML configuration denied");
            } else {
                LOGGER.info("Using previous XML configuration");
            }
            return approved;
        } else {
            return true;
        }
    }

    @Override
    public boolean collectPatterns() {
        try {
            patterns = ((AbstractNode) rootNode).getAllSelectedPatterns();
            return true;
        } catch (SAXException e) {
            view.publishException(e.getLocalizedMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean collectRULInputFiles() {
        collectRULsTask = CollectRULsTask.getInstance(mode, rulDirs);
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
                if (XML_FILE_TEMP.exists()) {
                    XML_FILE_TEMP.delete();
                }
                boolean success = XML_FILE.renameTo(XML_FILE_TEMP);
                if (!success) {
                    LOGGER.fine("First attempt to rename XML file failed");
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    success = XML_FILE.renameTo(XML_FILE_TEMP);
                }
                if (!success) {
                    view.publishIssue("Renaming/Writing of XML file failed. Cannot compile a new controller.");
                    return false;
                }
            }
            XMLParsing.writeXMLfromTree(rootNode, XML_FILE);
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
    
    @Override
    public void writeControllerFile() {
        ExecutableTask writeTask = WriteControllerTask.getInstance(mode, collectRULsTask, isLHD, patterns, inputDir.toURI(), outputFile, view);
        writeTask.execute();
        // result will be handled by determineResult in writeTask
    }
    
    private static class GUICompiler extends Compiler {
        
        private GUICompiler(File resourceDir, Mode mode) {
            super(resourceDir, mode, new GUIView());
            assert mode.isInteractive() : mode;
        }
        
        @Override
        public boolean readSettings() {
            // TODO
            if (super.DATA_FILES[0].exists() || super.DATA_FILES[1].exists() || super.DATA_FILES[2].exists()) {
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
                view.publishIssue("The settings file does not exist. You have to reinstall the compiler.");
                return false;
            } else {
                return true;
            }
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
        
        @Override
        public boolean readXML() {
            boolean success = super.readXML();
            if (success && this.mode.isInteractive()) {
                super.tree = new JTree(super.rootNode);
                new MyCheckTreeManager(super.tree, this.mode.isDetailed());
            }
            return success;
        }
    }
    
    private static class CommandLineCompiler extends Compiler {
        
        private CommandLineCompiler(File resourceDir, File inputPath, File outputPath, boolean isLHD) {
            super(resourceDir, Mode.COMMAND_LINE, new ConsoleView());
            super.inputDir = inputPath;
            super.outputDir = outputPath;
            super.isLHD = isLHD;
        }
        
        @Override
        public boolean readSettings() {
            return true;
        }
        
        @Override
        public void execute() {
            runBefore.run();
            runAfter.run();
        }
    }
}
