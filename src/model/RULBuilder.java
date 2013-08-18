package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;
import java.util.regex.Pattern;

import model.dbpf.DBPFUncompressedOutputStream;

/**
 * Assembles a RUL file out of multiple subfiles.
 * @author memo
 */
public class RULBuilder {

	Queue<File> inputFiles;
	DBPFUncompressedOutputStream out;
	private long lastModified;
	private DateFormat dateFormat;
	private boolean isESeries;
	
	/**
	 * constructs a RUL1-Builder. Does not parse the input files, but simply copies them.
	 * @param files inputfiles to be parsed.
	 * @param out stream into dbpf file.
	 * @param isESeries
	 * @return a RUL1-Builder for building RUL1.
	 */
	public static RULBuilder getRUL1Builder(Queue<File> files, DBPFUncompressedOutputStream out, boolean isESeries) {
		return new RULBuilder(files, out, isESeries);
	}
	
	/**
	 * constructs a RUL2-Builder. Parses the input files for the delivered patterns.
	 * If any of them match, the particular line is excluded. If no patterns are delivered,
	 * the input files do not need to be parsed which speeds up the compilation process.
	 * @param files inputfiles to be parsed.
	 * @param out stream into dbpf file.
	 * @param isESeries
	 * @param patternsForExclusion if null or empty inputfiles are not parsed, but copied
	 * 			which considerably speeds up compilation.
	 * @return a RUL2-Builder for building RUL2.
	 */
	public static RULBuilder getRUL2Builder(Queue<File> files, DBPFUncompressedOutputStream out, boolean isESeries, Queue<Pattern> patternsForExclusion) {
		if (patternsForExclusion == null || patternsForExclusion.isEmpty())
			return new RULBuilder(files, out, isESeries);
		else
			return new RUL2Builder(files, out, isESeries, patternsForExclusion);
	}
	
	/**
	 * constructs a RUL0-Builder. Parses the input files in an appropriate manner to
	 * separate orderings from hid sections.
	 * @param files inputfiles to be parsed.
	 * @param out stream into dbpf file.
	 * @param isLHD
	 * @param isESeries
	 * @return a RUL0-Builder for building RUL0.
	 */
	public static RULBuilder getRUL0Builder(Queue<File> files, DBPFUncompressedOutputStream out, boolean isLHD, boolean isESeries) {
		return new RUL0Builder(files, out, isLHD, isESeries);
	}
	
	/**
	 * @param files
	 * @param out
	 * @param isESeries
	 */
	RULBuilder(Queue<File> files, DBPFUncompressedOutputStream out, boolean isESeries) {
		this.inputFiles = files;
		this.out = out;
		dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.isESeries = isESeries;
	}
	
	/**
	 * main routine of copy/printing the RULs.
	 * @throws IOException 
	 */
	public void processFiles() throws IOException {
		calculateLastModified();
		printHeader();
		for (File file : inputFiles) {
			if (mustSkip(file)) continue;
			try (FileReader fReader = new FileReader(file);
					BufferedReader buffer = new BufferedReader(fReader)) {
				printSubFileHeader(file);
				out.writeFile(file);
				out.write("\r\n\r\n".getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @return whether file has to be skipped because of s/e-series.
	 */
	protected boolean mustSkip(File file) {
		String filename = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
		return (filename.endsWith("_e-series") && !isESeries
				|| filename.endsWith("_s-series") && isESeries);					
	}

	/**
	 * prints name and timestamp of subfile.
	 * @param inputFile
	 * @throws IOException 
	 */
	void printSubFileHeader(File inputFile) throws IOException {
		out.write((";### next file: " + inputFile.getName() + " ###\r\n"
				+ ";### last modified: " + dateFormat.format(new Date(inputFile.lastModified()))
				+ " ###\r\n").getBytes());
	}
	
	/**
	 * prints a timestamp.
	 * @throws IOException 
	 */
	void printHeader() throws IOException {
		out.write((";### Date created: "
				+ dateFormat.format(new Date(lastModified)) + " ###\r\n").getBytes());
	}
	
	/**
	 * finds the date of latest modification of input files.
	 */
	void calculateLastModified() {
		lastModified = 0;
		for (File file : inputFiles) {
			if (file.lastModified() > lastModified)
				lastModified = file.lastModified();
		}
	}
	
	/**
	 * @return the date of last modification within this file
	 */
	public long getLastModified() {
		return this.lastModified;
	}
}
