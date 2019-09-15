/**
 * Class for the server side operations.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Class to handle the server side operations.
 */
public class ServerHandler {
    /**
     * Singleton object of ServerHandler type class.
     */
    private static ServerHandler handler = null;

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
     * Handler which the server uses to request client operations.
     */
    private final Class<?> requestHandler;

    /**
     * Logger instance.
     */
    private GrepLogger logger;

    /**
     * Private constructor of ServerHandler type class.
     * 
     * @param portNumber Port on which server is listening.
     * @param handler    Handler that the server would be using to serve the clients.
     */
    private ServerHandler(int portNumber, Class<?> handler) 
    {
        this.port = portNumber;
        requestHandler = handler;
        logger = GrepLogger.getInstance();
    }

    /**
     * Private constructor of ServerHandler type class.
     * 
     * @param portNumber Port on which server is listening.
     */
    private ServerHandler(int portNumber) 
    {
        this(portNumber, GrepRequestHandler.class);
    }

    /**
     * Returns the singleton object of ServerHandler class.
     * 
     * @param portNumber Port on which server is listening.
     * @param typeOfHandler Handler that the server would be using to serve the clients.
     * @return ServerHandler class object.
     */
    public static ServerHandler getInstance(int portNumber, Class<?> typeOfhandler) 
    {
        if (handler == null) 
        {
            handler = new ServerHandler(portNumber, typeOfhandler);
        }

        return handler;
    }

    /**
     * Returns the singleton object of ServerHandler class.
     * 
     * @param portNumber Port on which server is listening.
     * @return ServerHandler class object.
     */
    public static ServerHandler getInstance(int portNumber) 
    {
        if (handler == null) 
        {
            handler = new ServerHandler(portNumber);
        }

        return handler;
    }

    /**
     * Run the server.
     * 
     * @throws IOException              if I/O error occurs.
     * @throws IllegalArgumentException if any illegal arguments are passed.
     */
    public void run() throws IOException, IllegalArgumentException 
    {
        this.setupServer();

        int noOfClientsServed = 0;

        try {

            while (true) {
                logger.LogInfo("[Server] No of clients serverd so far: " + noOfClientsServed
                        + ". Waiting for more connections.");

                // Server waiting for the client connection.
                Socket client = this.server.accept();
                noOfClientsServed += 1;

                // Creates a client handler to perform the client requested operations.
                Thread clientRequestHandler = (Thread)this.getObjectofClientRequestHandler(client);

                clientRequestHandler.start();
            }
        } 
        catch (Exception e) 
        {
            logger.LogException("[Server] Server running operation occurred error.", e);
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
            logger.LogInfo("[Server] Server started at Socket : " + this.server.getInetAddress() + " Port : "
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
        catch (IOException e) 
        {
            logger.LogException("[Server] Server socket creation failed with exception:", e);
        }
    }

    /**
     * Creates an object of type T.
     * @param <T> Type of class which the server uses to request client operations.
     * @param client Socket for the server and client connection.
     * @return Object of type T class.
     * @throws Exception If any error occurs in creating object.
     */
    private <T> T getObjectofClientRequestHandler(Socket client) throws Exception 
    {
        try
        {
            Constructor<?> constructor = requestHandler.getConstructor(Socket.class);
            Object clientRequestHandler = constructor.newInstance(new Object[]{ client });

            return (T)clientRequestHandler;
        }
        catch (Exception e)
        {
            logger.LogException("[Server] Server initilization failed.", e);
            throw e;
        }
    }
}