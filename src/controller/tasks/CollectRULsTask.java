package controller.tasks;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import controller.CompileMode;

public abstract class CollectRULsTask implements ExecutableTask {
    
    private FileFilter fileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String name = pathname.getName().toLowerCase();
            if (pathname.isDirectory()) {
                return true;
            } else if (name.endsWith(".txt") || name.endsWith(".ini") || name.endsWith(".rul")) {
                return isLHD ? !name.contains("rhd.") : !name.contains("lhd.");
            } else {
                return false;
            }
        }
    };
    
    private final File[] rulDirs;
    private final boolean isLHD;
    
    public static CollectRULsTask getInstance(CompileMode mode, File[] rulDirs, boolean isLHD) {
        return mode.isInteractive() ? new GUITask(rulDirs, isLHD) : new CommandLineTask(rulDirs, isLHD);
    }
    
    private CollectRULsTask(File[] rulDirs, boolean isLHD) {
        this.rulDirs = rulDirs;
        this.isLHD = isLHD;
    }
    
    public abstract Queue<File>[] get() throws InterruptedException, ExecutionException;
    
    private Queue<File>[] mainProcess() {
        LOGGER.info("Collecting input data.");
        @SuppressWarnings("unchecked")
        Queue<File>[] rulInputFiles = new Queue[4];
        for (int i = 0; i < rulInputFiles.length; i++)
            rulInputFiles[i] = collectRecursively(CollectRULsTask.this.rulDirs[i]);
        return rulInputFiles;
    }

    /**
     * Recursive collecting.
     * @param parent parent file of the directory.
     * @return Queue of subfiles.
     */
    private Queue<File> collectRecursively(File parent) {
        Queue<File> result = new LinkedList<File>();
        File[] subFiles = parent.listFiles(fileFilter);
        Arrays.sort(subFiles);                  // sort files alphabetically

        for (int i = 0; i < subFiles.length; i++) {
            if (subFiles[i].isDirectory())
                result.addAll(collectRecursively(subFiles[i]));
            else
                result.add(subFiles[i]);
        }
        return result;
    }
    private static class GUITask extends CollectRULsTask {
        
        private final SwingWorker<Queue<File>[], Void> worker;
        
        private GUITask(File[] rulDirs, boolean isLHD) {
            super(rulDirs, isLHD);
            this.worker = new SwingWorker<Queue<File>[], Void>() {
                
                @Override
                protected Queue<File>[] doInBackground() {
                    return GUITask.super.mainProcess();
                }
            };
        }
        
        @Override
        public void execute() {
            this.worker.execute();
        }
        
        @Override
        public Queue<File>[] get() throws InterruptedException, ExecutionException {
            return worker.get();
        }
    }
    
    private static class CommandLineTask extends CollectRULsTask {
        
        private Queue<File>[] result;
        
        private CommandLineTask(File[] rulDirs, boolean isLHD) {
            super(rulDirs, isLHD);
        }
        
        @Override
        public void execute() {
            result = CommandLineTask.super.mainProcess();
        }
        
        @Override
        public Queue<File>[] get() {
            return result;
        }
        
    }
}

