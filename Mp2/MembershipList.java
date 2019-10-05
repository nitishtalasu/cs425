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

    public static String getIpAddress(String id) 
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

        List<MembershipNode> neighbors = new ArrayList<MembershipNode>();
        neighbors.add(getPredecessor());
        List<MembershipNode> successorList = getSuccessors();
        for(MembershipNode node: successorList)
            neighbors.add(node);
        return neighbors;

    }

    public static List<MembershipNode> getSuccessors() {
        List<MembershipNode> successorList = new ArrayList<MembershipNode>();
        Message.Node node = MembershipList.getSelfNode();
        int pos = 0;
        for(MembershipNode mNode: nodes) {
            pos++;
            if (mNode.id.equals(node.id))
                break;
        }
        int len = nodes.size();
        int cur = pos + 1;
        int neighborCount = 0;
        do {
            if(cur == len-1) {
                cur = 0;
            }
            if(nodes.get(cur).nodeStatus == MembershipNode.Status.RUNNING) {
                successorList.add(nodes.get(cur));
                neighborCount++;
            }
            
            if(neighborCount == 2)
                break;
            cur++;

        } while (cur < len);
        
        return successorList;
    }

    public static MembershipNode getPredecessor() {
        MembershipNode predecessorNode = null;
        Message.Node node = MembershipList.getSelfNode();
        int pos = 0;
        for(MembershipNode mNode: nodes) {
            pos++;
            if (mNode.id.equals(node.id))
                break;
        }
        int len = nodes.size();
        int cur = pos - 1;
        do {
            if(cur == 0) {
                cur = len - 1;
            }

            if(nodes.get(cur).nodeStatus == MembershipNode.Status.RUNNING) {
                predecessorNode = nodes.get(cur);
                break;
            }
            cur--;
        } while (cur > 0);
    
        return predecessorNode;
    }
    
    
    public static void updateCount() {
        
        Message.Node node = getSelfNode();
        node.count++;
    }

    public static List<MembershipNode> getMembershipNodes() {
        return nodes;
    }
}