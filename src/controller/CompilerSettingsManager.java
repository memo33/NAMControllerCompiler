package controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class CompilerSettingsManager {
    
    private String input = "", output = "";
    private boolean lhdFlag = false;
    private final File dataFile;
    
    public CompilerSettingsManager(File dataFile) {
        this.dataFile = dataFile;
    }
    
    public void readSettings() throws FileNotFoundException {
        Scanner scanner = new Scanner(dataFile);
        input = scanner.hasNextLine() ? scanner.nextLine() : "";
        output = scanner.hasNextLine() ? scanner.nextLine() : "";
        lhdFlag = scanner.hasNextLine() ? scanner.nextLine().equals("lhd=true") : false;
//        eseriesFlag = scanner.hasNextLine() ? scanner.nextLine().equals("eseries=true") : true;
        scanner.close();
    }
    
    /**
     * Writes the settings into the dataFile.
     */
    public void writeSettings(File inputDir, File outputDir, boolean isLHD) throws FileNotFoundException {
        // TODO write XML settings
        PrintWriter printer = new PrintWriter(dataFile);
        printer.println(inputDir.getAbsolutePath());
        printer.println(outputDir.getAbsolutePath());
        printer.println(isLHD ? "lhd=true" : "lhd=false");
//        printer.println(isESeries ? "eseries=true" : "eseries=false");
        printer.close();
    }

    /**
     * @return the input
     */
    public String getInput() {
        return input;
    }

    /**
     * @return the output
     */
    public String getOutput() {
        return output;
    }

    /**
     * @return the lhdFlag
     */
    public boolean getLhdFlag() {
        return lhdFlag;
    }
}
