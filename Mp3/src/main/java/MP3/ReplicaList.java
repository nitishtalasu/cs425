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

    private ReplicaList() 
    {
        try
        {
            id = InetAddress.getLocalHost().getHostAddress();
            nodes = new ArrayList<ReplicaNode>();
            files = new ArrayList<ReplicaFile>();
            addSelfNode(id);
        } 
        catch (UnknownHostException e) 
        {
            logger.LogException("[MemnershipList] Failed to create the membership list object. ", e);
        }
    }

    public static synchronized void initializeReplicaList() 
    {
        if (replicaList == null) 
        {
            replicaList = new ReplicaList();
        }
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

        // ReplicaNode newNode = new ReplicaNode(localId, localId, fileName);
        // logger.LogInfo(newNode.toString());
        // logger.LogInfo(nodes.toString());
        // nodes.add(newNode);
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


    public static void reReplicateDeletedNodeFiles(String ipAddress) 
    {
        List<String> fileNames = new ArrayList<String>();
        for (ReplicaNode node : nodes) 
        {
            if (node.ipAddress.equals(ipAddress))
            {
                fileNames = node.sdfsFileNames;
                nodes.remove(node);
                break;
            }
        }

        List<ReplicaFile> filesToBeReplicated = new ArrayList<ReplicaFile>();
        for (ReplicaFile replicaFile : files) 
        {
            for (String file : fileNames) 
            {
                if (file.equals(replicaFile.FileName))
                {
                    replicaFile.ReplicaIpAddress.remove(ipAddress);
                    filesToBeReplicated.add(replicaFile);
                }
            }
        }

        for (ReplicaFile replicaFile : filesToBeReplicated) 
        {
            int countOfCurrentReplicas = getReplicaIpAddress(replicaFile.FileName).size();
            List<ReplicaNode> possibleNewReplicaIpAddress = getReplicaMachines();
            for(int i = countOfCurrentReplicas; i <= 4; i++)
            {
                if (replicaFile.ReplicaIpAddress.isEmpty())
                {
                    logger.LogError("[ReplicaList] There is no active replicas for this fileName" +
                        replicaFile.FileName);
                }

                if(replicateFile(
                    replicaFile.ReplicaIpAddress.get(0), 
                    possibleNewReplicaIpAddress.get(i - countOfCurrentReplicas).ipAddress, 
                    replicaFile.FileName))
                {
                    logger.LogInfo("[ReplicaList] Replicated file " + replicaFile.FileName + " to " +
                        possibleNewReplicaIpAddress.get(i - countOfCurrentReplicas).ipAddress);
                }
                else
                {
                    logger.LogInfo("[ReplicaList] Failed to replicate file " + replicaFile.FileName + " to " +
                        possibleNewReplicaIpAddress.get(i - countOfCurrentReplicas).ipAddress);
                }
            }
        }
	}

    private static boolean replicateFile(String currentReplica, String newReplicaIpAddress, String file) 
    {
        TcpClientModule client = new TcpClientModule();
        return client.reReplicateFiles(currentReplica, file, newReplicaIpAddress);
    }


    public static void addReplicaNode(String ipAddress, List<String> fileNames) 
    {
        ReplicaNode newNode = new ReplicaNode(ipAddress, ipAddress, fileNames);
        for (ReplicaNode node : nodes) 
        {
            if (node.ipAddress.equals(newNode.ipAddress))
            {
                logger.LogInfo("[ReplicaList] Deleting existing node of having IpAddress " +
                    node.ipAddress);
                nodes.remove(node);
                break;
            }
        }

        logger.LogInfo("[ReplicaList] Adding new node of having IpAddress " +
                    newNode.ipAddress);
        for (String file : fileNames) 
        {
            logger.LogInfo("[ReplicaList] Adding files to the node " + file);
        }
        nodes.add(newNode);

        for (String fileName : fileNames) 
        {
            boolean fileExists = false;
            for (ReplicaFile file : files) 
            {
                if (file.FileName.equals(fileName))
                {
                    file.ReplicaIpAddress.add(ipAddress);
                    fileExists = true;
                }
            }

            if (!fileExists)
            {
                List<String> replicaIpAddress = new ArrayList<String>();
                replicaIpAddress.add(ipAddress);
                ReplicaFile newFile = new ReplicaFile(fileName, replicaIpAddress);
                files.add(newFile);
            }
        }
	}

    public static void addNewFile(String sdfsFileName) 
    {
        for (ReplicaNode node : nodes) 
        {
            if (node.id.equals(node.ipAddress))
            {
                logger.LogInfo("[ReplicaList] Adding file to the list " + sdfsFileName);
                node.sdfsFileNames.add(sdfsFileName);
            }          
        }
	}

    public static void addSelfNode(String ipAddress) 
    {
        logger.LogInfo("[ReplicaList] Initilizing self replicaNode");

        ReplicaNode selfNode = new ReplicaNode(ipAddress);
        nodes.add(selfNode);
	}
}