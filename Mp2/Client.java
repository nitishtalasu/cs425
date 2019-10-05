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
    private String clientInput = ""; 
    private DataOutputStream outputStream = null; 
    private Socket socket = null; 
    private DataInputStream inputStream = null; 
    private String vmId = "";
    private FileWriter clientLog = null;

    /**
     * Logger instance.
     */
    private GrepLogger logger;
      
    /**
     * constructor of ClientModule type class.
     * 
     * @param threadGroup Parent thread group.
     * @param socket Socket connection.
     * @param clientInput Grep input given by user.
     * @param vmId the associated vm log file id.
     */
    public ClientModule(ThreadGroup threadGroup, Socket socket, String clientInput, String vmId)
    throws Exception  
    { 
        super(threadGroup, vmId);
        this.socket = socket; 
        this.clientInput = clientInput; 
        this.vmId = vmId;
        this.logger = GrepLogger.getInstance();

        try
        {	
            this.inputStream = new DataInputStream(socket.getInputStream());	
            this.outputStream = new DataOutputStream(socket.getOutputStream());		
        }	
        catch(Exception e)	
        {	
            logger.LogException("Failed to get stream for the connected socket.", e);
            throw e;	
        }
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
        logger.LogInfo("Client thread started: " + socket); 
        //time at which thread starts
        long startTime = System.currentTimeMillis();
        String str = "";


        try
        {   
            while(true) {
                if(str = sc.nextLine()) {
                    if (str.equalsIgnoreCase("JOIN")) {
                        
                        Message msg = new Message(MessageType.JOIN, new Message.Node(id, 1));
                    }
                    else if(str.equalsIgnoreCase("LEAVE")) {
                        Message msg = new Message(MessageType.LEAVE, new Message.Node(id, 1));
                    }
                    // break;
                }
            }
            int port = 5000;
            List<MembershipNode> neighborList = mList.getNeighbors(node);
            for (MembershipNode mNode: mList) {
                if (hostIP.equals(mNode.id)) {
                    node = mNode;
                }
            }
        } 
        catch(IOException i) 
        { 
            logger.LogException("[Client] Client grep query faield.", i); 
        } 
        try
        { 
            //closing streams and sockets
            this.inputStream.close(); 
            this.outputStream.close(); 
            //calculating time at which thread ends
            long endTime = System.currentTimeMillis();
            logger.LogInfo("thread runtime for  "+this.vmId+": " + (endTime - startTime));
            this.clientLog.close();
	        this.socket.close(); 
        } 
        catch(IOException i) 
        { 
            logger.LogException("[Client] Exception in establishing socket:", i);
        } 
    }
}

