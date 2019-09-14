import java.io.*;
import java.net.Socket;

// ClientThread class 
@SuppressWarnings("deprecation")
class ClientThread extends Thread  
{ 
    private String clientInput = ""; 
    private DataOutputStream outputStream = null; 
    private Socket socket = null; 
    private DataInputStream inputStream = null; 
    private String vmId = "";
    private FileWriter clientLog = null;
      
    // Constructor 
    public ClientThread(Socket socket, String clientInput, DataInputStream inputStream, DataOutputStream outputStream, String vmId)  
    { 
        this.socket = socket; 
        this.clientInput = clientInput; 
        this.outputStream = outputStream; 
        this.inputStream = inputStream;
        this.vmId = vmId;
	
    } 
  
    @Override
    public void run()  
    { 
        System.out.println("Client thread started: " + socket); 
        long startTime = System.currentTimeMillis();

        // string to read message from input 
        String lineOutputs = "";

            try
            { 
                this.outputStream.writeUTF(this.clientInput);
                this.outputStream.writeUTF(this.vmId);
                String filepath = "output_"+vmId;
                clientLog = new FileWriter(filepath);
                boolean eof = false;
                while (!eof) {
                    try {
                        lineOutputs = this.inputStream.readUTF();
                        //System.out.println(lineOutputs);
                        clientLog.write(lineOutputs);
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
            this.inputStream.close(); 
            this.outputStream.close(); 
            long endTime = System.currentTimeMillis();
            System.out.println("thread runtime for  "+this.vmId+": " + (endTime - startTime));
            this.clientLog.close();
	    this.socket.close(); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        } 
    }
}
