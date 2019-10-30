import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Class handles the client message requests.
 */
public class MessageHandler extends Thread
{
    private String message;

    private GrepLogger logger;
  
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
                    logger.LogInfo("[MessageHandler] Printing membership list");
                    MembershipList.printMembershipList();
                    break;
                }
                case HEARTBEAT:
                {
                    processHeartBeat(msg);
                    break;
                }
                case LEAVE:
                {
                    logger.LogInfo("[MessageHandler] Received Left msg.");
                    memberLeaving(msg);
                    break;
                }
                default:
                {
                    logger.LogError("[MessageHandler] Message type " + msg.type + "is not handled.");
                }
            }
        }
        catch(Exception e)
        {

        }
    }

    /**
     * Handling the LEAVE message type.
     * Marks the node status as LEFT.
     * @param msg Message object.
     */
    private void memberLeaving(Message msg) 
    {
        if (msg.nodes.size() !=  1)
        {
            logger.LogWarning("[MessageHandler] More nodes are being passed in message. So dropping the message.");
            return;
        }

        MembershipList.changeNodeStatus(msg.nodes.get(0), MembershipNode.Status.LEFT);
    }

    /**
     * Handling the HEARTBEAT message type.
     * Merges the received membershiplist with its own list.
     * @param msg Message object.
     */
    private void processHeartBeat(Message msg) 
    {
        if (msg.nodes.size() ==  0)
        {
            logger.LogWarning("[MessageHandler] Nodes hearbeat is zero. So dropping the message.");
            return;
        }

        MembershipList.updateNodeStatus(msg.nodes);
    }

    /**
     * Handling the JOIN message type.
     * Adds the node to the membershiplist and sends the entire membershiplist to the sender.
     * @param msg Message object.
     */
    private void newMemberJoined(Message msg) 
    {
        byte[] buffer = new byte[1024];
        if (msg.nodes.size() !=  1)
        {
            logger.LogWarning("[MessageHandler] More nodes are being passed in message. So dropping the message.");
            return;
        }

        Message.Node selfNode = MembershipList.getSelfNode();
        if(selfNode.id.equals(msg.nodes.get(0).id))
        {
            logger.LogInfo("[MessageHandler] Same host getting join msg. So dropping it");
            return;
        }
        MembershipList.addNode(msg.nodes.get(0));

        try
        {
            Message ack = new Message(MessageType.HEARTBEAT, MembershipList.getMsgNodes());
                    
            buffer = Message.toJson(ack).getBytes(); 
            String address = MembershipList.getIpAddress(ack.nodes.get(0).id);
            InetAddress neighborAddress = InetAddress.getByName(address);
            DatagramSocket hb = new DatagramSocket();
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, neighborAddress, 5000); 
            
            hb.send(dp); 
            hb.close();
        }
        catch(Exception e)
        {
            logger.LogException("[MessageHandler] Failed in sending ack for join message. ", e);
        }
        
    }
}
