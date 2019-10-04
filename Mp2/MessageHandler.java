
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
                    newMemberJoined(msg);
                    break;
                }
                case HEARTBEAT:
                {
                    processHeartBeat(msg);
                    break;
                }
                case LEAVE:
                {
                    memberLeaving(msg);
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

    private void memberLeaving(Message msg) 
    {
        if (msg.nodes.size() !=  1)
        {
            logger.LogWarning("More nodes are being passed in message. So dropping the message.");
            return;
        }

        MembershipList.changeNodeStatus(msg.nodes.get(0), MembershipNode.Status.LEFT);
    }

    private void processHeartBeat(Message msg) 
    {
        if (msg.nodes.size() ==  0)
        {
            logger.LogWarning("Nodes hearbeat is zero. So dropping the message.");
            return;
        }

        MembershipList.updateNodeStatus(msg.nodes);
    }

    private void newMemberJoined(Message msg) 
    {
        if (msg.nodes.size() !=  1)
        {
            logger.LogWarning("More nodes are being passed in message. So dropping the message.");
            return;
        }

        MembershipList.addNode(msg.nodes.get(0));
    }
}
