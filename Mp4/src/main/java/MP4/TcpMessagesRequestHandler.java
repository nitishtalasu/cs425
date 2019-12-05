package MP4;


/**
 * This class handles the TCP operations requested by the clients.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    private File myFile = null;

    private OutputStream out = null;

    private ServerSocket receiver = null;

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
                
                String reply = ProcessMessage(msgType);

                // Writing the reply to the stream.
               
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
                reply = PutFiles("put");
                break;

            case DELETE:
                reply = DeleteFile();
                break;

            case REREPLICATE:
                reply = ReReplicateFile("replicate");
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

            case DELETE_SUCCESS:
                reply = DeleteFilesSuccess();
                break;
            
            case FILEELAPSED:
                reply = FileTimeElapsed();
                break;

            case FILELIST:
                reply = GetFileNames();
                break;

            case MAPLE:
                reply = MapleJob();
                break;

            case MAPLETASK:
                reply = MapleTask();
                break;

            case MAPLETASKCOMPLETED:
                reply = CompleteMapleTask();
                break;

            case JUICE:
                reply = JuiceJob();
                break;

            case JUICETASK:
                reply = JuiceTask();
                break;
            
            case JUICETASKCOMPLETED:
                reply = CompleteJuiceTask();
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
        
        MembershipList.setLeaderIpAddress(clientIpAddress);
        String selfIp = MembershipList.getIpAddress(MembershipList.getSelfNode().id);
        if (!selfIp.equals(clientIpAddress))
        {
            ReplicaList.clearReplicas();
        }
        logger.LogInfo("[TcpMessageHandler] Newly elected leader: " + MembershipList.getLeaderIpAddress());

        return reply;
    }

    private String CoordinationMessage() 
    {
        String reply = "";
        // ReplicaList.printReplicaNodes();
        List<String> files = ReplicaList.getLocalReplicas();
        if (files != null)
        {
            reply = new Gson().toJson(files);
        }

        return reply;
    }    

    private String GetFiles()
    {
        String reply = "OK";
        try 
        {
            String sdfsFileName = this.socketInputStream.readUTF();
            String currentDir = System.getProperty("user.dir");
 
            localReadFile = new FileReader(currentDir+"/src/main/java/MP4/sdfsFile/"+sdfsFileName);



            myFile = new File(currentDir+"/src/main/java/MP4/sdfsFile/"+sdfsFileName);
            byte[] buffer; 
            
            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream in = new BufferedInputStream(fis);
            long fileLength = myFile.length(); 
            long current = 0;
            while(current!=fileLength){ 
                int size = 10000;
                if(fileLength - current >= size)
                    current += size;    
                else{ 
                    size = (int)(fileLength - current); 
                    current = fileLength;
                } 
                buffer = new byte[size]; 
                in.read(buffer, 0, size); 
                out.write(buffer);
                System.out.print("Sending file ... "+(current*100)/fileLength+"% complete!");
            }   
            out.flush();
            out.close();
            in.close();
           

            //variable to check end of file
            // BufferedReader br = new BufferedReader(localReadFile);
            // read line by line
            // String line;
            // while ((line = br.readLine()) != null) {
            //     this.socketOutputStream.writeUTF(line);
            // }  
            // this.socketOutputStream.writeUTF("EOF");
        }
        catch(IOException e)
        {
            logger.LogException("[TCPMessageRequestHandler] Unable to get file data.", e); 
        }
        return reply;
    }

    private String FileTimeElapsed() 
    {
        String reply = "OK";

        try
        {
            String sdfsFileName = this.socketInputStream.readUTF();
            long timeElapsed = ReplicaList.GetFileTimeElapsed(sdfsFileName);
            logger.LogInfo("[TCPMessageRequestHandler] Received time elapsed of file: "+ sdfsFileName + 
                " is " + timeElapsed);
            this.socketOutputStream.writeUTF(String.valueOf(timeElapsed));
        }
        catch(Exception e)
        {
            logger.LogException("[TcpMessageRequestHandler] Failed with ", e);
            reply = "NACK";
        }
        
        
        return reply;
    }

    private synchronized String PutFiles(String type)
    {
        String reply = "OK";
        logger.LogInfo("[TCPMessageRequestHandler] Entered PutFile method.");
        try
        {
            String sdfsFileName = this.socketInputStream.readUTF();
            String currentDir = System.getProperty("user.dir");
           
            localWriteFile = new FileWriter(currentDir+"/src/main/java/MP4/sdfsFile/"+sdfsFileName, true);

            File test = new File(currentDir+"/src/main/java/MP4/sdfsFile/"+sdfsFileName);
            int maxsize = 999999999;
            int byteread;
            int current = 0;
            // byte[] buffer = new byte[maxsize];
            
            InputStream is = socket.getInputStream();
            // File test = new File("D:\\AtomSetup.exe");
            test.createNewFile();
            FileOutputStream fos = new FileOutputStream(test);
            BufferedOutputStream out = new BufferedOutputStream(fos);
            byte[] buffer = new byte[16384];

            while ((byteread = is.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, byteread);
            }
            
            out.flush();
            fos.close();
            is.close();

            // boolean eof = false;
            //     while (!eof) 
            //     {
            //         try 
            //         {
            //             //read data sent by server, line-by-line, and write to file
            //             String lineOutputs = this.socketInputStream.readUTF();
            //             if (lineOutputs.equals("EOF"))
            //             {
            //                 eof = true;
            //                 localWriteFile.close();
            //                 break;
            //             }
            //             localWriteFile.write(lineOutputs);
                       
            //             localWriteFile.write(System.getProperty("line.separator"));
            //         } 
            //         catch (EOFException e) 
            //         {
            //             eof = true;
            //             reply = "NACK";
            //             localWriteFile.close();
            //             logger.LogInfo("Completed writing logs to file: "+sdfsFileName);
            //         }
            //     }

            ReplicaList.addNewFile(sdfsFileName);
        }
        catch(IOException e) 
        {
            reply = "NACK";
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
            String currentDir = System.getProperty("user.dir");
           
            File file = new File(currentDir + "/src/main/java/MP4/sdfsFile/" +sdfsFileName);
            if(file.delete()) 
            { 
                logger.LogInfo("[TCPMessageRequestHandler] File deleted successfully"); 
            } 
            else
            { 
                System.out.println("[TCPMessageRequestHandler] Failed to delete the file"); 
            } 
            ReplicaList.deleteFileFromNode(sdfsFileName);
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while deleting file", e); 
        }
        return reply;
    }

    private String ReReplicateFile(String type)
    {
        String reply = "OK";

        try
        {
            String sdfsFileName = this.socketInputStream.readUTF();
            String ipAddressToReplicate = this.socketInputStream.readUTF();
            logger.LogInfo("[TCPMessageRequestHandler] Received rereplication of file: "+ sdfsFileName + 
                " to the replica " + ipAddressToReplicate);
            List<String> ipAddresses = new ArrayList<String>();
            ipAddresses.add(ipAddressToReplicate);
            TcpClientModule client = new TcpClientModule();
            client.putFiles(sdfsFileName, sdfsFileName, ipAddresses, type);
            client.putSuccess(sdfsFileName);
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
           logger.LogInfo("[TCPMessageRequestHandler] Returning replicaIpAddress");
           for (String string : addresses) 
           {
                logger.LogInfo("[TCPMessageRequestHandler] IpAddress: " + string);
           } 
           String json = this.toJson(addresses);
           logger.LogInfo("[TCPMessageRequestHandler] Returning replicaIpAddress: " + json);
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
           Set<String> set = new LinkedHashSet<>();
           set.addAll(addresses);
           addresses.clear();
           addresses.addAll(set);
           String json = this.toJson(addresses);
           logger.LogInfo("[TCPMessageRequestHandler] [GetReplicaList] Sending replica list as: " + json);
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
        
           ReplicaList.replicationCompleted(sdfsFileName);
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while deleting file", e); 
        }
        return reply;
    }

    private String DeleteFilesSuccess()
    {
        String reply = "OK";
        try
        {
           String sdfsFileName = this.socketInputStream.readUTF();
  
           ReplicaList.deleteReplicaFile(sdfsFileName);
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while deleting file", e); 
        }
        return reply;
    }

    private String GetFileNames()
    {
        String reply = "OK";
        try
        {
           String fileName = this.socketInputStream.readUTF();
           List<String> files = ReplicaList.GetFileNames(fileName);
           String json = this.toJson(files);
           logger.LogInfo("[TCPMessageRequestHandler][GetFileNames] Sending replica file list as: " + json);
           this.socketOutputStream.writeUTF(json);
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while sending file list: ", e); 
            reply = "NACK";
        }
        return reply;
    }

    private String MapleJob()
    {
        String reply = "OK";
        try
        {
            String selfIp = this.socket.getLocalAddress().toString();
            if (!selfIp.equals(Introducer.IPADDRESS.getValue()))
            {
                logger.LogInfo("[TCPMessageRequestHandler] Maple job message reached node " +
                    "which is not introducer. So dropping it.");
                reply = "NACK"; 
            }

            String mapleExeName = this.socketInputStream.readUTF();
            String intermediatePrefix  = this.socketInputStream.readUTF();
            String numOfMaples = this.socketInputStream.readUTF();

            if(!Maple.createJob(mapleExeName, intermediatePrefix, numOfMaples))
            {
                reply = "NACK";
            }
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while creating the maple job: ", e); 
        }

        return reply;
    }

    private String MapleTask()
    {
        String reply = "OK";
        try
        {
            String inputCommand = this.socketInputStream.readUTF();

            Maple maple = new Maple(inputCommand);
            maple.start();
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while starting the maple task", e); 
            reply = "NACK";
        }

        return reply;
    }

    private String CompleteMapleTask()
    {
        String reply = "OK";
        try
        {
            String taskId = this.socketInputStream.readUTF();

            String selfIp = this.socket.getLocalAddress().toString();
            if (!selfIp.equals(Introducer.IPADDRESS.getValue()))
            {
                logger.LogInfo("[TCPMessageRequestHandler] Maple job message reached node " +
                    "which is not introducer. So dropping it.");
                reply = "NACK"; 
            }

            MapleJuiceList.changeTaskStatus(taskId, TaskStatus.FINISHED);
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while completing the maple task", e); 
            reply = "NACK";
        }

        return reply;
    }

    private String JuiceJob()
    {
        String reply = "OK";
        try
        {
            String selfIp = this.socket.getLocalAddress().toString();
            if (!selfIp.equals(Introducer.IPADDRESS.getValue()))
            {
                logger.LogInfo("[TCPMessageRequestHandler] Juice job message reached node " +
                    "which is not introducer. So dropping it.");
                reply = "NACK"; 
            }

            String juiceExe = this.socketInputStream.readUTF();
            String intermediatePrefixName  = this.socketInputStream.readUTF();
            String numOfJuiceTasks = this.socketInputStream.readUTF();
            String fileOutput = this.socketInputStream.readUTF();
            String deleteIntermediateFilesOption = this.socketInputStream.readUTF();

            if(!Juice.createJob(
                    juiceExe, 
                    intermediatePrefixName, 
                    numOfJuiceTasks, 
                    fileOutput, 
                    deleteIntermediateFilesOption))
            {
                reply = "NACK";
            }
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while creating the juice job: ", e); 
        }

        return reply;
    }

    private String JuiceTask()
    {
        String reply = "OK";
        try
        {
            String inputCommand = this.socketInputStream.readUTF();

            Juice juice = new Juice(inputCommand);
            juice.start();
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while starting the juice task", e); 
            reply = "NACK";
        }

        return reply;
    }

    private String CompleteJuiceTask()
    {
        String reply = "OK";
        try
        {
            String taskId = this.socketInputStream.readUTF();

            String selfIp = this.socket.getLocalAddress().toString();
            if (!selfIp.equals(Introducer.IPADDRESS.getValue()))
            {
                logger.LogInfo("[TCPMessageRequestHandler] Juice job message reached node " +
                    "which is not introducer. So dropping it.");
                reply = "NACK"; 
            }

            MapleJuiceList.changeTaskStatus(taskId, TaskStatus.FINISHED);
        }
        catch(IOException e) 
        {
            logger.LogException("[TCPMessageRequestHandler] Exception while completing the juice task", e); 
            reply = "NACK";
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
