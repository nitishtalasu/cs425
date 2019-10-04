/**
 * This class handles the operations requested by the clients.
 * 
 * @author Nitish Talasu(ntalasu2@illinois.edu)
 */

/**
 * Class handles the client requests.
 */
public class MessageHandler extends Thread
{
    private String message;

    /**
     * Logger instance.
     */
    private GrepLogger logger;
  
    /**
     * Constructor for the class MessageHandler
     * @param socket Socket of the client and server connection.
     */
    public MessageHandler(String message)  
    { 
        this.message = message;
        logger = GrepLogger.getInstance();
    }

    /**
     * Overrides the run method of Thread class. This serves the client requested operations.
     * {@inheritDoc}
     */
    @Override
    public void run()  
    { 
        try
        {
            Message msg = Message.getMessageObject(message);
            switch(msg.type)
            {
                case JOIN:
                {
                    newMemberJoined();
                    break;
                }
                case HEARTBEAT:
                {
                    processHeartBeat();
                    break;
                }
                case LEAVE:
                {
                    memberLeaving();
                    break;
                }
                default:
                {
                    logger.LogError("Message type " + msg.type + "is not handled.");
                }
            }
        }
        catch(Exception e)
        {

        }
    }

    private void memberLeaving() 
    {

    }

    private void processHeartBeat() 
    {

    }

    private void newMemberJoined() 
    {
        
    }
}
