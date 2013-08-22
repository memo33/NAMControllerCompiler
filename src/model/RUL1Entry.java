package model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Queue;

import jdpbfx.DBPFTGI;

public class RUL1Entry extends RULEntry {
    
    private final boolean isESeries;

    public RUL1Entry(DBPFTGI tgi, Queue<File> inputFiles, boolean isESeries) {
        super(tgi, inputFiles);
        this.isESeries = isESeries;
    }

    @Override
    protected void provideData() throws IOException {
        for (File file : inputFiles) {
            if (!RULEntry.fileMatchesSeries(file, isESeries)) {
                continue;
            }
            FileInputStream fis = null;
            FileChannel fc = null;
            try {
                fis = new FileInputStream(file);
                fc = fis.getChannel();
                printSubFileHeader(file);
                super.writer.flush();
                
                // writer is empty now, so we can transfer to its parent: sink
                long size = fc.size();
                long len = fc.transferTo(0, size, super.sink);
                if (len != size) {
                    throw new IOException("Could not transfer file completely: " + file);
                }

                super.writer.write(newline + newline);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fc != null) {
                    fc.close();
                }
                if (fis != null) {
                    fis.close();
                }
            }
        }
        writer.flush();
    }
}
