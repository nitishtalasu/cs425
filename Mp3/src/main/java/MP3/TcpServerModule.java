import java.io.*;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServerModule {
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
     * Handler which the server uses to request client operations.
     */
    private final Class<?> requestHandler;

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
    private TcpServerModule(int portNumber, Class<?> handler) 
    {
        this.port = portNumber;
        requestHandler = handler;
        logger = GrepLogger.getInstance();
    }

    /**
     * Returns the singleton object of TcpServerModule class.
     * 
     * @param portNumber Port on which server is listening.
     * @param typeOfHandler Handler that the server would be using to serve the clients.
     * @return TcpServerModule class object.
     */
    public static TcpServerModule getInstance(int portNumber, Class<?> typeOfhandler) 
    {
        if (handler == null) 
        {
            handler = new TcpServerModule(portNumber, typeOfhandler);
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
                Thread clientRequestHandler = new TcpMessagesRequestHandler(client);

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
}