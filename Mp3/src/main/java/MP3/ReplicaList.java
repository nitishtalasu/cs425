package MP3;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private static volatile CopyOnWriteArrayList<ReplicaNode> nodes;

    private static volatile CopyOnWriteArrayList<ReplicaFile> files;

    private static GrepLogger logger = GrepLogger.getInstance();

    private ReplicaList() 
    {
        try
        {
            id = InetAddress.getLocalHost().getHostAddress();
            nodes = new CopyOnWriteArrayList<ReplicaNode>();
            files = new CopyOnWriteArrayList<ReplicaFile>();
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
        List<ReplicaNode> replicaMachines = new ArrayList<ReplicaNode>();
        printReplicaNodes();
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

    private static List<ReplicaNode> getPossibleReplicaMachines(List<String> replicaIps) 
    {
        List<ReplicaNode> possibleReplicas = new ArrayList<ReplicaNode>();
        for (ReplicaNode node : nodes) 
        {
            if (!replicaIps.contains(node.ipAddress))
            {
                possibleReplicas.add(node);
            }
        }

        return possibleReplicas;
    }

    public static synchronized List<String> addReplicaFiles(String fileName)
    {
        String localId = "";
        try{
            localId = InetAddress.getLocalHost().getHostAddress();
        }
        catch(UnknownHostException e){}
        

        //List<String> replicaIpAddress = new ArrayList<String>();
        ReplicaFile replicaFile;// = new ReplicaFile(fileName, replicaIpAddress);
        boolean fileAlreadyExist = false;
        for (ReplicaFile file : files) 
        {
            if(file.FileName.equals(fileName))
            {
                //replicaIpAddress = file.ReplicaIpAddress; 
                files.remove(file);
            }
        }

        List<ReplicaNode> replicaNodes = getReplicaMachines();
        List<String> replicaIpAddress = new ArrayList<String>();
        int currentReplicas = 0;//replicaIpAddress.size();
        for (ReplicaNode node : replicaNodes) 
        {
            if (currentReplicas >= 4)
            {
                break;
            }
            replicaIpAddress.add(node.ipAddress);
            currentReplicas++;
        }

        replicaFile = new ReplicaFile(fileName, replicaIpAddress);
        files.add(replicaFile);

        return replicaIpAddress;
    }

    public static synchronized void deleteReplicaFile(String fileName)
    {
        List<String> replicaIpAddress = new ArrayList<String>();
        logger.LogInfo("[ReplicaList][deletereplicafile] Before deleting.");
        for (ReplicaFile file : files) 
        {
            logger.LogInfo(file.FileName);
        }
        for (ReplicaFile file : files) 
        {
            if (file.FileName.equals(fileName))
            {
                files.remove(file);
            }
        }

        logger.LogInfo("[ReplicaList][deletereplicafile] After deleting.");
        for (ReplicaFile file : files) 
        {
            logger.LogInfo(file.FileName);
        }
    }

    public static synchronized void replicationCompleted(String fileName)
    {
        logger.LogInfo("[Replicalist][replicationCompleted] Entered replicationCompleted");
        for (ReplicaFile replicaFile : files) 
        {
            logger.LogInfo("[Replicalist][replicationCompleted] fileName: " + replicaFile.FileName);
            
            if (replicaFile.FileName.equals(fileName))
            {
                replicaFile.updateStatus("Replicated");
                for (String ip : replicaFile.ReplicaIpAddress)
                {
                    logger.LogInfo("[Replicalist][replicationCompleted] Ips: " + ip);
                    boolean replicaNodeExists = false;
                    ReplicaNode replicaNode = null;
                    for (ReplicaNode node : nodes) 
                    {
                        if(node.ipAddress.equals(ip))
                        {
                            replicaNodeExists = true;
                            replicaNode = node;
                            nodes.remove(node);
                        }
                    }

                    if(replicaNodeExists)
                    {
                        if (!replicaNode.sdfsFileNames.contains(replicaFile.FileName))
                        {
                            replicaNode.sdfsFileNames.add(replicaFile.FileName);
                        }
                    }
                    else
                    {
                        replicaNode = new ReplicaNode(ip, ip, replicaFile.FileName);
                    }
                    nodes.add(replicaNode);
                }

                break;
            }

        }
    }

    public static synchronized List<String> getReplicaIpAddress(String fileName)
    {
        List<String> replicaIpAddress = new ArrayList<String>();
        logger.LogInfo("[Replicalist][getReplicaIpAddress] fileName: " + fileName);
        printReplicaNodes();
        for (ReplicaFile file : files) 
        {
            if (file.FileName.equals(fileName))
            {
                logger.LogInfo("[Replicalist][getReplicaIpAddress] FileName found");
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
            // for (String file : fileNames) 
            // {
            //     if (file.equals(replicaFile.FileName))
            //     {
            //         replicaFile.ReplicaIpAddress.remove(ipAddress);
            //         filesToBeReplicated.add(replicaFile);
            //     }
            // }

            if (fileNames.contains(replicaFile.FileName))
            {
                replicaFile.ReplicaIpAddress.remove(ipAddress);
                filesToBeReplicated.add(replicaFile);
            }
        }

        for (ReplicaFile replicaFile : filesToBeReplicated) 
        {
            logger.LogInfo("[ReplicaList] [reReplicateDeletedNodeFiles] Files that have to be replicated: " + replicaFile.FileName);   
        }

        for (ReplicaFile replicaFile : filesToBeReplicated) 
        {
            int countOfCurrentReplicas = replicaFile.ReplicaIpAddress.size();
            List<ReplicaNode> possibleNewReplicaIpAddress = getPossibleReplicaMachines(replicaFile.ReplicaIpAddress);
            for(int i = countOfCurrentReplicas; i < 4; i++)
            {
                if (replicaFile.ReplicaIpAddress.isEmpty())
                {
                    logger.LogError("[ReplicaList] There is no active replicas for this fileName" +
                        replicaFile.FileName);
                        break;
                }

                String currentReplicaIp = replicaFile.ReplicaIpAddress.get(0);
                String newReplicaIp = possibleNewReplicaIpAddress.get(i - countOfCurrentReplicas).ipAddress;
                logger.LogInfo("[ReplicaList] [reReplicateDeletedNodeFiles] Replicating file " + replicaFile.FileName + " from " +
                        currentReplicaIp + " to " + newReplicaIp);

                replicaFile.ReplicaIpAddress.add(newReplicaIp);
                if(replicateFile(currentReplicaIp, newReplicaIp, replicaFile.FileName))
                {
                    logger.LogInfo("[ReplicaList] [reReplicateDeletedNodeFiles] Replicated file " + replicaFile.FileName + " to " +
                        newReplicaIp); 
                }
                else
                {
                    logger.LogInfo("[ReplicaList] [reReplicateDeletedNodeFiles] Failed to replicate file " + replicaFile.FileName + " to " +
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
                    if (!file.ReplicaIpAddress.contains(ipAddress))
                    {
                        file.ReplicaIpAddress.add(ipAddress);
                    } 
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
        logger.LogInfo("[ReplicaList] [AddNewFile] Printing nodes");
        printReplicaNodes();
        MembershipNode selfNodeDetails = MembershipList.getSelfNodeDetails();
        for (ReplicaNode node : nodes) 
        {
            if (node.id.equals(selfNodeDetails.ipAddress) && !node.sdfsFileNames.contains(sdfsFileName))
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
        //ReplicaFile filesNode = new ReplicaFile();
        nodes.add(selfNode);
    }
    
    public static void printReplicaNodes()
    {
        logger.LogInfo("[ReplicaList] Printing replicaNodes");
        for (ReplicaNode node : nodes) 
        {
            logger.LogInfo("[ReplicaList] Printing replicaNode of IP: " + node.ipAddress);
            for (String file : node.sdfsFileNames) 
            {
                logger.LogInfo("[ReplicaList] FileName: " + file);
            }
        }
    }

    public static void printReplicaFiles()
    {
        logger.LogInfo("[ReplicaList][printReplicaFiles] Printing replicafiles");
        for (ReplicaFile file : files) 
        {
            logger.LogInfo("[ReplicaList][printReplicaFiles] Printing replicafile of fileName: " + file.FileName);
            for (String ip : file.ReplicaIpAddress) 
            {
                logger.LogInfo("[ReplicaList][printReplicaFiles] Ip: " + ip);
            }
        }
    }

    public static void deleteFileFromNode(String sdfsFileName) 
    {
        for (ReplicaNode node : nodes) 
        {
            if(node.sdfsFileNames.contains(sdfsFileName))
            {
                // for (String file: node.sdfsFileNames)
                // {
                //     if(file.equals(sdfsFileName))
                //     {
                //         node.sdfsFileNames.remove(sdfsFileName);
                //     }
                // }
                node.sdfsFileNames.remove(sdfsFileName);
            }
            
        }
	}

    public static void clearReplicas() 
    {
        files.clear();
        String selfIp = MembershipList.getIpAddress(MembershipList.getSelfNode().id);
        for (ReplicaNode node : nodes) 
        {
            if(!selfIp.equals(node.ipAddress))
            {
                nodes.remove(node);
            }
        }
	}
}