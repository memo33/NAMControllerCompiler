package controller;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import controller.CommandLineArguments.ArgumentID;

/**
 * integration test
 */
@RunWith(Parameterized.class)
public class ControllerTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();
    
    private final File resourcesTestDir = new File(NAMControllerCompilerMain.RESOURCE_DIR, "test");
    private final File inputDir = new File(resourcesTestDir, "Controller");
    private final boolean isLHD;
    
    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = {{false}, {true}};
        return Arrays.asList(data);
    }
    
    public ControllerTest(boolean isLHDParameter) {
        this.isLHD = isLHDParameter;
    }
    
    @Test
    public void test() throws SecurityException, NoSuchFieldException, Exception {
        File outputDir = tempFolder.newFolder();
        CommandLineArguments arguments = CommandLineArguments.getInstance();
        arguments.setArgument(ArgumentID.INPUT_DIR, inputDir.getAbsolutePath());
        arguments.setArgument(ArgumentID.OUTPUT_DIR, outputDir.getAbsolutePath());
        arguments.setArgument(ArgumentID.RHD_FLAG, isLHD ? "0" : "1");
        Compiler compiler = Compiler.getCommandLineCompiler(resourcesTestDir, arguments);
        assertTrue(compiler.readSettings());
        assertTrue(compiler.checkXMLExists());
        assertTrue(compiler.checkInputFilesExist());
        assertTrue(compiler.readXML());
        
        assertTrue(compiler.collectPatterns());
        assertTrue(compiler.collectRULInputFiles());
        assertTrue(compiler.checkOutputFilesExist());
        assertTrue(compiler.writeSettings());

        exit.expectSystemExitWithStatus(0);
        compiler.writeControllerFile();
    }

}
