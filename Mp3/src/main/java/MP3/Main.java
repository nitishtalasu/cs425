package MP3;

/**
 * Entry point for the application.
 * 
 * @author Nitish Talasu (ntalasu2@illinois.edu)
 */

 /**
  * Entry point for the application
  */
  public class Main
  { 
      public static GrepLogger logger =
          GrepLogger.initialize("FailureDetector", "FailureDetector.log");
  
      public static void main(String[] args)  
      { 
          try
          {
            int serverPort = Ports.UDPPort.getValue();
            logger.LogInfo("[Main] Starting the server on port: " + serverPort);
            Thread server = ServerModule.getInstance(serverPort);
            server.start();
            MembershipList.initializeMembershipList();
            ReplicaList.initializeReplicaList();

            logger.LogInfo("[Main] Starting the heartbeat handler");
            Thread hbThread = new HeartBeatThread();
            hbThread.start();

            logger.LogInfo("[Main] Starting the failure detector");
            Thread failureThread = new FailureDetector();
            failureThread.start();
            

            logger.LogInfo("[Main] Starting the TCP Server");
            Thread tcpServer = TcpServerModule.getInstance(Ports.TCPPort.getValue());
            tcpServer.start();

            logger.LogInfo("[Main] Starting the client");
            Thread client = ClientModule.getInstance(serverPort);
            client.start();



            server.join();
            hbThread.join();
            failureThread.join();
            client.join();
            logger.LogInfo("[Main] closing progam");

          }
          catch(Exception e)
          {
              logger.LogError(e.getMessage());
          }
          finally
          {
              logger.cleanupLogger();
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
                  logger.LogError("[Main] First argument" + args[0] + " must be an integer. " +
                      "Exiting the application");
                  System.exit(-1);
              }
          }
  
          logger.LogInfo("[Main] As port was not passed. Setting port to default value 5000.");
          return 5500;
      }
  } 
  