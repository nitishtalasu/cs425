/**
 * Class for the client side thread operations.
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TcpClientModule 
{ 
    private DataOutputStream outputStream = null; 
    private Socket socket = null; 
    private DataInputStream inputStream = null; 
    private String vmId = "";
    private FileWriter localWriteFile = null;
    private FileReader localReadFile = null;


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

    public List<String> getAddressesFromLeader(String sdfsFileName)
    {
        String ip = MembershipList.getLeaderIpAddress();
        String json = "";
        this.initializeStreams(ip);
        try
        {
            this.outputStream.writeUTF(MessageType.LIST.toString());
            this.outputStream.writeUTF(sdfsFileName);
            json = this.inputStream.readUTF();
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient] Addresses received."); 
            }  
        }
        catch(IOException i) 
        { 
            logger.LogException("[TCPClient] Unable to receive file data.", i); 
        } 
        this.closeSocket();
        return getListObject(json);
    }

    public void putSuccess(String sdfsFileName)
    {
        String ip = MembershipList.getLeaderIpAddress();
        this.initializeStreams(ip);
        try
        {
            this.outputStream.writeUTF(MessageType.PUT_SUCCESS.toString());
            this.outputStream.writeUTF(sdfsFileName);
            
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient] Addresses received."); 
            }  
        }
        catch(IOException i) 
        { 
            logger.LogException("[TCPClient] Unable to receive file data.", i); 
        } 
        this.closeSocket();
        
        
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
                String currentDir = System.getProperty("user.dir");
                logger.LogInfo("Current directory"+ currentDir);
                localWriteFile = new FileWriter(currentDir + "/src/main/java/MP3/localFile/"+localFileName);

                //variable to check end of file
                boolean eof = false;
                while (!eof) {
                    try {
                        //read data sent by server, line-by-line, and write to file
                        String lineOutputs = this.inputStream.readUTF();
                        if (lineOutputs.equals("EOF"))
                        {
                            eof = true;
                            localWriteFile.close();
                            break;
                        }
                        localWriteFile.write(lineOutputs);
                        localWriteFile.write(System.getProperty("line.separator"));
                    } catch (EOFException e) {
                        eof = true;
                        localWriteFile.close();
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
            try
            {
                this.localWriteFile.close();
            }
            catch(IOException e)
            {
                logger.LogException("[TCPClient] Unable to close write file", e); 
            }
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
                // String choice = "";
                // String writeStatus = this.inputStream.readUTF();
                // logger.LogInfo("[TCPClient] 1.  "+ writeStatus);
                // if(writeStatus != "")
                // {   
                //     logger.LogInfo(writeStatus);
                //     Scanner sc = new Scanner(System.in);
                //     choice = sc.nextLine();
                //     this.outputStream.writeUTF(choice);

                // }
                // if(choice.equalsIgnoreCase("no"))
                // {
                //     continue;
                // }
                
                // generating files (for each server input) to store logs received from servers
                this.outputStream.writeUTF(sdfsFileName);
                String currentDir = System.getProperty("user.dir");
                logger.LogInfo("Current directory"+ currentDir);
                localReadFile = new FileReader(currentDir+"/src/main/java/MP3/localFile/"+localFileName);
                BufferedReader br = new BufferedReader(localReadFile);
                // read line by line
                String line;
                while ((line = br.readLine()) != null) {
                    logger.LogInfo(line);
                    this.outputStream.writeUTF(line);
                }  
                this.outputStream.writeUTF("EOF");

                String reply = this.inputStream.readUTF();
                logger.LogInfo("[TCPClient] 2.  "+ reply);
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
            try
            {
                this.localReadFile.close();
            }
            catch(IOException e) 
            { 
                logger.LogException("[TCPClient] Unable to close read file", e); 
            } 
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
    public void deleteSuccess(String sdfsFileName)
    {
        String ip = MembershipList.getLeaderIpAddress();
        this.initializeStreams(ip);
        try
        {
            this.outputStream.writeUTF(MessageType.DELETE_SUCCESS.toString());
            this.outputStream.writeUTF(sdfsFileName);
            
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient] Addresses received."); 
            }  
        }
        catch(IOException i) 
        { 
            logger.LogException("[TCPClient] Unable to receive file data.", i); 
        } 
        this.closeSocket();
        
        
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
            this.socket = new Socket(address, Ports.TCPPort.getValue());
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
    
    public static List<String> getListObject(String jsonString)
    {     
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<String> msg = gson.fromJson(jsonString, List.class);
        //String jsonEmp = gson.toJson(emp);

        return msg;
    }

    public List<String> getreplicasFromLeader(String sdfsFileName) 
    {
		String ip = MembershipList.getLeaderIpAddress();
        String json = "";
        this.initializeStreams(ip);
        try
        {
            this.outputStream.writeUTF(MessageType.REPLICALIST.toString());
            this.outputStream.writeUTF(sdfsFileName);
            json = this.inputStream.readUTF();
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient] Addresses received."); 
            }  
        }
        catch(IOException i) 
        { 
            logger.LogException("[TCPClient] Unable to receive file data.", i); 
        } 
        this.closeSocket();
        return getListObject(json);
	}
}

