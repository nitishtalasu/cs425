
/**
 * Logger for grep server.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Logger for server.
 */
public class GrepLogger {

    private static GrepLogger grepLogger = null;

    private static Logger logger;

    /**
     * Constructor for the grep logger.
     * 
     * @param loggerName Logger name.
     * @param logFileName Log file name.
     * @param logAppend Log file append mode.
     */
    private GrepLogger(
        String loggerName,
        String logFileName,
        boolean logAppendMode)
    {
        logger = Logger.getLogger(loggerName);
        addFileHandler(logFileName, logAppendMode);
    }

    /**
     * Returns the singleton object of GrepLogger class.
     * 
     * @param loggerName Logger name.
     * @param logFileName Log file name.
     * @param logAppendMode Log file append mode.
     * @return GrepLogger class object.
     */
    public static GrepLogger initialize(
        String loggerName, 
        String logFileName, 
        boolean logAppendMode,
        boolean setParentHandlers) 
    {
        if (grepLogger == null) 
        {
            grepLogger = new GrepLogger(loggerName, logFileName, logAppendMode);
        }

        return grepLogger;
    }

    /**
     * Returns the singleton object of GrepLogger class.
     * 
     * @param loggerName Logger name.
     * @param logFileName Log file name.
     * @return GrepLogger class object.
     */
    public static GrepLogger initialize(
        String loggerName, 
        String logFileName) 
    {
        return initialize(loggerName, logFileName, true, true);
    }

    /**
     * Gets the instance of Grep logger.
     * @return Grep logger instance.
     */
    public static GrepLogger getInstance()
    {
        if (grepLogger == null)
        {
            initialize("Unknown", "Unknown.log");
        }

        return grepLogger;
    }

    /**
     * Logs info message.
     * @param message Message to be logged.
     */
    public void LogInfo(String message)
    {
        logger.info(message);
    }

    /**
     * Logs warning messages.
     * @param message Message to be logged.
     */
    public void LogWarning(String message)
    {
        logger.warning(message);
    }

    /**
     * Logs error message.
     * @param message Message to be logged.
     */
    public void LogError(String message)
    {
        logger.log(Level.SEVERE, message);
    }

    /**
     * Logs exception.
     * @param message Message to be logged.
     * @param exp Exception that occured.
     */
    public void LogException(String message, Exception exp)
    {
        String errorMessage = message + "Failed with exception:" + exp.getMessage();
        logger.log(Level.SEVERE, errorMessage);
        exp.printStackTrace();
    }


    /**
     * Generate logs in a file.
     * @param logFileName File name for the log file.
     * @param patternsSerializedString Contains serialized patterns along with the count.
     */
    public static void generateLogs(String logFileName, String patternsSerializedString)
    {
        GrepLogger logger = GrepLogger.getInstance();
        logger.LogInfo("[Logger] Got seriliazed pattern as " + patternsSerializedString);

        // Stores the last file handler and adds new handler to generate log file.
        // After log generated original handler will be restored.
        setUseParentHandlers(false);
        Handler fileHandler = getFileHandler();
        removeFileHandler();
        addFileHandler(logFileName, false);
        StringTokenizer patternTokens = new StringTokenizer(patternsSerializedString, ",");
        for (int token = 1; patternTokens.hasMoreTokens(); token++)
        {
            String patternToken = patternTokens.nextToken();
            String[] pattern = patternToken.split("=");
            
            /**
             * Generate log with given patterns and their counts. Along with random log lines
             * are generated and these are unknown to the client. The random lines are written
             * based on the generated random number. These random lines are randomized in the 
             * file.
             */
            int randomLines = ThreadLocalRandom.current().nextInt()/10000;
            int patternLinesWritten = 1;
            int randomLinesWritten = 1;
            while (patternLinesWritten <= Integer.parseInt(pattern[1])) 
            {
                // Log an INFO message.
                logger.LogInfo(pattern[0]); 
                patternLinesWritten++;

                if (randomLinesWritten < randomLines)
                {
                    Random random = new Random();
                    int randomNumber = random.nextInt(10);
                    int n = 1;
                    while(n <= randomNumber)
                    {
                        logger.LogInfo("Random log line added.");
                        n++;
                        randomLinesWritten++;
                    }
                }
            }
        }
        removeFileHandler();
        addFileHandler(fileHandler);
        setUseParentHandlers(true);
    }

    /**
     * Setting false to default parent handlers
     * @param setParentHandlers Flag for setting value.
     */
    private static void setUseParentHandlers(boolean setParentHandlers)
    {
        logger.setUseParentHandlers(setParentHandlers);
    }

    /**
     * Adds handler to the logger.
     * 
     * @param handler Handler to be added.
     */
    private static void addFileHandler(Handler handler) 
    {
        logger.addHandler(handler);
    }

    /**
     * Adds file handler to the logger.
     * 
     * @param logFileName Log file name.
     */
    private static void addFileHandler(String logFileName, boolean logAppendMode)
    {
        try 
        {
            // Adding file handler for logging.
            FileHandler logFileHandler;
            logFileHandler = new FileHandler(logFileName, logAppendMode);
            logger.addHandler(logFileHandler);

            // Print a brief summary of the LogRecord in a human readable format.
            SimpleFormatter formatter = new SimpleFormatter();	
            logFileHandler.setFormatter(formatter);
        } 
        catch (SecurityException | IOException e) 
        {
            logger.log(Level.WARNING, "Cannot add file handler.");
            e.printStackTrace();
        }
    }

    /**
     * Gets the file handler.
     * @return Handler.
     */
    private static Handler getFileHandler()
    {
        Handler[] handlers = logger.getHandlers();

        if (handlers[0] instanceof FileHandler)
        return handlers[0];

        return null;
    }

    /**
     * Removes the last added handler.
     */
    private static void removeFileHandler() 
    {
        Handler[] handlers = logger.getHandlers();
        Handler toBeRemoved = handlers[0];
        if (toBeRemoved instanceof FileHandler)
        logger.removeHandler(toBeRemoved);
    }

    public void cleanupLogger() {
        if (logger != null) {
          Handler[] handlers = logger.getHandlers();
          for (Handler handler : handlers) {
            handler.close();
            logger.removeHandler(handler);
          }
        }
      }

}