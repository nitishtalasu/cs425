/**
 * This class handles the operations requested by the clients.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.io.*; 
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class handles the client requests.
 */
public class TcpMessagesRequestHandler extends Thread
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
     * Constructor for the class TcpMessagesRequestHandler
     * @param socket Socket of the client and server connection.
     */
    public TcpMessagesRequestHandler(Socket socket)  
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
        logger.LogInfo("[Server] Server started serving client: " + this.socket); 
        
        while (true)  
        {
            try 
            {
                // Clients sending the log file name.
                String vmLogFileName = this.socketInputStream.readUTF();
                
                // Creating the process with given client command.
                logger.LogInfo("[Server] Server executing the process with command: "); 
                
                // Reads from buffer and sends back to the client in socket output stream.
                String outputLine;
                int matchedLinescount = 0;
                while ((outputLine = processOutputReader.readLine()) != null)
                {
                    this.socketOutputStream.writeUTF(vmLogFileName + " " + outputLine);
                    matchedLinescount++;
                }

                // Writing the matched lines count to the stream.
                this.socketOutputStream.writeUTF(vmLogFileName + " " + matchedLinescount);            
                logger.LogInfo("[Server] Client request has been served.");
            } 
            catch (Exception ex) 
            {
                logger.LogException("[Server] Client requested operation failed with:", ex);
            }

            break;
        }
        
        logger.LogInfo("[Server] Closing connection"); 
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
            logger.LogException("[Server] Stream initializations failed:", e);
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
            logger.LogException("[Server] Failed in closing resources with message:", e); 
        } 
	}
}
