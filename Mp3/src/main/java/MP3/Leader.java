import java.util.List;

class Leader
{
    public static void ReReplicateDeletedNodeFiles() 
    {

    }

    public static void ProcessReply(String reply) 
    {
    }
    
    public static List<String> GetReplicas(String file)
    {
        return ReplicaList.getReplicaIpAddress(file);
    }

    public static List<String> addReplicaFile(String file)
    {
        return ReplicaList.addReplicaFiles(file);
    }

    public static void DeleteReplicas(String file)
    {
        ReplicaList.deleteReplicaFiles(file);
    }
}

