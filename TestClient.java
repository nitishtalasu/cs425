/**
 * Main class for running tests.
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */

import java.io.*;
import java.util.Scanner;
import java.util.Properties;

public class TestClient {
    
    /**
    *  server addresses
    */
    static String addresses[] = null;
    /** 
     *  VM log IDs
    */
    static String vmIds[] = null;
    /**
     *  grep patterns to be sent to each server
     */
    static String vm_patterns[] = null;

    /**
     * stores the content of each output file generated by grep
     */
    static String output = "";
    /**
     *  stores output after parsing from property files
     */
    static String split_output[] = null;

    /**
     *  input streams to access properties file
     */
    static InputStream inputTestProps;
    static  InputStream inputServerProps;

    /**
    * retrieves properties from test.properties file
    */ 
    static Properties testProps;
    static Properties serverProps;
    static enum patterns {infrequent, frequent, regex;}
    static int pass = 0, count, vm_count;

    /**
     * Thread group used to monitor the threads.
     */
    static ThreadGroup threadGroup = new ThreadGroup("TestClient");

    /**
     * Logger instance.
     */
    private static GrepLogger logger = GrepLogger.initialize("TestClient", "TestClient.log");

    /**
     * default constructor for TestClient.
     * 
     */
    public TestClient() throws IOException {
        inputTestProps = new FileInputStream("test.properties");
        testProps = new Properties();
        inputServerProps = new FileInputStream("server_parameters.properties");
        serverProps = new Properties();
    }
    public static void main(String args[]) throws IOException {

        TestClient test = new TestClient();
        // loads properties from respective property files
        testProps.load(inputTestProps);
        serverProps.load(inputServerProps);
        
        // calls the log generator method and returns number of log files generated (one for each server)
        int pass = log_generator(threadGroup);

        // input the test to be run
        System.out.println("Enter 1 for infrequent pattern test, 2 for frequent, 3 for regex, 4 for failure");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int option = Integer.parseInt(br.readLine());
        switch(option){
            case 1: {
                // run the grep command for infrequent pattern
                String infrequentPattern = "this is log for VM2";
                logger.LogInfo("Running test for infrequent pattern");
                run_server(threadGroup, pass, infrequentPattern, patterns.valueOf("infrequent").ordinal());
                break;
            }
            case 2: {
                // run the grep command for frequent pattern
                String frequentPattern = "frequentpattern hello123";
                run_server(threadGroup, pass, frequentPattern, patterns.valueOf("frequent").ordinal());
                break;
            }
            case 3: {
                // run the grep command for regex pattern
                String regexPattern = "-E '^{0-9}*[a-z]{3}'";
                run_server(threadGroup, pass, regexPattern, patterns.valueOf("regex").ordinal());
                break;
            }
            case 4: {
                // run the grep command for infrequent pattern and compare against expected frequentvalue
                String frequentPattern = "this is log for VM2";
                logger.LogInfo("Running test for failure case");
                run_server(threadGroup, pass, frequentPattern, patterns.valueOf("frequent").ordinal());
                break;
            }
        }
        waitForThreadsToComplete(threadGroup);
    }
    /**
     * Method to generate a log file in each server
     * @param parentThreadGroup Parent thread group.
     */
    public static int log_generator(ThreadGroup parentThreadGroup) throws IOException {

        try {
            // get the corresponding property values from the .properties files
            addresses = serverProps.getProperty("IP_address").split(",");
            vmIds = serverProps.getProperty("VM_ID").split(",");
        } 
        catch (Exception e) {
            logger.LogException("Error in reading properties.", e);
        }

        // creates a thread group that waits for all threads to end before proceeding further
        ThreadGroup logGeneratorThreadGroup = new ThreadGroup(parentThreadGroup, "LogGenerator");
	
        // variable to store name of each logfile whose values are obtained from .properties file
        String[] logfile = new String[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            logfile[i] = vmIds[i];
            // invoke Client.main() thread that invokes each server with (server address, test pattern, logfileID, port)
            Client client = new Client(addresses[i], testProps.getProperty(addresses[i]), logfile[i], 5500);
            client.create_thread(logGeneratorThreadGroup);
        }

        waitForThreadsToComplete(logGeneratorThreadGroup);

        // checks if the expected number of files have been generated
        for (int i=0; i < logfile.length; i++) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(("output_"+logfile[i])));
                output = br.readLine();

                if(output.equalsIgnoreCase("Log file generated"))
                    pass++;
            }
            catch(FileNotFoundException e) {
                logger.LogException("The logfile generation for "+ logfile[i]+" did not succeed", e);
                logger.LogError("Test failed");
                System.exit(1);
            }
        }
        return pass;
    }
        
     /**
     * Method to run each server 
     * @param parentThreadGroup Parent thread group.
     * @param pass count indicating number of logfiles successfully created
     * @param clientInput input against which grep is tested
     * @param pattern indicates whether input is frequent pattern, infrequent pattern or regex(only for printing)
     * 
     */
    public static void run_server(ThreadGroup parentThreadGroup, int pass, String clientInput, int pattern) throws IOException {
        int pass_local = pass;
        String[] logfile = new String[addresses.length];
        ThreadGroup grepTestGroup = new ThreadGroup(parentThreadGroup, "GrepTest");

        /**
        * checks if number of server addresses provided in test.properties 
        * matches number of logfiles generated
        *  */
        if(pass_local == addresses.length) {
            
            // generate a client thread each to talk to one server
            for (int i = 0; i < addresses.length; i++) {
                logfile[i] = vmIds[i];
                Client client = new Client(addresses[i], clientInput, logfile[i], 5000);
                client.create_thread(grepTestGroup);   
            }

            waitForThreadsToComplete(grepTestGroup);

            pass_local = 0;
            for (int i=0; i < addresses.length; i++) {
                try {
                        // read lines from newly generated local log files
                        BufferedReader br = new BufferedReader(new FileReader("output_"+logfile[i]));
                        String temp;
                        while ((temp = br.readLine()) != null) {
                            output = temp;
                        }

                        // obtain linecount from output which is in the form <fileName> <linecount>
                        split_output = output.split(" ");

                        // line count obtained from server
                        count = Integer.parseInt(split_output[1]);

                        // extracts the VM patterns provided in test.properties for each server address
                        vm_patterns = testProps.getProperty(addresses[i]).split(",");
			
                        // further extracts the line count provided for each pattern (in test.properties) 
                        
                        vm_count = Integer.parseInt((vm_patterns[pattern].split("="))[1]);

                        // compares if returned count matched expected line count
                        if (count != vm_count) {
                            logger.LogInfo("obtained:"+count);
                            logger.LogInfo("expected:"+vm_count);
                            logger.LogError("Line count does not match. Test failed.");
                            System.exit(1);
                        }
                        else {
                            pass_local++;
                        }
                    }
                catch(Exception e) {
                    logger.LogException("Test failed while running tests: ", e);
                    System.exit(1);
                }
            }
        } 
        logger.LogInfo("Test passed");  
    }

    // waits for all threads of a thread group to complete, by checking the count of active threads
    private static void waitForThreadsToComplete(ThreadGroup threadGroup)
    {	     
        while(threadGroup.activeCount() > 0)
        {
            logger.LogInfo("Waiting for " + threadGroup.activeCount() +	
                " threads to Complete");	
            try 	
            {	
                // waits 500 milliseconds before checking count each time
                Thread.sleep(500);	
            }	
            catch (Exception e)	
            {	
                logger.LogError("Thread timed out while running test");	
                System.exit(1);	
            }
        }
    }
}
