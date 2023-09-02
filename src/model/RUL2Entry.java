package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import controller.NAMControllerCompilerMain;

import jdpbfx.DBPFTGI;

public class RUL2Entry extends RULEntry {

//    private final boolean isESeries;
    private final boolean isLHD;
    private final Deque<Pattern> patterns;

    public RUL2Entry(DBPFTGI tgi, Queue<File> inputFiles, boolean isLHD, Collection<Pattern> patternsForExclusion, ChangeListener changeListener, ExecutorService executor) {
        super(tgi, inputFiles, changeListener, executor);
        this.isLHD = isLHD;
        this.patterns = new LinkedList<Pattern>(patternsForExclusion);
//        this.isESeries = isESeries;
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
            FileReader fReader = null;
            BufferedReader buffer = null;
            try {
                fReader = new FileReader(file);
                buffer = new BufferedReader(fReader);
                super.printSubFileHeader(file);

                for (String line = buffer.readLine(); line != null; line = buffer.readLine()) {
                    // special treatment for RUL2 header
                    if (!headerFound && line.trim().equalsIgnoreCase("[ruleoverrides]")) {
                        writer.write(line + newline);
                        headerFound = true;
                        continue;
                    }

                    // RHD/LHD-specific lines
                    if (!isLHD && line.startsWith(";###RHD###")) {
                        line = line.substring(10);
                    } else if (isLHD && line.startsWith(";###LHD###")) {
                        line = line.substring(10);
                    }

                    String[] splits = line.split(";", 2);
                    if (splits.length == 0) {
                        continue;
                    }
                    String[] iidsStrings = (line = (splits[0].trim())).split(",|=");
                    if (line.isEmpty()) {
                        continue;
                    }

                    boolean matchFound = false;
                    Iterator<Pattern> iter = patterns.iterator();

                    if (iidsStrings.length != 12) {
                        NAMControllerCompilerMain.LOGGER.warning("Invalid RUL override format for line: " + line);
                        matchFound = true;
                    } else {
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
                writer.write(newline);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (buffer != null) {
                    buffer.close();
                }
                if (fReader != null) {
                    fReader.close();
                }
            }
        }
        writer.flush();
    }

}
