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
import java.util.regex.Pattern;

import jdpbfx.DBPFTGI;

public class RUL2Entry extends RULEntry {
    
    private final boolean isESeries;
    private final Deque<Pattern> patterns;
    
    public RUL2Entry(DBPFTGI tgi, Queue<File> inputFiles, boolean isESeries, Collection<Pattern> patternsForExclusion) {
        super(tgi, inputFiles);
        this.patterns = new LinkedList<Pattern>(patternsForExclusion);
        this.isESeries = isESeries;
    }
    
    /*
     * parses files line by line, tests if pattern matches and excludes these lines as well as empty lines and comments;
     * @throws IOException 
     */
    @Override
    public void provideData() throws IOException {
        for (File file : inputFiles) {
            System.out.println(file.getName());
            if (!RULEntry.fileMatchesSeries(file, isESeries)) {
                continue;
            }
            FileReader fReader = null;
            BufferedReader buffer = null;
            try {
                fReader = new FileReader(file);
                buffer = new BufferedReader(fReader);
                super.printSubFileHeader(file);
                
                for (String line = buffer.readLine(); line != null; line = buffer.readLine()) {
                    String[] splits = line.split(";", 2);
                    if (splits.length == 0) {
                        continue;
                    }
                    String[] iidsStrings = (line = (splits[0].trim().toLowerCase())).split(",|=");
                    if (line.isEmpty()) {
                        continue;
                    }
                    
                    boolean someIidsMatch = false;
                    Iterator<Pattern> iter = patterns.iterator();
                    
                    LOOPENTRY:
                    for (Pattern p = iter.next(); iter.hasNext(); p = iter.next()) {
                        if (iidsStrings.length != 12) {
                            System.err.println(line);
                            someIidsMatch = true;
                            break;
                        }
                        for (int i = 9; i >= 0; i -= 3) {
                            someIidsMatch |= p.matcher(iidsStrings[i]).matches();
                            if (someIidsMatch) {
                                // let's maintain LRU order
                                iter.remove();
                                patterns.addFirst(p);
                                break LOOPENTRY;
                            }
                        }
                    }
                    
                    if (!someIidsMatch) {
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
