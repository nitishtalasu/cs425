/**
 * Class for the client side thread operations.
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
 
public class TcpClientModule 
{ 
    private DataOutputStream outputStream = null; 
    private Socket socket = null; 
    private DataInputStream inputStream = null; 
    private String vmId = "";
    private FileWriter localWriteFile = null;
    private FileReader localReadFile = null;
    private int port = 5000;

    /**
     * Logger instance.
     */
    private GrepLogger logger;
      
    /**
     * constructor of ClientThread type class.
     * 
     */
    public TcpClientModule()
    { 
        // this.socket = socket; 
        // this.vmId = vmId;
        this.logger = GrepLogger.getInstance();
    } 


    public void getFiles(String sdfsFileName, String localFileName, List<String> addresses)
    {   

        for(String address: addresses) 
        {

            this.initializeStreams(address);
            try
            { 

                logger.LogInfo("[TCPClient] Connected to "+ address + ".");
                
                this.outputStream.writeUTF(MessageType.GET.toString());
                
                this.outputStream.writeUTF(sdfsFileName);
                // generating files (for each server input) to store logs received from servers
                localWriteFile = new FileWriter(localFileName);

                //variable to check end of file
                boolean eof = false;
                while (!eof) {
                    try {
                        //read data sent by server, line-by-line, and write to file
                        String lineOutputs = this.inputStream.readUTF();
                        localWriteFile.write(lineOutputs);
                        localWriteFile.write(System.getProperty("line.separator"));
                    } catch (EOFException e) {
                        eof = true;
                        logger.LogInfo("Completed writing logs to file: "+localFileName);
                    }
                } 
                String reply = this.inputStream.readUTF();
                if(reply.equals("OK"))
                {
                    logger.LogInfo("[TCPClient] File received."); 
                }  
            } 
            catch(IOException i) 
            { 
                logger.LogException("[TCPClient] Unable to receive file data.", i); 
            } 
    
            this.localWriteFile.close();
            this.closeSocket();
        }
    }

    public void putFiles(String sdfsFileName, String localFileName, List<String> addresses)
    {   

        for(String address: addresses) 
        {
            this.initializeStreams(address);
            try
            { 
                // sends VM log ID and user input to server
                logger.LogInfo("[TCPClient] Connected to "+ address + ".");
                
                this.outputStream.writeUTF(MessageType.PUT.toString());
                String choice = "";
                String writeStatus = this.inputStream.readUTF();
                if(writeStatus != "")
                {   
                    logger.LogInfo(writeStatus);
                    Scanner sc = new Scanner(System.in);
                    choice = sc.nextLine();
                    this.outputStream.writeUTF(choice);

                }
                if(choice.equalsIgnoreCase("no"))
                {
                    continue;
                }
                
                // generating files (for each server input) to store logs received from servers
                localReadFile = new FileReader(localFileName);
                BufferedReader br = new BufferedReader(localReadFile);
                // read line by line
                String line;
                while ((line = br.readLine()) != null) {
                    this.outputStream.writeUTF(line);
                }  

                String reply = this.inputStream.readUTF();
                if(reply.equals("OK"))
                {
                    logger.LogInfo("[TCPClient] File sent."); 
                }
                else
                {
                    logger.LogError("[TCPClient] File not sent."); 
                }
            } 
            catch(IOException i) 
            { 
                logger.LogException("[TCPClient] Unable to put file data.", i); 
            } 

            this.localReadFile.close();
            this.closeSocket();
        }
    }

    public boolean reReplicateFiles(String currentReplicaAddress, String sdfsFileName, String ipAddressToReplicate)
    {   
        this.initializeStreams(currentReplicaAddress);
        try
        { 
            // sends VM log ID and user input to server
            logger.LogInfo("[TCPClient] Connected to "+ currentReplicaAddress + ".");
            
            this.outputStream.writeUTF(MessageType.REREPLICATE.toString());
            this.outputStream.writeUTF(sdfsFileName);
            this.outputStream.writeUTF(ipAddressToReplicate);
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient] Replica node accepted the request to replicate file " + 
                    sdfsFileName + " to " + ipAddressToReplicate);
                return true;
            }
            else
            {
                logger.LogError("[TCPClient] Replica node rejected the request to replicate file " + 
                    sdfsFileName + " to " + ipAddressToReplicate);
            }
        } 
        catch(IOException i) 
        { 
            logger.LogException("[TCPClient] Unable to put file data.", i);
            return false;
        } 

        this.closeSocket();
        return false;
    }

    public void deleteFiles(String sdfsFileName, List<String> addresses)
    {   

        for(String address: addresses) 
        {
            this.initializeStreams(address);
            try
            {
                logger.LogInfo("[TCPClient] Connected to "+ address + ".");
                
                this.outputStream.writeUTF(MessageType.DELETE.toString());
                this.outputStream.writeUTF(sdfsFileName);
                String reply = this.inputStream.readUTF();
                if(reply.equals("OK"))
                {
                    logger.LogInfo("[TCPClient] File deleted."); 
                }
            } 
            catch(IOException i) 
            { 
                logger.LogException("[TCPClient] Unable to delete file.", i); 
            } 
            this.closeSocket();
        }
    }

     /**
     * Initializes the socket and its input and output streams.
     */
    private void initializeStreams(String address)
    {
        try 
        {
            this.socket = new Socket(address, port);
            this.inputStream = new DataInputStream(this.socket.getInputStream());
            this.outputStream = new DataOutputStream(this.socket.getOutputStream());
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
            this.inputStream.close(); 
            this.outputStream.close();
            this.socket.close();
        }
        catch(IOException e)
        { 
            logger.LogException("[TcpMessageHandler] Failed in closing resources with message:", e); 
        } 
	}
}

