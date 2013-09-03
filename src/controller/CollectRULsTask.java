package controller;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.SwingWorker;

public class CollectRULsTask extends SwingWorker<Queue<File>[], Void> {
    
    private final File[] rulDirs;
    private static FileFilter fileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory() ||
                    pathname.getName().endsWith(".txt") ||
                    pathname.getName().endsWith(".rul");
        }
    };
    
    public CollectRULsTask(File[] rulDirs) {
        this.rulDirs = rulDirs;
    }

    @Override
    protected Queue<File>[] doInBackground() {
        NAMControllerCompilerMain.LOGGER.info("Collecting input data.");
        @SuppressWarnings("unchecked")
        Queue<File>[] rulInputFiles = new Queue[3];
        for (int i = 0; i < rulInputFiles.length; i++)
            rulInputFiles[i] = collectRULInputFilesRecursion(rulDirs[i]);
        return rulInputFiles;
    }
    
    /**
     * Recursive collecting.
     * @param parent parent file of the directory.
     * @return Queue of subfiles.
     */
    private Queue<File> collectRULInputFilesRecursion(File parent) {
        Queue<File> result = new LinkedList<File>();
        File[] subFiles = parent.listFiles(fileFilter);
        Arrays.sort(subFiles);                  // sort files alphabetically

        for (int i = 0; i < subFiles.length; i++) {
            if (subFiles[i].isDirectory())
                result.addAll(collectRULInputFilesRecursion(subFiles[i]));
            else
                result.add(subFiles[i]);
        }
        return result;
    }
}
