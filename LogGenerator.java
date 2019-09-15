/**
 * Used to generate dummy logs for testing.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

public class LogGenerator
{
    public static void main(String[] args)
    {
        try
        {
            System.out.println("[LogGenerator] Starting the server on port 5500.");
            ServerHandler.getInstance(5500, LogGeneratorHandler.class).run();
        }
        catch(Exception e)
        {
            System.err.println(e.getMessage());
        }
    }
}