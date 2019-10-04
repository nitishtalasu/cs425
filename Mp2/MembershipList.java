import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class MembershipList
{
    private static MembershipList membershipList = null;

    private static volatile List<MembershipNode> nodes;

    private MembershipList()
    {
        nodes = new ArrayList<MembershipNode>();
    }

    public static MembershipList getMembershipList() 
    {
        if (membershipList == null) 
        {
            membershipList = new MembershipList();
        }

        return membershipList;
    }

    public static void addNode(Message.Node node)
    {
        MembershipNode newNode = 
            new MembershipNode(
                node.id,
                getIpAddress(node.id),
                (long)node.count,
                LocalDateTime.now(),
                MembershipNode.Status.RUNNING);

        if(!nodes.contains(newNode))
        {
            nodes.add(newNode);
        }

        Collections.sort(nodes);
    }

    private static String getIpAddress(String id) 
    {
        String ipAddress = id.split("_")[0];
        return ipAddress;
    }

    public static void deleteNode(Message.Node node)
    {
        int nodeIndex = -1;
        for (MembershipNode var : nodes) 
        {
            if (var.id.compareToIgnoreCase(node.id) == 0)
            {
                nodeIndex = nodes.indexOf(var);
                break;
            }
        }

        if(nodeIndex != -1)
        {
            nodes.remove(nodeIndex);
        }
    }

    public static void changeNodeStatus(Message.Node node, MembershipNode.Status newStatus)
    {
        for (MembershipNode var : nodes) 
        {
            if (var.id.compareToIgnoreCase(node.id) == 0)
            {
                var.nodeStatus = newStatus;
                break;
            }
        }
    }

    public static void updateNodeStatus(List<Message.Node> hbNodes) 
    {
        for (Message.Node hbNode : hbNodes) 
        {
            for (MembershipNode membershipNode : nodes) 
            {
                if (hbNode.id.equals(membershipNode.id))
                {
                    if (hbNode.count > membershipNode.count)
                    {
                        membershipNode.count = hbNode.count;
                        membershipNode.lastHeartbeatReceived = LocalDateTime.now();
                    }
                }
            }    
        }
    }
    
    public static void printMembershipList()
    {
        GrepLogger logger = GrepLogger.getInstance();
        for (MembershipNode node : nodes) 
        {
            logger.LogInfo(node.toString());
        }
    }

    public static List<Message.Node> getMsgNodes()
    {
        List<Message.Node> msgNodes = new ArrayList<Message.Node>();
        Message newmsg = new Message();

        for (MembershipNode node : nodes) 
        {
            Message.Node newNode = newmsg.new Node(node.id, node.count);
            msgNodes.add(newNode);
        }

        return msgNodes;
    }

    public static List<MembershipNode> getNeighbors(MembershipNode node) {

        int count = 0;
        List<MembershipNode> neighborList = new ArrayList<MembershipNode>();
        for(MembershipNode mNode : nodes) {
            count ++;
            if ((node.id).equals(mNode.id)) {
                pos = count;
                break;
            
            }
        }
        
        int len = mList.size();
        count = 0;
        for(MembershipNode mNode: nodes) {
            count ++;
            if (count == (pos-1)%len || count == (pos+1)%len || count == (pos+2)%len) {
                neighborList.add(mNode);
            }
        }
        return neighborList;
    
    }
}