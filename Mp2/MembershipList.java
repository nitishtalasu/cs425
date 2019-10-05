import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MembershipList {
    private static String id;

    private static MembershipList membershipList = null;

    private static volatile List<MembershipNode> nodes;

    private MembershipList() {
        try
        {
            id = InetAddress.getLocalHost().getHostAddress()+ "_" + LocalDateTime.now();
        } 
        catch (UnknownHostException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        nodes = new ArrayList<MembershipNode>();
    }

    public static MembershipList initializeMembershipList() 
    {
        if (membershipList == null) 
        {
            membershipList = new MembershipList();
        }

        return membershipList;
    }

    public static void setSelfNode()
    {
        try 
        {
            id = InetAddress.getLocalHost().getHostAddress()+ "_" + LocalDateTime.now();
            Message msg = new Message();
            Message.Node node = msg.new Node(id, 1);
            addNode(node);
        } 
        catch (UnknownHostException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static Message.Node getSelfNode()
    {
        for (MembershipNode node : nodes) 
        {
            if (node.id.compareToIgnoreCase(id) == 0)
            {
                Message msg = new Message();
                Message.Node selfNode = msg.new Node(node.id, node.count);
                return selfNode;
            }
        }
        
        return null;
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
            Collections.sort(nodes);
        }
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
            boolean nodePresent = false;
            for (MembershipNode membershipNode : nodes) 
            {
                if (hbNode.id.equals(membershipNode.id))
                {
                    nodePresent = true;
                    if (hbNode.count > membershipNode.count)
                    {
                        membershipNode.count = hbNode.count;
                        membershipNode.lastHeartbeatReceived = LocalDateTime.now();
                        membershipNode.nodeStatus = MembershipNode.Status.RUNNING;
                   }
                }
            }

            if (!nodePresent)
            {
                addNode(hbNode);
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
            if (node.nodeStatus == MembershipNode.Status.RUNNING)
            {
                Message.Node newNode = newmsg.new Node(node.id, node.count);
                msgNodes.add(newNode);
            }
        }

        return msgNodes;
    }
    public static List<MembershipNode> getNeighbors() {

        Message.Node node = getSelfNode();
        int count = 0, pos = 0;
        List<MembershipNode> neighborList = new ArrayList<MembershipNode>();
        
        pos = nodes.indexOf(node);
        int len = nodes.size();
        count = 0;
        for(MembershipNode mNode: nodes) {
            count ++;
            if (count == (pos-1)%len || count == (pos+1)%len || count == (pos+2)%len) {
                if (mNode.nodeStatus == MembershipNode.Status.RUNNING)
                    neighborList.add(mNode);
            }
        }
        return neighborList;
    
    }
    public static void updateCount() {
        
        Message.Node node = getSelfNode();
        node.count++;
    }

    public static List<MembershipNode> getMembershipNodes() {
        return nodes;
    }
}