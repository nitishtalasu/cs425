/**
 * Main class for server.
 * 
 * @author Nitish Talasu (ntalasu2@illinois.edu)
 */

 /**
  * Main class for server.
  */
public class Server
{ 
    public static void main(String[] args)  
    { 
        try
        {
            int serverPort = getPortNumber(args);
            System.out.println("[Server] Starting the server on port: " + serverPort);
            ServerHandler.getInstance(serverPort).run();
        }
        catch(Exception e)
        {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Gets the port number from the arguments.
     * @param args Command line arguments.
     * @return Port number if specified, otherwise sends default value of 5000.
     * @throws NumberFormatException if argument cannot be parsed as int.
     */
    private static int getPortNumber (String[] args)
    {
        if(args.length > 0)
        {
            try 
            {
                int port = Integer.parseInt(args[0]);
                return port;
            }
            catch (NumberFormatException e) 
            {
                System.err.println("[Server] First argument" + args[0] + " must be an integer. " +
                    "Exiting the application");
                System.exit(-1);
            }
        }

        System.out.println("[Server] As port was not passed. Setting port to default value 5000.");
        return 5000;
    }
} 
