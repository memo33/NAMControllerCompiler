package controller;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class CompilerSettingsManager {
    
    private String input = "", output = "";
    private boolean lhdFlag = false;
    private final File dataFile, tempFile1, tempFile2;
    
    public CompilerSettingsManager(File dataFile) {
        this.dataFile = dataFile;
        this.tempFile1 = new File(dataFile.getPath() + "~1");
        this.tempFile2 = new File(dataFile.getPath() + "~2");
    }
    
    public void readSettings() throws FileNotFoundException {
        Scanner scanner = new Scanner(dataFile.exists() ? dataFile : (tempFile1.exists() ? tempFile1 : tempFile2));
        input = scanner.hasNextLine() ? scanner.nextLine() : "";
        output = scanner.hasNextLine() ? scanner.nextLine() : "";
        lhdFlag = scanner.hasNextLine() ? scanner.nextLine().equals("lhd") : false;
//        eseriesFlag = scanner.hasNextLine() ? scanner.nextLine().equals("eseries=true") : true;
        scanner.close();
    }
    
    /**
     * Writes the settings into the dataFile.
     */
    public void writeSettings(File inputDir, File outputDir, boolean isLHD) throws FileNotFoundException {
        // TODO write XML settings
        PrintWriter printer = new PrintWriter(tempFile1);
        printer.println(inputDir.getAbsolutePath());
        printer.println(outputDir.getAbsolutePath());
        printer.println(isLHD ? "lhd" : "rhd");
//        printer.println(isESeries ? "eseries=true" : "eseries=false");
        printer.close();
        
        if (dataFile.exists()) {
            if (tempFile2.exists()) {
                tempFile2.delete();
            }
            boolean success = dataFile.renameTo(tempFile2);
            if (!success) {
                LOGGER.warning("Renaming of settings file failed. Settings may not have been saved.");
                return;
            }
        }
        boolean success = tempFile1.renameTo(dataFile);
        if (!success) {
            LOGGER.warning("Renaming of settings file failed. Settings may not have been saved.");
        } else {
            LOGGER.info("Settings successfully saved.");
        }
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
