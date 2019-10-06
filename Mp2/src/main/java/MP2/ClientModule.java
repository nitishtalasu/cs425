/**
 * Class for the client side operations.
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
class ClientModule extends Thread  
{ 
    private static ClientModule handler = null;
    
    private final int port;

    private byte[] buffer = new byte[1024]; 

    private GrepLogger logger;
      
    private ClientModule(int portNumber) 
    {
        this.port = portNumber;
        logger = GrepLogger.getInstance();
    }
    
    public static ClientModule getInstance(int portNumber) 
    {
        if (handler == null) 
        {
            handler = new ClientModule(portNumber);
        }

        return handler;
    }

    /**
     * Run the client.
     * 
     * @throws IOException              if I/O error occurs.
     * @throws IllegalArgumentException if any illegal arguments are passed.
     */
    @Override
    public void run()  
    { 
        
        Scanner sc = new Scanner(System.in);
        String str = "";
        //time at which thread starts
        //long startTime = System.currentTimeMillis();
           
        try
        {   
            System.out.println("Waiting for user input..");
            while(true) {
                try
                {
                    str = sc.nextLine();
                    if(str != null) {
                        Message msg = null;
                        Message.Node node = MembershipList.getSelfNode();
                        List<Message.Node> nodeList = new ArrayList<Message.Node>();
                        nodeList.add(node);
                        if (str.equalsIgnoreCase("JOIN")) {
                        
                            MembershipList.changeNodeStatus(node, MembershipNode.Status.RUNNING);
                            msg = new Message(MessageType.JOIN, nodeList);
                        }
                        else if(str.equalsIgnoreCase("LEAVE")) {
                            MembershipList.changeNodeStatus(node, MembershipNode.Status.LEFT);
                            msg = new Message(MessageType.LEAVE, nodeList);
                            logger.LogInfo("LEFT");
                            //MembershipList.printMembershipList();
                            
                        }
                        else if(str.equalsIgnoreCase("printlist")) {
                            MembershipList.printMembershipList();
                            continue;
                        }
                        else if(str.equalsIgnoreCase("printId"))
                        {
                            Message.Node selfNode = MembershipList.getSelfNode();
                            logger.LogInfo("[ClientInput] Self Id: " + selfNode);
                            continue;
                        }
                        else if(str.equalsIgnoreCase("printneighbors"))
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
                            logger.LogWarning("Wrong command");
                            continue;
                        }
                    
                        this.buffer = Message.toJson(msg).getBytes();   
                        
                        if (str.equalsIgnoreCase("JOIN")) {
                            String introducer_address = Introducer.IPADDRESS.getValue();
                            int introducerPort = Integer.parseInt(Introducer.PORT.getValue());
                            
                            InetAddress introducerAddress = InetAddress.getByName(introducer_address);
                        
                            DatagramSocket client = new DatagramSocket();
                            DatagramPacket dp = new DatagramPacket(this.buffer, this.buffer.length, 
                                                                        introducerAddress, introducerPort); 
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
                            DatagramPacket dp = new DatagramPacket(this.buffer, this.buffer.length, 
                                                                    neighborAddress, 5000); 
                            client.send(dp); 
                            client.close();
                        }
                        this.buffer = new byte[1024]; 
                    }
                }
                catch(Exception ex) 
                { 
                    logger.LogException("[Client] User request failed", ex); 
                } 
            }          
        } 
        catch(Exception e) 
        { 
            logger.LogException("[Client] User request failed", e); 
        } 
        finally
        {
            sc.close();
        }
    }
}

