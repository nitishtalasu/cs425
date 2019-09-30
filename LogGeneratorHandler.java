
/**
 * This class handles the operation to generate logs requested by the clients.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.io.*;
import java.net.Socket;

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
     * Logger instance.
     */
    private GrepLogger logger;
  
    /**
     * Constructor for the class LogGeneratorHandler
     * @param socket Socket of the client and server connection.
     */
    public LogGeneratorHandler(Socket socket)  
    { 
        this.socket = socket;
        this.initializeStreams();
        logger = GrepLogger.getInstance();
    }

    /**
     * Overrides the run method of Thread class. This serves the client requested operations.
     * {@inheritDoc}
     */
    @Override
    public void run()  
    { 
        logger.LogInfo("[LogGenerator] Server started serving client: " + this.socket); 
        
        /**
         * Server serves client requests as follows:
         * 1) Reads the patterns serialized string(format has been shared by both client and server) 
         *    via socket input stream.
         * 2) Generates the log file.
         * 3) Sends ACK message "Log file generated to client."
         * 4) Closes all the resources used in serving the client.
         */
        try 
        {
            // Reads the patterns and number of lines to be there in generated logs.
            // The pattern is being shared with client which calls this server.
            // Expected format : "pattern1=count,pattern2=count,...."
            String dummyLogFileName = this.socketInputStream.readUTF();
            String patternsSerializedString = this.socketInputStream.readUTF();
            GrepLogger.generateLogs(dummyLogFileName, patternsSerializedString);
            this.socketOutputStream.writeUTF("Log file generated"); 
            logger.LogInfo("[LogGenerator] Client request has been served.");
        } 
        catch (Exception ex) 
        {
            logger.LogException("[LogGenerator] Client requested operation failed with:", ex);
        }
        
        logger.LogInfo("[LogGenerator] Closing connection"); 
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
            logger.LogException("[LogGenerator] Stream initializations failed:", e);
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
            logger.LogException("[LogGenerator] Failed in closing resources with message:", e);
        } 
	}
}
