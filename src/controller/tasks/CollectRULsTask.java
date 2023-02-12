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
    
    private static FileFilter fileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory() ||
                    pathname.getName().endsWith(".txt") ||
                    pathname.getName().endsWith(".ini") ||
                    pathname.getName().endsWith(".rul");
        }
    };
    
    private final File[] rulDirs;
    
    public static CollectRULsTask getInstance(CompileMode mode, File[] rulDirs) {
        return mode.isInteractive() ? new GUITask(rulDirs) : new CommandLineTask(rulDirs); 
    }
    
    private CollectRULsTask(File[] rulDirs) {
        this.rulDirs = rulDirs;
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
        
        private GUITask(File[] rulDirs) {
            super(rulDirs);
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
        
        private CommandLineTask(File[] rulDirs) {
            super(rulDirs);
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

