package MP4;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;

/**
 * Class for maintaing the ReplicaList and operations on replicalist nodes.
 * This keeps volatile object for replicaList so that each thread can access it,
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

    public static synchronized void printLocalReplicas()
    {
        // logger.LogInfo("[ReplicaList] Printing replicaNodes");
        for (ReplicaNode node : nodes) 
        {
            if(node.id.equals(id))
            {
                logger.LogInfo("Printing replicas in IP: " + node.ipAddress);
                for (String file : node.sdfsFileNames) 
                {
                    System.out.println("FileName: " + file);
                }
            }
        }
    }

    public static synchronized List<ReplicaNode> getReplicaMachines() 
    {
        int quorum = 4;
        List<ReplicaNode> replicaMachines = new ArrayList<ReplicaNode>();
        // printReplicaNodes();
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
            System.out.println("Node: "+ node.ipAddress);
            if (!replicaIps.contains(node.ipAddress))
            {
                System.out.println("Different replica: "+ node.ipAddress);
                possibleReplicas.add(node);
            }
        }

        return possibleReplicas;
    }

    public static synchronized List<String> addReplicaFiles(String fileName)
    {
        List<String> replicaIpAddress = new ArrayList<String>();
        ReplicaFile replicaFile;// = new ReplicaFile(fileName, replicaIpAddress);
        boolean fileAlreadyExist = false;
        for (ReplicaFile file : files) 
        {
            if(file.FileName.equals(fileName))
            {
                replicaIpAddress = file.ReplicaIpAddress; 
                files.remove(file);
            }
        }

        List<ReplicaNode> replicaNodes = getReplicaMachines();
        //List<String> replicaIpAddress = new ArrayList<String>();
        int currentReplicas = replicaIpAddress.size();
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
        // printReplicaNodes();
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

    public static void CheckForReReplication() 
    {
		List<ReplicaFile> filesToBeReplicated = new ArrayList<ReplicaFile>();
        for (ReplicaFile file : files) 
        {
            if (file.ReplicaIpAddress.size() < 4)
            {
                filesToBeReplicated.add(file);
                //break;

            }
        }

        reReplicateFiles(filesToBeReplicated);
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
            if (fileNames.contains(replicaFile.FileName))
            {
                replicaFile.ReplicaIpAddress.remove(ipAddress);
                filesToBeReplicated.add(replicaFile);
            }
        } 

        reReplicateFiles(filesToBeReplicated);
    }
    
    private static void reReplicateFiles(List<ReplicaFile> filesToBeReplicated)
    {
        for (ReplicaFile replicaFile : filesToBeReplicated) 
        {
            System.out.println("[ReplicaList] [reReplicateDeletedNodeFiles] Files that have to be replicated: " + replicaFile.FileName);   
        }

        for (ReplicaFile replicaFile : filesToBeReplicated) 
        {
            int countOfCurrentReplicas = replicaFile.ReplicaIpAddress.size();
            System.out.println("[ReplicaList] The current replicas count for file " + replicaFile.FileName + " : " + countOfCurrentReplicas);
            List<ReplicaNode> possibleNewReplicaIpAddress = getPossibleReplicaMachines(replicaFile.ReplicaIpAddress);
            for(int i = countOfCurrentReplicas; i < 4; i++)
            {
                if (replicaFile.ReplicaIpAddress.isEmpty())
                {
                    System.out.println("[ReplicaList] There is no active replicas for this fileName" +
                        replicaFile.FileName);
                        break;
                }

                String currentReplicaIp = replicaFile.ReplicaIpAddress.get(0);
                String newReplicaIp = possibleNewReplicaIpAddress.get(i - countOfCurrentReplicas).ipAddress;
                System.out.println("[ReplicaList] [reReplicateDeletedNodeFiles] Replicating file " + replicaFile.FileName + " from " +
                        currentReplicaIp + " to " + newReplicaIp);

                replicaFile.ReplicaIpAddress.add(newReplicaIp);
                if(replicateFile(currentReplicaIp, newReplicaIp, replicaFile.FileName))
                {
                    System.out.println("[ReplicaList] [reReplicateDeletedNodeFiles] Replicated file " + replicaFile.FileName + " to " +
                        newReplicaIp); 
                }
                else
                {
                    System.out.println("[ReplicaList] [reReplicateDeletedNodeFiles] Failed to replicate file " + replicaFile.FileName + " to " +
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
        // printReplicaNodes();
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
        System.out.println("[ReplicaList] Printing replicaNodes");
        for (ReplicaNode node : nodes) 
        {
            System.out.println("[ReplicaList] Printing replicaNode of IP: " + node.ipAddress);
            for (String file : node.sdfsFileNames) 
            {
                System.out.println("[ReplicaList] FileName: " + file);
            }
        }
    }

    public static void printReplicaFiles(String sdfsFileName)
    {
        System.out.println("[ReplicaList][printReplicaFiles] Printing replicafiles");
        for (ReplicaFile file : files) 
        {
            if(file.FileName.equalsIgnoreCase(sdfsFileName))
            {
                System.out.println("[ReplicaList][printReplicaFiles] Printing replicafile of fileName: " + file.FileName);
        
                for (String ip : file.ReplicaIpAddress) 
                {
                    System.out.println("[ReplicaList][printReplicaFiles] Ip: " + ip);
                }
            }
        }
    }

    public static void printReplicaFiles()
    {
        System.out.println("[ReplicaList][printReplicaFiles] Printing replicafiles");
        for (ReplicaFile file : files) 
        {
                System.out.println("[ReplicaList][printReplicaFiles] Printing replicafile of fileName: " + file.FileName);
        
                for (String ip : file.ReplicaIpAddress) 
                {
                    System.out.println("[ReplicaList][printReplicaFiles] Ip: " + ip);
                }
        }
    }

    public static void deleteFileFromNode(String sdfsFileName) 
    {
        for (ReplicaNode node : nodes) 
        {
            if(node.sdfsFileNames.contains(sdfsFileName))
            {
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

    public static long GetFileTimeElapsed(String sdfsFileName) 
    {
        long timeElapsed = -1;
        for (ReplicaFile replicaFile : files) 
        {
            if (replicaFile.FileName.equals(sdfsFileName))
            {
                LocalDateTime currentTime = LocalDateTime.now();
                timeElapsed = ChronoUnit.MILLIS.between(replicaFile.LastUpdatedTime, currentTime);
            }
        }
		return timeElapsed;
    }
    
    public static synchronized List<String> GetFileNames(String fileName, String fileExtension)
    {
        List<String> fileNames = new ArrayList<String>();
        for (ReplicaFile replicaFile : files) 
        {
            logger.LogInfo("[ReplicaList][getfileNames] Filename and extension: " +  
                replicaFile.FileName + " and " + FilenameUtils.getExtension(replicaFile.FileName));
            if (replicaFile.FileName.contains(fileName) && 
                FilenameUtils.getExtension(replicaFile.FileName).equals(fileExtension))
            {
                fileNames.add(replicaFile.FileName);
            }
        }

        return fileNames;
    }
}
