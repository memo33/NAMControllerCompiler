package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import model.dbpf.DBPFUncompressedOutputStream;

/**
 * Specifically for the RUL0 file. The RUL0 file is actually parsed in here.
 * @author memo
 */
public class RUL0Builder extends RULBuilder {

	private Queue<BufferedReader> bufReaders;
	private boolean isLHD, isESeries;
	private PrintWriter printer;

	RUL0Builder(Queue<File> files, DBPFUncompressedOutputStream out, boolean isLHD, boolean isESeries) {
		super(files, out, isESeries);
		this.isLHD = isLHD;
		this.isESeries = isESeries;
		bufReaders = new LinkedList<BufferedReader>();
		printer = new PrintWriter(out);
	}

	@Override
	public void processFiles() throws IOException {
		super.calculateLastModified();
		super.printHeader();
		Collection<FileReader> fReaders = new LinkedList<FileReader>();

		/* print the orderings */
		for (File file : inputFiles) {
			if (mustSkip(file)) continue;
			FileReader fReader = new FileReader(file);
			fReaders.add(fReader);
			BufferedReader bufReader = new BufferedReader(fReader);
			bufReaders.add(bufReader);
			for (String line = bufReader.readLine(); line != null; line = bufReader.readLine()) {
				if (line.startsWith(";###separator") || line.startsWith(";### separator")) {
					break;
				}
				printLineChecked(line);
			}
		}
		printer.println();
		printer.flush();
		/* print the HID sections */
		Iterator<File> fileIter = inputFiles.iterator();
		Iterator<BufferedReader> bufIter = bufReaders.iterator();
		while (fileIter.hasNext() && bufIter.hasNext()) {
			File file = fileIter.next();
			BufferedReader bufReader = bufIter.next();
			super.printSubFileHeader(file);
			for (String line = bufReader.readLine(); line != null; line = bufReader.readLine())
				printLineChecked(line);
			printer.println();
			bufReader.close();
			printer.flush();
		}
		for (FileReader fReader : fReaders)
			fReader.close();
	}

	/**
	 * checks for E-Series and LHD/RHD specific code.
	 * @param line
	 */
	private void printLineChecked(String line) {
		if (line.startsWith(";###RHD###") && !isLHD)
			printer.println(line.substring(10));
		else if (line.startsWith(";###LHD###") && isLHD)
			printer.println(line.substring(10));
		else if (line.startsWith(";###E-Series###") && isESeries)
			printer.println(line.substring(15));
		else
			printer.println(line);
	}

}
