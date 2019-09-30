/**
 * Class for testing log generator.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Class for testing log generator.
 */
public class TestLogGenerator {

    /**
     * Logger instance.
     */
    public static GrepLogger logger =
        GrepLogger.initialize("TestLogGenerator", "TestLogGenerator.log");
    
    public static void main(String[] args) 
    {
        String testMethod = parseTestMethod(args);
        if (testMethod.equalsIgnoreCase("generatedummylog"))
        {
            generateDummyLog();     
        }
        else
        {
            logger.LogError("Invalid Test method passed.");
            logger.LogInfo("Usage: TestLogGenerator <TestMethod>\n<TestMethod> : GenerateDummyLog");
            System.exit(1);
        }
    }

    /**
     * Test Method for generating dummy logs.
     */
    private static void generateDummyLog()
    {
        String fileName = "test.log";
        GrepLogger.generateLogs(fileName, "This is test log file=123");

        try 
        {
            BufferedReader fileReader = new BufferedReader(new FileReader("test.log"));
            String line;
            int macthedLinesCount = 0;
            boolean testPassed = true;
            while ((line = fileReader.readLine()) != null) 
            {
                if (line.contains("This is test log file")) 
                {
                    macthedLinesCount++;
                }
            }

            if (macthedLinesCount != 123)
            {
                logger.LogError("Test Failed.");
            }

            logger.LogInfo("Test Passed.");
            fileReader.close();
            
            File file = new File(fileName);
            if(file.delete())
            {
                logger.LogInfo("Test log file deleted.");
            }
            else
            {
                logger.LogWarning("Test log file deletion failed. Please delete file manually if exists.");
            }
        } 
        catch (FileNotFoundException e)
        {
            logger.LogException("File test.log not found. Test failed", e);
            System.exit(1);
        }
        catch(IOException e)
        {
            logger.LogException("File test.log not found. Test failed", e);
            System.exit(1);
        }
    }

    /**
     * Parses test method from arguments.
     * @param args Command line arguments.
     * @return Retuns test method passed in arguments.
     */
    private static String parseTestMethod(String[] args) 
    {
        if (args.length == 0)
        {
            logger.LogError("Mising test method argument.");
            logger.LogInfo("Usage: TestLogGenerator <TestMethod>");
            System.exit(1);
        }

        if (args.length > 1)
        {
            logger.LogError("Too many arguments are passed.");
            logger.LogInfo("Usage: TestLogGenerator <TestMethod>");
            System.exit(1);
        }

        return args[0];
    }
}