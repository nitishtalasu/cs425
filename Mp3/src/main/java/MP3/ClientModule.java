package MP3;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

/**
 * Thread for handling client inputs.
 * Join      : To join the system (Sends join message to introducer).
 * Leave     : To leave the system (Sends Leave message to its neighbors).
 * PrintList : To print the membership list.
 * PrintId   : To print the node Id.
 * get sdfsfilename localfilename      : Get replicas of a file
 * put localfilename sdfsfilename      : Write/update to replicas of a file
 * delete sdfsfilename                 : Delete all replicas of a file
 * ls sdfsfilename                     : list all addresses where a file is stored
 * store     : list all files stored at current machine
 * Exit      : To exit the program.
 */
class ClientModule extends Thread  
{ 
    private static ClientModule handler = null;
    
    private final int port;

    TcpClientModule tcp;

    private byte[] buffer = new byte[1024]; 

    private GrepLogger logger;
      
    private ClientModule(int portNumber) 
    {
        this.port = portNumber;
        logger = GrepLogger.getInstance();
        try 
        {
            this.tcp = new TcpClientModule();
        }
        catch(Exception e){

        }
    }
    
    public static ClientModule getInstance(int portNumber) 
    {
        if (handler == null) 
        {
            handler = new ClientModule(portNumber);
        }

        return handler;
    }

    @Override
    public void run()  
    { 
        
        Scanner sc = new Scanner(System.in);
        String str = "";
                  
        try
        {   
            System.out.println("Waiting for user input..");
            while(true) {
                try
                {
                    String command[] = null;
                    
                    str = sc.nextLine();
                    if(str != null) {
                        
                        String sdfsFileName;
                        String localFileName;
                        List<String> addresses;
                        Message msg = null;
                        Message.Node node = MembershipList.getSelfNode();
                        
                        List<Message.Node> nodeList = new ArrayList<Message.Node>();
                        nodeList.add(node);
                       
                        command = str.split(" ");
                        if(command[0].equalsIgnoreCase("get"))
                        {   
                            sdfsFileName = command[1];
                            localFileName = command[2];
                            // call Leader and get addresses
                            addresses = this.tcp.getreplicasFromLeader(sdfsFileName);
                            if(addresses == null)
                                logger.LogInfo("[Client: Get] No replicas found");
                           
                            //this.tcp.getFiles(sdfsFileName, localFileName, addresses);
                            this.tcp.getFilesParallel(sdfsFileName, localFileName, addresses);

                        }
                        else if(command[0].equalsIgnoreCase("put"))
                        {   
                            sdfsFileName = command[2];
                            localFileName = command[1];
                           
                            long timeElapsedAfterFileInsert = this.tcp.getFileLastUpdatedTime(sdfsFileName);
                            if (timeElapsedAfterFileInsert <= 60000 && timeElapsedAfterFileInsert != -1)
                            {
                                logger.LogInfo("[Client : Put] The file was inserted one minute before. "+
                                    "Do you still want to proceed? (y/n)"); 
                                str = sc.nextLine();
                            }
                            if (str.equalsIgnoreCase("n"))
                            {
                                logger.LogInfo("[Client : Put] Aborting the file insertion.");
                                continue;
                            }
                             // call Leader and get addresses
                            addresses = this.tcp.getAddressesFromLeader(sdfsFileName);
                            if(addresses == null)
                                logger.LogInfo("[Client: Put] No replicas found");

                            this.tcp.putFilesParallel(sdfsFileName, localFileName, addresses, "put");
                            // this.tcp.putCorpus(sdfsFileName, localFileName, addresses);
                            this.tcp.putSuccess(sdfsFileName);
                           
                        }
                        else if(command[0].equalsIgnoreCase("delete"))
                        {   
                            sdfsFileName = command[1];
                            // call Leader and get addresses
                            addresses = this.tcp.getreplicasFromLeader(sdfsFileName);
                            if(addresses == null)
                                logger.LogInfo("[Client: Delete] No replicas found");
                           
                            //this.tcp.deleteFiles(sdfsFileName, addresses);
                            this.tcp.deleteFilesParallel(sdfsFileName, addresses);
                            this.tcp.deleteSuccess(sdfsFileName);
                            
                        }
                        else if(command[0].equalsIgnoreCase("ls"))
                        {   
                            sdfsFileName = command[1];
                            ReplicaList.printReplicaFiles(sdfsFileName);
                            continue;
                        }
                        else if(command[0].equalsIgnoreCase("store"))
                        {   
                            
                            ReplicaList.printLocalReplicas();
                            continue;
                        }
                        else if (str.equalsIgnoreCase("Join")) 
                        {
                            MembershipList.changeNodeStatus(node, MembershipNode.Status.RUNNING);
                            msg = new Message(MessageType.JOIN, nodeList);
                        }
                        else if(command[0].equalsIgnoreCase("Leave")) 
                        {
                            MembershipList.changeNodeStatus(node, MembershipNode.Status.LEFT);
                            ReplicaList.clearReplicas();
                            msg = new Message(MessageType.LEAVE, nodeList);                           
                        }
                        else if(command[0].equalsIgnoreCase("PrintList")) 
                        {
                            MembershipList.printMembershipList();
                            continue;
                        }
                        else if(command[0].equalsIgnoreCase("PrintId"))
                        {
                            Message.Node selfNode = MembershipList.getSelfNode();
                            logger.LogInfo("[ClientInput] Self Id: " + selfNode);
                            continue;
                        }
                        else if(command[0].equalsIgnoreCase("PrintNeighbors"))
                        {
                            List<MembershipNode> neighbors = MembershipList.getNeighbors();
                            for (MembershipNode neighbor : neighbors) 
                            {
                                logger.LogInfo("[ClientInput] Neigbor: " + neighbor);
                            }
                            continue;
                        }
                        else if(command[0].equalsIgnoreCase("Pl"))
                        {
                            logger.LogInfo("[ClientInput] Leader: " + MembershipList.getLeaderIpAddress());
                            continue;
                        }
                        else if(command[0].equalsIgnoreCase("Pr"))
                        {
                            ReplicaList.printReplicaNodes();
                            continue;
                        }
                       
                        else if(command[0].equalsIgnoreCase("exit")) {
                            System.exit(0);
                        }
                        else {
                            logger.LogWarning("[ClientInput] Wrong command");
                            continue;
                        }
                    
                        this.buffer = Message.toJson(msg).getBytes();   
                        
                        if (str.equalsIgnoreCase("JOIN")) 
                        {
                            String introducer_address = Introducer.IPADDRESS.getValue();
                            int introducerPort = Integer.parseInt(Introducer.PORT.getValue());
                            
                            InetAddress introducerAddress = InetAddress.getByName(introducer_address);
                        
                            DatagramSocket client = new DatagramSocket();
                            DatagramPacket dp = new DatagramPacket(
                                this.buffer, 
                                this.buffer.length, 
                                introducerAddress, 
                                introducerPort); 
                            client.send(dp); 
                            client.close();
                            continue;
                        }
                        
                        List<MembershipNode> neighborList = MembershipList.getNeighbors();
        
                        for(MembershipNode neighbor: neighborList) {
        
                            String address = neighbor.ipAddress;
                            InetAddress neighborAddress = InetAddress.getByName(address);
                            DatagramSocket client = new DatagramSocket();
                            DatagramPacket dp = new DatagramPacket(
                                this.buffer, 
                                this.buffer.length, 
                                neighborAddress, 
                                Ports.UDPPort.getValue()); 
                            client.send(dp); 
                            client.close();
                        }
                        this.buffer = new byte[1024]; 
                    }
                }
                catch(Exception ex) 
                { 
                    logger.LogException("[ClientInput] User request failed", ex); 
                } 
            }          
        } 
        catch(Exception e) 
        { 
            logger.LogException("[ClientInput] User request failed", e); 
        } 
        finally
        {
            sc.close();
        }
    }
}

