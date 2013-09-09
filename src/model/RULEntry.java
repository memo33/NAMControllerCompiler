package model;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;
import java.util.logging.Level;

import javax.swing.event.ChangeListener;

import jdpbfx.DBPFEntry;
import jdpbfx.DBPFTGI;

public abstract class RULEntry extends DBPFEntry {
    
    private static final int BUFFER_SIZE = 8 * 1024;
    static final String newline = "\r\n";

    private final DateFormat dateFormat;
    private long lastModified;
    
    final ChangeListener changeListener;
    
    Queue<File> inputFiles;
    OutputStreamWriter writer = null;
    WritableByteChannel sink;

    RULEntry(DBPFTGI tgi, Queue<File> inputFiles, ChangeListener changeListener) {
        super(tgi);
        this.inputFiles = inputFiles;
        dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.changeListener = changeListener;
        this.calculateLastModified();
    }
    
    /**
     * finds the date of latest modification of input files.
     */
    private void calculateLastModified() {
        lastModified = 0;
        for (File file : inputFiles) {
            if (file.lastModified() > lastModified)
                lastModified = file.lastModified();
        }
    }
    
    /**
     * prints a timestamp.
     * @throws IOException 
     */
    void printHeader() throws IOException {
        writer.write(String.format(";### Date created: %s ###%s",
                dateFormat.format(new Date(lastModified)),
                newline));
    }
    
    /**
     * prints name and timestamp of subfile.
     * @param inputFile
     * @throws IOException 
     */
    void printSubFileHeader(File inputFile) throws IOException {
        writer.write(String.format(";### next file: %s ###%s;### last modified: %s ###%s",
                inputFile.getName(),
                newline,
                dateFormat.format(new Date(inputFile.lastModified())),
                newline));
    }

    /**
     * Must provide an implementation that writes all the needed data to the
     * {@link #writer}.
     * @throws IOException
     */
    abstract void provideData() throws IOException;

    @Override
    public ReadableByteChannel createDataChannel() {
        Pipe pipe = null;
        try {
            pipe = Pipe.open();
            sink = pipe.sink();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    OutputStream os = null;
                    try {
                        os = Channels.newOutputStream(sink);
                        writer = new OutputStreamWriter(new BufferedOutputStream(os, BUFFER_SIZE));
                        printHeader();
                        provideData();
                    } catch (IOException e) {
                        Thread currentThread = Thread.currentThread();
                        currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, e);
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e) {
                                Thread currentThread = Thread.currentThread();
                                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, e);
                            }
                        }
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                Thread currentThread = Thread.currentThread();
                                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, e);
                            }
                        }
                        if (sink != null) {
                            try {
                                sink.close();
                            } catch (IOException e) {
                                Thread currentThread = Thread.currentThread();
                                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, e);
                            }
                        }
                    }
                }
            }).start();
            return pipe.source();
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "IOException while creating data channel for RUL entry", e1);
            return null;
        }
    }
    
    /**
     * @return whether file has to be skipped because of s/e-series.
     */
    static boolean fileMatchesSeries(File file, boolean eSeriesFlag) {
        String filename = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
        return !(filename.endsWith("_e-series") && !eSeriesFlag
                || filename.endsWith("_s-series") && eSeriesFlag);                    
    }
    
    public long getLastModified() {
        return this.lastModified;
    }

}
