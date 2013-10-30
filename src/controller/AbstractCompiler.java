package controller;

import controller.tasks.ExecutableTask;
import view.View;

public abstract class AbstractCompiler implements ExecutableTask {
    
    final Runnable runBefore;
    final Runnable runAfter;
    final View view;
    final CompileMode mode;
    
    public AbstractCompiler(CompileMode mode, View view) {
        this.mode = mode;
        this.view = view;
        runBefore = new Runnable() {
            @Override
            public void run() {
                boolean success = true;
                success = success && readSettings();
                success = success && checkXMLExists();
                if (!AbstractCompiler.this.mode.isDetailed()) {
                    success = success && checkInputFilesExist();
                }
                success = success && readXML();
                if (!success) {
                    AbstractCompiler.this.view.publishIssue("Compiling aborted.");
                    System.exit(-1);
                }
            }
        };
        runAfter = new Runnable() {
            @Override
            public void run() {
                boolean success = true;
                success = success && collectPatterns();
                if (AbstractCompiler.this.mode.isDetailed()) {
                    success = success && checkInputFilesExist();
                }
                success = success && collectRULInputFiles();
                success = success && checkOutputFilesExist();
                success = success && writeSettings();
                if (!success) {
                    AbstractCompiler.this.view.publishInfoMessage("Compiling aborted.");
                } else {
                    writeControllerFile();
                }
            }
        };
    }

    public abstract boolean readSettings();
    public abstract boolean checkXMLExists();
    public abstract boolean checkInputFilesExist();
    public abstract boolean readXML();
    public abstract boolean collectPatterns();
    public abstract boolean collectRULInputFiles();
    public abstract boolean checkOutputFilesExist();
    public abstract boolean writeSettings();
    public abstract void writeControllerFile();
    
    @Override
    public abstract void execute();
}
