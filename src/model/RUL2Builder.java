package model;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;
import java.util.regex.Pattern;

import model.dbpf.DBPFUncompressedOutputStream;

public class RUL2Builder extends RULBuilder {

	private Queue<Pattern> patterns;
	private PrintWriter printer;
	private BufferedOutputStream bufferedOut;
	
	RUL2Builder(Queue<File> files, DBPFUncompressedOutputStream out, boolean isESeries, Queue<Pattern> patternsForExclusion) {
		super(files, out, isESeries);
		this.patterns = patternsForExclusion;
		bufferedOut = new BufferedOutputStream(out);
		printer = new PrintWriter(bufferedOut);
	}
	
	/*
	 * parses files line by line, tests if pattern matches and excludes these lines as well as empty lines and comments;
	 * @throws IOException 
	 */
	@Override
	public void processFiles() throws IOException {
		super.calculateLastModified();
		super.printHeader();
		for (File file : inputFiles) {
			if (mustSkip(file)) continue;
			FileReader fReader = null;
			BufferedReader buffer = null;
			try {
			    fReader = new FileReader(file);
				buffer = new BufferedReader(fReader);
				super.printSubFileHeader(file);
				
				for (String line = buffer.readLine(); line != null; line = buffer.readLine()) {
					String[] splits = line.split(";", 2);
					if (splits.length == 0) continue;
					String[] iidsStrings = (line = (splits[0].trim().toLowerCase())).split(",|=");
					boolean someIidsMatch = false;
					loopentry:
					for (Pattern p : patterns) {
						for (int i = 0; i < iidsStrings.length; i += 3) {
							someIidsMatch |= p.matcher(iidsStrings[i]).matches();
							if (someIidsMatch) break loopentry;
						}
					}
					if (!someIidsMatch && !line.isEmpty())
						printer.println(line);
				}
				printer.println();
				printer.flush();
				bufferedOut.flush();
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
	}
	
}
