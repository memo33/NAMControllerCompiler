package testsuites;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class MyTestRunner {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Result result1 = JUnitCore.runClasses(AllUnitTests.class);
        Result result2 = JUnitCore.runClasses(AllIntegrationTests.class);
        for (Failure failure : result1.getFailures()) {
            System.out.println(failure);
        }
        for (Failure failure : result2.getFailures()) {
            System.out.println(failure);
        }
        printMessage("Unit", result1);
        printMessage("Integration", result2);
        System.out.println("---------------------");
        if (result1.getFailureCount() == 0 && result2.getFailureCount() == 0) {
            System.out.println("All tests successful!");
        } else {
            System.out.println("Some tests failed!");
        }
    }

    private static void printMessage(String testname, Result result) {
        System.out.printf("%s tests completed in %d milliseconds.%n" +
                "Number of tests: %d%n" +
                "Number of failures: %d%n",
                testname, result.getRunTime(), result.getRunCount(), result.getFailureCount());
    }

}
