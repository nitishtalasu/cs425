
 /**
  * Main class for client.
  */

import java.io.*; 
import java.net.Socket; 
import java.util.Scanner;
@SuppressWarnings("deprecation")
public class Client 
{ 
    // initialize input stream,output stream, address and port 
    private String clientInput = null; 
    private DataOutputStream outputStream = null; 
    private DataInputStream inputStream = null; 
    private String address = null;
    private String vmId = null;
    private int port;

    // constructor to initialize ip address and port 
    public Client(String address, String vmId, int port) 
    {   
        this.address = address;
        this.vmId = vmId;
        this.port = port;
       
    }
    public void create_thread() {
        try
        { 
            Scanner sc = new Scanner(System.in);
            Socket socket = new Socket(address, port); 
            System.out.println("Connected"); 
              
            // sends output to the socket 
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream()); 

            //clientInput = "-c -E ^[0-9]*[a-z]{5}";

            System.out.println("Type grep command and press enter");
            System.out.println("For example: -c -E ^[0-9]*[a-z]{5}");
            clientInput = sc.nextLine();

            sc.close();
            
            Thread t = new ClientThread(socket, clientInput, inputStream, outputStream, vmId);
            t.start(); 
        }
        catch(Exception e) 
        { 
            System.out.println(e); 
        }   
    }

    public static void main(String args[]) 
    { 
        String addresses[] = {"172.22.154.195","172.22.156.195","172.22.152.200","172.22.154.196","172.22.156.196","172.22.152.201","172.22.154.197","172.22.156.197","172.22.152.202","172.22.154.198"};
        String vmIds[] = {"vm1.log", "vm2.log", "vm3.log", "vm4.log", "vm5.log", "vm6.log", "vm7.log", "vm8.log", "vm9.log", "vm10.log"};
        for(int i=0; i<addresses.length; i++) {
            Client client = new Client(addresses[i], vmIds[i], 5000); 
            client.create_thread();
        }
    }
}
