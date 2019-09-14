
 /**
  * Main class for client.
  */

import java.io.*; 
import java.net.Socket;
import java.util.Properties;
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
    public Client(String address, String clientInput, String vmId, int port) 
    {   
        this.address = address;
        this.clientInput = clientInput;
        this.vmId = vmId;
        this.port = port;
       
    }
    public void create_thread() {
        try
        { 
            Socket socket = new Socket(address, port); 
            System.out.println("Connected"); 
              
            // sends output to the socket 
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream()); 
            //clientInput = "-c -E ^[0-9]*[a-z]{5}"            
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
        String addresses[]=null, vmIds[]=null;
        try{
            InputStream input = new FileInputStream("server_parameters.properties");
            Properties prop = new Properties();
            prop.load(input);

            // get the property value and print it out
            addresses = prop.getProperty("IP_address").split(",");
            vmIds = prop.getProperty("VM_ID").split(",");
        }
        catch(Exception e) {
            System.out.println(e);
        }

        Scanner sc = new Scanner(System.in);
        System.out.println("Type grep command and press enter");
        System.out.println("For example: -c -E ^[0-9]*[a-z]{5}");
        String clientInput = sc.nextLine();
        sc.close();

        for(int i=0; i < addresses.length; i++) {
            Client client = new Client(addresses[i], clientInput, vmIds[i], 5000); 
            client.create_thread();
        }
    }
}
