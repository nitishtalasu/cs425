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
    public void create_thread() {
        try {
            /**
             * creates a client socket.
            */
            Socket socket = new Socket(address, port);
            socket.setSoTimeout(100000);
            System.out.println("Connected to "+address);

            // creates an input stream that allows reading data sent by server
            inputStream = new DataInputStream(socket.getInputStream());

            // creates an output stream that allows writing data to a server
            outputStream = new DataOutputStream(socket.getOutputStream());
            
            // creates a thread process for given input
            Thread t = new ClientThread(socket, clientInput, inputStream, outputStream, vmId);
            t.start();

        } catch (Exception e) {
            System.out.println("Connection to "+address+" accessing " +vmId+" failed due to:");
            System.out.println(e);
        }
    }

    public static void main(String args[]) {
        String addresses[] = null, vmIds[] = null;
        try {

            // reads the server related properties from a given file
            InputStream input = new FileInputStream("server_parameters.properties");
            Properties prop = new Properties();
            prop.load(input);

            // gets the corresponding property value separated by delimiter ','
            addresses = prop.getProperty("IP_address").split(",");
            vmIds = prop.getProperty("VM_ID").split(",");

        } catch (Exception e) {
            System.out.println("[Client] Exception in handling property files:");
            System.out.println(e);
        }

        // reads user input for grep command
        Scanner sc = new Scanner(System.in);
        System.out.println("Type grep command and press enter");
        System.out.println("For example: -c -E ^[0-9]*[a-z]{5}");
        String clientInput = sc.nextLine();
        sc.close();

        // creates a separate thread for each server connection
        for (int i = 0; i < addresses.length; i++) {
            Client client = new Client(addresses[i], clientInput, vmIds[i], 5000);
            client.create_thread();
        }
    }
}
