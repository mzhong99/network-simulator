import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.Collections;

import java.util.Map;
import java.util.List;

import java.util.HashMap;
import java.util.ArrayList;

public class TestRunner {
    
    public static void main(String[] args) {
        
        // Stores results by class unit test
        Map<String, Result> results = new HashMap<String, Result>();
        
        // Add results of actually running each unit test class
        Result gridResults = JUnitCore.runClasses(GridTest.class);
        results.put("[GridTest]", gridResults);

        List<String> testClasses = new ArrayList<String>(results.keySet());
        Collections.sort(testClasses);

        // Display results
        for (String testClass : testClasses) {
            Result result = results.get(testClass);
            if (result.wasSuccessful()) {
                System.out.printf("%s: Successfully passed all tests!\n", testClass);
            }
            else {
                System.out.printf("%s: Some tests failed. Failed tests:\n", testClass);
                for (Failure failure : result.getFailures()) {
                    System.out.println("===========================================================");
                    System.out.println(failure.toString());
                    System.out.println("===========================================================");
                    System.out.println(failure.getTrace());
                }
            }
        }
    }
}
