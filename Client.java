/**
 * Main class for client.
 * 
 * @author Prateeth Reddy Chagari (chagari2@illinois.edu)
 */

import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

@SuppressWarnings("deprecation")
public class Client {
    /**
     * User input for grep.
     */
    private String clientInput = null;
    /**
     * Output stream of the socket.
     */
    private DataOutputStream outputStream = null;
    /**
    * Input stream of the socket.
    */
    private DataInputStream inputStream = null;
    /**
     * Server address.
     */
    private String address = null;
    /**
     * VM log ID.
     */
    private String vmId = null;
    /**
     * port number
     */
    private int port;

    /**
     * Logger instance.
     */
    private static GrepLogger logger = GrepLogger.initialize("GrepClient", "GrepClient.log");

    /**
     * Constructor for the class ClientRequestHandler
     * @param address Server address to connect to.
     * @param clientInput Grep command provided by client.
     * @param vmId log file ID of a particular server.
     * @param port server port number.
     */
    public Client(String address, String clientInput, String vmId, int port) {
        this.address = address;
        this.clientInput = clientInput;
        this.vmId = vmId;
        this.port = port;

    }

    /**
     * Method to create a thread that connects to each server 
     */
    public void create_thread(ThreadGroup threadGroup) {
        try {
            /**
             * creates a client socket.
            */
            Socket socket = new Socket(address, port);
            socket.setSoTimeout(100000);
            logger.LogInfo("Connected to "+address);
            
            // creates a thread process for given input
            Thread t = new ClientThread(threadGroup, socket, clientInput, vmId);
            t.start();

        } catch (Exception e) {
            logger.LogException("Connection to "+address+" accessing " +vmId+" failed due to:", e);
        }
    }

    public static void main(String args[]) {
        String addresses[] = null, vmIds[] = null;
        long startTime = System.currentTimeMillis();
        try {

            // reads the server related properties from a given file
            InputStream input = new FileInputStream("server_parameters.properties");
            Properties prop = new Properties();
            prop.load(input);

            // gets the corresponding property value separated by delimiter ','
            addresses = prop.getProperty("IP_address").split(",");
            vmIds = prop.getProperty("VM_ID").split(",");

        } catch (Exception e) {
            logger.LogException("[Client] Exception in handling property files:", e);
        }

        // reads user input for grep command
        Scanner sc = new Scanner(System.in);
        logger.LogInfo("Type grep command and press enter");
        logger.LogInfo("For example: -c -E ^[0-9]*[a-z]{5}");
        String clientInput = sc.nextLine();
        sc.close();

        ThreadGroup threadGroup = new ThreadGroup("grepClient");

        // creates a separate thread for each server connection
        for (int i = 0; i < addresses.length; i++) {
            Client client = new Client(addresses[i], clientInput, vmIds[i], 5000);
            client.create_thread(threadGroup);
        }
        ThreadCount.waitForThreadsToComplete(threadGroup, logger);
        long endTime = System.currentTimeMillis();
        System.out.println("Total runtime: "+(endTime - startTime));
        
    }
}
