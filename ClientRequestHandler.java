/**
 * This class handles the operations requested by the clients.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.io.*; 
import java.net.Socket;

/**
 * Class handles the client requests.
 */
public class ClientRequestHandler extends Thread
{
    /**
     * Client socket.
     */
    private final Socket socket;

    /**
     * Input stream of the socket.
     */
    private DataInputStream socketInputStream;

    /**
     * Output stream of the socket.
     */
    private DataOutputStream socketOutputStream;
  
    /**
     * Constructor for the class ClientRequestHandler
     * @param socket Socket of the client and server connection.
     */
    public ClientRequestHandler(Socket socket)  
    { 
        this.socket = socket;
        this.initializeStreams();   
    }

    /**
     * Overrides the run method of Thread class. This serves the client requested operations.
     * {@inheritDoc}
     */
    @Override
    public void run()  
    { 
        System.out.println("Server started serving client: " + this.socket); 
        
        /**
         * Server serves client requests as follows:
         * 1) Reads the grep command from the client via socket input stream.
         * 2) Invokes the grep on the machine.
         * 3) Reads the output of grep and sends back to the client via socket output stream. 
         *    Along with the output the server also sending the number of output lines.
         * 4) Closes all the resources used in serving the client.
         */
        while (true)  
        {
            try 
            {
                // Reads the command line from client from the socket input stream channel.
                String line = this.socketInputStream.readUTF();        
                String command = "";// = "grep " + line + " F:\\STUDIES\\Grad\\Distributed Systems(CS425)\\MP\\cs425\\vm1.log";
                command = command.concat("grep ");                
                command = command.concat(line + " /home/chagari2/Mp1/cs425/vm1.log");
                
                // Creating the process with given client command.
                System.out.println("[Server] Server executing the process with command: " + command);
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                Runtime rt = Runtime.getRuntime();
                Process process = rt.exec(command);
                
                // Buffer for reading the ouput from stream. 
                BufferedReader processOutputReader =
                    new BufferedReader(new InputStreamReader(process.getInputStream())); 
                
                // Reads from buffer and sends back to the client in socket output stream.
                String outputLine;
                while ((outputLine = processOutputReader.readLine()) != null)
                {
                         System.out.println(outputLine);
                         this.socketOutputStream.writeUTF(outputLine);
                }
                
                System.out.println("Client request has been served.");
            } 
            catch (Exception ex) 
            {
                System.out.println("Client requested operation failed with:");
                ex.printStackTrace();
            }

            break;
        }
        
        System.out.println("Closing connection"); 
        this.closeSocket();
    } 

	/**
     * Initializes the input and output streams.
     */
    private void initializeStreams()
    {
        try 
        {
            this.socketInputStream = new DataInputStream(this.socket.getInputStream());
            this.socketOutputStream = new DataOutputStream(this.socket.getOutputStream());
        } 
        catch (IOException e)
        {
            System.out.println("Stream initializations failed:");
            e.printStackTrace();
        }
    }

    /**
     * Closes all the resources that are used in serving the client.
     */
    private void closeSocket() 
    {
        try
        { 
            this.socketInputStream.close(); 
            this.socketOutputStream.close();
            this.socket.close();
        }
        catch(IOException e){ 
            System.out.println("Failed in closing resources with message:");
            e.printStackTrace(); 
        } 
	}
}
