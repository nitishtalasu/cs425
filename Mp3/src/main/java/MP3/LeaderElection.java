package MP3;
/*
* The component performs leader election.
* Whenever a leader election is triggered, this component sends messages such as coordination message,
* election, elected messages, to elect a new leader.
**/
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;

class LeaderElection extends Thread
{
    private static GrepLogger logger = GrepLogger.getInstance();

    public LeaderElection()
    {
        MembershipList.setLeaderIpAddress("");
    }

    @Override
    public void run()
    {
        do
        {
            String newLeaderIpAddress = MembershipList.selectNewLeader();
            List<String> higherIpAddress = MembershipList.getHigherNodesIpAdress();
            if (higherIpAddress.size() == 0)
            {
                logger.LogInfo("[LeaderElection] No higher node present in membership list. " +
                    "So electing myself as leader.");
                LeaderElected();
            }
            else
            {
                logger.LogInfo("[LeaderElection] Starting leader election.");
                int ackReceived = 0;
                for (String nodeIpAddress : higherIpAddress) 
                {
                    ackReceived += SendLeaderElectionMessage(nodeIpAddress);
                    if (ackReceived == 1)
                    {
                        break;
                    }
                }

                if(ackReceived > 0)
                {
                    try
                    {
                        Thread.sleep(2000);
                    }
                    catch(Exception e)
                    {

                    }
                }
                else
                {
                    logger.LogInfo("[LeaderElection] No ACKs received from higher nodes. " +
                        "So electing myself as leader.");
                    LeaderElected();
                }
            }
        }
        while(MembershipList.getLeaderIpAddress().isEmpty());

        logger.LogInfo("[LeaderElection] leader election completed with leader: " + MembershipList.getLeaderIpAddress());
    }

    private void LeaderElected()
    {
        SendLeaderElectedMessage();
        //ReplicaList.clearReplicas();
        MembershipList.setLeaderIpAddress(MembershipList.getSelfNodeDetails().ipAddress);
        SendCoordinationMessage();
        Leader.CheckForReReplication();
    }

    private int SendLeaderElectionMessage(String nodeIpAddress) 
    {
        logger.LogInfo("[LeaderElection] Sending election message to node "+ nodeIpAddress + ".");
        return SendElectionMessage(MessageType.ELECTION, nodeIpAddress);
    }

    private void SendLeaderElectedMessage()
    {
        List<MembershipNode> nodes = MembershipList.getMembershipNodes();
        for (MembershipNode membershipNode : nodes) 
        {
            logger.LogInfo("[LeaderElection] Sending victory message to node "+ membershipNode.ipAddress + ".");
            SendElectionMessage(MessageType.VICTORY, membershipNode.ipAddress);
        }
    }

    private void SendCoordinationMessage() 
    {
        List<MembershipNode> nodes = MembershipList.getMembershipNodes();
        for (MembershipNode membershipNode : nodes) 
        {
            logger.LogInfo("[LeaderElection] Sending Coordination message to node "+ membershipNode.ipAddress + ".");
            SendCoordinationMessage(MessageType.COORDINATION, membershipNode.ipAddress);
        }
    }

    private void SendCoordinationMessage(MessageType coordination, String ipAddress)
    {
        String reply = SendTcpMessage(coordination, ipAddress);
        logger.LogInfo("[LeaderElection] Received reply to Coordination message to node "+
            ipAddress + " as " + reply +".");
        Leader.ProcessReply(reply, ipAddress);
    }

    private int SendElectionMessage(MessageType electionType, String nodeIpAddress) 
    {
        String ack = SendTcpMessage(electionType, nodeIpAddress);
        if(ack.equalsIgnoreCase("OK"))
        {
            logger.LogInfo("[LeaderElection] Got OK message from "+ nodeIpAddress + ".");
            return 1;
        }
        logger.LogInfo("[LeaderElection] Got NAK message from "+ nodeIpAddress + ".");
        return 0;
    }

    private String SendTcpMessage(MessageType msgType, String ipAddress)
    {
        try
        {
            Socket socket = new Socket(ipAddress, Ports.TCPPort.getValue());
            socket.setSoTimeout(100000);
            // logger.LogInfo("[LeaderElection] Connected to "+ ipAddress + ".");
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());	
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());	
            String reply;
            outputStream.writeUTF(msgType.toString());
            reply = inputStream.readUTF();
            return reply;
        }
        catch(Exception e)
        {
            // logger.LogException("[LeaderElection] Connection failed to "+ ipAddress + ".", e);
        }

        return "";
    }
} 