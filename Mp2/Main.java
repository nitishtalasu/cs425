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
      /**
       * Logger instance.
       */
      public static GrepLogger logger =
          GrepLogger.initialize("FailureDetector", "FailureDetector.log");
  
      public static void main(String[] args)  
      { 
          try
          {
              int serverPort = getPortNumber(args);
              logger.LogInfo("[Server] Starting the server on port: " + serverPort);
              ServerModule.getInstance(serverPort).run();
          }
          catch(Exception e)
          {
              logger.LogError(e.getMessage());
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
                  logger.LogError("[Server] First argument" + args[0] + " must be an integer. " +
                      "Exiting the application");
                  System.exit(-1);
              }
          }
  
          logger.LogInfo("[Server] As port was not passed. Setting port to default value 5000.");
          return 5000;
      }
  } 
  