package model;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jdpbfx.DBPFTGI;

import org.codehaus.groovy.control.CompilationFailedException;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import controller.NAMControllerCompilerMain;

public class RUL2Entry extends RULEntry {
    
//    private final boolean isESeries;
    private final Deque<Pattern> patterns;
    private final MetaOverrideParser parser;
    
    private MetaOverrideWriter metaOverrideWriter;
    private DefaultOverrideWriter defaultOverrideWriter;
    
    public RUL2Entry(DBPFTGI tgi, Queue<File> inputFiles, Collection<Pattern> patternsForExclusion, ChangeListener changeListener, MetaOverrideParser parser) {
        super(tgi, inputFiles, changeListener);
        this.patterns = new LinkedList<Pattern>(patternsForExclusion);
//        this.isESeries = isESeries;
        this.parser = parser;
    }
    
    /*
     * parses files line by line, tests if pattern matches and excludes these lines as well as empty lines and comments;
     * @throws IOException 
     */
    @Override
    public void provideData() throws IOException {
        NAMControllerCompilerMain.LOGGER.info("Writing file RUL2");
        boolean headerFound = false;
        
        for (File file : inputFiles) {
            this.changeListener.stateChanged(new ChangeEvent(file));
//            if (!RULEntry.fileMatchesSeries(file, isESeries)) {
//                continue;
//            }
            OverrideWriter overrideWriter;
            boolean isGroovy = false;
            if (file.getName().endsWith(RUL1Entry.METARUL_FILEEXTENSION) ||
                    (isGroovy = file.getName().endsWith(".groovy"))) {
                if (metaOverrideWriter == null) {
                    metaOverrideWriter = new MetaOverrideWriter();
                }
                overrideWriter = metaOverrideWriter;
            } else {
                if (defaultOverrideWriter == null) {
                    defaultOverrideWriter = new DefaultOverrideWriter();
                }
                overrideWriter = defaultOverrideWriter;
            }
            
            InputStream is = null;
            InputStreamReader isReader = null;
            FileReader fReader = null;
            BufferedReader buffer = null;
            try {
                if (isGroovy) {
                    is = createGroovyInputStream(file);
                    isReader = new InputStreamReader(is);
                    buffer = new BufferedReader(isReader);
                } else {
                    fReader = new FileReader(file);
                    buffer = new BufferedReader(fReader);
                }
                super.printSubFileHeader(file);
                
                for (String line = buffer.readLine(); line != null; line = buffer.readLine()) {
                    // special treatment for RUL2 header
                    if (!headerFound && line.trim().equalsIgnoreCase("[ruleoverrides]")) {
                        writer.write(line + newline);
                        headerFound = true;
                        continue;
                    }
                    // else
                    overrideWriter.writeLineChecked(line, writer, patterns);
                }
                writer.write(newline);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CompilationFailedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (buffer != null) {
                    buffer.close();
                }
                if (fReader != null) {
                    fReader.close();
                }
                if (isReader != null) {
                    isReader.close();
                }
                if (is != null) {
                    is.close();
                }
            }
        }
//        System.out.println(((MetaOverrideWriter) overrideWriter).runner.getReport().print());
        writer.flush();
    }
    
    static InputStream createGroovyInputStream(File groovyFile) throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException {
        ClassLoader parent = RUL2Entry.class.getClassLoader();
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        Class<?> groovyClass = loader.parseClass(groovyFile);

        final GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
        PipedInputStream pis = new PipedInputStream();
        final PipedOutputStream pos = new PipedOutputStream(pis);
        final PrintStream printer = new PrintStream(pos);
        groovyObject.setProperty("out", printer);
        new Thread(new Runnable() { // TODO threading
            
            @Override
            public void run() {
                groovyObject.invokeMethod("run", new Object[] {});
                printer.close();
                try {
                    pos.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
        loader.close();
        return pis;
    }

    private static class DefaultOverrideWriter implements OverrideWriter {

        @Override
        public void writeLineChecked(String line, OutputStreamWriter writer,
                Deque<Pattern> patterns) throws IOException {
            String[] splits = line.split(";", 2);
            if (splits.length == 0) {
                return;
            }
            line = splits[0].trim();
            if (line.isEmpty()) {
                return;
            }
            
            String[] iidsStrings = line.split(",|=");
            boolean matchFound = false;
            if (iidsStrings.length != 12) {
                NAMControllerCompilerMain.LOGGER.warning("Invalid RUL override format for line: " + line);
                matchFound = true;
            } else {
                Iterator<Pattern> iter = patterns.iterator();
                OUT: while (iter.hasNext()) {
                    Pattern p = iter.next();
                    for (int i = 9; i >= 0; i -= 3) {
                        matchFound |= p.matcher(iidsStrings[i]).matches();
                        if (matchFound) {
                            // let's maintain MRU order
                            iter.remove();
                            patterns.addFirst(p);
                            break OUT;
                        }
                    }
                }
            }
            
            if (!matchFound) {
                writer.write(line + newline);
            }
        }
    }
    
    private class MetaOverrideWriter implements OverrideWriter {
        
        private final String COMMENT_DELIMITER = "#";
        
//        private final ProfilingParseRunner<OverrideRule> runner = new ProfilingParseRunner<OverrideRule>(parser.Override());
        private final ReportingParseRunner<OverrideRule> runner = new ReportingParseRunner<OverrideRule>(parser.Override());

        @Override
        public void writeLineChecked(String line, OutputStreamWriter writer,
                Deque<Pattern> patterns) throws IOException {
            String[] splits = line.split(COMMENT_DELIMITER, 2);
            if (splits.length == 0) {
                return;
            }
            line = splits[0].trim();
            if (line.isEmpty()) {
                return;
            }
            
            ParsingResult<OverrideRule> result = runner.run(line);
            if (!result.parseErrors.isEmpty()) {
                NAMControllerCompilerMain.LOGGER.warning(ErrorUtils.printParseError(result.parseErrors.get(0)));
                result.parseErrors.clear();
            } else {
                OverrideRule overrideRule = result.resultValue;
                assert overrideRule != null : "Result value was null";
                
                String[] iidsStrings = new String[] {
                        overrideRule.getOutputTuple().getLeftTile().getID().asString(),
                        overrideRule.getOutputTuple().getRightTile().getID().asString(),
                        overrideRule.getInputTuple().getLeftTile().getID().asString(),
                        overrideRule.getInputTuple().getRightTile().getID().asString(),
                };
                Iterator<Pattern> iter = patterns.iterator();
                while (iter.hasNext()) {
                    Pattern p = iter.next();
                    for (int i = 0; i < iidsStrings.length; i++) {
                        if (p.matcher(iidsStrings[i]).matches()) {
                            // let's maintain MRU order
                            iter.remove();
                            patterns.addFirst(p);
                            return;
                        }
                    }
                }
                writer.write(overrideRule + newline);
            }
        }
    }
    
//    private static class RegexOverrideWriter implements OverrideWriter {
//        
//        private final String COMMENT_DELIMITER = ";";
//        private final RegexParser parser = new RegexParser();
//
//        @Override
//        public void writeLineChecked(String line, OutputStreamWriter writer,
//                Deque<Pattern> patterns) throws IOException {
//            String[] splits = line.split(COMMENT_DELIMITER, 2);
//            if (splits.length == 0) {
//                return;
//            }
//            line = splits[0].trim();
//            if (line.isEmpty()) {
//                return;
//            }
//            
//            OverrideRule overrideRule = parser.parseOverrideRule(line);
//            if (overrideRule == null) {
//                NAMControllerCompilerMain.LOGGER.warning("Invalid RUL override format for line: " + line);
//            } else {
//                String[] iidsStrings = new String[] {
//                        overrideRule.getOutputTuple().getLeftTile().getID().asString(),
//                        overrideRule.getOutputTuple().getRightTile().getID().asString(),
//                        overrideRule.getInputTuple().getLeftTile().getID().asString(),
//                        overrideRule.getInputTuple().getRightTile().getID().asString(),
//                };
//                Iterator<Pattern> iter = patterns.iterator();
//                while (iter.hasNext()) {
//                    Pattern p = iter.next();
//                    for (int i = 0; i < iidsStrings.length; i++) {
//                        if (p.matcher(iidsStrings[i]).matches()) {
//                            // let's maintain MRU order
//                            iter.remove();
//                            patterns.addFirst(p);
//                            return;
//                        }
//                    }
//                }
//                writer.write(overrideRule + newline);
//            }
//        }
//        
//        private class RegexParser {
//            
//            String hexUInt = "0x\\p{XDigit}{8}\\s*";
//            String rot = "[0-3]\\s*";
//            String flip = "[01]\\s*";
//            String prevent = "0\\s*,\\s*0\\s*,\\s*0\\s*";
//            
//            String iidTile = String.format("%s,\\s*%s,\\s*%s", hexUInt, rot, flip);
//            String iidPrevTile = prevent + "|" + iidTile;
//            
//            String metaName = "[!\\[\\]=;,]+";
//            String metaDir = "[!\\[\\]=;]+";
//            String metaNetwork = metaName + ",\\s*" + metaDir;
//            
//            String metaTile = String.format("\\[%s(?:;\\s*%s)*\\](?:,%s,\\s*%s)?", metaNetwork, metaNetwork, rot, flip);
//            
//            String override = String.format("(%s|%s)\\s*,\\s*(%s|%s)\\s*=\\s*(%s|%s)\\s*,\\s*(%s|%s)\\s*",
//                    iidTile, metaTile,
//                    iidTile, metaTile,
//                    iidPrevTile, metaTile,
//                    iidPrevTile, metaTile);
//            
//            Pattern overridePattern = Pattern.compile(override);
//            Pattern iidPrevTilePattern = Pattern.compile(iidPrevTile);
//            
//            
//            OverrideRule parseOverrideRule(String line) {
//                Matcher m = overridePattern.matcher(line);
//                if (!m.matches()) {
//                    return null;
//                }
//                return new OverrideRule(getNetworkTile(m.group(1)), getNetworkTile(m.group(2)), getNetworkTile(m.group(3)), getNetworkTile(m.group(4)));
//            }
//            
//            private NetworkTile getNetworkTile(String tile) {
//                Matcher mTile =iidPrevTilePattern.matcher(tile);
//                if (mTile.matches()) {
//                    String[] items = tile.split(",");
//                    return new NetworkTile(new StringIID(items[0]), Integer.decode(items[1]), Integer.decode(items[2]) == 1);
//                } else {
//                    // TODO
//                }
//                return null;
//            }
//        }
//        
//    }
}
