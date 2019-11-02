import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class for maintaing the membershiplist and operations on membershiplist nodes.
 * This keeps volatile object for membershipList so that each thread and can access it,
 * and methods are synchronized to handle concurrent accessing to the methods.
 */
public class ReplicaList 
{
    private static String id;

    private static ReplicaList replicaList = null;

    private static volatile List<ReplicaNode> nodes;

    private static volatile List<ReplicaFile> files;

    private static GrepLogger logger = GrepLogger.getInstance();

    public ReplicaList() {
        // try
        // {
            nodes = new ArrayList<ReplicaNode>();
            files = new ArrayList<ReplicaFile>();

        // } 
        // catch (UnknownHostException e) 
        // {
        //     logger.LogException("[MemnershipList] Failed to create the membership list object. ", e);
        // }
    }

    public static synchronized List<ReplicaNode> getReplicas(String sdfsFileName) 
    {
        List<ReplicaNode> replica = new ArrayList<ReplicaNode>();
        for (ReplicaNode var: nodes)
        {
            for (String fileName: var.sdfsFileNames)
            {
                if(fileName.equalsIgnoreCase(sdfsFileName))
                {
                    replica.add(var);
                    break;
                }
            }
        }
        if (replica == null)
            return null;
        return replica;
    }

    public static synchronized void printReplicas(String sdfsFileName) 
    {
        for (ReplicaNode var: nodes)
        {
            for (String fileName: var.sdfsFileNames)
            {
                if(fileName.equalsIgnoreCase(sdfsFileName))
                {
                    logger.LogInfo(var.toString());
                    break;
                }
            }
        }
    }
    
    public static synchronized List<String> getLocalReplicas() 
    {
        String ip = "";
        try
        {
            ip = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) 
        {
            logger.LogException("[MemnershipList] Failed to create the membership list object. ", e);
        }
        for (ReplicaNode var: nodes)
        {
            if(var.ipAddress.equals(ip))
            {
                return var.sdfsFileNames;
            }
        }
        return null;
    }

    public static synchronized List<ReplicaNode> getReplicaMachines() 
    {
        int quorum = 4;
        List<ReplicaNode> replicaMachines = null;

        Collections.sort(nodes, new SortByFiles());
        for (ReplicaNode var: nodes)
        {
            if (quorum == 0)
                break;
            replicaMachines.add(var);
            quorum--;
        }
        return replicaMachines;
    }

    public static synchronized List<String> addReplicaFiles(String fileName)
    {
        String localId = "";
        try{
            localId = InetAddress.getLocalHost().getHostAddress();
        }
        catch(UnknownHostException e){}
        ReplicaNode newNode = new ReplicaNode(localId, localId, fileName);
        logger.LogInfo(newNode.toString());
        logger.LogInfo(nodes.toString());
        nodes.add(newNode);
        List<ReplicaNode> replicaNodes = getReplicaMachines();

        List<String> replicaIpAddress = new ArrayList<String>();
        for (ReplicaNode node : replicaNodes) 
        {
            replicaIpAddress.add(node.ipAddress);
        }
        ReplicaFile replicaFile = new ReplicaFile(fileName, replicaIpAddress);

        return replicaIpAddress;
    }

    public static synchronized void deleteReplicaFiles(String fileName)
    {
        List<ReplicaNode> replicaNodes = getReplicaMachines();
        List<String> replicaIpAddress = new ArrayList<String>();
        for (ReplicaNode node : replicaNodes) 
        {
            replicaIpAddress.remove(node.ipAddress);
        }
        ReplicaFile replicaFile = new ReplicaFile(fileName, replicaIpAddress);
    }

    public static synchronized void replicationCompleted(String fileName)
    {
        for (ReplicaFile replicaFile : files) 
        {
            if (replicaFile.FileName.equals(fileName))
            {
                replicaFile.updateStatus("Replicated");
            }
        }
    }

    public static synchronized List<String> getReplicaIpAddress(String fileName)
    {
        List<String> replicaIpAddress = new ArrayList<String>();
        for (ReplicaFile file : files) 
        {
            if (file.equals(fileName))
            {
                replicaIpAddress = file.ReplicaIpAddress;
            }
        }

        return replicaIpAddress;
    }
}