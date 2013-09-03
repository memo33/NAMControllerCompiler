package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import controller.NAMControllerCompilerMain;

import jdpbfx.DBPFTGI;

/**
 * Specifically for the RUL0 file. The RUL0 file is actually parsed in here.
 * @author memo
 */
public class RUL0Entry extends RULEntry {

	private final boolean isLHD;//, isESeries;

	public RUL0Entry(DBPFTGI tgi, Queue<File> inputFiles, boolean isLHD, ChangeListener changeListener) {
	    super(tgi, inputFiles, changeListener);
		this.isLHD = isLHD;
//		this.isESeries = isESeries;
	}

	@Override
	public void provideData() throws IOException {
        NAMControllerCompilerMain.LOGGER.info("Writing file RUL0");
		Collection<FileReader> fReaders = new ArrayDeque<FileReader>(inputFiles.size());
		Queue<BufferedReader> bufReaders = new ArrayDeque<BufferedReader>(fReaders.size());
		try {
    		/* print the orderings */
    		for (File file : inputFiles) {
    		    this.changeListener.stateChanged(new ChangeEvent(file));
//    			if (!RULEntry.fileMatchesSeries(file, isESeries)) {
//    			    continue;
//    			}
    			FileReader fReader = new FileReader(file);
    			fReaders.add(fReader);
    			BufferedReader bufReader = new BufferedReader(fReader);
    			bufReaders.add(bufReader);
    			// read until separator, end of ordering
    			for (String line = bufReader.readLine(); line != null; line = bufReader.readLine()) {
    				if (line.startsWith(";###separator") || line.startsWith(";### separator")) {
    					break;
    				}
    				printLineChecked(line);
    			}
    		}
    		writer.write(newline);
    		/* print the HID sections */
    		Iterator<File> fileIter = inputFiles.iterator();
    		Iterator<BufferedReader> bufIter = bufReaders.iterator();
    		while (fileIter.hasNext() && bufIter.hasNext()) {
    			File file = fileIter.next();
    			BufferedReader bufReader = bufIter.next();
    			
    			super.printSubFileHeader(file);
    			for (String line = bufReader.readLine(); line != null; line = bufReader.readLine())
    				printLineChecked(line);
    			writer.write(newline);
    			bufReader.close();
    		}
    		writer.flush();
		} finally {
    		for (FileReader fReader : fReaders) {
    			fReader.close();
    		}
		}
	}

	/**
	 * checks for E-Series and LHD/RHD specific code.
	 * @param line
	 * @throws IOException 
	 */
	private void printLineChecked(String line) throws IOException {
        if (line.startsWith(";###RHD###") && !isLHD)
            writer.write(line.substring(10) + newline);
        else if (line.startsWith(";###LHD###") && isLHD)
            writer.write(line.substring(10) + newline);
//        else if (line.startsWith(";###E-Series###") && isESeries)
//            writer.write(line.substring(15) + newline);
        else
            writer.write(line + newline);
	}

}
