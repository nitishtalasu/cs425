package MP4;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

/**
 * Class for maintaing the membershiplist and operations on membershiplist nodes.
 * This keeps volatile object for membershipList so that each thread and can access it,
 * and methods are synchronized to handle concurrent accessing to the methods.
 */
public class MembershipList 
{
    private static String id;

    private static volatile String leaderIpAddress = "";

    private static MembershipList membershipList = null;

    private static volatile CopyOnWriteArrayList<MembershipNode> nodes;

    private static GrepLogger logger = GrepLogger.getInstance();

    private static volatile CopyOnWriteArraySet<String> workersIpAddress;

    private MembershipList() 
    {
        try
        {
           
            id = InetAddress.getLocalHost().getHostAddress()+ "_" + LocalDateTime.now();
            nodes = new CopyOnWriteArrayList<MembershipNode>();
            workersIpAddress = new CopyOnWriteArraySet<String>();
            Message msg = new Message();
            Message.Node node = msg.new Node(id, 1);
            addNode(node);
            changeNodeStatus(node, MembershipNode.Status.LEFT);
            if(getIpAddress(id).equals(Introducer.IPADDRESS.getValue()))
                getLeaderIpAddress();
        } 
        catch (Exception e) 
        {
            logger.LogException("[MemnershipList] Failed to create the membership list object. ", e);
        }
    }

    /**
     * To initialize the membership list.
     */
    public static synchronized void initializeMembershipList() 
    {
        if (membershipList == null) 
        {
            membershipList = new MembershipList();
        }
    }

    /**
     * To set the self node in the membership list.
     */
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
            
            logger.LogException("[MemnershipList] Failed to set the self node in membershiplist. ", e);
        }
    }

    /**
     * Gets the self node details.
     * @return Self node deatils.
     */
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

    /**
     * Gets the self node details.
     * @return Self node deatils.
     */
    public static synchronized MembershipNode getSelfNodeDetails()
    {
        for (MembershipNode node : nodes) 
        {
            if (node.id.compareToIgnoreCase(id) == 0)
            {
                return node;
            }
        }
        
        return null;
    }

    /**
     * Adds the node to the membershiplist.
     * @param node Node details to be added.
     */
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
        workersIpAddress.add(getIpAddress(node.id));
        Collections.sort(nodes);
    }

    /**
     * Gets the IpAddress from the node Id.
     * @param id Node Id.
     * @return IpAddress of the given node Id.
     */
    public static synchronized String getIpAddress(String id) 
    {
        String ipAddress = id.split("_")[0];
        return ipAddress;
    }

    /**
     * Deleted the given node in membershiplist.
     * @param node Node to be deleted.
     */
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
            workersIpAddress.remove(getIpAddress(node.id));
        }
        
        logger.LogInfo("[MembershipList] Membershiplist after node got deleted:");
        printMembershipList();
    }

    /**
     * Gets the node status in the membershiplist for given node.
     */
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

    /**
     * Change the node status for the given node.
     * @param node Node for which the status has to be changed.
     * @param newStatus New status that has to be updated.
     */
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

    /**
     * Change the node status for the given node.
     * @param node Node for which the status has to be changed.
     * @param newStatus New status that has to be updated.
     */
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

    /**
     * Updates the heartbeat counts in the membershiplist according to the heartbeat received from the neighbors.
     * If count in own membershiplist is less than the incoming one, then updates it.
     * It also adds new nodes if present.
     * @param hbNodes Nodes that are sent in heartbeat.
     */
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
                logger.LogInfo("[Membershiplist] New node has been added to membershiplist. New membershiplist:");
                printMembershipList();
            }    
        }
    }
    
    /**
     * Prints the membership list.
     */
    public static synchronized void printMembershipList()
    {
        GrepLogger logger = GrepLogger.getInstance();
        for (MembershipNode node : nodes) 
        {
            logger.LogInfo(node.toString());
        }
    }

    /**
     * Gets the nodes to be sent in the heartbeat messages.
     * @return Nodes to be sent.
     */
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
        
        return msgNodes;
    }

    /**
     * Gets the neighbors of the current node.
     * @return Neighbors of the node.
     */
    public static synchronized List<MembershipNode> getNeighbors() {

        List<MembershipNode> neighbors = new ArrayList<MembershipNode>();
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

    /**
     * Gets the successors of the current node.
     * @return Successors of the node.
     */
    public static synchronized List<MembershipNode> getSuccessors() {
        List<MembershipNode> successorList = new ArrayList<MembershipNode>();
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
            logger.LogError("[Membershiplist] Error in finding position.");
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

    /**
     * Gets the predecessor of the current node.
     * @return Predecessors of the node.
     */
    public static synchronized MembershipNode getPredecessor() 
    {
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
            logger.LogError("[Membershiplist] Error in finding position."); 
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
    
    /**
     * Updates the heartbeat count of the node.
     */
    public static synchronized void updateCount(Message.Node node) 
    {
        
        for (MembershipNode var : nodes) 
        {
            if (var.id.compareToIgnoreCase(node.id) == 0)
            {
                var.count++;
                break;
            }
        }
    }

    /**
     * Gets all the membership nodes.
     * @return Nodes in the membership list.
     */
    public static synchronized List<MembershipNode> getMembershipNodes() 
    {
        return nodes;
    }

    public static synchronized String getLeaderIpAddress()
    {
        return leaderIpAddress;
    }

    public static synchronized void setLeaderIpAddress(String ipAddress)
    {
        leaderIpAddress = ipAddress;
    }

    public static synchronized String selectNewLeader()
    {
        return nodes.get(0).ipAddress;
    }

    public static synchronized List<String> getHigherNodesIpAdress()
    {
        String ownIpAddress = getIpAddress(id);
        List<String> higherNodesIpAddress = new ArrayList<String>();
        for(MembershipNode node : nodes)
        {
            if(node.ipAddress.equalsIgnoreCase(ownIpAddress))
            {
                break;
            }
            higherNodesIpAddress.add(node.ipAddress);
        }

        return higherNodesIpAddress;
    }

    public static boolean IsAddressHigher(String clientIpAddress) 
    {
        boolean isHigher = false;
        String ownIpAdderss = getIpAddress(id);
        for (MembershipNode membershipNode : nodes) 
        {
            if(membershipNode.ipAddress.equals(clientIpAddress))
            {
                isHigher = true;
                break;
            }
            
            if(membershipNode.ipAddress.equals(ownIpAdderss))
            {
                isHigher = false;
                break;
            }
        }

		return isHigher;
    }
    
    public static synchronized Set<String> getWorkersIpAddress() 
    {
        return workersIpAddress;
    }

    public static synchronized void addWorkersIpAddress(String workerIpAddress)
    {
        workersIpAddress.add(workerIpAddress);
    }

    public static synchronized void removeWorkersIpAddress(String workerIpAddress)
    {
        workersIpAddress.remove(workerIpAddress);
    }
}