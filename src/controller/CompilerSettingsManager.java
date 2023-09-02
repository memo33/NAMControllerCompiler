package controller;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.logging.Level;

import controller.CommandLineArguments.ArgumentID;

public class CompilerSettingsManager {

//    private String input = "", output = "";
//    private boolean lhdFlag = false;
    private final File dataFile, tempFile1, tempFile2;
    private final CommandLineArguments arguments;

    public CompilerSettingsManager(File[] dataFiles, CommandLineArguments arguments) {
        this.dataFile = dataFiles[0];
        this.tempFile1 = dataFiles[1];
        this.tempFile2 = dataFiles[2];
        this.arguments = arguments;
    }

    public void readSettings() throws FileNotFoundException {
        File fileToRead = dataFile.exists() ? dataFile : (tempFile1.exists() ? tempFile1 : tempFile2);
        Scanner scanner = new Scanner(fileToRead);
        int i = 0;
        while (scanner.hasNextLine()) {
            this.arguments.setArgument(i, scanner.nextLine().trim());
            i++;
        }
        if (i != CommandLineArguments.getExpectedArgumentCount()) {
            LOGGER.log(Level.SEVERE, "The number of arguments found in \"{0}\" does not match the expected number of " + CommandLineArguments.getExpectedArgumentCount(), fileToRead);
        }
        scanner.close();
    }

    /**
     * Writes the settings into the dataFile.
     */
    public void writeSettings(File inputDir, File outputDir, boolean isLHD) throws FileNotFoundException {
        arguments.setArgument(ArgumentID.INPUT_DIR, inputDir.getAbsolutePath());
        arguments.setArgument(ArgumentID.OUTPUT_DIR, outputDir.getAbsolutePath());
        arguments.setArgument(ArgumentID.RHD_FLAG, isLHD ? "0" : "1");

        PrintWriter printer = new PrintWriter(tempFile1);
        for (String arg : arguments) {
            printer.println(arg);
        }
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
        return arguments.getArgument(ArgumentID.INPUT_DIR);
    }

    /**
     * @return the output
     */
    public String getOutput() {
        return arguments.getArgument(ArgumentID.OUTPUT_DIR);
    }

    /**
     * @return the lhdFlag
     */
    public boolean getLhdFlag() {
        return arguments.getArgument(ArgumentID.RHD_FLAG).equals("0");
    }
}
