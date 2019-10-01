
/**
 * Class for the server side operations.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Class to handle the server side operations.
 */
public class ServerModule extends Thread {
    /**
     * Singleton object of ServerHandler type class.
     */
    private static ServerModule handler = null;

    /**
     * A server socket waits for client requests to come in over the network. It
     * performs operations based on the client request and then returns a result to
     * the requester.
     */
    private DatagramSocket server;

    /**
     * Port number where server is running.
     */
    private final int port;

    /**
     * buffer for the packet.
     */
    private byte[] buffer = new byte[1024]; 

    /**
     * Logger instance.
     */
    private GrepLogger logger;

    /**
     * Private constructor of ServerHandler type class.
     * 
     * @param portNumber Port on which server is listening.
     */
    private ServerModule(int portNumber) 
    {
        this.port = portNumber;
        logger = GrepLogger.getInstance();
    }

    /**
     * Returns the singleton object of ServerHandler class.
     * 
     * @param portNumber Port on which server is listening.
     * @return ServerHandler class object.
     */
    public static ServerModule getInstance(int portNumber) 
    {
        if (handler == null) 
        {
            handler = new ServerModule(portNumber);
        }

        return handler;
    }

    /**
     * Run the server.
     */
    @Override
    public void run()
    {
        try 
        {
            this.setupServer();
        }
        catch(Exception e)
        {
            return;
        }

        try
        {
            int noOfClientsServed = 0;; 
            while (true) 
            { 
                logger.LogInfo("[Server] No of clients serverd so far: " + noOfClientsServed
                            + ". Waiting for more connections.");

                // Step 2 : create a DatgramPacket to receive the data. 
                DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length); 
    
                // Step 3 : revieve the data in byte buffer. 
                this.server.receive(packet); 
    
                String message = ToString(this.buffer);

                logger.LogInfo("[Server] Received Serialized message from " + packet.getAddress() + ": " + message );
                MessageHandler msgHandler = new MessageHandler(message);
                msgHandler.start();
    
                // Clear the buffer after every message. 
                this.buffer = new byte[1024]; 
                noOfClientsServed += 1;
            } 
        }
        catch(Exception e)
        {
            logger.LogException("[Server] Server crashed", e);
        }
        finally 
        {
            this.closeServer();
        }
    }

    /**
     * Setups the server on the port.
     * 
     * @throws IOException
     */
    private void setupServer() throws IOException
    {
        try 
        {
            this.server = new DatagramSocket(this.port); 
            logger.LogInfo("[Server] Server started at Socket : " + this.server.getLocalSocketAddress() + " Port : "
                    + this.server.getLocalPort());
        } 
        catch (IOException e) 
        {
            logger.LogException("[Server] Server socket creation failed.", e);
            throw e;
        }
        catch (IllegalArgumentException e) 
        {
            logger.LogException("[Server] The port is outside the specified range of valid port "
                    + "   values.Exception stack trace:", e);
            throw e;
        }
    }

    /**
     * Closes the running server.
     */
    private void closeServer() 
    {
        if (this.server.isClosed()) {
            return; /* Early return as server socket already closed */
        }

        try 
        {
            if (!this.server.isClosed()) 
            {
                this.server.close();
            }
        }
        catch (Exception e) 
        {
            logger.LogException("[Server] Server socket creation failed with exception:", e);
        }
    }

    /**
     * Converts byte array to string.
     * @param byteArray Byte array to be converted to string.
     * @return String from the byte array.
     */
    private static String ToString(byte[] byteArray) 
    { 
        StringBuilder sb = new StringBuilder(); 
        int i = 0; 
        while (byteArray[i] != 0) 
        { 
            sb.append((char) byteArray[i]); 
            i++; 
        } 
        return sb.toString(); 
    } 
}