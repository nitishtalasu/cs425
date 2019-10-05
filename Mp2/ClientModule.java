/**
 * Class for the client side operations.
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.List;

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
                str = sc.nextLine();
                if(str != null) {
                    Message msg = null;
                    if (str.equalsIgnoreCase("JOIN")) {
                        Message.Node node = MembershipList.getSelfNode();
                        MembershipList.changeNodeStatus(node, MembershipNode.Status.RUNNING);
                        msg = new Message(MessageType.JOIN, MembershipList.getMsgNodes());
                    }
                    else if(str.equalsIgnoreCase("LEAVE")) {
                        msg = new Message(MessageType.LEAVE, MembershipList.getMsgNodes());
                        
                    }
                    else if(str.equalsIgnoreCase("PRINT")) {
                        MembershipList.printMembershipList();
                        continue;
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

                        DatagramSocket client = new DatagramSocket(introducerPort, introducerAddress);
                        DatagramPacket dp = new DatagramPacket(this.buffer, this.buffer.length, 
                                                                    introducerAddress, this.port); 
                        client.connect(introducerAddress, introducerPort); 
                        client.send(dp); 
                        client.close();
                        continue;
                    }
                    
                    List<MembershipNode> neighborList = MembershipList.getNeighbors();
    
                    for(MembershipNode neighbor: neighborList) {
    
                        String address = neighbor.ipAddress;
                        InetAddress neighborAddress = InetAddress.getByName(address);
                        DatagramSocket client = new DatagramSocket();
                        DatagramPacket dp = new DatagramPacket(this.buffer, this.buffer.length, 
                                                                neighborAddress, this.port); 
                        client.send(dp); 
                        client.close();
                    }
                    this.buffer = new byte[1024]; 
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

