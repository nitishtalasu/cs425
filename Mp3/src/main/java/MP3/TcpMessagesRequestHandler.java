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
        logger.LogInfo("[TcpMessageHandler] Server started serving client: " + this.socket); 
        
        while (true)  
        {
            try 
            {
                // Reading message type.
                String msgType = this.socketInputStream.readUTF();
                
                // Creating the process with given client command.
                logger.LogInfo("[TcpMessageHandler] Server received message type: " + msgType);

                String reply = ProcessMessage(msgType);

                // Writing the reply to the stream.
                logger.LogInfo("[TcpMessageHandler] Server sending reply: " + reply);
                this.socketOutputStream.writeUTF(reply);            
                logger.LogInfo("[TcpMessageHandler] Client request has been served.");
            } 
            catch (Exception ex) 
            {
                logger.LogException("[TcpMessageHandler] Client requested operation failed with:", ex);
            }

            break;
        }
        
        logger.LogInfo("[TcpMessageHandler] Closing connection"); 
        this.closeSocket();
    } 

    private String ProcessMessage(String msgType) 
    {
        String reply = "";
        MessageType msgTypeEnum = Enum.valueOf(MessageType.class, msgType);
        switch (msgTypeEnum) 
        {
            case ElECTION:
                reply = ElectionMessage();
                break;
            
            case VICTORY:
                reply = VictoryMessage();
                break;

            case COORDINATION:
                reply = CoordinationMessage();
                break;

            default:
                logger.LogWarning("[TcpMessageHandler] Either failed to resolve message type. Or" +
                    "Forgot to add msgType: " + msgType);
                break;
        }

        return reply;
    }

    private String ElectionMessage() 
    {
        String reply = "";
        String clientIpAddress = this.socket.getInetAddress().getHostAddress();
        boolean isClientAddressHigher = MembershipList.IsAddressHigher(clientIpAddress);
        if(isClientAddressHigher)
        {
            reply = "NACK";
        }
        else
        {
            reply = "OK";
            logger.LogInfo("[TcpMessageHandler] Received election message from lower Id. So starting election");
            LeaderElection leaderElection = new LeaderElection();
            leaderElection.start();
        }

        return reply;
    }
    
    private String VictoryMessage() 
    {
        String reply = "";
        String clientIpAddress = this.socket.getInetAddress().getHostAddress();
        MembershipList.setLeaderIpAddress(clientIpAddress);
        logger.LogInfo("[TcpMessageHandler] Newly elected leader: " + MembershipList.getLeaderIpAddress());

        return reply;
    }

    private String CoordinationMessage() 
    {
        String reply = "";
        //reply = ReplicaList.getAllFiles().toJson().toString();

        return reply;
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
            logger.LogException("[TcpMessageHandler] Stream initializations failed:", e);
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
            logger.LogException("[TcpMessageHandler] Failed in closing resources with message:", e); 
        } 
	}
}
