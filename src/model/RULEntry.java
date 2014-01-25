package model;

import static controller.NAMControllerCompilerMain.LOGGER;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.swing.event.ChangeListener;

import jdpbfx.DBPFEntry;
import jdpbfx.DBPFTGI;

import org.codehaus.groovy.control.CompilationFailedException;

public abstract class RULEntry extends DBPFEntry {

    private static final int BUFFER_SIZE = 8 * 1024;
    static final String newline = "\r\n";

    private final DateFormat dateFormat;
    private long lastModified;

    final ChangeListener changeListener;

    Queue<File> inputFiles;
    OutputStreamWriter writer = null;

    private final ExecutorService executor;
    private final ExecutorService groovyExecutor;
    private Future<Void> result;

    RULEntry(DBPFTGI tgi, Queue<File> inputFiles, ChangeListener changeListener, ExecutorService executor, ExecutorService groovyExecutor) {
        super(tgi);
        this.inputFiles = inputFiles;
        dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.changeListener = changeListener;
        this.calculateLastModified();
        this.executor = executor;
        this.groovyExecutor = groovyExecutor;
    }

    /**
     * finds the date of latest modification of input files.
     */
    private void calculateLastModified() {
        lastModified = 0;
        for (File file : inputFiles) {
            if (file.lastModified() > lastModified) {
                lastModified = file.lastModified();
            }
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

    public Future<Void> getExecutionResult() {
        return this.result;
    }

    @Override
    public ReadableByteChannel createDataChannel() {
        try {
            PipedInputStream pis = new PipedInputStream();
            // sink needs to be connected here to avoid subsequent reads from unconnected pipe
            final PipedOutputStream sink = new PipedOutputStream(pis);

            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        try (OutputStreamWriter writerReference = new OutputStreamWriter(
                                new BufferedOutputStream(sink, BUFFER_SIZE))) {
                            writer = new OutputStreamWriter(new BufferedOutputStream(sink, BUFFER_SIZE));
                            printHeader();
                            provideData();
                        } catch (IOException e1) {
                            Thread currentThread = Thread.currentThread();
                            currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, e1);
                        } // auto-closes writer
                    } finally {
                        if (sink != null) {
                            try {
                                sink.close();
                            } catch (IOException e) {
                                Thread currentThread = Thread.currentThread();
                                currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, e);
                            }
                        }
                    }
                    return null;
                }
            };
            result = executor.submit(callable);
            return Channels.newChannel(pis);
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "IOException while creating data channel for RUL entry", e1);
            throw new RuntimeException("IOException while creating data channel for RUL entry", e1);
        }
    }

//    /**
//     * @return whether file has to be skipped because of s/e-series.
//     */
//    static boolean fileMatchesSeries(File file, boolean eSeriesFlag) {
//        String filename = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
//        return !(filename.endsWith("_e-series") && !eSeriesFlag
//                || filename.endsWith("_s-series") && eSeriesFlag);
//    }

    public long getLastModified() {
        return this.lastModified;
    }

    InputStream createGroovyInputStream(File groovyFile) throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException {
        ClassLoader parent = RUL2Entry.class.getClassLoader();
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        Class<?> groovyClass = loader.parseClass(groovyFile);

        final GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
        PipedInputStream pis = new PipedInputStream();
        final PrintStream printer = new PrintStream(new PipedOutputStream(pis));
        groovyObject.setProperty("out", printer);
        Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() {
                groovyObject.invokeMethod("run", new Object[] {});
                printer.close(); // closes underlying stream
                return null;
            }
        };
        assert groovyExecutor != null; // groovy is currently only supported for RUL1 and RUL2
        groovyExecutor.submit(callable); // will not throw any exception, so no need to keep a reference to result
        loader.close();
        return pis;
    }
}
