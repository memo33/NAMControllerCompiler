package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jdpbfx.DBPFTGI;
import model.parser.MetaDefinitionsParser;
import model.parser.MetaOverrideParser;
import model.parser.RUL1Parser;

import org.codehaus.groovy.control.CompilationFailedException;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import controller.NAMControllerCompilerMain;

public class RUL1Entry extends RULEntry {
    
//    private final boolean isESeries;
    private final boolean isLHD;
    
    private final boolean collectMetaData;
    private final File metaRuleDefinitionsFile;
    private final MetaController metaController;
    private final ReportingParseRunner<?> rul1Runner;
    private final ReportingParseRunner<?> metaRul1Runner;
    
    private final String META_COMMENT_DELIMITER = "#";
    static final String METARUL_FILEEXTENSION = ".metarul";

//    public RUL1Entry(DBPFTGI tgi, Queue<File> inputFiles, boolean isLHD, ChangeListener changeListener) {
//        super(tgi, inputFiles, changeListener);
//        this.isLHD = isLHD;
//        this.collectMetaData = false;
//        this.metaRuleDefinitionsFile = null;
//        this.metaController = null;
//        this.rul1Runner = null;
//        this.metaRul1Runner = null;
//    }
    
    public RUL1Entry(DBPFTGI tgi, Queue<File> inputFiles, boolean isLHD, ChangeListener changeListener,
            File metaRuleDefinitionsFile, MetaController metaController, MetaOverrideParser overrideParser) {
        super(tgi, inputFiles, changeListener);
        this.isLHD = isLHD;
        this.collectMetaData = true;
        this.metaRuleDefinitionsFile = metaRuleDefinitionsFile;
        this.metaController = metaController;
        this.rul1Runner = new ReportingParseRunner<Object>(Parboiled.createParser(RUL1Parser.class).RUL1IntersectionDefinition());
        this.metaRul1Runner = new ReportingParseRunner<Object>(overrideParser.MetaIntersectionDefinition());
    }

    @Override
    protected void provideData() throws IOException {
        NAMControllerCompilerMain.LOGGER.info("Writing file RUL1");
        
        if (collectMetaData) {
            readDefinitionsFile();
        }
        for (File file : inputFiles) {
            this.changeListener.stateChanged(new ChangeEvent(file));
//            if (!RULEntry.fileMatchesSeries(file, isESeries)) {
//                continue;
//            }
            
            final RUL1Writer rul1Writer;
            boolean isGroovy = false;
            if (collectMetaData) {
                if (file.getName().endsWith(METARUL_FILEEXTENSION) ||
                        (isGroovy = file.getName().endsWith(".groovy"))) {
                    rul1Writer = new MetaRUL1Writer();
                } else {
                    rul1Writer = new MetaCollectingRUL1Writer();
                }
            } else {
                if (file.getName().endsWith(METARUL_FILEEXTENSION)) {
                    continue;
                }
                rul1Writer = new DefaultRUL1Writer();
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(!isGroovy ?
                    new FileInputStream(file) :
                        RUL2Entry.createGroovyInputStream(file)))) {
                printSubFileHeader(file);
                
                int i = 1;
                for (String line = br.readLine(); line != null; line = br.readLine(), i++) {
                    try {
                        rul1Writer.writeLineChecked(line);
                    } catch (DuplicateDefinitionException e) {
                        NAMControllerCompilerMain.LOGGER.log(Level.WARNING, String.format("Duplicate definition in line %d of file %s:%n%s", i, file, line));
                    }
                }

                writer.write(newline);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CompilationFailedException | InstantiationException | IllegalAccessException e) {
                assert isGroovy;
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        writer.flush();
    }
        
    private void readDefinitionsFile() throws FileNotFoundException {
        if (!metaRuleDefinitionsFile.exists()) {
            NAMControllerCompilerMain.LOGGER.log(Level.WARNING, "Source file \"{0}\" does not exist", metaRuleDefinitionsFile);
            return;
        }
        Scanner scanner = new Scanner(metaRuleDefinitionsFile);
        
        MetaDefinitionsParser parser = Parboiled.createParser(MetaDefinitionsParser.class);
        ReportingParseRunner<String> nameSynonymRunner = new ReportingParseRunner<String>(parser.NameSynonymDefinition());
        ReportingParseRunner<String> groupDefinitionRunner = new ReportingParseRunner<String>(parser.GroupDefinition());
        ReportingParseRunner<String> directionSynonymRunner = new ReportingParseRunner<String>(parser.DirectionSynonymDefinition());
        
        for (int i = 1; scanner.hasNextLine(); i++) {
            String[] splits = scanner.nextLine().split(META_COMMENT_DELIMITER, 2);
            if (splits.length == 0) {
                continue;
            }
            String line = splits[0].trim();
            if (line.isEmpty()) {
                continue;
            }
            
            try {
                if (line.startsWith("DEFINE-SYNONYM-NAME")) {
                    ParsingResult<String> result = nameSynonymRunner.run(line);
                    if (!result.parseErrors.isEmpty()) {
                        NAMControllerCompilerMain.LOGGER.warning(ErrorUtils.printParseError(result.parseErrors.get(0)));
                        result.parseErrors.clear();
                    } else {
                        String majorName = result.valueStack.pop();
                        while (!result.valueStack.isEmpty()) {
                            metaController.putNameSynonym(result.valueStack.pop(), majorName);
                        }
                    }
                    assert result.valueStack.isEmpty();
                } else if (line.startsWith("DEFINE-GROUP")) {
                    ParsingResult<String> result = groupDefinitionRunner.run(line);
                    if (!result.parseErrors.isEmpty()) {
                        NAMControllerCompilerMain.LOGGER.warning(ErrorUtils.printParseError(result.parseErrors.get(0)));
                        result.parseErrors.clear();
                    } else {
                        String groupName = result.valueStack.pop();
                        while (!result.valueStack.isEmpty()) {
                            metaController.putGroupDefinition(result.valueStack.pop(), groupName);
                        }
                    }
                    assert result.valueStack.isEmpty();
                } else if (line.startsWith("DEFINE-SYNONYM-DIRECTION")) {
                    ParsingResult<String> result = directionSynonymRunner.run(line);
                    if (!result.parseErrors.isEmpty()) {
                        NAMControllerCompilerMain.LOGGER.warning(ErrorUtils.printParseError(result.parseErrors.get(0)));
                        result.parseErrors.clear();
                    } else {
                        String groupName = result.valueStack.pop();
                        String majorDirLabel = result.valueStack.pop();
                        while (!result.valueStack.isEmpty()) {
                            metaController.putGroupDirectionSynonym(groupName, result.valueStack.pop(), majorDirLabel);
                        }
                    }
                    assert result.valueStack.isEmpty();
                } else {
                    NAMControllerCompilerMain.LOGGER.log(Level.WARNING, "Invalid format in line %d of file %s:%n%s", new Object[]{i, metaRuleDefinitionsFile, line});
                }
            } catch (DuplicateDefinitionException e) {
                NAMControllerCompilerMain.LOGGER.log(Level.WARNING, String.format("Duplicate definition in line %d of file %s:%n%s", i, metaRuleDefinitionsFile, line));
            }
        }
        scanner.close();
    }
    
    private static interface RUL1Writer {
        
        public void writeLineChecked(String line) throws IOException, DuplicateDefinitionException;
    }
    
    private class DefaultRUL1Writer implements RUL1Writer {
        /**
         * checks for LHD/RHD specific code.
         * @param line
         * @throws IOException 
         * @throws DuplicateDefinitionException 
         */
        @Override
        public void writeLineChecked(String line) throws IOException, DuplicateDefinitionException {
            if (!isLHD && line.startsWith(";###RHD###")) {
                writer.write(line.substring(10) + newline);
            } else if (isLHD && line.startsWith(";###LHD###")) {
                writer.write(line.substring(10) + newline);
            } else {
                writer.write(line + newline);
            }
        }
    }
    
    private class MetaCollectingRUL1Writer extends DefaultRUL1Writer {
        
        private String leftNetworkName, rightNetworkName;
        private boolean isAllowedOverridesFile = false;
        
        @Override
        public void writeLineChecked(String line) throws IOException, DuplicateDefinitionException {
            String[] splits = line.split(";", 2);
            boolean isGood = true;
            if (splits.length == 0) {
                isGood = false;
            } else {
                line = splits[0].trim();
            }
            if (line.isEmpty()) {
                isGood = false;
            } else if (line.startsWith("[")) {
                isGood = false;
                String lline = line.toLowerCase().substring(1); // get rid of opening bracket
                if (lline.contains("intersectionsolutions")) {
                    String[] networkNames = {"avenue", "street", "onewayroad", "dirtroad", "lightrail", "monorail", "subway", "groundhighway", "highway", "road", "rail"};
                    // get left name
                    for (int i = 0; i < networkNames.length; i++) {
                        if (lline.startsWith(networkNames[i])) {
                            leftNetworkName = networkNames[i];
                            lline = lline.substring(networkNames[i].length());
                            break;
                        }
                    }
                    // get right name
                    for (int i = 0; i < networkNames.length; i++) {
                        if (lline.startsWith(networkNames[i])) {
                            rightNetworkName = networkNames[i];
                            lline = lline.substring(networkNames[i].length());
                            break;
                        }
                    }
                    assert leftNetworkName != null && rightNetworkName != null && lline.startsWith("intersectionsolutions") : line;
                } else {
                    isAllowedOverridesFile = true;
                }
            }
            
            if (isGood && !isAllowedOverridesFile) {
                if (leftNetworkName == null || rightNetworkName == null) {
                    throw new IllegalStateException("Intersection solution network names have not been defined at this point");
                }
                ParsingResult<?> result = rul1Runner.run(line);
                if (!result.parseErrors.isEmpty()) {
                    NAMControllerCompilerMain.LOGGER.warning(ErrorUtils.printParseError(result.parseErrors.get(0)));
                    result.parseErrors.clear();
                } else {
                    // currently does not respect RHD/LHD directives
                    NetworkTile nt = (NetworkTile) result.valueStack.pop();
                    String rightDir = (String) result.valueStack.pop();
                    String leftDir = (String) result.valueStack.pop();
                    metaController.putMetaNetworkDefinition(
                            new MetaNetworkTile(
                                    metaController.createMetaNetwork(leftNetworkName, leftDir),
                                    metaController.createMetaNetwork(rightNetworkName, rightDir)
                                    ), nt);
                }
                assert result.valueStack.isEmpty();
            }
            super.writeLineChecked(line);
        }
    }
    
    private class MetaRUL1Writer implements RUL1Writer {

        /*
         * Does not write, but only parses line and passes meta information to
         * meta controller.
         */
        @Override
        public void writeLineChecked(String line) throws IOException, DuplicateDefinitionException {
            String[] splits = line.split(META_COMMENT_DELIMITER, 2);
            if (splits.length == 0) {
                return;
            }
            line = splits[0].trim();
            if (line.isEmpty()) {
                return;
            }
            
            ParsingResult<?> result = metaRul1Runner.run(line);
            if (!result.parseErrors.isEmpty()) {
                NAMControllerCompilerMain.LOGGER.warning(ErrorUtils.printParseError(result.parseErrors.get(0)));
                result.parseErrors.clear();
            } else {
                result.valueStack.swap();
                metaController.putMetaNetworkDefinition((MetaNetworkTile) result.valueStack.pop(), (NetworkTile) result.valueStack.pop());
            }
            assert result.valueStack.isEmpty();
        }
        
    }
}
