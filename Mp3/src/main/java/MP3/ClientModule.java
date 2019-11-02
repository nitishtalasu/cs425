import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

/**
 * Thread for handling client inputs.
 * Join      : To join the system (Sends join message to introducer).
 * Leave     : To leave the system (Sends Leave message to its neighbors).
 * PrintList : To print the membership list.
 * PrintId   : To print the node Id.
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
                    str = sc.nextLine();
                    // logger.LogInfo(str);
                    if(str != null) {
                        String command[];
                        String sdfsFileName;
                        String localFileName;
                        List<String> addresses;
                        Message msg = null;
                        Message.Node node = MembershipList.getSelfNode();
                        List<Message.Node> nodeList = new ArrayList<Message.Node>();
                        nodeList.add(node);
                        command = str.split(" ");
                        logger.LogInfo(command[0]); 
                        if(command[0].equalsIgnoreCase("get"))
                        {   
                            sdfsFileName = command[1];
                            localFileName = command[2];
                            // call Leader and get addresses
                            
                            addresses = this.tcp.getAddressesFromLeader(sdfsFileName);
                            if(addresses == null)
                                logger.LogInfo("[Client: Get] No replicas found");
                           
                            this.tcp.getFiles(sdfsFileName, localFileName, addresses);

                        }
                        else if(command[0].equalsIgnoreCase("put"))
                        {   
                            sdfsFileName = command[2];
                            localFileName = command[1];
                            // call Leader and get addresses
                            addresses = this.tcp.getAddressesFromLeader(sdfsFileName);
                            if(addresses == null)
                                logger.LogInfo("[Client: Put] No replicas found");

                            this.tcp.putFiles(sdfsFileName, localFileName, addresses);
                            this.tcp.putSuccess(sdfsFileName);
                           
                        }
                        else if(command[0].equalsIgnoreCase("delete"))
                        {   
                            sdfsFileName = command[1];
                            // call Leader and get addresses
                            addresses = this.tcp.getAddressesFromLeader(sdfsFileName);
                            if(addresses == null)
                                logger.LogInfo("[Client: Delete] No replicas found");
                           
                            this.tcp.deleteFiles(sdfsFileName, addresses);
                            
                        }
                        else if(command[0].equalsIgnoreCase("ls"))
                        {   
                            sdfsFileName = command[1];
                            ReplicaList.printReplicas(sdfsFileName);
                        }
                        else if(command[0].equalsIgnoreCase("store"))
                        {   
                            sdfsFileName = command[1];
                            List<String> fileNames = ReplicaList.getLocalReplicas();
                            logger.LogInfo("The files in the current machine are: ");
                            for(String filename: fileNames)
                            {
                                logger.LogInfo(filename);
                            }
                        }
                        if (str.equalsIgnoreCase("Join")) 
                        {
                            MembershipList.changeNodeStatus(node, MembershipNode.Status.RUNNING);
                            msg = new Message(MessageType.JOIN, nodeList);
                        }
                        else if(str.equalsIgnoreCase("Leave")) 
                        {
                            MembershipList.changeNodeStatus(node, MembershipNode.Status.LEFT);
                            msg = new Message(MessageType.LEAVE, nodeList);                           
                        }
                        else if(str.equalsIgnoreCase("PrintList")) 
                        {
                            MembershipList.printMembershipList();
                            continue;
                        }
                        else if(str.equalsIgnoreCase("PrintId"))
                        {
                            Message.Node selfNode = MembershipList.getSelfNode();
                            logger.LogInfo("[ClientInput] Self Id: " + selfNode);
                            continue;
                        }
                        else if(str.equalsIgnoreCase("PrintNeighbors"))
                        {
                            List<MembershipNode> neighbors = MembershipList.getNeighbors();
                            for (MembershipNode neighbor : neighbors) 
                            {
                                logger.LogInfo("[ClientInput] Neigbor: " + neighbor);
                            }
                            continue;
                        }
                        else if(str.equalsIgnoreCase("exit")) {
                            System.exit(0);
                        }
                        else {
                            logger.LogWarning("[ClientInput] Wrong command");
                            continue;
                        }
                    
                        this.buffer = Message.toJson(msg).getBytes();   
                        
                        if (str.equalsIgnoreCase("JOIN")) 
                        {
                            logger.LogInfo("HELLO");
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
                            System.out.println(address);
                            InetAddress neighborAddress = InetAddress.getByName(address);
                            System.out.println(neighborAddress);
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

