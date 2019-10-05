import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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
        byte[] buffer = new byte[1024];
        // CheckIfThisNodeIsIntroducer();
        if (msg.nodes.size() !=  1)
        {
            logger.LogWarning("More nodes are being passed in message. So dropping the message.");
            return;
        }

        
        MembershipList.addNode(msg.nodes.get(0));

        try
        {
            Message ack = new Message(MessageType.HEARTBEAT, MembershipList.getMsgNodes());
                    
            buffer = Message.toJson(ack).getBytes(); 

            String address = MembershipList.getIpAddress(ack.nodes.get(0).id);
            InetAddress neighborAddress = InetAddress.getByName(address);
            DatagramSocket hb = new DatagramSocket(5000, neighborAddress);
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, neighborAddress, 5000); 
            
            hb.connect(neighborAddress, 5000); 
            hb.send(dp); 
            hb.close();
        }
        catch(Exception e)
        {
            logger.LogException("Failed in sending ack. ", e);
        }
        
    }
}
