/**
 * Used to generate dummy logs for testing.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

public class LogGenerator
{
    private static GrepLogger logger =
        GrepLogger.initialize("LogGenerator", "LogGenerator.log");
    public static void main(String[] args)
    {
        try
        {
            logger.LogInfo("[LogGenerator] Starting the server on port 5500.");
            ServerHandler.getInstance(5500, LogGeneratorHandler.class).run();
        }
        catch(Exception e)
        {
            logger.LogException("[LogGenerator] Log Generator crashed.", e);
        }
    }
}