
/**
 * This class handles the operation to generate logs requested by the clients.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Class handles the client requests of generating the logs.
 */
public class LogGeneratorHandler extends Thread
{
    /**
     * Client socket.
     */
    private final Socket socket;

    /**
     * Input stream of the socket.
     */
    private DataInputStream socketInputStream;

    /**
     * Output stream of the socket.
     */
    private DataOutputStream socketOutputStream;
  
    /**
     * Constructor for the class LogGeneratorHandler
     * @param socket Socket of the client and server connection.
     */
    public LogGeneratorHandler(Socket socket)  
    { 
        this.socket = socket;
        this.initializeStreams();   
    }

    /**
     * Overrides the run method of Thread class. This serves the client requested operations.
     * {@inheritDoc}
     */
    @Override
    public void run()  
    { 
        System.out.println("[LogGenerator] Server started serving client: " + this.socket); 
        
        /**
         * Server serves client requests as follows:
         * 1) Reads the patterns serialized string(format has been shared by both client and server) 
         *    via socket input stream.
         * 2) Generates the log file.
         * 3) Closes all the resources used in serving the client.
         */
        while (true)  
        {
            try 
            {
                // Reads the patterns and number of lines to be there in generated logs.
                // The pattern is being shared with client which calls this server.
                // Expected format : "pattern1:count,pattern2:count,...."
                String dummyLogFileName = this.socketInputStream.readUTF();
                String patternsSerializedString = this.socketInputStream.readUTF();
                StringTokenizer patternTokens = new StringTokenizer(patternsSerializedString, ",");

                Logger logger = Logger.getLogger("logGenerator");
 
                // Simple file logging Handler.
                FileHandler logFileHandler;

                // We are removing default handlers and adding file handler.
                logger.setUseParentHandlers(false);
                logFileHandler = new FileHandler(dummyLogFileName, false);
                logger.addHandler(logFileHandler);

                // Print a brief summary of the LogRecord in a human readable format.
                SimpleFormatter formatter = new SimpleFormatter();	
                logFileHandler.setFormatter(formatter);

                for (int token = 1; patternTokens.hasMoreTokens(); token++)
                {
                    String patternToken = patternTokens.nextToken();
                    System.out.println("[LogGenerator] Got token as "+ token + ":" + patternToken);
                    String[] pattern = patternToken.split("=");

                    int n = 1;
                    while (n <= Integer.parseInt(pattern[1])) 
                    {
                        // Log an INFO message.
                        logger.info(pattern[0]); 
                        n++;
                    }
                }

                // Generating random log lines. Not creating random lines more than 10000.
                int randomLines = ThreadLocalRandom.current().nextInt()/10000;
                int n = 1;
                while (n <= randomLines) 
                {
                    // Log an INFO message.
                    logger.info("Random log line added."); 
                    n++;
                }

                this.socketOutputStream.writeUTF("Log file generated"); 
                logFileHandler.close();
                System.out.println("[LogGenerator] Client request has been served.");
            } 
            catch (Exception ex) 
            {
                System.err.println("[LogGenerator] Client requested operation failed with:");
                ex.printStackTrace();
            }

            break;
        }
        
        System.out.println("[LogGenerator] Closing connection"); 
        this.closeSocket();
    } 

	/**
     * Initializes the input and output streams.
     */
    private void initializeStreams()
    {
        try 
        {
            this.socketInputStream = new DataInputStream(this.socket.getInputStream());
            this.socketOutputStream = new DataOutputStream(this.socket.getOutputStream());
        } 
        catch (IOException e)
        {
            System.err.println("[LogGenerator] Stream initializations failed:");
            e.printStackTrace();
        }
    }

    /**
     * Closes all the resources that are used in serving the client.
     */
    private void closeSocket() 
    {
        try
        { 
            this.socketInputStream.close(); 
            this.socketOutputStream.close();
            this.socket.close();
        }
        catch(IOException e)
        { 
            System.err.println("[LogGenerator] Failed in closing resources with message:");
            e.printStackTrace(); 
        } 
	}
}
