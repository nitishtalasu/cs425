
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


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

    private FileWriter localWriteFile = null;

    private FileReader localReadFile = null;

    private int writeCount = 0;
  
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
                
                if (msgType.equals(MessageType.PUT))
                {
                    if(writeCount >= 1)
                    {
                        this.socketOutputStream.writeUTF("Another write in process. Continue?");
                        // ask user if they want to overwrite
                        String choice = this.socketInputStream.readUTF();
                        if (choice.equals("no"))
                        {
                            // send negative reply to TcpClientModule
                            continue;
                        }
                    }
                    writeCount++;
                }
                
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

            case ELECTION:
                reply = ElectionMessage();
                break;
            
            case VICTORY:
                reply = VictoryMessage();
                break;

            case COORDINATION:
                reply = CoordinationMessage();
                break;
            
            case GET:
                reply = GetFiles();
                break;

            case PUT:
                reply = PutFiles();
                break;

            case DELETE:
                reply = DeleteFile();
                break;

            case REREPLICATE:
                reply = ReReplicateFile();
                break;
            
            case LIST:
                reply = ReplicaList();
                break;

            case REPLICALIST:
                reply = GetReplicaList();
                break;

            case PUT_SUCCESS:
                reply = PutFilesSuccess();
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
        String reply = "OK";
        String clientIpAddress = this.socket.getInetAddress().getHostAddress();
        //logger.logInfo("Blablabla " + clientIpAddress);
        MembershipList.setLeaderIpAddress(clientIpAddress);
        logger.LogInfo("[TcpMessageHandler] Newly elected leader: " + MembershipList.getLeaderIpAddress());

        return reply;
    }

    private String CoordinationMessage() 
    {
        String reply = "";
        ReplicaList.printReplicaNodes();
        List<String> files = ReplicaList.getLocalReplicas();
        if (files != null)
        {
            reply = new Gson().toJson(files);
        }
        //this.socketOutputStream.writeUTF(json);

        return reply;
    }    

    private String GetFiles()
    {
        String reply = "OK";
        try 
        {
            String sdfsFileName = this.socketInputStream.readUTF();

            localReadFile = new FileReader(sdfsFileName);

            //variable to check end of file
            BufferedReader br = new BufferedReader(localReadFile);
            // read line by line
            String line;
            while ((line = br.readLine()) != null) {
                this.socketOutputStream.writeUTF(line);
            }  
        }
        catch(IOException e)
        {
            logger.LogException("[TCPMessageRequestHandler] Unable to get file data.", e); 
        }
        return reply;
    }

    private String PutFiles()
    {
        String reply = "OK";
        try
        {
            String sdfsFileName = this.socketInputStream.readUTF();
            localWriteFile = new FileWriter(sdfsFileName);
            boolean eof = false;
                while (!eof) 
                {
                    try 
                    {
                        //read data sent by server, line-by-line, and write to file
                        String lineOutputs = this.socketInputStream.readUTF();
                        localWriteFile.write(lineOutputs);
                        localWriteFile.write(System.getProperty("line.separator"));
                    } 
                    catch (EOFException e) 
                    {
                        eof = true;
                        reply = "NACK";
                        logger.LogInfo("Completed writing logs to file: "+sdfsFileName);
                    }
                }

            ReplicaList.addNewFile(sdfsFileName);
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Unable to put file data.", e); 
        }
        return reply;
    }

    private String DeleteFile()
    {
        String reply = "OK";
        try
        {
            String sdfsFileName = this.socketInputStream.readUTF();
            File file = new File(sdfsFileName);
            if(file.delete()) 
            { 
                logger.LogInfo("[TCPMessageRequestHandler] File deleted successfully"); 
            } 
            else
            { 
                System.out.println("[TCPMessageRequestHandler] Failed to delete the file"); 
            } 
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while deleting file", e); 
        }
        return reply;
    }

    private String ReReplicateFile()
    {
        String reply = "OK";

        try
        {
            String sdfsFileName = this.socketInputStream.readUTF();
            String ipAddressToReplicate = this.socketInputStream.readUTF();
            List<String> ipAddresses = new ArrayList<String>();
            ipAddresses.add(ipAddressToReplicate);
            TcpClientModule client = new TcpClientModule();
            client.putFiles(sdfsFileName, sdfsFileName, ipAddresses);
        }
        catch(Exception e)
        {
            logger.LogException("[TcpMessageRequestHandler] Failed with ", e);
        }
        
        
        return reply;
    }

    private String ReplicaList()
    {
        String reply = "OK";
        try
        {
           String sdfsFileName = this.socketInputStream.readUTF();
           List<String> addresses = ReplicaList.addReplicaFiles(sdfsFileName);
           String json = this.toJson(addresses);
           this.socketOutputStream.writeUTF(json);
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while deleting file", e); 
        }
        return reply;
    }

    private String GetReplicaList()
    {
        String reply = "OK";
        try
        {
           String sdfsFileName = this.socketInputStream.readUTF();
           List<String> addresses = ReplicaList.getReplicaIpAddress(sdfsFileName);
           String json = this.toJson(addresses);
           this.socketOutputStream.writeUTF(json);
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while deleting file", e); 
            reply = "NACK";
        }
        return reply;
    }

    private String PutFilesSuccess()
    {
        String reply = "OK";
        try
        {
           String sdfsFileName = this.socketInputStream.readUTF();
        //    List<String> addresses = ReplicaList.getReplicaIpAddress(sdfsFileName);
        //    String json = this.toJson(addresses);
           ReplicaList.replicationCompleted(sdfsFileName);
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while deleting file", e); 
        }
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

    private String toJson(List<String> msg)
    {     
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(msg);

        return json;
    }
}
