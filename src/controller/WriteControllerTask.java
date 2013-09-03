package controller;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jdpbfx.DBPFEntry;
import jdpbfx.DBPFFile;
import jdpbfx.DBPFTGI;
import jdpbfx.types.DBPFLText;
import model.RUL0Entry;
import model.RUL1Entry;
import model.RUL2Entry;
import model.RULEntry;
import view.ProgressPanel;
import controller.Compiler.Mode;

public class WriteControllerTask extends SwingWorker<Boolean, String> {
    
    private final DBPFTGI[] RUL_TGIS = {
        DBPFTGI.RUL.modifyTGI(-1L, -1L, 0x10000000L),
        DBPFTGI.RUL.modifyTGI(-1L, -1L, 0x10000001L),
        DBPFTGI.RUL.modifyTGI(-1L, -1L, 0x10000002L)};
    private final DBPFTGI LTEXT_TGI = new DBPFTGI(0x2026960bL, 0x123006aaL, 0x6a47ffffL);
    
//    private final Queue<File>[] rulInputFiles;
    private final Mode mode;
    private final CollectRULsTask collectRULsTask;
    private final JFrame parentFrame;
    private final boolean isLHD;
    private final Queue<Pattern> patterns;
    private final File outputFile;

    private long starttime;
    private ProgressPanel progressPanel;
    private JDialog dialog;
    
    public WriteControllerTask(Mode mode, CollectRULsTask collectRULsTask, JFrame parentFrame,
            boolean isLHD, Queue<Pattern> patterns,
            File outputFile) {
        this.mode = mode;
        this.collectRULsTask = collectRULsTask;
        this.parentFrame = parentFrame;
        this.isLHD = isLHD;
        this.patterns = patterns;
        this.outputFile = outputFile;
    }

    @Override
    protected Boolean doInBackground() throws FileNotFoundException, IOException, InterruptedException, ExecutionException {
        starttime = System.currentTimeMillis();
        Queue<File>[] rulInputFiles = this.collectRULsTask.get();
        
        int max = 0;
        for (int i = 0; i < rulInputFiles.length; i++) {
            max += rulInputFiles[i].size();
        }

//        final ProgressMonitor progressMonitor =
//                new ProgressMonitor(frame, "Processing file...",
//                        // spaces in order to size the dialog
//                        String.format("%96s", ""), 0, max);
//        progressMonitor.setMillisToDecideToPopup(frame != null ? 0 : Integer.MAX_VALUE);
//        ChangeListener changeListener = new ChangeListener() {
//            int prog = 0;
//            @Override
//            public void stateChanged(ChangeEvent e) {
//                File file = (File) e.getSource();
//                NAMControllerCompilerMain.LOGGER.fine(file.toString());
//                progressMonitor.setProgress(prog);
//                progressMonitor.setNote(file.getName());
//                prog++;
//            }
//        };
        
        progressPanel = new ProgressPanel("Processing file...", "", 0, max + 1);
        progressPanel.setPreferredSize(new Dimension(400, 0));
        if (this.mode.isInteractive()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
//                    Object[] options = {UIManager.getString("OptionPane.cancelButtonText")};
//                    int result = JOptionPane.showOptionDialog(parentComponent, progressPanel, null, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
                    dialog = new JDialog((Frame) parentFrame, true);
                    dialog.add(new JOptionPane(progressPanel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[0]));
                    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                    dialog.pack();
                    dialog.setLocationByPlatform(true);
                    dialog.setVisible(true);
                }
            });
        }
        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                File file = (File) e.getSource();
                NAMControllerCompilerMain.LOGGER.fine(file.toString());
                publish(file.getName());
            }
        };
        
        Queue<DBPFEntry> writeList = new ArrayDeque<DBPFEntry>();
        long lastModf = 0;
        
        // RUL files
        for (int i = 0; i < 3; i++) {
            RULEntry rulEntry;
            if (i==0) {
                rulEntry = new RUL0Entry(RUL_TGIS[i], rulInputFiles[i], isLHD, changeListener);
            } else if (i==1) {
                rulEntry = new RUL1Entry(RUL_TGIS[i], rulInputFiles[i], changeListener);
            } else {
                rulEntry = new RUL2Entry(RUL_TGIS[i], rulInputFiles[i], patterns, changeListener);
            }
            if(rulEntry.getLastModified() > lastModf) {
                lastModf = rulEntry.getLastModified();
            }
            writeList.add(rulEntry);
        }
        // LText (Controller marker)
        NAMControllerCompilerMain.LOGGER.info("Adding Controller marker description text");
        DBPFLText ltext = new DBPFLText(new byte[0], LTEXT_TGI, false);
        ltext.setString(getControllerMarkerText(lastModf, isLHD));
        writeList.add(ltext);

        // write to file
        return DBPFFile.Writer.write(outputFile, writeList);
    }
    
    @Override
    protected void process(List<String> chunks) {
        this.progressPanel.incrementProgress(chunks.size());
        this.progressPanel.setNote(chunks.get(chunks.size() - 1));
    }

    @Override
    protected void done() {
        this.progressPanel.incrementProgress(1);
        try {
            if (this.isCancelled()) {
                NAMControllerCompilerMain.LOGGER.info("Writing cancelled.");
                // TODO cancelled
                // currently not possible
                NAMControllerCompilerMain.LOGGER.severe("Compiler finished with errors.");
                JOptionPane.showMessageDialog(parentFrame, "An error occured.", "Error", JOptionPane.ERROR_MESSAGE);
                this.disposeFrames();
                System.exit(-1);
            } else if (this.get()) {
                NAMControllerCompilerMain.LOGGER.log(Level.INFO,
                        "Writing of Controller completed. Total time consumed: " +
                        "{0} milliseconds.", this.getTimeConsumed());
                JOptionPane.showMessageDialog(parentFrame, "The NAMController file has been successfully compiled.");
                this.disposeFrames();
                System.exit(0);
            } else {
                NAMControllerCompilerMain.LOGGER.severe("Compiler finished with errors.");
                JOptionPane.showMessageDialog(parentFrame, "An error occured.", "Error", JOptionPane.ERROR_MESSAGE);
                this.disposeFrames();
                System.exit(-1);
            }
        } catch (InterruptedException e) {
            NAMControllerCompilerMain.LOGGER.log(Level.SEVERE, "Compiler finished with errors.", e);
            JOptionPane.showMessageDialog(parentFrame, "An error occured: " + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            this.disposeFrames();
            System.exit(-1);
        } catch (ExecutionException e) {
            NAMControllerCompilerMain.LOGGER.log(Level.SEVERE, "Compiler finished with errors.", e);
            JOptionPane.showMessageDialog(parentFrame, "An error occured: " + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            this.disposeFrames();
            System.exit(-1);
        }
    }
    
    private void disposeFrames() {
        if (this.dialog != null) {
            this.dialog.dispose();
        }
        if (this.parentFrame != null) {
            this.parentFrame.dispose();
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
        return String.format("Version: %s - %s",
                isLHD ? "LHD" : "RHD",
                dateFormat.format(new Date(date)));
//        return "Version: " + (isLHD ? "LHD " : "RHD ") + (isESeries ? "(e-series) - " : "(s-series) - ") + dateFormat.format(new Date(date));
    }
    
    long getTimeConsumed() {
        return System.currentTimeMillis() - starttime;
    }
}
