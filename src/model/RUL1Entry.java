package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jdpbfx.DBPFTGI;
import controller.NAMControllerCompilerMain;

/**
 * Used for RUL1 and the Network INI.
 */
public class RUL1Entry extends RULEntry {

//    private final boolean isESeries;
    private final boolean isLHD;
    private final boolean isINI;

    public RUL1Entry(DBPFTGI tgi, Queue<File> inputFiles, boolean isLHD, boolean isINI, ChangeListener changeListener, ExecutorService executor) {
        super(tgi, inputFiles, changeListener, executor);
        this.isLHD = isLHD;
        this.isINI = isINI;
    }

    @Override
    protected void provideData() throws IOException {
        NAMControllerCompilerMain.LOGGER.info("Writing file " + (this.isINI ? "Network INI" : "RUL1"));
        for (File file : inputFiles) {
            this.changeListener.stateChanged(new ChangeEvent(file));
//            if (!RULEntry.fileMatchesSeries(file, isESeries)) {
//                continue;
//            }
            FileInputStream fis = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                fis = new FileInputStream(file);
                isr = new InputStreamReader(fis);
                br = new BufferedReader(isr);
                printSubFileHeader(file);
//                super.writer.flush();

                // potentially inefficient because file is read line by line, but RUL1 is small,
                // so this is secondary
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    printLineChecked(line);
                }

                writer.write(newline);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (fis != null) {
                    fis.close();
                }
            }
        }
        writer.flush();
    }

    /**
     * checks for LHD/RHD specific code.
     * @param line
     * @throws IOException
     */
    private void printLineChecked(String line) throws IOException {
        if (!isLHD && line.startsWith(";###RHD###")) {
            writer.write(line.substring(10) + newline);
        } else if (isLHD && line.startsWith(";###LHD###")) {
            writer.write(line.substring(10) + newline);
        } else {
            writer.write(line + newline);
        }
    }
}
