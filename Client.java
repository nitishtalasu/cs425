
import java.net.*; 
import java.io.*; 
@SuppressWarnings("deprecation")
public class Client 
{ 
    // initialize socket and input output streams 
    // private Socket socket            = null; 
    private String  input   = null; 
    private DataOutputStream out     = null; 
    private String address = null;
    private int port;
   // private BufferedReader reader=null;
    // constructor to put ip address and port 
    public Client(String address, int port) 
    {   
        this.address =  address;
        this.port = port;
       
    }
    public void create_thread() {
    //   try{
        // establish a connection 
   //     while(true) {
        try
        { 
            Socket socket = new Socket(address, port); 
            System.out.println("Connected"); 
             // takes input from terminal 
             //
             input  = "Hello there"; 
  
             // sends output to the socket 
             out    = new DataOutputStream(socket.getOutputStream()); 
             Thread t = new ClientThread(socket, input, out);

             t.start(); 
        }
        catch(Exception e) 
        { 
            System.out.println(e); 
        } 
    //}
}
}
// ClientThread class 
@SuppressWarnings("deprecation")
class ClientThread extends Thread  
{ 
    private String input = ""; 
    private DataOutputStream out = null; 
    private Socket socket = null; 
      
  
    // Constructor 
    public ClientThread(Socket socket, String input, DataOutputStream out)  
    { 
        this.socket = socket; 
        this.input = input; 
        this.out = out; 
    } 
  
    @Override
    public void run()  
    { 
        System.out.println("Client thread started: " + socket); 


        // string to read message from input 
       String line = ""; 
  
        // keep reading until "Over" is input 
         
         
            try
            { 
                line = this.input; 
                this.out.writeUTF(line); 
            } 
            catch(IOException i) 
            { 
                System.out.println(i); 
            } 
        
  
        // close the connection 
        try
        { 
         //   this.input.close(); 
            this.out.close(); 
            this.socket.close(); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        } 
    }
}

