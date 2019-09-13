/**
 * Class for the server side operations.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Class to handle the server side operations.
 */
public class ServerHandler
{ 
    /**
     * Singleton object of ServerHandler type class.
     */
    private static ServerHandler handler = null;

    /**
     * A server socket waits for client requests to come in over the network. 
     * It performs operations based on the client request and then returns a result to the requester.
     */
    private ServerSocket server;

    /**
     * Port number where server is running.
     */
    private final int port;

    /**
     * Private constructor of ServerHandler type class.
     * @param portNumber Port on which server is listening.
     */
    private ServerHandler(int portNumber)
    {
        this.port = portNumber;
    }
 
    /**
     * Returns the singleton object of ServerHandler class.
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
     * @throws IOException if I/O error occurs.
     * @throws IllegalArgumentException if any illegal arguments are passed.
     */
    public void run() throws IOException, IllegalArgumentException
    {
        this.setupServer();

        int noOfClientsServed = 0;
          
        try
        {

            while (true)  
            { 
                System.out.println("No of clients serverd so far: " + noOfClientsServed + 
                    ". Waiting for more connections.");

                // Server waiting for the client connection.
                Socket client = this.server.accept();                   
                noOfClientsServed += 1;
                
                // Creates a client handler to perform the client requested operations.
                ClientRequestHandler clientRequestHandler = new ClientRequestHandler(client);

                clientRequestHandler.start();           
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            this.closeServer();
        }
    }

    /**
     * Setups the server on the port.
     * @throws IOException if I/O error occurs in creating socket.
     * @throws IllegalArgumentException if port is out of range.
     */
    private void setupServer() throws IOException, IllegalArgumentException
    {
        try
        {
            this.server = new ServerSocket(this.port);
            System.out.println("[Server] Server started at Socket : " +
                this.server.getInetAddress() + " Port : " +
                this.server.getLocalPort());
        }
        catch(IOException e)
        {
            System.out.println("Server socket creation failed with exception:");
            e.printStackTrace();
            throw e;
        }
        catch(IllegalArgumentException e)
        {
            System.out.println("The port is outside the specified range of valid port values. " +
                "Exception stack trace:");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Closes the running server.
     */
    private void closeServer()
    {
        if (this.server.isClosed())
        {
            return;         /* Early return as server socket already closed */
        }

        try
        {
            if (!this.server.isClosed())
            {
                this.server.close();
            }
        }
        catch(IOException e)
        {
            System.out.println("Server socket creation failed with exception:");
            e.printStackTrace();
        }
    }
}