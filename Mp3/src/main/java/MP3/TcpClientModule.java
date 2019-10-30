/**
 * Class for the client side thread operations.
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */
import java.io.*;
import java.net.Socket;
import java.util.List;
 
public class TcpClientModule 
{ 
    private DataOutputStream outputStream = null; 
    private Socket socket = null; 
    private DataInputStream inputStream = null; 
    private String vmId = "";
    private FileWriter localWriteFile = null;
    private FileReader localReadFile = null;
    private int port = 5000;

    /**
     * Logger instance.
     */
    private GrepLogger logger;
      
    /**
     * constructor of ClientThread type class.
     * 
     */
    public TcpClientModule() throws Exception  
    { 
        // this.socket = socket; 
        // this.vmId = vmId;
        this.logger = GrepLogger.getInstance();
    } 


    public void getFiles(String sdfsFileName, String localFileName, List<String> addresses)
    {   

        for(String address: addresses) 
        {
            try
            {	
                Socket socket = new Socket(address, port);
                this.inputStream = new DataInputStream(socket.getInputStream());	
                this.outputStream = new DataOutputStream(socket.getOutputStream());		
            }	
            catch(Exception e)	
            {	
                logger.LogException("Failed to get stream for the connected socket.", e);
                throw e;	
            }
            try
            { 
                // sends VM log ID and user input to server
                this.outputStream.writeUTF("GET");
                this.outputStream.writeUTF(sdfsFileName);
                
                // generating files (for each server input) to store logs received from servers
                localWriteFile = new FileWriter(localFileName);

                //variable to check end of file
                boolean eof = false;
                while (!eof) {
                    try {
                        //read data sent by server, line-by-line, and write to file
                        String lineOutputs = this.inputStream.readUTF();
                        localWriteFile.write(lineOutputs);
                        localWriteFile.write(System.getProperty("line.separator"));
                    } catch (EOFException e) {
                        eof = true;
                        logger.LogInfo("Completed writing logs to file: "+localFileName);
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
    
                this.localWriteFile.close();
                this.socket.close(); 
            } 
            catch(IOException i) 
            { 
                logger.LogException("[Client] Exception in establishing socket:", i);
            } 
        }
    }
    public void putFiles(String sdfsFileName, String localFileName, List<String> addresses)
    {   

        for(String address: addresses) 
        {
            try
            {	
                Socket socket = new Socket(address, port);
                this.inputStream = new DataInputStream(socket.getInputStream());	
                this.outputStream = new DataOutputStream(socket.getOutputStream());		
            }	
            catch(Exception e)	
            {	
                logger.LogException("Failed to get stream for the connected socket.", e);
                throw e;	
            }
            try
            { 
                // sends VM log ID and user input to server
                this.outputStream.writeUTF("PUT");
                this.outputStream.writeUTF(sdfsFileName);
       
                
                // generating files (for each server input) to store logs received from servers
                localReadFile = new FileReader(localFileName);
                BufferedReader br = new BufferedReader(localReadFile);
                // read line by line
                String line;
                while ((line = br.readLine()) != null) {
                    this.outputStream.writeUTF(line);
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
    
                this.localReadFile.close();
                this.socket.close(); 
            } 
            catch(IOException i) 
            { 
                logger.LogException("[Client] Exception in establishing socket:", i);
            } 
        }
    }
    public void deleteFiles(String sdfsFileName, List<String> addresses)
    {   

        for(String address: addresses) 
        {
            try
            {	
                Socket socket = new Socket(address, port);
                this.inputStream = new DataInputStream(socket.getInputStream());	
                this.outputStream = new DataOutputStream(socket.getOutputStream());		
            }	
            catch(Exception e)	
            {	
                logger.LogException("Failed to get stream for the connected socket.", e);
                throw e;	
            }
            try
            {
                this.outputStream.writeUTF("DELETE");
                this.outputStream.writeUTF(sdfsFileName);
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
                this.socket.close(); 
            } 
            catch(IOException i) 
            { 
                logger.LogException("[Client] Exception in establishing socket:", i);
            } 
        }
    }
}

