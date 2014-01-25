package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jdpbfx.DBPFTGI;
import model.parser.MetaOverrideParser;

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

    public RUL2Entry(DBPFTGI tgi, Queue<File> inputFiles, Collection<Pattern> patternsForExclusion, ChangeListener changeListener, MetaOverrideParser parser, ExecutorService executor, ExecutorService groovyExecutor) {
        super(tgi, inputFiles, changeListener, executor, groovyExecutor);
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

            try (BufferedReader buffer = new BufferedReader(!isGroovy ?
                    new FileReader(file) :
                        new InputStreamReader(createGroovyInputStream(file)))) {
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
            } catch (CompilationFailedException | InstantiationException | IllegalAccessException e) {
                assert isGroovy;
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
//        System.out.println(((MetaOverrideWriter) overrideWriter).runner.getReport().print());
        writer.flush();
    }

    private static interface OverrideWriter {

        public void writeLineChecked(String line, OutputStreamWriter writer, Deque<Pattern> patterns) throws IOException;
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
}
