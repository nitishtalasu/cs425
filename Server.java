// Java implementation of  Server side 
// It contains two classes : Server and ClientHandler 
// Save file as Server.java 

package cs425;

import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 
  
// Server class 
public class Server  
{ 
    public static void main(String[] args) throws IOException  
    { 
        // server is listening on port 5000 
        ServerSocket ss = new ServerSocket(5000); 
        Integer noOfClientsConnected = 0;
          
        // running infinite loop for getting 
        // client request 
        while (true)  
        { 
            Socket s = null; 
              
            try 
            { 
                System.out.println("No of clients connected so far: " + noOfClientsConnected + ". Waiting for more connections.");

                // socket object to receive incoming client requests 
                s = ss.accept(); 
                  
                noOfClientsConnected += 1;
                System.out.println("A new client is connected : " + s); 
                  
                // obtaining input and out streams 
                DataInputStream dis = new DataInputStream(s.getInputStream()); 
                DataOutputStream dos = new DataOutputStream(s.getOutputStream()); 
                  
                System.out.println("Assigning new thread for this client"); 
  
                // create a new thread object 
                Thread t = new ClientHandler(s, dis, dos); 
  
                // Invoking the start() method 
                t.start(); 
                  
            } 
            catch (Exception e){
                e.printStackTrace(); 
            } 
        }
    } 
} 
  
// ClientHandler class 
class ClientHandler extends Thread  
{ 
    final DataInputStream dis; 
    final DataOutputStream dos; 
    final Socket s; 
      
  
    // Constructor 
    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos)  
    { 
        this.s = s; 
        this.dis = dis; 
        this.dos = dos; 
    } 
  
    @Override
    public void run()  
    { 
        System.out.println("Server started serving client: " + s); 
         
        String received; 
        String toreturn; 
        while (true)  
        { 
            try {                  
                String line = ""; 
  
            // reads message from client until "Over" is sent 
            while (!line.equals("Over")) 
            { 
                try
                { 
                    line = dis.readUTF(); 
                    System.out.println(line); 
  
                } 
                catch(IOException i) 
                { 
                    System.out.println(i); 
                } 
            } 
            System.out.println("Closing connection"); 
            break;
  
            } catch (Exception e) { 
                e.printStackTrace(); 
            } 
        } 
          
        try
        { 
            // closing resources 
            this.dis.close(); 
            this.dos.close();
            this.s.close();
              
        }catch(IOException e){ 
            e.printStackTrace(); 
        } 
    } 
} 