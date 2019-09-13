// Java implementation of  Server side 
// It contains two classes : Server and ClientHandler 
// Save file as Server.java 


import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 
  
// Server class 
@SuppressWarnings("deprecation")
public class Server  
{ 
    public static void main(String[] args) throws IOException  
    { 
        // server is listening on port 5000 
        ServerSocket ss = new ServerSocket(5000); 
        int noOfClientsConnected = 0;
          
        try
        {

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
        finally
        {
            ss.close();
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
                //String line = ""; 
  
            // reads message from client until "Over" is sent 
            // while (!line.equals("Over")) 
            // { 
            //     try
            //     { 
            //         line = dis.readUTF(); 
            //         System.out.println(line); 
  
            //     } 
            //     catch(IOException i) 
            //     { 
            //         System.out.println(i); 
            //     } 
            // } 
            try {
                Runtime rt = Runtime.getRuntime();
                // String[] cmd = {"/bin/sh", "-c", "grep 'Report Process started' server.log|wc -l"};
                // Process proc = rt.exec(cmd);
                // printStream(proc.getInputStream());
                // System.out.println("Error : ");
                // printStream(proc.getErrorStream());
                String line;
                line = dis.readUTF();
                String command = "";
                command = command.concat("grep ");
                System.out.println(command);
                
                command = command.concat(line + " /home/chagari2/Mp1/cs425/vm1.log");
                
                System.out.println(command);
                Process proc = rt.exec(command);    
                
                BufferedReader is = new BufferedReader(new InputStreamReader(proc.getInputStream())); 
                String oLine;
                while ((oLine = is.readLine()) != null)
                {
                         System.out.println(oLine);
                         dos.writeUTF(oLine);
                }
                
                System.out.println("Done");
            } catch (Exception ex) {
                ex.printStackTrace();
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
