/**
 * Class for the client side thread operations.
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */
import java.io.*;
import java.net.Socket;
 
class ClientThread extends Thread  
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
     * constructor of ClientThread type class.
     * 
     * @param socket Socket connection.
     * @param clientInput Grep input given by user.
     * @param inputStream input stream to receive data from server.
     * @param outputStream output stream to send data to server.
     * @param vmId the associated vm log file id.
     */
    public ClientThread(Socket socket, String clientInput, DataInputStream inputStream, DataOutputStream outputStream, String vmId)  
    { 
        this.socket = socket; 
        this.clientInput = clientInput; 
        this.outputStream = outputStream; 
        this.inputStream = inputStream;
        this.vmId = vmId;
        this.logger = GrepLogger.getInstance();
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
        logger.LogInfo("Client thread started: " + socket); 
        //time at which thread starts
        long startTime = System.currentTimeMillis();

        // string to read message received from server 
        String lineOutputs = "";

            try
            { 
                // sends VM log ID and user input to server
                this.outputStream.writeUTF(this.vmId);
                this.outputStream.writeUTF(this.clientInput);
                
                // generating files (for each server input) to store logs received from servers
                String filepath = "output_"+vmId;
                clientLog = new FileWriter(filepath);

                //variable to check end of file
                boolean eof = false;
                while (!eof) {
                    try {
                        //read data sent by server, line-by-line, and write to file
                        lineOutputs = this.inputStream.readUTF();
                        clientLog.write(lineOutputs);
                        clientLog.write(System.getProperty("line.separator"));
                    } catch (EOFException e) {
                        eof = true;
                        logger.LogInfo("Completed writing logs to file: "+filepath);
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

