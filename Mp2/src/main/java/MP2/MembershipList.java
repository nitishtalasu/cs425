import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class MembershipList {
    private static String id;

    private static MembershipList membershipList = null;

    private static volatile List<MembershipNode> nodes;

    private MembershipList() {
        try
        {
            id = InetAddress.getLocalHost().getHostAddress()+ "_" + LocalDateTime.now();
            nodes = new ArrayList<MembershipNode>();
            Message msg = new Message();
            Message.Node node = msg.new Node(id, 1);
            addNode(node);
            changeNodeStatus(node, MembershipNode.Status.LEFT);
        } 
        catch (UnknownHostException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static synchronized void initializeMembershipList() 
    {
        if (membershipList == null) 
        {
            membershipList = new MembershipList();
        }
    }

    public static synchronized void setSelfNode()
    {
        try 
        {
            id = InetAddress.getLocalHost().getHostAddress()+ "_" + LocalDateTime.now();
            Message msg = new Message();
            Message.Node node = msg.new Node(id, 1);
            addNode(node);
            changeNodeStatus(node, MembershipNode.Status.RUNNING);
        } 
        catch (UnknownHostException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static synchronized Message.Node getSelfNode()
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

    public static synchronized void addNode(Message.Node node)
    {
        MembershipNode newNode = 
            new MembershipNode(
                node.id,
                getIpAddress(node.id),
                (long)node.count,
                LocalDateTime.now(),
                MembershipNode.Status.RUNNING);

        for (MembershipNode var : nodes) 
        {
            if (var.id.compareToIgnoreCase(node.id) == 0)
            {
                var.count = node.count;
                var.lastHeartbeatReceived = LocalDateTime.now();
                var.nodeStatus = MembershipNode.Status.RUNNING;
                Collections.sort(nodes);
                return;
            }
        }

        nodes.add(newNode);
        Collections.sort(nodes);

        // System.out.println("[Add nodes]");
        // System.out.println(nodes);
    }

    public static synchronized String getIpAddress(String id) 
    {
        String ipAddress = id.split("_")[0];
        return ipAddress;
    }

    public static synchronized void deleteNode(MembershipNode node)
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
        printMembershipList();
    }
    public static synchronized MembershipNode.Status getNodeStatus(Message.Node node) {
        
        MembershipNode.Status status = null;
        for (MembershipNode var: nodes) 
        {

            if(var.id.compareToIgnoreCase(node.id) == 0)
            {   
                status = var.nodeStatus;
            }
        }
        return status;
    }
    public static synchronized void changeNodeStatus(Message.Node node, MembershipNode.Status newStatus)
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

    public static synchronized void changeNodeStatus(MembershipNode node, MembershipNode.Status newStatus)
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

    public static synchronized void updateNodeStatus(List<Message.Node> hbNodes) 
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
    
    public static synchronized void printMembershipList()
    {
        GrepLogger logger = GrepLogger.getInstance();
        for (MembershipNode node : nodes) 
        {
            logger.LogInfo(node.toString());
        }
    }

    public static synchronized List<Message.Node> getMsgNodes()
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
        //System.out.println("[getMSGNODES]");
        //System.out.println(msgNodes);
        return msgNodes;
    }
    public static synchronized List<MembershipNode> getNeighbors() {

        List<MembershipNode> neighbors = new ArrayList<MembershipNode>();
        //neighbors.add(getPredecessor());
        MembershipNode pNode = getPredecessor();
        if (pNode != null)
        {
            neighbors.add(pNode);
        }

        List<MembershipNode> successorList = getSuccessors();
        for(MembershipNode node: successorList)
            neighbors.add(node);
        return neighbors;

    }

    public static synchronized List<MembershipNode> getSuccessors() {
        List<MembershipNode> successorList = new ArrayList<MembershipNode>();
        Message.Node node = MembershipList.getSelfNode();
        // int pos = 0;
        // for(MembershipNode mNode: nodes) {
        //     pos++;
        //     if (mNode.id.equals(node.id))
        //         break;
        // }

        int index = -1;
        for(int i = 0; i < nodes.size(); i++)
        {
            if (node.id.equals(nodes.get(i).id))
            {
                index = i;
                break;
            }
        }
        
        if (index == -1)
        {
            System.err.println("Error in finding position"); 
            return successorList;
        }

        for (int i = 1; successorList.size() < 2; i++ )
        {
            if (i == nodes.size())
            {
                break;
            }
            MembershipNode sNode = nodes.get((i + index) % nodes.size());
            if (sNode.nodeStatus == MembershipNode.Status.RUNNING)
            {
                successorList.add(sNode);
            }
        }
        
        return successorList;
    }

    public static synchronized MembershipNode getPredecessor() {
        MembershipNode predecessorNode = null;
        Message.Node node = MembershipList.getSelfNode();
        int index = -1;
        for(int i = 0; i < nodes.size(); i++)
        {
            if (node.id.equals(nodes.get(i).id))
            {
                index = i;
                break;
            }
        }
        
        if (index == -1)
        {
            System.err.println("Error in finding position"); 
            return predecessorNode;
        }

        for (int i = 1; predecessorNode == null; i++ )
        {
            if (i == nodes.size())
            {
                break;
            }
            MembershipNode pNode = nodes.get((nodes.size() + index - i) % nodes.size());
            if (pNode.nodeStatus == MembershipNode.Status.RUNNING)
            {
                predecessorNode = pNode;
            }
        }
    
        return predecessorNode;
    }
    
    
    public static synchronized void updateCount(Message.Node node) {
        
        for (MembershipNode var : nodes) 
        {
            if (var.id.compareToIgnoreCase(node.id) == 0)
            {
                var.count++;
                break;
            }
        }
    }

    public static synchronized List<MembershipNode> getMembershipNodes() {
        return nodes;
    }
}