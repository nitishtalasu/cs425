/**
 * This class handles the operations requested by the clients.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.io.*; 
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class handles the client requests.
 */
public class GrepRequestHandler extends Thread
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
     * Logger instance.
     */
    private GrepLogger logger;

    /**
     * Regex pattern to tokenize the user input.
     */
    private static Pattern regexPattern = 
        Pattern.compile("\".*?(?<!\\\\)\"|'.*?(?<!\\\\)'|-*[A-Za-z]+");
  
    /**
     * Constructor for the class GrepRequestHandler
     * @param socket Socket of the client and server connection.
     */
    public GrepRequestHandler(Socket socket)  
    { 
        this.socket = socket;
        this.initializeStreams();   
        logger = GrepLogger.getInstance();
    }

    /**
     * Overrides the run method of Thread class. This serves the client requested operations.
     * {@inheritDoc}
     */
    @Override
    public void run()  
    { 
        logger.LogInfo("[Server] Server started serving client: " + this.socket); 
        
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
                // Clients sending the log file name.
                String vmLogFileName = this.socketInputStream.readUTF();
                logger.LogInfo(vmLogFileName);
                File logFile = new File(vmLogFileName);
                if (!logFile.exists())
                {
                    this.socketOutputStream.writeUTF(vmLogFileName + " Please check file name.");
                    break;
                }
                
                String fileAbsPath = logFile.getAbsolutePath();
                // Reads the command line from client from the socket input stream channel.
                String line = this.socketInputStream.readUTF();   
                
                // Tokenising the client input based on the regex.
                List<String> regexPatternMatchingList = new ArrayList<String>();
                Matcher regexMatcher = regexPattern.matcher(line);
                while (regexMatcher.find()) {
                    regexPatternMatchingList.add(regexMatcher.group());
                }

                List<String> commandArgs = new ArrayList<String>();
                commandArgs.add("grep");
                for (String match : regexPatternMatchingList) {
                    if(match.charAt(0) == '\"' && match.charAt((match.length()-1)) == '\"') {
                        match = match.substring(1, match.length()-1);
                    }
                    commandArgs.add(match);
                }
                commandArgs.add(fileAbsPath);
                
                // Creating the process with given client command.
                logger.LogInfo("[Server] Server executing the process with command: " + commandArgs);
                ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
                Runtime rt = Runtime.getRuntime();
                //Process process = rt.exec(commandArgs);
                Process process = processBuilder.start();
                
                // Buffer for reading the ouput from stream. 
                BufferedReader processOutputReader =
                    new BufferedReader(new InputStreamReader(process.getInputStream())); 
                
                // Reads from buffer and sends back to the client in socket output stream.
                String outputLine;
                int matchedLinescount = 0;
                while ((outputLine = processOutputReader.readLine()) != null)
                {
                    this.socketOutputStream.writeUTF(vmLogFileName + " " + outputLine);
                    matchedLinescount++;
                }

                // Writing the matched lines count to the stream.
                this.socketOutputStream.writeUTF(vmLogFileName + " " + matchedLinescount);            
                logger.LogInfo("[Server] Client request has been served.");
            } 
            catch (Exception ex) 
            {
                logger.LogException("[Server] Client requested operation failed with:", ex);
            }

            break;
        }
        
        logger.LogInfo("[Server] Closing connection"); 
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
            logger.LogException("[Server] Stream initializations failed:", e);
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
        catch(IOException e)
        { 
            logger.LogException("[Server] Failed in closing resources with message:", e); 
        } 
	}
}
