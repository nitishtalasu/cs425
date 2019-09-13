// ClientThread class 
@SuppressWarnings("deprecation")
class ClientThread extends Thread  
{ 
    private String input = ""; 
    private DataOutputStream out = null; 
    private Socket socket = null; 
    private DataInputStream in = null; 
    private FileWriter clientLog = null;
      
    // Constructor 
    public ClientThread(Socket socket, String input, DataInputStream in, DataOutputStream out, FileWriter clientLog)  
    { 
        this.socket = socket; 
        this.input = input; 
        this.out = out; 
        this.in = in;
        this.clientLog = clientLog;
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
                        // System.out.println(line2);
                        clientLog.write(line2);
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
            this.clientLog.close();
            this.socket.close(); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        } 
    }
}

