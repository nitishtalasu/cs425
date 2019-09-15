/**
 * Class to check count of active threads
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */

public class ThreadCount {
    public static void waitForThreadsToComplete(ThreadGroup threadGroup, GrepLogger logger)
    {	  
        // checks count of active threads   
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