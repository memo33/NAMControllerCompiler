package controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import model.RULBuilder;
import model.dbpf.DBPFTGI;
import model.dbpf.DBPFUncompressedOutputStream;
import view.DevelopersFrame;
import view.checkboxtree.CheckTreeManager;
import controller.XMLParsing.MyNode;

/**
 * Main class of NAMControllerCompiler.
 * @author memo
 */
public class NAMControllerCompilerMain {

	private static File inputDir, outputDir;
	private static boolean isLHD, isESeries;
	private static File[] rulDirs;				// rul0, rul1, rul2
	private static File outputFile;
	private static File dataFile = new File("NAMControllerCompilerData.txt");
	private static File xmlFile = new File("xml/RUL2_IID_structure.xml");
	private static Queue<File>[] rulInputFiles;
	private static long starttime;
	private static Queue<Pattern> patterns;		// regexes matching iids to exclude from rul2

	private static int typeRUL = 0x0a5bcf4b, groupRUL = 0xaa5bcf57;
	private static int[] instanceRULs = {0x10000000, 0x10000001, 0x10000002};

	private static FileFilter fileFilter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory() ||
					pathname.getName().endsWith(".txt") ||
					pathname.getName().endsWith(".rul");
		}
	};

	/**
	 * Arguments are optional. If no arguments are delivered, the GUI is started.
	 * @param args
	 * 				path to input directory,
	 * 				path to output directory,
	 * 				(optionally) "lhd=true" to specify that LHD controller is built.
	 */
	public static void main(String[] args) {
		starttime = System.currentTimeMillis();
		
		if (args.length == 0) {
			showGUI();
			return;
		}
		if (args.length != 2 && args.length != 3)
			throw new RuntimeException(
					"Wrong number of arguments. Specify the path to input" +
							" folder, to output folder and, optionally, if you want to" +
					" compile a LHD controller by \"lhd=true\".");
		isLHD = args.length==3 && args[2].toLowerCase().equals("lhd=true");
		isESeries = true; // TODO

		inputDir = new File(args[0]);
		outputDir = new File(args[1]);
		try {
			init();
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}

		try {
			writeControllerFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Inititializes the main files and tests, if they exist.
	 * @throws FileNotFoundException
	 */
	private static void init() throws FileNotFoundException {
		rulDirs = new File[3];
		for (int i = 0; i < rulDirs.length; i++)
			rulDirs[i] = new File(inputDir, "RUL" + i);
		testIfFilesExist();

		rulInputFiles = collectRULInputFiles();

		outputFile = new File(outputDir, "NetworkAddonMod_Controller_"
				+ (isLHD ? "LEFT" : "RIGHT") + "_HAND_VERSION"
				+ (isESeries ? "_e-series" : "_s-series")
				+ ".dat");		
	}
	
	/**
	 * Gets data from dataFile and displays Frame.
	 */
	private static void showGUI() {
		String input = "", output = "";
		boolean lhdFlag = false, eseriesFlag = true;
		if (dataFile.exists()) {
			try {
				Scanner scanner = new Scanner(dataFile);
				input = scanner.hasNextLine() ? scanner.nextLine() : "";
				output = scanner.hasNextLine() ? scanner.nextLine() : "";
				lhdFlag = scanner.hasNextLine() ? scanner.nextLine().equals("lhd=true") : false;
				eseriesFlag = scanner.hasNextLine() ? scanner.nextLine().equals("eseries=true") : true;
				scanner.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		try {
			JTree tree = XMLParsing.buildJTreeFromXML(xmlFile);
			final CheckTreeManager checkTreeManager = new CheckTreeManager(tree);

			final DevelopersFrame devFrame = new DevelopersFrame(input, output, lhdFlag, eseriesFlag, tree);
			devFrame.addStartListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					starttime = System.currentTimeMillis();
					patterns = collectPatterns(checkTreeManager);
					
					NAMControllerCompilerMain.inputDir = new File(devFrame.getInputPath());
					NAMControllerCompilerMain.outputDir = new File(devFrame.getOutputPath());
					NAMControllerCompilerMain.isLHD = devFrame.isLHD();
					NAMControllerCompilerMain.isESeries = devFrame.isESeries();
					try {
						init();
					} catch (FileNotFoundException e) {
						JOptionPane.showMessageDialog(devFrame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
	//				devFrame.setEnabled(false);
					if (outputFile.exists()) {
						int opt = JOptionPane.showConfirmDialog(devFrame, "The file " + outputFile.getName() + " already exists. Do you wish to overwrite?", "Fill already exists", JOptionPane.YES_NO_OPTION);
						if (opt != JOptionPane.YES_OPTION)
							return;
					}
					writeSettings();
					try {
						writeControllerFile();
						JOptionPane.showMessageDialog(devFrame, "The file has been successfully compiled.");
						System.exit(0);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(devFrame, "An error occured.", "Error", JOptionPane.ERROR_MESSAGE);
	//					devFrame.setEnabled(true);
					}
				}
			});
			devFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			devFrame.pack();
			devFrame.setLocationRelativeTo(null);
			devFrame.setVisible(true);
		} catch (PatternSyntaxException e1) { // TODO differentiate between exceptions, currently all catches are the same
		    JOptionPane.showMessageDialog(null, "An Exception was thrown:\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e1.printStackTrace();
        } catch (ParserConfigurationException e1) {
            JOptionPane.showMessageDialog(null, "An Exception was thrown:\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e1.printStackTrace();
        } catch (SAXException e1) {
            JOptionPane.showMessageDialog(null, "An Exception was thrown:\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e1.printStackTrace();
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(null, "An Exception was thrown:\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e1.printStackTrace();
        }
	}
	
	/**
	 * Writes the settings into the dataFile.
	 */
	private static void writeSettings() {
	    PrintWriter printer = null;
	    try {
	        printer = new PrintWriter(dataFile);
			printer.println(inputDir.getAbsolutePath());
			printer.println(outputDir.getAbsolutePath());
			printer.println(isLHD ? "lhd=true" : "lhd=false");
			printer.println(isESeries ? "eseries=true" : "eseries=false");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
		    if (printer != null) {
		        printer.close();
		    }
		}
	}

	/**
	 * The actual writing of the controller file.
	 * @throws IOException
	 */
	public static void writeControllerFile() throws IOException {
	    DBPFUncompressedOutputStream out = null;
		try {
		    out = new DBPFUncompressedOutputStream(outputFile);

			RULBuilder[] rulBuilders = new RULBuilder[3];
			long lastModf = 0;
			// RUL files
			for (int i = 0; i < rulBuilders.length; i++) {
				log("Writing file RUL" + i);
				out.writeTGI(new DBPFTGI(typeRUL, groupRUL, instanceRULs[i]));
				if (i==0)
					rulBuilders[i] = RULBuilder.getRUL0Builder(rulInputFiles[i], out, isLHD, isESeries);
				else if (i==1)
					rulBuilders[i] = RULBuilder.getRUL1Builder(rulInputFiles[i], out, isESeries);
				else
					rulBuilders[i] = RULBuilder.getRUL2Builder(rulInputFiles[i], out, isESeries, patterns);
					
				rulBuilders[i].processFiles();
				
				if(rulBuilders[i].getLastModified() > lastModf)
					lastModf = rulBuilders[i].getLastModified();
			}
			// LText (Controller marker)
			log("Writing file LText");
			out.writeTGI(new DBPFTGI(0x2026960b, 0x123006aa, 0x6a47ffff));
			writeLText(getDateString(lastModf, isLHD), out);
			
		} catch (IOException e) {
			log("Compiler finished with errors.");
			throw e;
		} finally {
		    if (out != null) {
		        out.close();
		    }
			log("Done. Total time consumed: " +
					(System.currentTimeMillis() - starttime) + " milliseconds.");
		}
	}
	
	/**
	 * Writing the LText file.
	 * @param text
	 * @param out
	 * @throws IOException
	 */
	private static void writeLText(String text, DBPFUncompressedOutputStream out) throws IOException {
		out.write(text.length());
		byte[] b = {0,0,0x10};
		out.write(b);
		char[] c = text.toCharArray();
		for (int i = 0; i < c.length; i++) {
			out.write(c[i]);
			out.write(0);
		}
		out.flush();
	}
	
	/**
	 * Get content of the controller description LText.
	 * @param date
	 * @param isLHD
	 * @return
	 */
	private static String getDateString(long date, boolean isLHD) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy MMM dd - HH:mm:ss (z)", Locale.ENGLISH);
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return "Version: " + (isLHD ? "LHD " : "RHD ") + (isESeries ? "(e-series) - " : "(s-series) - ") + dateFormat.format(new Date(date));
	}

	/**
	 * @return an array containing three Queues containing all
	 * .txt- and .rul-subfiles of the input folders in alphabetical
	 * order.
	 */
	private static Queue<File>[] collectRULInputFiles() {
		log("Collecting input data.");
		@SuppressWarnings("unchecked")
		Queue<File>[] rulInputFiles = new Queue[3];
		for (int i = 0; i < rulInputFiles.length; i++)
			rulInputFiles[i] = collectRULInputFilesRecursion(rulDirs[i]);
		return rulInputFiles;
	}
	
	private static void log(String message) {
		System.out.println("NAMControllerCompiler: " + message);
	}

	/**
	 * Recursive collecting.
	 * @param parent parent file of the directory.
	 * @return Queue of subfiles.
	 */
	private static Queue<File> collectRULInputFilesRecursion(File parent) {
		Queue<File> returnFiles = new LinkedList<File>();
		File[] subFiles = parent.listFiles(fileFilter);
		Arrays.sort(subFiles);					// sort files alphabetically

		for (int i = 0; i < subFiles.length; i++) {
			if (subFiles[i].isDirectory())
				returnFiles.addAll(collectRULInputFilesRecursion(subFiles[i]));
			else
				returnFiles.add(subFiles[i]);
		}
		return returnFiles;
	}

	/**
	 * @throws FileNotFoundException if specified directories do not exist or specified
	 * directory is not a directory.
	 */
	private static void testIfFilesExist() throws FileNotFoundException {
		List<File> files = new ArrayList<File>();
		files.add(inputDir);
		files.add(outputDir);
		for (File file : rulDirs) {
			files.add(file);
		}
		for (File file : files) {
			if (!file.exists())
				throw new FileNotFoundException("Directory does not exist: " + file.getPath());
			if (!file.isDirectory())
				throw new FileNotFoundException("File is not a directory: " + file.getPath());
		}
	}
	
	/**
	 * Collects the Regex-patterns from the selected nodes, specified by the checkTreeManager.
	 * @param checkTreeManager from the checkBoxTree.
	 * @return a queue containing the patterns.
	 */
	private static Queue<Pattern> collectPatterns(CheckTreeManager checkTreeManager){
		Queue<Pattern> patterns = new ArrayDeque<Pattern>();
		TreePath[] checkedPaths = checkTreeManager.getSelectionModel().getSelectionPaths();
		for (int i = 0; i < checkedPaths.length; i++) {
			log("Selected Node: " + checkedPaths[i].toString());
			MyNode node = (MyNode) checkedPaths[i].getLastPathComponent();
			collectPatterns(patterns, node);
		}
		return patterns;
	}
	
	/**
	 * Recursive collecting.
	 * @param patterns an existing queue into which the patterns are to be inserted.
	 * @param node of the sub-tree.
	 */
	private static void collectPatterns(Queue<Pattern> patterns, MyNode node) {
		if (node.hasPatterns()) {
			for (Pattern p : node) {
				patterns.add(p);
			}
		} else {
			@SuppressWarnings("rawtypes")
			Enumeration children = node.children();
			while (children.hasMoreElements()) {
				MyNode child = (MyNode) children.nextElement();
				collectPatterns(patterns, child);
			}	
		}
	}
}
