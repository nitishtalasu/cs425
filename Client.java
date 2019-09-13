
 /**
  * Main class for client.
  */

import java.io.*; 
import java.net.Socket; 

@SuppressWarnings("deprecation")
public class Client 
{ 
    // initialize input stream,output stream, address and port 
    private String input   = null; 
    private DataOutputStream out     = null; 
    private DataInputStream in    = null; 
    private String address = null;
    private int port;
    private FileWriter clientLog = null;

    // constructor to initialize ip address and port 
    public Client(String address, int port) 
    {   
        this.address =  address;
        this.port = port;
       
    }
    public void create_thread() {
        try
        { 
            Socket socket = new Socket(address, port); 
            System.out.println("Connected"); 
             
            // takes input from terminal 
            System.out.println("enter grep");
            input = "-e ^[0-9]*[a-z]{5}";
              
            // sends output to the socket 
            in = new DataInputStream(socket.getInputStream());
            
            out    = new DataOutputStream(socket.getOutputStream()); 

            clientLog = new FileWriter("clientLog.log");

            Thread t = new ClientThread(socket, input, in, out, logOutput, clientLog);

            t.start(); 
        }
        catch(Exception e) 
        { 
            System.out.println(e); 
        }   
    }

    public static void main(String args[]) 
    { 
        String addresses[] = {"172.22.156.195","172.22.152.200","172.22.154.196","172.22.156.196","172.22.152.201","172.22.154.197","172.22.156.197","172.22.152.202","172.22.154.198"};
        for(int i=0; i<addresses.length; i++) {
            Client client = new Client(addresses[i], 5000); 
            client.create_thread();
        } 
    }
}
