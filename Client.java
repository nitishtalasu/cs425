
import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 
@SuppressWarnings("deprecation")
public class Client 
{ 
    // initialize input output streams 
    private String input   = null; 
    private DataOutputStream out     = null; 
    private DataInputStream in    = null; 
    private String address = null;
    private int port;

    // constructor to put ip address and port 
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
            Thread t = new ClientThread(socket, input, in, out);

            t.start(); 
        }
        catch(Exception e) 
        { 
            System.out.println(e); 
        }   
    }
}
// ClientThread class 
@SuppressWarnings("deprecation")
class ClientThread extends Thread  
{ 
    private String input = ""; 
    private DataOutputStream out = null; 
    private Socket socket = null; 
    private DataInputStream in = null; 
      
    // Constructor 
    public ClientThread(Socket socket, String input, DataInputStream in, DataOutputStream out)  
    { 
        this.socket = socket; 
        this.input = input; 
        this.out = out; 
        this.in = in;
    } 
  
    @Override
    public void run()  
    { 
        System.out.println("Client thread started: " + socket); 

        // string to read message from input 
        String line = ""; 
        String line2 = "";

            try
            { 
                line = this.input; 
                this.out.writeUTF(line); 

                boolean eof = false;
                while (!eof) {
                    try {
                        line2 = this.in.readUTF();
                        System.out.println(line2);
                    } catch (EOFException e) {
                        eof = true;
                    }
                }   
            } 
            catch(IOException i) 
            { 
                System.out.println(i); 
            } 
        try
        { 
            this.in.close(); 
            this.out.close(); 
            this.socket.close(); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        } 
    }
}

