/**
 * Class for the client side operations.
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

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
      
        //time at which thread starts
        //long startTime = System.currentTimeMillis();
           
        try
        {   
            System.out.println("Waiting for user input..");
            while(true) {
                if(str = sc.nextLine()) {
                    if (str.equalsIgnoreCase("JOIN")) {
                        setSelfNode();
                        Message msg = new Message(MessageType.JOIN, getMsgNodes());
                    }
                    else if(str.equalsIgnoreCase("LEAVE")) {
                        Message msg = new Message(MessageType.LEAVE, getMsgNodes());
                        
                    }
                    else if(str.equalsIgnoreCase("PRINT")) {
                        printMembershipList();
                        continue;
                    }
                    else {
                        logger.log(Level.WARNING, "Wrong command");
                        continue;
                    }
                
                    this.buffer = msg.toJson().getByteArray();   
    
                    List<MembershipNode> neighborList = getNeighbors();
    
                    for(MembershipNode neighbor: neighborList) {
    
                        String address = neighbor.ipAddress;
                        DatagramSocket client = new DatagramSocket(this.port, address);
                        DatagramPacket dp = new DatagramPacket(this.buffer, this.buffer.length, address, this.port); 
                        client.connect(address, port); 
                        client.send(dp); 
                        client.close();
                    }
                    this.buffer = new byte[1024]; 
                }
            }
            sc.close();
           
        } 
        catch(Exception e) 
        { 
            logger.LogException("[Client] User request failed", e); 
        } 
    }
}

