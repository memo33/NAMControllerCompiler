package controller.tasks;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.parboiled.Parboiled;

import controller.CompileMode;
import jdpbfx.DBPFEntry;
import jdpbfx.DBPFFile;
import jdpbfx.DBPFTGI;
import jdpbfx.types.DBPFLText;
import model.MetaController;
import model.MetaOverrideParser;
import model.RUL0Entry;
import model.RUL1Entry;
import model.RUL2Entry;
import model.RULEntry;
import view.View;

public abstract class WriteControllerTask implements ExecutableTask {
   
    private final DBPFTGI[] RUL_TGIS = {
            DBPFTGI.RUL.modifyTGI(-1L, -1L, 0x10000000L),
            DBPFTGI.RUL.modifyTGI(-1L, -1L, 0x10000001L),
            DBPFTGI.RUL.modifyTGI(-1L, -1L, 0x10000002L)};
    private final DBPFTGI LTEXT_TGI = DBPFTGI.valueOf(0x2026960bL, 0x123006aaL, 0x6a47ffffL);
    
    private final CollectRULsTask collectRULsTask;
    private final boolean isLHD;
    private final Queue<Pattern> patterns;
    private final URI inputURI;
    private final File outputFile;
    private final View view;
    private final File metaRuleDefinitionsFile;

    private long starttime;
    
    public static ExecutableTask getInstance(CompileMode mode, CollectRULsTask collectRULsTask,
            boolean isLHD, Queue<Pattern> patterns, URI inputURI, File outputFile, View view, File metaRuleDefinitionsFile) {
        if (mode.isInteractive()) {
            return new GUITask(collectRULsTask, isLHD, patterns, inputURI, outputFile, view, metaRuleDefinitionsFile);
        } else {
            return new CommandLineTask(collectRULsTask, isLHD, patterns, inputURI, outputFile, view, metaRuleDefinitionsFile);
        }
    }

    public abstract boolean get() throws InterruptedException, ExecutionException;

    private WriteControllerTask(CollectRULsTask collectRULsTask,
            boolean isLHD, Queue<Pattern> patterns, URI inputURI, File outputFile, View view, File metaRuleDefinitionsFile) {
        this.collectRULsTask = collectRULsTask;
        this.isLHD = isLHD;
        this.patterns = patterns;
        this.inputURI = inputURI;
        this.outputFile = outputFile;
        this.view = view;
        this.metaRuleDefinitionsFile = metaRuleDefinitionsFile;
    }
    
    private Boolean mainProcess(final Publisher publisher) throws FileNotFoundException, IOException, InterruptedException, ExecutionException {
        starttime = System.currentTimeMillis();
        Queue<File>[] rulInputFiles = WriteControllerTask.this.collectRULsTask.get();
        int max = 0;
        for (int i = 0; i < rulInputFiles.length; i++) {
            max += rulInputFiles[i].size();
        }
        
        WriteControllerTask.this.view.initProgress("Processing file...", 0, max + 1);
        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                File file = (File) e.getSource();
                LOGGER.fine(WriteControllerTask.this.inputURI.relativize(file.toURI()).toString());
                publisher.publish(file.getName());
            }
        };
        
        Queue<DBPFEntry> writeList = new ArrayDeque<DBPFEntry>();
        {
            long lastModf = 0;
            
            final MetaController metaController = new MetaController();
            MetaOverrideParser overrideParser = Parboiled.createParser(MetaOverrideParser.class, metaController);
            
            // RUL files
            for (int i = 0; i < RUL_TGIS.length; i++) {
                RULEntry rulEntry;
                if (i==0) {
                    rulEntry = new RUL0Entry(RUL_TGIS[i], rulInputFiles[i], WriteControllerTask.this.isLHD, changeListener);
                } else if (i==1) {
                    rulEntry = new RUL1Entry(RUL_TGIS[i], rulInputFiles[i], WriteControllerTask.this.isLHD, changeListener, metaRuleDefinitionsFile, metaController, overrideParser);
//                    rulEntry = new RUL1Entry(RUL_TGIS[i], rulInputFiles[i], WriteControllerTask.this.isLHD, changeListener);
                } else {
                    rulEntry = new RUL2Entry(RUL_TGIS[i], rulInputFiles[i], WriteControllerTask.this.patterns, changeListener, overrideParser);
                }
                if(rulEntry.getLastModified() > lastModf) {
                    lastModf = rulEntry.getLastModified();
                }
                writeList.add(rulEntry);
            }
            // LText (Controller marker)
            LOGGER.info("Adding Controller marker description text");
            DBPFLText ltext = new DBPFLText(new byte[0], LTEXT_TGI, false);
            ltext.setString(getControllerMarkerText(lastModf, WriteControllerTask.this.isLHD));
            writeList.add(ltext);
        }
        // write to file
        return DBPFFile.Writer.write(WriteControllerTask.this.outputFile, writeList);
    }
    
    private void determineResult() {
//      try {
//          if (!this.isCancelled() && this.get()) {
//              LOGGER.log(Level.INFO,
//                      "Total time for writing: {0} milliseconds.", this.getTimeConsumed());
//              view.publishInfoMessage("The file \"{0}\" has been successfully compiled", outputFile.toString());
//              view.dispose();
//              System.exit(0);
//          } else {
//              view.publishIssue("Compiler finished with errors.");
//              view.dispose();
//              System.exit(-1);
//          }
//      } catch (InterruptedException e) {
//          view.publishException("Compiler finished with errors: " + e.getLocalizedMessage(), e);
//          view.dispose();
//          System.exit(-1);
//      } catch (ExecutionException e) {
//          view.publishException("Compiler finished with errors: " + e.getLocalizedMessage(), e);
//          view.dispose();
//          System.exit(-1);
//      }
      boolean successful = false;
      try {
          successful = this.get();
      } catch (InterruptedException e) {
          view.publishException(e.getLocalizedMessage(), e);
      } catch (ExecutionException e) {
          view.publishException(e.getLocalizedMessage(), e);
      } catch (CancellationException e) { // should not happen
          view.publishException("Unexpected exception! " + e.getLocalizedMessage(), e);
      }
      // FIXME Find out for success, if the UncaughtExceptionHandler has not received any exceptions
      if (!successful) {
          view.publishIssue("Compiler finished with errors.");
          view.dispose();
          System.exit(-1);
      } else {
          LOGGER.log(Level.INFO,
                  "Total time for writing: {0} milliseconds.", this.getTimeConsumed());
          view.publishInfoMessage("The file \"{0}\" has been successfully compiled", outputFile.toString());
          view.dispose();
          System.exit(0);
      }
  }

    /**
     * Get content of the controller description LText.
     * @param date
     * @param isLHD
     * @return
     */
    private String getControllerMarkerText(long date, boolean isLHD) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy MMM dd - HH:mm:ss (z)", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//        return "Version: " + (isLHD ? "LHD " : "RHD ") + (isESeries ? "(e-series) - " : "(s-series) - ") + dateFormat.format(new Date(date));
        return String.format("Version: %s - %s",
                isLHD ? "LHD" : "RHD",
                dateFormat.format(new Date(date)));
    }
    
    private long getTimeConsumed() {
        return System.currentTimeMillis() - starttime;
    }

    private static class GUITask extends WriteControllerTask {
        
        private final SwingWorker<Boolean, String> worker;
        
        private GUITask(CollectRULsTask collectRULsTask,
                boolean isLHD, Queue<Pattern> patterns, URI inputURI, File outputFile, View view, File metaRuleDefinitionsFile) {
            super(collectRULsTask, isLHD, patterns, inputURI, outputFile, view, metaRuleDefinitionsFile);
            worker = new MyWorker();
        }

        @Override
        public void execute() {
            worker.execute();
        }

        @Override
        public boolean get() throws InterruptedException, ExecutionException {
            return worker.get();
        }

        private class MyWorker extends SwingWorker<Boolean, String> implements Publisher {
            @Override
            protected Boolean doInBackground() throws FileNotFoundException, IOException, InterruptedException, ExecutionException {
                return GUITask.super.mainProcess(this);
            }
            @Override
            protected void process(List<String> chunks) {
                // gets called in EDT
                GUITask.super.view.publishProgressIncrement(chunks.size(), chunks.get(chunks.size() - 1));
            }
            @Override
            protected void done() {
                GUITask.super.view.publishProgressIncrement(1, "Finished.");
                GUITask.super.determineResult();
            }
            @Override
            public void publish(String message) {
                super.publish(message);
            }
        };
    }
    
    private static class CommandLineTask extends WriteControllerTask implements Publisher {

        private boolean result;
        private Exception executionExceptionCause = null;
        
        private CommandLineTask(CollectRULsTask collectRULsTask,
                boolean isLHD, Queue<Pattern> patterns, URI inputURI, File outputFile, View view, File metaRuleDefinitionsFile) {
            super(collectRULsTask, isLHD, patterns, inputURI, outputFile, view, metaRuleDefinitionsFile);
        }

        @Override
        public void execute() {
            try {
                this.result = CommandLineTask.super.mainProcess(this);
            } catch (Exception e) {
                this.executionExceptionCause = e;
            }
            CommandLineTask.super.determineResult();
        }

        @Override
        public boolean get() throws InterruptedException, ExecutionException {
            if (executionExceptionCause != null) {
                throw new ExecutionException(executionExceptionCause);
            } else {
                return this.result;
            }
        }
        
        @Override
        public void publish(String message) {
            super.view.publishProgressIncrement(1, message);
        }
    }

    private interface Publisher {
        public void publish(String message);
    }

}
