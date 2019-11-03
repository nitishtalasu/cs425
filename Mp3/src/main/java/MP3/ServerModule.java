package MP3;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Class to handle the server side operations.
 */
public class ServerModule extends Thread {
    
    private static ServerModule handler = null;

    private DatagramSocket server;

    private final int port;

    private byte[] buffer = new byte[1024]; 

    private GrepLogger logger;

    private ServerModule(int portNumber) 
    {
        this.port = portNumber;
        logger = GrepLogger.getInstance();
    }

    public static ServerModule getInstance(int portNumber) 
    {
        if (handler == null) 
        {
            handler = new ServerModule(portNumber);
        }

        return handler;
    }

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
                // Create a DatgramPacket to receive the data. 
                DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length); 
    
                // Recieve the data in byte buffer. 
                this.server.receive(packet); 
    
                String message = ToString(this.buffer);

                //System.out.println("[Server] Received Serialized message from " + packet.getAddress() + ": " + message );
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