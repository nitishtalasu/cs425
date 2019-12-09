package MP4;

/**
 * Class for the client side thread operations.
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TcpClientModule 
{ 
    private DataOutputStream outputStream = null; 
    private Socket socket = null; 
    private OutputStream out = null;
    private ServerSocket receiver = null;
    private DataInputStream inputStream = null; 
    private String vmId = "";
    private FileWriter localWriteFile = null;
    private FileReader localReadFile = null;
    private File myFile = null;


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
        this.logger = GrepLogger.getInstance();
    } 

    /**
     * method to get addresses of replicas from leader
     * 
     */
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

    public List<String> getFileNamesFromLeader(String fileName, String fileExtension)
    {
        String ip = MembershipList.getLeaderIpAddress();
        String json = "";
        this.initializeStreams(ip);
        try
        {
            this.outputStream.writeUTF(MessageType.FILELIST.toString());
            this.outputStream.writeUTF(fileName);
            this.outputStream.writeUTF(fileExtension);
            json = this.inputStream.readUTF();
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient] FileNames received." + json); 
            }  
        }
        catch(IOException i) 
        { 
            logger.LogException("[TCPClient] Unable to receive files data.", i); 
        } 
        this.closeSocket();
        return getListObject(json);
    }

    /**
     * method to send success message on writing replicas
     * 
     */
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

    public void getFilesParallel(final String sdfsFileName, final String localFileName, List<String> addresses)
    {   
        try
        {
            int threadNum = 4;
            ExecutorService executor = Executors.newFixedThreadPool(threadNum);
            List<FutureTask<Integer>> taskList = new ArrayList<FutureTask<Integer>>();
            for (int i = 0; i < addresses.size(); i++) 
            {
                final String address = addresses.get(i);
                FutureTask<Integer> futureTask = new FutureTask<Integer>(new Callable<Integer>() {
                    @Override
                    public Integer call() 
                    {
                        TcpClientModule client = new TcpClientModule();
                        return client.getFileParallel(sdfsFileName, localFileName, address);
                    }
                });
                taskList.add(futureTask);
                executor.execute(futureTask);
            }

            int amount = 0;
            for (int j = 0; j < addresses.size(); j++) 
            {
                FutureTask<Integer> futureTask = taskList.get(j);
                amount += futureTask.get();
            }
            executor.shutdown();

            if(amount == addresses.size())
            {
                logger.LogInfo("[TcpClient: GetFileParallel] Successfully fetched the file: "+ sdfsFileName);
            }
            if(amount <= 2)
            {
                logger.LogInfo("[TcpClient: GetFileParallel] Quorum not met for: "+ sdfsFileName);
            }
        }
        catch(Exception e)
        {
            logger.LogException("[TcpClient: GetFileParallel] failed to put files with ", e);
        }
    }

    public int getFileParallel(String sdfsFileName, String localFileName, String address)
    {
        int ret = 1;
        
        this.initializeStreams(address);
        try
        { 

            logger.LogInfo("[TCPClient] Connected to "+ address + ".");
            
            this.outputStream.writeUTF(MessageType.GET.toString());           
            this.outputStream.writeUTF(sdfsFileName);

            // generating files (for each server input) to store logs received from servers
            String currentDir = System.getProperty("user.dir");
            localWriteFile = new FileWriter(currentDir + "/src/main/java/MP4/localFile/"+localFileName + "_" + address);
            File test = new File(currentDir + "/src/main/java/MP4/localFile/"+localFileName + "_" + address);     
            int bufferSize=0;
            bufferSize=socket.getReceiveBufferSize();
            FileOutputStream fout = new FileOutputStream(test);
            byte[] buffer = new byte[bufferSize];
            int read = 0;
            long count = 0;
            long length = this.inputStream.readLong();
            while(count != length && (read = this.inputStream.read(buffer)) != -1)
            {
                fout.write(buffer, 0, read);
                count += read;
            }
            fout.close();

            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient] File received."); 

            }
            else
            {
                ret = 0;
            }  
        } 
        catch(Exception e) 
        { 
            logger.LogException("[TCPClient] Unable to receive file data.", e);
            ret = 0;
        } 
        try
        {
            this.localWriteFile.close();
        }
        catch(Exception e)
        {
            logger.LogException("[TCPClient] Unable to close write file", e);
            ret = 0;
        }
        this.closeSocket();
        return ret;
    }

    /**
     * method to get replicas from different nodes
     * 
     */
    public void getFiles(String sdfsFileName, String localFileName, List<String> addresses)
    {   
        long startTime = System.currentTimeMillis();
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
                localWriteFile = new FileWriter(currentDir + "/src/main/java/MP4/localFile/"+localFileName);
                
                File test = new File(currentDir+"/src/main/java/MP4/localFile/"+localFileName);
                int bufferSize=0;
                bufferSize=socket.getReceiveBufferSize();
                FileOutputStream fout = new FileOutputStream(test);
                byte[] buffer = new byte[bufferSize];
                int read = 0;
                long count = 0;
                long length = this.inputStream.readLong();
                String reply = "";
                while(count != length && (read = this.inputStream.read(buffer)) != -1)
                {
                    if (count + read <= length)
                    {
                        fout.write(buffer, 0, read);
                    }
                    else
                    {
                        fout.write(buffer, 0, (int)(length - count ));
                        byte[] slice = Arrays.copyOfRange(buffer, (int)(length - count), read);
                        reply = new String(slice);
                        logger.LogInfo("[TcpClientModule][getFiles] reply:" + reply );
                    }
                    
                    count += read;
                }
                fout.close();

                if (length == count)
                {
                    reply = this.inputStream.readUTF();
                }
                
                if(reply.equals("OK"))
                {
                    logger.LogInfo("[TCPClient] File received."); 

                }  
            } 
            catch(Exception e) 
            { 
                logger.LogException("[TCPClient] Unable to receive file data.", e); 
            } 
        
            this.closeSocket();
        }
            long endTime = System.currentTimeMillis();
            System.out.println("[TCPClient] Rereplication time for " + sdfsFileName + " : " + (endTime - startTime));
    }

    public boolean putFilesParallel(
        final String sdfsFileName,
        final String localFileName,
        final List<String> addresses,
        final String type)
    {
        try
        {
            int threadNum = 4;
            ExecutorService executor = Executors.newFixedThreadPool(threadNum);
            List<FutureTask<Integer>> taskList = new ArrayList<FutureTask<Integer>>();
            for (int i = 0; i < addresses.size(); i++) 
            {
                final String address = addresses.get(i);
                FutureTask<Integer> futureTask_1 = new FutureTask<Integer>(new Callable<Integer>() {
                    @Override
                    public Integer call() 
                    {
                        TcpClientModule client = new TcpClientModule();
                        return client.putFileParallel(sdfsFileName, localFileName, address , type);
                    }
                });
                taskList.add(futureTask_1);
                executor.execute(futureTask_1);
            }

            int amount = 0;
            for (int j = 0; j < addresses.size(); j++)
            {
                FutureTask<Integer> futureTask = taskList.get(j);
                amount += futureTask.get();
            }
            executor.shutdown();

            if(amount == addresses.size())
            {
                logger.LogInfo("[TcpClient: PutFileParallel] Successfully inserted the file: "+ sdfsFileName);
            }
            else if(amount <= 2)
            {
                System.out.println("[TcpClient: PutFileParallel] Deleting files as quorum not met for the file: "+ sdfsFileName);
                List<String> addr = getreplicasFromLeader(sdfsFileName);
                //deleteFilesParallel(sdfsFileName, addr);
                return false;
            }
        }
        catch(Exception e)
        {
            logger.LogException("[TcpClient: PutFileParallel] failed to put files with ", e);
            return false;
        }

        return true;
    }

    public int putFileParallel(String sdfsFileName, String localFileName, String address, String type)
    {
        this.initializeStreams(address);
        int ret = 1;
        try
        { 
            // sends VM log ID and user input to server
            logger.LogInfo("[TCPClient] Connected to "+ address + ".");
            
            this.outputStream.writeUTF(MessageType.PUT.toString());
            this.outputStream.writeUTF(sdfsFileName);
            String currentDir = System.getProperty("user.dir");
            if(type.equals("replicate"))
            {
                // localReadFile = new FileReader(currentDir+"/src/main/java/MP4/sdfsFile/"+localFileName);
                myFile = new File(currentDir+"/src/main/java/MP4/sdfsFile/"+localFileName);
            }
            else
            {
                // localReadFile = new FileReader(currentDir+"/src/main/java/MP4/localFile/"+localFileName);
                myFile = new File(currentDir+"/src/main/java/MP4/localFile/"+localFileName);
            }

            this.outputStream.writeLong(myFile.length());
            DataInputStream in = new DataInputStream(new FileInputStream(myFile));
            byte[] arr = new byte[1024 * 1024];
            int len = 0;
            while((len = in.read(arr)) != -1)
            {
                this.outputStream.write(arr, 0, len);
            }
            this.outputStream.flush();  
            in.close();         
            System.out.println("Finished sending");

            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient] File sent."); 
            }
            else
            {
                ret = 0;
                logger.LogError("[TCPClient] File not sent."); 
            }
        } 
        catch(Exception i) 
        { 
            logger.LogException("[TCPClient] Unable to put file data.", i);
            ret = 0;
        } 


        this.closeSocket();
        return ret;
    }

    

    public void putFiles(String sdfsFileName, String localFileName, List<String> addresses, String type)
    {   
        long startTime = System.currentTimeMillis();
        for(String address: addresses) 
        {
            this.initializeStreams(address);
            try
            { 
                // sends VM log ID and user input to server
                logger.LogInfo("[TCPClient] Connected to "+ address + ".");
                
                this.outputStream.writeUTF(MessageType.PUT.toString());
                this.outputStream.writeUTF(sdfsFileName);
                String currentDir = System.getProperty("user.dir");
             
                if(type.equals("replicate"))
                {
                    // localReadFile = new FileReader(currentDir+"/src/main/java/MP4/sdfsFile/"+localFileName);
                    myFile = new File(currentDir+"/src/main/java/MP4/sdfsFile/"+localFileName);
                    System.out.println("[TcpClientModule: putfiles] Replicating from sdfsfile : " + localFileName);
                }
                else
                {
                    // localReadFile = new FileReader(currentDir+"/src/main/java/MP4/localFile/"+localFileName);
                    myFile = new File(currentDir+"/src/main/java/MP4/localFile/"+localFileName);
                }
    
                this.outputStream.writeLong(myFile.length());
                DataInputStream in = new DataInputStream(new FileInputStream(myFile));
                byte[] arr = new byte[1024 * 1024];
                int len = 0;
                while((len = in.read(arr)) != -1)
                {
                    this.outputStream.write(arr, 0, len);
                }
                this.outputStream.flush();  
                in.close();         
                System.out.println("Finished sending");
        
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
            catch(Exception i) 
            { 
                logger.LogException("[TCPClient] Unable to put file data.", i); 
            } 
        
            this.closeSocket();
        }
            long endTime = System.currentTimeMillis();
            System.out.println("[TCPClient] Rereplication time for " + sdfsFileName + " : " + (endTime - startTime));
    }

     /**
     * method to re-replicate files on node failure
     * 
     */
    public boolean reReplicateFiles(String currentReplicaAddress, String sdfsFileName, String ipAddressToReplicate)
    {   
        this.initializeStreams(currentReplicaAddress);
        try
        { 
            // sends VM log ID and user input to server
            logger.LogInfo("[TCPClient] Connected to "+ currentReplicaAddress + ".");
            long startTime = System.currentTimeMillis();
            this.outputStream.writeUTF(MessageType.REREPLICATE.toString());
            this.outputStream.writeUTF(sdfsFileName);
            this.outputStream.writeUTF(ipAddressToReplicate);
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient] Replica node accepted the request to replicate file " + 
                    sdfsFileName + " to " + ipAddressToReplicate);
                long endTime = System.currentTimeMillis();
                System.out.println("[TCPClient] Rereplication time for " + sdfsFileName + " : " + (endTime - startTime));
                return true;
            }
            else
            {
                logger.LogError("[TCPClient] Replica node rejected the request to replicate file " + 
                    sdfsFileName + " to " + ipAddressToReplicate);
            }
        } 
        catch(Exception i) 
        { 
            logger.LogException("[TCPClient] Unable to put file data.", i);
            return false;
        } 

        this.closeSocket();
        return false;
    }

     /**
     * method to send success message to leader on deleting replica
     * 
     */
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

     /**
     * method to delete replicas of file
     * 
     */
    public void deleteFiles(String sdfsFileName, List<String> addresses)
    {   
        long startTime = System.currentTimeMillis();
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

        long endTime = System.currentTimeMillis();
        System.out.println("[TCPClient] Rereplication time for " + sdfsFileName + " : " + (endTime - startTime));
    }


    public void deleteFilesParallel(final String sdfsFileName, List<String> addresses)
    {   
        try
        {
            int threadNum = 4;
            ExecutorService executor = Executors.newFixedThreadPool(threadNum);
            List<FutureTask<Integer>> taskList = new ArrayList<FutureTask<Integer>>();
            for (int i = 0; i < addresses.size(); i++) 
            {
                final String address = addresses.get(i);
                FutureTask<Integer> futureTask = new FutureTask<Integer>(new Callable<Integer>() {
                    @Override
                    public Integer call() 
                    {
                        TcpClientModule client = new TcpClientModule();
                        return client.deleteFileParallel(sdfsFileName, address);
                    }
                });
                taskList.add(futureTask);
                executor.execute(futureTask);
            }

            int amount = 0;
            for (int j = 0; j < addresses.size(); j++) {
                FutureTask<Integer> futureTask = taskList.get(j);
                amount += futureTask.get();
            }
            executor.shutdown();

            if(amount == addresses.size())
            {
                logger.LogInfo("[TcpClient: PutFileParallel] Successfully inserted the file: "+ sdfsFileName);
            }
        }
        catch(Exception e)
        {
            logger.LogException("[TcpClient: PutFileParallel] failed to put files with ", e);
        }
    }

    public int deleteFileParallel(String sdfsFileName, String address)
    {
        int ret = 1;
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
            else
            {
                ret = 0;
            }
        } 
        catch(IOException i) 
        { 
            logger.LogException("[TCPClient] Unable to delete file.", i); 
            ret = 0;
        } 
        this.closeSocket();

        return ret;
    }

    public int submitMapleJob(String mapleExeName, String intermediatePrefix,  int numOfMaples)
    {
        int ret = 1;
        this.initializeStreams(Introducer.IPADDRESS.getValue());
        try
        {
            logger.LogInfo("[TCPClient][submitMapleJob] Connected to "+ Introducer.IPADDRESS.getValue() + ".");
            
            this.outputStream.writeUTF(MessageType.MAPLE.toString());
            this.outputStream.writeUTF(mapleExeName);
            this.outputStream.writeUTF(intermediatePrefix);
            this.outputStream.writeUTF(String.valueOf(numOfMaples));
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient][submitMapleJob] Maple job submitted."); 
            }
            else
            {
                ret = 0;
            }
        } 
        catch(Exception e) 
        { 
            logger.LogException("[TCPClient] [submitMapleJob] Maple job submission failed: ", e); 
            ret = 0;
        } 
        this.closeSocket();

        return ret;
    }

    public int submitMapleTask(
        String taskId,
        String ipAddress, 
        String mapleExeName, 
        String inputFile,
        String intermediatePrefix,
        List<String> processedKeys)
    {
        int ret = 1;
        this.initializeStreams(ipAddress);
        try
        {
            logger.LogInfo("[TCPClient][submitMapleTask] Connected to "+ ipAddress + ".");
            
            this.outputStream.writeUTF(MessageType.MAPLETASK.toString());
            String command = taskId + " " + mapleExeName + " " + inputFile + " " + intermediatePrefix;
            this.outputStream.writeUTF(command);
            this.outputStream.writeUTF(toJson(processedKeys));
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient][submitMapleTask] Maple task submitted."); 
            }
            else
            {
                ret = 0;
            }
        } 
        catch(Exception e) 
        { 
            logger.LogException("[TCPClient][submitMapleTask] Maple task submission failed: ", e); 
            ret = 0;
        } 
        this.closeSocket();

        return ret;
    }

    public int completeMapleTask(String taskId)
    {
        int ret = 1;
        this.initializeStreams(Introducer.IPADDRESS.getValue());
        try
        {
            logger.LogInfo("[TCPClient][completeMapleTask] Connected to "+ Introducer.IPADDRESS.getValue() + ".");
            
            this.outputStream.writeUTF(MessageType.MAPLETASKCOMPLETED.toString());
            this.outputStream.writeUTF(taskId);
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient][completeMapleTask] Sent Maple task completion message."); 
            }
            else
            {
                ret = 0;
            }
        } 
        catch(Exception e) 
        { 
            logger.LogException("[TCPClient][completeMapleTask] Maple task completion message failed: ", e); 
            ret = 0;
        } 
        this.closeSocket();

        return ret;
    }

    public int submitJuiceJob(
        String juiceExe, 
        String numOfJuiceJobs, 
        String intermediatePrefixName, 
        String fileOutput,
        String deleteIntermediateFilesOption) 
    {
        int ret = 1;
        this.initializeStreams(Introducer.IPADDRESS.getValue());
        try
        {
            logger.LogInfo("[TCPClient][submitJuiceJob] Connected to "+ Introducer.IPADDRESS.getValue() + ".");
            
            this.outputStream.writeUTF(MessageType.JUICE.toString());
            this.outputStream.writeUTF(juiceExe);
            this.outputStream.writeUTF(intermediatePrefixName);
            this.outputStream.writeUTF(numOfJuiceJobs);
            this.outputStream.writeUTF(fileOutput);
            this.outputStream.writeUTF(deleteIntermediateFilesOption);
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient][submitJuiceJob] Maple job submitted."); 
            }
            else
            {
                ret = 0;
            }
        } 
        catch(Exception e) 
        { 
            logger.LogException("[TCPClient][submitJuiceJob] Maple job submission failed: ", e); 
            ret = 0;
        } 
        this.closeSocket();

        return ret;
    }
    
    public int submitJuiceTask(
        String taskId,
        String ipAddress, 
        String juiceExeName, 
        String inputFileName, 
        String outputFileName,
        List<String> processedKeys)
    {
        int ret = 1;
        this.initializeStreams(ipAddress);
        try
        {
            logger.LogInfo("[TCPClient][submitJuiceTask] Connected to "+ ipAddress + ".");
            
            this.outputStream.writeUTF(MessageType.JUICETASK.toString());
            String command = taskId + " " + juiceExeName + " " + inputFileName + " " + outputFileName;
            this.outputStream.writeUTF(command);
            this.outputStream.writeUTF(toJson(processedKeys));
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient][submitJuiceTask] Juice task submitted."); 
            }
            else
            {
                ret = 0;
            }
        } 
        catch(Exception e) 
        { 
            logger.LogException("[TCPClient][submitJuiceTask] Juice task submission failed: ", e); 
            ret = 0;
        } 
        this.closeSocket();

        return ret;
    }

    public int completeJuiceTask(String taskId)
    {
        int ret = 1;
        this.initializeStreams(Introducer.IPADDRESS.getValue());
        try
        {
            logger.LogInfo("[TCPClient][completeJuiceTask] Connected to "+ Introducer.IPADDRESS.getValue() + ".");
            
            this.outputStream.writeUTF(MessageType.JUICETASKCOMPLETED.toString());
            this.outputStream.writeUTF(taskId);
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient][completeJuiceTask] Sent Juice task completion message."); 
            }
            else
            {
                ret = 0;
            }
        } 
        catch(Exception e) 
        { 
            logger.LogException("[TCPClient][completeJuiceTask] Juice task completion message failed: ", e); 
            ret = 0;
        } 
        this.closeSocket();

        return ret;
    }

    public int putProcessedKey(String taskId, String key)
    {
        int ret = 1;
        this.initializeStreams(Introducer.IPADDRESS.getValue());
        try
        {
            logger.LogInfo("[TCPClient][putProcessedKey] Connected to "+ Introducer.IPADDRESS.getValue() + ".");
            
            this.outputStream.writeUTF(MessageType.ADDPROCESSEDKEY.toString());
            this.outputStream.writeUTF(taskId);
            this.outputStream.writeUTF(key);
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK"))
            {
                logger.LogInfo("[TCPClient][putProcessedKey] Processed key added successfully : " + key); 
            }
            else
            {
                ret = 0;
            }
        } 
        catch(Exception e) 
        { 
            logger.LogException("[TCPClient][putProcessedKey] Adding processed key failed: ", e); 
            ret = 0;
        } 
        this.closeSocket();

        return ret;
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

        return msg;
    }

    public static String toJson(List<String> msg)
    {     
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(msg);

        return json;
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

    public long getFileLastUpdatedTime(String sdfsFileName) 
    {
        String ip = MembershipList.getLeaderIpAddress();
        String timeElapsedString = "";
        long timeElapsed = -1;
        this.initializeStreams(ip);
        try
        {
            this.outputStream.writeUTF(MessageType.FILEELAPSED.toString());
            this.outputStream.writeUTF(sdfsFileName);
            timeElapsedString = this.inputStream.readUTF();
            logger.LogInfo("[TCPClient] Received time elapsed from server: "+ timeElapsedString); 
            timeElapsed = Long.parseLong(timeElapsedString);
            String reply = this.inputStream.readUTF();
            if(reply.equals("OK") && timeElapsed != -1)
            {
                logger.LogInfo("[TCPClient] Received OK from server."); 
            }
            else
            {
                logger.LogInfo("[TCPClient] Received NACK from server. Maybe file does not exist or got deleted."); 
            }
        }
        catch(Exception i) 
        { 
            logger.LogException("[TCPClient] Unable to receive data.", i); 
        } 
        this.closeSocket();

        return timeElapsed;
    }
}

