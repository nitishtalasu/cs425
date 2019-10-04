import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public static void addNode(MembershipNode node)
    {
        if(!nodes.contains(node))
        {
            nodes.add(node);
        }

        Collections.sort(nodes);
    }

    public static void deleteNode(String nodeId)
    {
        int nodeIndex = -1;
        for (MembershipNode node : nodes) 
        {
            if (node.id.compareToIgnoreCase(nodeId) == 0)
            {
                nodeIndex = nodes.indexOf(node);
                break;
            }
        }

        if(nodeIndex != -1)
        {
            nodes.remove(nodeIndex);
        }
    }

    public static void changeNodeStatus(String nodeId, MembershipNode.Status newStatus)
    {
        for (MembershipNode node : nodes) 
        {
            if (node.id.compareToIgnoreCase(nodeId) == 0)
            {
                node.nodeStatus = newStatus;
                break;
            }
        }
    }
}