package MP4;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;


public class TcpServerModule extends Thread
{
    /**
     * Singleton object of TcpServerModule type class.
     */
    private static TcpServerModule handler = null;

    /**
     * A server socket waits for client requests to come in over the network. It
     * performs operations based on the client request and then returns a result to
     * the requester.
     */
    private ServerSocket server;

    /**
     * Port number where server is running.
     */
    private final int port;

    /**
     * Logger instance.
     */
    private GrepLogger logger;

    /**
     * Private constructor of TcpServerModule type class.
     * 
     * @param portNumber Port on which server is listening.
     * @param handler    Handler that the server would be using to serve the clients.
     */
    private TcpServerModule(int portNumber) 
    {
        this.port = portNumber;
        logger = GrepLogger.getInstance();
    }

    /**
     * Returns the singleton object of TcpServerModule class.
     * 
     * @param portNumber Port on which server is listening.
     * @param typeOfHandler Handler that the server would be using to serve the clients.
     * @return TcpServerModule class object.
     */
    public static TcpServerModule getInstance(int portNumber) 
    {
        if (handler == null) 
        {
            handler = new TcpServerModule(portNumber);
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
            logger.LogException("[TCPServerModule] Error in setting up server ", e);
        }

        int noOfClientsServed = 0;

        try 
        {

            while (true) 
            {
                logger.LogInfo("[TcpServer] No of clients serverd so far: " + noOfClientsServed
                        + ". Waiting for more connections.");

                // Server waiting for the client connection.
                Socket client = this.server.accept();
                noOfClientsServed += 1;

                // Creates a client handler to perform the client requested operations.
                Thread tcpMessageRequestHandler = new TcpMessagesRequestHandler(client);

                tcpMessageRequestHandler.start();
            }
        } 
        catch (Exception e) 
        {
            logger.LogException("[TcpServer] Server running operation occurred error.", e);
        } 
        finally 
        {
            this.closeServer();
        }
    }

    /**
     * Setups the server on the port.
     * 
     * @throws IOException if I/O error occurs in creating socket.
     * @throws IllegalArgumentException if port is out of range.
     */
    private void setupServer() throws IOException, IllegalArgumentException 
    {
        try 
        {
            this.server = new ServerSocket(this.port);
            logger.LogInfo("[TcpServer] Server started at Socket : " + this.server.getInetAddress() + " Port : "
                    + this.server.getLocalPort());
        } 
        catch (IOException e) 
        {
            logger.LogException("[TcpServer] Server socket creation failed.", e);
            throw e;
        }
        catch (IllegalArgumentException e) 
        {
            logger.LogException("[TcpServer] The port is outside the specified range of valid port "
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
        catch (IOException e) 
        {
            logger.LogException("[TcpServer] Server socket creation failed with exception:", e);
        }
    }
}