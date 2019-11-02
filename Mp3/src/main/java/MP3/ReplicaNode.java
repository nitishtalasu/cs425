import java.time.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
/**
 * Class for nodes in the membership list.
 */
public class ReplicaNode implements Comparable<ReplicaNode>
{
    public String id;
    
    public String ipAddress;

    public List<String> sdfsFileNames = new ArrayList<String>();

    public ReplicaNode(String id, String ipAddress, String sdfsFileName)
    {
        this.id = id;
        this.ipAddress = ipAddress;
        this.sdfsFileNames.add(sdfsFileName);
    }

    public ReplicaNode(String id, String ipAddress, List<String> sdfsFileNames)
    {
        this.id = id;
        this.ipAddress = ipAddress;
        this.sdfsFileNames = sdfsFileNames;
    }

    @Override
    public int compareTo(ReplicaNode node)
    {
        return this.id.compareTo(node.id);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" Id : " + this.id);
        sb.append(" IpAddress : " + this.ipAddress);

        return sb.toString();
    }
}

class SortByFiles implements Comparator<ReplicaNode> 
    { 
        int countA, countB;
        public int compare(ReplicaNode a, ReplicaNode b) 
        { 
            countA = a.sdfsFileNames.size();
            countB = b.sdfsFileNames.size();
            return countA - countB; 
        } 
    } 